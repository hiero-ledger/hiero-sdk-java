// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.CryptoServiceGrpc;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.ResponseHeader;
import org.hiero.sdk.proto.TransactionGetRecordQuery;

/**
 * Get the record for a transaction.
 * <p>
 * If the transaction requested a record, then the record lasts for one hour, and a state proof is available for it.
 * If the transaction created an account, file, or smart contract instance, then the record will contain the ID for
 * what it created. If the transaction called a smart contract function, then the record contains the result of
 * that call. If the transaction was a cryptocurrency transfer, then the record includes the TransferList
 * which gives the details of that transfer. If the transaction didn't return anything that should be
 * in the record, then the results field will be set to nothing.
 */
public final class TransactionRecordQuery extends Query<TransactionRecord, TransactionRecordQuery> {
    @Nullable
    private TransactionId transactionId = null;

    private boolean includeChildren = false;
    private boolean includeDuplicates = false;

    /**
     * Constructor.
     */
    public TransactionRecordQuery() {}

    /**
     * Extract the transaction id.
     *
     * @return                          the transaction id
     */
    @Nullable
    @Override
    public TransactionId getTransactionIdInternal() {
        return transactionId;
    }

    /**
     * Set the ID of the transaction for which the record is requested.
     *
     * @param transactionId The TransactionId to be set
     * @return {@code this}
     */
    public TransactionRecordQuery setTransactionId(TransactionId transactionId) {
        Objects.requireNonNull(transactionId);
        this.transactionId = transactionId;
        return this;
    }

    /**
     * Should duplicates be included?
     *
     * @return                          should duplicates be included
     */
    public boolean getIncludeDuplicates() {
        return includeDuplicates;
    }

    /**
     * Whether records of processing duplicate transactions should be returned along with the record
     * of processing the first consensus transaction with the given id whose status was neither
     * INVALID_NODE_ACCOUNT nor INVALID_PAYER_SIGNATURE or, if no such
     * record exists, the record of processing the first transaction to reach consensus with the
     * given transaction id.
     *
     * @param value The value that includeDuplicates should be set to; true to include duplicates, false to exclude
     * @return {@code this}
     */
    public TransactionRecordQuery setIncludeDuplicates(boolean value) {
        includeDuplicates = value;
        return this;
    }

    /**
     * Extract the children be included.
     *
     * @return                          should children be included
     */
    public boolean getIncludeChildren() {
        return includeChildren;
    }

    /**
     * Whether the response should include the records of any child transactions spawned by the
     * top-level transaction with the given transactionID.
     *
     * @param value The value that includeChildren should be set to; true to include children, false to exclude
     * @return {@code this}
     */
    public TransactionRecordQuery setIncludeChildren(boolean value) {
        includeChildren = value;
        return this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (transactionId != null) {
            Objects.requireNonNull(transactionId.accountId).validateChecksum(client);
        }
    }

    @Override
    void onMakeRequest(org.hiero.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        var builder = TransactionGetRecordQuery.newBuilder()
                .setIncludeChildRecords(includeChildren)
                .setIncludeDuplicates(includeDuplicates);
        if (transactionId != null) {
            builder.setTransactionID(transactionId.toProtobuf());
        }

        queryBuilder.setTransactionGetRecord(builder.setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getTransactionGetRecord().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(org.hiero.sdk.proto.Query request) {
        return request.getTransactionGetRecord().getHeader();
    }

    @Override
    TransactionRecord mapResponse(Response response, AccountId nodeId, org.hiero.sdk.proto.Query request) {
        var recordResponse = response.getTransactionGetRecord();
        List<TransactionRecord> children = mapRecordList(recordResponse.getChildTransactionRecordsList());
        List<TransactionRecord> duplicates = mapRecordList(recordResponse.getDuplicateTransactionRecordsList());
        return TransactionRecord.fromProtobuf(
                recordResponse.getTransactionRecord(), children, duplicates, transactionId);
    }

    private List<TransactionRecord> mapRecordList(List<org.hiero.sdk.proto.TransactionRecord> protoRecordList) {
        List<TransactionRecord> outList = new ArrayList<>(protoRecordList.size());
        for (var protoRecord : protoRecordList) {
            outList.add(TransactionRecord.fromProtobuf(protoRecord));
        }
        return outList;
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Query, Response> getMethodDescriptor() {
        return CryptoServiceGrpc.getGetTxRecordByTxIDMethod();
    }

    @Override
    ExecutionState getExecutionState(Status status, Response response) {
        var retry = super.getExecutionState(status, response);
        if (retry != ExecutionState.SUCCESS) {
            return retry;
        }

        switch (status) {
            case BUSY:
            case UNKNOWN:
            case RECEIPT_NOT_FOUND:
            case RECORD_NOT_FOUND:
                return ExecutionState.RETRY;
            case OK:
                // When fetching payment an `OK` in the query header means the cost is in the response
                if (paymentTransactions == null || paymentTransactions.isEmpty()) {
                    return ExecutionState.SUCCESS;
                } else {
                    break;
                }
            default:
                return ExecutionState.REQUEST_ERROR;
        }

        var receiptStatus = Status.valueOf(response.getTransactionGetRecord()
                .getTransactionRecord()
                .getReceipt()
                .getStatus());

        switch (receiptStatus) {
            case BUSY:
            case UNKNOWN:
            case OK:
            case RECEIPT_NOT_FOUND:
            case RECORD_NOT_FOUND:
            case PLATFORM_NOT_ACTIVE:
                return ExecutionState.RETRY;

            default:
                return ExecutionState.SUCCESS;
        }
    }
}
