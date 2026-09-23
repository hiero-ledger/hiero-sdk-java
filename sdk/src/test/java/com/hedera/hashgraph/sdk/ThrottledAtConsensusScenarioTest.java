// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.Query;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.Transaction;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionGetReceiptResponse;
import com.hedera.hashgraph.sdk.proto.TransactionGetRecordResponse;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThrottledAtConsensusScenarioTest {

    private static final AccountId NODE = AccountId.fromString("1.1.1");
    private static final AccountId RECEIVER = AccountId.fromString("3.3.3");
    private static final long TRANSFER_AMOUNT = 1L;

    /**
     * A minimal fake ledger. The first transfer to arrive finalizes as THROTTLED_AT_CONSENSUS and
     * moves nothing; every later one settles as SUCCESS and credits the receiver. Receipts and
     * records are answered from that state, so a record always carries the ID it was asked about.
     */
    private static class FakeLedgerService extends CryptoServiceGrpc.CryptoServiceImplBase {
        final Map<TransactionId, Status> finalized = new LinkedHashMap<>();
        final List<TransactionId> submissions = new ArrayList<>();
        long receiverBalance = 0;

        @Override
        public void cryptoTransfer(Transaction request, StreamObserver<TransactionResponse> observer) {
            try {
                var signed = SignedTransaction.parseFrom(request.getSignedTransactionBytes());
                var body = TransactionBody.parseFrom(signed.getBodyBytes());
                var transactionId = TransactionId.fromProtobuf(body.getTransactionID());

                submissions.add(transactionId);
                if (submissions.size() == 1) {
                    // the network-wide consensus throttle for CryptoTransfer is exhausted: no funds move
                    finalized.put(transactionId, Status.THROTTLED_AT_CONSENSUS);
                } else {
                    finalized.put(transactionId, Status.SUCCESS);
                    receiverBalance += creditedTo(body, RECEIVER);
                }

                observer.onNext(TransactionResponse.newBuilder()
                        .setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)
                        .build());
                observer.onCompleted();
            } catch (Exception e) {
                observer.onError(e);
            }
        }

        @Override
        public void getTransactionReceipts(Query request, StreamObserver<Response> observer) {
            var transactionId = TransactionId.fromProtobuf(
                    request.getTransactionGetReceipt().getTransactionID());
            observer.onNext(Response.newBuilder()
                    .setTransactionGetReceipt(TransactionGetReceiptResponse.newBuilder()
                            .setReceipt(com.hedera.hashgraph.sdk.proto.TransactionReceipt.newBuilder()
                                    .setStatus(statusOf(transactionId).code)))
                    .build());
            observer.onCompleted();
        }

        @Override
        public void getTxRecordByTxID(Query request, StreamObserver<Response> observer) {
            var transactionIdProto = request.getTransactionGetRecord().getTransactionID();
            var status = statusOf(TransactionId.fromProtobuf(transactionIdProto));
            observer.onNext(Response.newBuilder()
                    .setTransactionGetRecord(TransactionGetRecordResponse.newBuilder()
                            .setHeader(ResponseHeader.newBuilder().setNodeTransactionPrecheckCode(ResponseCodeEnum.OK))
                            .setTransactionRecord(com.hedera.hashgraph.sdk.proto.TransactionRecord.newBuilder()
                                    .setTransactionID(transactionIdProto)
                                    .setReceipt(com.hedera.hashgraph.sdk.proto.TransactionReceipt.newBuilder()
                                            .setStatus(status.code))))
                    .build());
            observer.onCompleted();
        }

        private Status statusOf(TransactionId transactionId) {
            return finalized.getOrDefault(transactionId, Status.RECEIPT_NOT_FOUND);
        }

        private static long creditedTo(TransactionBody body, AccountId account) {
            long credited = 0;
            for (var accountAmount : body.getCryptoTransfer().getTransfers().getAccountAmountsList()) {
                if (AccountId.fromProtobuf(accountAmount.getAccountID()).equals(account)) {
                    credited += accountAmount.getAmount();
                }
            }
            return credited;
        }
    }

    private static TransferTransaction transferToReceiver(Client client) {
        return new TransferTransaction()
                .setNodeAccountIds(List.of(NODE))
                .addHbarTransfer(client.getOperatorAccountId(), Hbar.fromTinybars(-TRANSFER_AMOUNT))
                .addHbarTransfer(RECEIVER, Hbar.fromTinybars(TRANSFER_AMOUNT));
    }

    @Test
    @DisplayName("getReceipt() then getRecord() on a throttled transfer moves funds exactly once")
    void getReceiptThenGetRecordMovesFundsExactlyOnce() throws Exception {
        var service = new FakeLedgerService();
        var server = new TestServer("throttleScenarioFunds", service);

        var transfer = transferToReceiver(server.client);
        var response = transfer.execute(server.client);

        var receipt = response.getReceipt(server.client);
        Assertions.assertEquals(Status.SUCCESS, receipt.status);
        Assertions.assertEquals(TRANSFER_AMOUNT, service.receiverBalance);

        var record = response.getRecord(server.client);
        Assertions.assertNotNull(record);

        Assertions.assertEquals(TRANSFER_AMOUNT, service.receiverBalance);
        Assertions.assertEquals(2, service.submissions.size());

        server.close();
    }

    @Test
    @DisplayName("the response and the record describe the attempt that actually ran")
    void responseAndRecordDescribeTheAttemptThatRan() throws Exception {
        var service = new FakeLedgerService();
        var server = new TestServer("throttleScenarioBinding", service);

        var transfer = transferToReceiver(server.client);
        var response = transfer.execute(server.client);
        var throttledId = response.transactionId;

        response.getReceipt(server.client);

        var replacementId = transfer.getTransactionId();
        Assertions.assertNotEquals(throttledId, replacementId);
        Assertions.assertEquals(Status.THROTTLED_AT_CONSENSUS, service.finalized.get(throttledId));
        Assertions.assertEquals(Status.SUCCESS, service.finalized.get(replacementId));

        Assertions.assertEquals(replacementId, response.transactionId);

        var record = response.getRecord(server.client);

        Assertions.assertEquals(replacementId, transfer.getTransactionId());
        Assertions.assertEquals(replacementId, record.transactionId);
        Assertions.assertNotEquals(throttledId, record.transactionId);
        Assertions.assertEquals(Status.SUCCESS, record.receipt.status);

        server.close();
    }

    @Test
    @DisplayName("repeated lookups after a throttled retry never resubmit again")
    void repeatedLookupsDoNotResubmit() throws Exception {
        var service = new FakeLedgerService();
        var server = new TestServer("throttleScenarioRepeat", service);

        var transfer = transferToReceiver(server.client);
        var response = transfer.execute(server.client);

        response.getReceipt(server.client);
        var replacementId = response.transactionId;

        response.getReceipt(server.client);
        response.getRecord(server.client);
        response.getReceipt(server.client);

        Assertions.assertEquals(2, service.submissions.size());
        Assertions.assertEquals(TRANSFER_AMOUNT, service.receiverBalance);
        Assertions.assertEquals(replacementId, response.transactionId);

        server.close();
    }
}
