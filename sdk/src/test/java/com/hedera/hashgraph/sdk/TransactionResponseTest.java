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
import com.hedera.hashgraph.sdk.proto.TransactionGetRecordResponse;
import com.hedera.hashgraph.sdk.proto.TransactionRecord;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TransactionResponseTest {

    private static Response buildRecordResponse(ResponseCodeEnum precheckStatus, ResponseCodeEnum receiptStatus) {
        return Response.newBuilder()
                .setTransactionGetRecord(TransactionGetRecordResponse.newBuilder()
                        .setHeader(ResponseHeader.newBuilder()
                                .setNodeTransactionPrecheckCode(precheckStatus)
                                .build())
                        .setTransactionRecord(TransactionRecord.newBuilder()
                                .setReceipt(com.hedera.hashgraph.sdk.proto.TransactionReceipt.newBuilder()
                                        .setStatus(receiptStatus)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private static Response buildSuccessRecordResponse() {
        return buildRecordResponse(ResponseCodeEnum.OK, ResponseCodeEnum.SUCCESS);
    }

    private static TransactionId transactionIdOf(Transaction request) throws Exception {
        var signedTransaction = SignedTransaction.parseFrom(request.getSignedTransactionBytes());
        var body = TransactionBody.parseFrom(signedTransaction.getBodyBytes());
        return TransactionId.fromProtobuf(body.getTransactionID());
    }

    private static TransactionId recordQueryTransactionIdOf(Query request) {
        return TransactionId.fromProtobuf(request.getTransactionGetRecord().getTransactionID());
    }

    @Test
    void getReceiptPinnedToSubmittingNodeByDefault() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("getReceiptPinnedDefault", service);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());

        var txResponse = new AccountCreateTransaction().execute(server.client);
        var receipt = txResponse.getReceipt(server.client);

        var receiptQuery = txResponse.getReceiptQuery(server.client);
        Assertions.assertEquals(1, receiptQuery.getNodeAccountIds().size());
        Assertions.assertEquals(
                txResponse.nodeId, receiptQuery.getNodeAccountIds().get(0));
        Assertions.assertNotNull(receipt);

        server.close();
    }

    @Test
    void getRecordPinnedToSubmittingNodeByDefault() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("getRecordPinnedDefault", service);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.query(buildSuccessRecordResponse()));

        var txResponse = new AccountCreateTransaction().execute(server.client);
        var record = txResponse.getRecord(server.client);

        var recordQuery = txResponse.getRecordQuery(server.client);
        Assertions.assertEquals(1, recordQuery.getNodeAccountIds().size());
        Assertions.assertEquals(
                txResponse.nodeId, recordQuery.getNodeAccountIds().get(0));
        Assertions.assertNotNull(record);

        server.close();
    }

    @Test
    void failoverEnabledSubmittingNodeQueriedFirst() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("failoverEnabledFirst", service);
        server.client.setAllowReceiptNodeFailover(true);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.query(buildSuccessRecordResponse()));

        var txResponse = new AccountCreateTransaction().execute(server.client);

        var receiptQuery = txResponse.getReceiptQuery(server.client);
        Assertions.assertEquals(2, receiptQuery.getNodeAccountIds().size());
        Assertions.assertEquals(
                txResponse.nodeId, receiptQuery.getNodeAccountIds().get(0));

        var recordQuery = txResponse.getRecordQuery(server.client);
        Assertions.assertEquals(2, recordQuery.getNodeAccountIds().size());
        Assertions.assertEquals(
                txResponse.nodeId, recordQuery.getNodeAccountIds().get(0));

        var record = txResponse.getRecord(server.client);
        Assertions.assertNotNull(record);

        server.close();
    }

    @Test
    void receiptFailoverOnUnavailableAdvancesToNextNode() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("receiptFailoverUnavailable", service);
        server.client.setAllowReceiptNodeFailover(true);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.error(io.grpc.Status.UNAVAILABLE.asRuntimeException()));
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());

        var txResponse = new AccountCreateTransaction().execute(server.client);
        var receipt = txResponse.getReceipt(server.client);

        Assertions.assertNotNull(receipt);
        Assertions.assertEquals(2, service.buffer.queryRequestsReceived.size());

        server.close();
    }

    @Test
    void recordFailoverOnUnavailableAdvancesToNextNode() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("recordFailoverUnavailable", service);
        server.client.setAllowReceiptNodeFailover(true);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.error(io.grpc.Status.UNAVAILABLE.asRuntimeException()));
        service.buffer.enqueueResponse(TestResponse.query(buildSuccessRecordResponse()));

        var txResponse = new AccountCreateTransaction().execute(server.client);
        var record = txResponse.getRecord(server.client);

        Assertions.assertNotNull(record);

        server.close();
    }

    @Test
    void failoverWithExplicitTransactionNodes() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("failoverExplicitNodes", service);
        server.client.setAllowReceiptNodeFailover(true);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.query(buildSuccessRecordResponse()));

        var txResponse = new AccountCreateTransaction()
                .setNodeAccountIds(List.of(AccountId.fromString("1.1.1"), AccountId.fromString("1.1.2")))
                .execute(server.client);

        var receiptQuery = txResponse.getReceiptQuery(server.client);
        Assertions.assertEquals(2, receiptQuery.getNodeAccountIds().size());
        Assertions.assertEquals(
                txResponse.nodeId, receiptQuery.getNodeAccountIds().get(0));

        var recordQuery = txResponse.getRecordQuery(server.client);
        Assertions.assertEquals(2, recordQuery.getNodeAccountIds().size());
        Assertions.assertEquals(
                txResponse.nodeId, recordQuery.getNodeAccountIds().get(0));

        var record = txResponse.getRecord(server.client);
        Assertions.assertNotNull(record);

        server.close();
    }

    @Test
    void failoverWithoutExplicitTransactionNodes() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("failoverNoExplicitNodes", service);
        server.client.setAllowReceiptNodeFailover(true);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.query(buildSuccessRecordResponse()));

        var txResponse = new AccountCreateTransaction().execute(server.client);

        var receiptQuery = txResponse.getReceiptQuery(server.client);
        Assertions.assertEquals(2, receiptQuery.getNodeAccountIds().size());
        Assertions.assertEquals(
                txResponse.nodeId, receiptQuery.getNodeAccountIds().get(0));

        var recordQuery = txResponse.getRecordQuery(server.client);
        Assertions.assertEquals(2, recordQuery.getNodeAccountIds().size());
        Assertions.assertEquals(
                txResponse.nodeId, recordQuery.getNodeAccountIds().get(0));

        var record = txResponse.getRecord(server.client);
        Assertions.assertNotNull(record);

        server.close();
    }

    @Test
    void defaultBehaviorPinnedWhenNodeUnhealthy() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("pinnedWhenUnhealthy", service);
        server.client.setMaxAttempts(2);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.error(io.grpc.Status.UNAVAILABLE.asRuntimeException()));
        service.buffer.enqueueResponse(TestResponse.error(io.grpc.Status.UNAVAILABLE.asRuntimeException()));

        var txResponse = new AccountCreateTransaction().execute(server.client);

        Assertions.assertThrows(Exception.class, () -> {
            txResponse.getReceipt(server.client);
        });

        Assertions.assertEquals(2, service.buffer.queryRequestsReceived.size());

        server.close();
    }

    @Test
    void getReceiptRebindsResponseAfterThrottledRetry() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("throttleRebind", service);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.receipt(Status.THROTTLED_AT_CONSENSUS));
        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());

        var txResponse = new AccountCreateTransaction().execute(server.client);
        var originalTransactionId = txResponse.transactionId;

        var receipt = txResponse.getReceipt(server.client);

        Assertions.assertEquals(Status.SUCCESS, receipt.status);
        // exactly one resubmission
        Assertions.assertEquals(2, service.buffer.transactionRequestsReceived.size());

        var resubmitted = service.buffer.transactionRequestsReceived.get(1);
        Assertions.assertNotEquals(originalTransactionId, txResponse.transactionId);
        Assertions.assertEquals(transactionIdOf(resubmitted), txResponse.transactionId);
        Assertions.assertArrayEquals(
                com.hedera.hashgraph.sdk.Transaction.hash(
                        resubmitted.getSignedTransactionBytes().toByteArray()),
                txResponse.transactionHash);
        Assertions.assertEquals(txResponse.transactionId, txResponse.getTransactionId());
        Assertions.assertEquals(txResponse.nodeId, txResponse.getNodeId());
        Assertions.assertArrayEquals(txResponse.transactionHash, txResponse.getTransactionHash());
        // the receipt query is now pinned to the node the resubmission went to
        Assertions.assertEquals(
                txResponse.nodeId,
                txResponse.getReceiptQuery(server.client).getNodeAccountIds().get(0));

        // a second getReceipt() must not resubmit again
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        Assertions.assertEquals(Status.SUCCESS, txResponse.getReceipt(server.client).status);
        Assertions.assertEquals(2, service.buffer.transactionRequestsReceived.size());

        server.close();
    }

    @Test
    void getRecordAfterThrottleResubmitsOnceAndQueriesNewId() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("throttleRecord", service);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.receipt(Status.THROTTLED_AT_CONSENSUS));
        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.query(buildSuccessRecordResponse()));

        var txResponse = new AccountCreateTransaction().execute(server.client);
        var originalTransactionId = txResponse.transactionId;

        var record = txResponse.getRecord(server.client);

        Assertions.assertNotNull(record);
        // a single getRecord() causes exactly one resubmission, not two
        Assertions.assertEquals(2, service.buffer.transactionRequestsReceived.size());
        Assertions.assertNotEquals(originalTransactionId, txResponse.transactionId);

        var recordQuery = service.buffer.queryRequestsReceived.get(service.buffer.queryRequestsReceived.size() - 1);
        Assertions.assertEquals(txResponse.transactionId, recordQueryTransactionIdOf(recordQuery));

        server.close();
    }

    @Test
    void getRecordThrowsWhenRecordReceiptStatusIsNotSuccess() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("recordThrottledStatus", service);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(
                TestResponse.query(buildRecordResponse(ResponseCodeEnum.OK, ResponseCodeEnum.THROTTLED_AT_CONSENSUS)));

        var txResponse = new AccountCreateTransaction().execute(server.client);

        var exception =
                Assertions.assertThrows(ReceiptStatusException.class, () -> txResponse.getRecord(server.client));
        Assertions.assertEquals(Status.THROTTLED_AT_CONSENSUS, exception.receipt.status);

        server.close();
    }

    @Test
    void getRecordReturnsUnsuccessfulRecordWhenValidateStatusIsDisabled() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("recordNoValidate", service);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.receipt(Status.THROTTLED_AT_CONSENSUS));
        service.buffer.enqueueResponse(TestResponse.successfulReceipt());
        service.buffer.enqueueResponse(
                TestResponse.query(buildRecordResponse(ResponseCodeEnum.OK, ResponseCodeEnum.THROTTLED_AT_CONSENSUS)));

        var txResponse = new AccountCreateTransaction().execute(server.client).setValidateStatus(false);
        var record = txResponse.getRecord(server.client);

        Assertions.assertEquals(Status.THROTTLED_AT_CONSENSUS, record.receipt.status);
        // validateStatus(false) also suppresses the throttle retry, so nothing was resubmitted
        Assertions.assertEquals(1, service.buffer.transactionRequestsReceived.size());

        server.close();
    }

    @Test
    void doesNotResubmitThrottledTransactionWhenPayerIsNotOperator() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("throttleForeignPayer", service);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.receipt(Status.THROTTLED_AT_CONSENSUS));

        var txResponse = new AccountCreateTransaction()
                .setTransactionId(TransactionId.generate(new AccountId(0, 0, 999)))
                .execute(server.client);
        var originalTransactionId = txResponse.transactionId;

        var exception =
                Assertions.assertThrows(ReceiptStatusException.class, () -> txResponse.getReceipt(server.client));

        Assertions.assertEquals(Status.THROTTLED_AT_CONSENSUS, exception.receipt.status);
        Assertions.assertEquals(1, service.buffer.transactionRequestsReceived.size());
        Assertions.assertEquals(originalTransactionId, txResponse.transactionId);

        server.close();
    }

    @Test
    void doesNotResubmitThrottledTransactionWhenRegenerateTransactionIdIsDisabled() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("throttleNoRegenerate", service);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.receipt(Status.THROTTLED_AT_CONSENSUS));

        var txResponse =
                new AccountCreateTransaction().setRegenerateTransactionId(false).execute(server.client);
        var originalTransactionId = txResponse.transactionId;

        var exception =
                Assertions.assertThrows(ReceiptStatusException.class, () -> txResponse.getReceipt(server.client));

        Assertions.assertEquals(Status.THROTTLED_AT_CONSENSUS, exception.receipt.status);
        Assertions.assertEquals(1, service.buffer.transactionRequestsReceived.size());
        Assertions.assertEquals(originalTransactionId, txResponse.transactionId);

        server.close();
    }

    @Test
    void stopsResubmittingThrottledTransactionAfterMaxRetryAttempts() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("throttleMaxAttempts", service);

        service.buffer.enqueueResponse(TestResponse.transactionOk());
        service.buffer.enqueueResponse(TestResponse.receipt(Status.THROTTLED_AT_CONSENSUS));
        for (var i = 0; i < 4; i++) {
            service.buffer.enqueueResponse(TestResponse.transactionOk());
            service.buffer.enqueueResponse(TestResponse.receipt(Status.THROTTLED_AT_CONSENSUS));
        }

        var txResponse = new AccountCreateTransaction().execute(server.client);

        var exception =
                Assertions.assertThrows(ReceiptStatusException.class, () -> txResponse.getReceipt(server.client));

        Assertions.assertEquals(Status.THROTTLED_AT_CONSENSUS, exception.receipt.status);
        // one original submission plus four resubmissions
        Assertions.assertEquals(5, service.buffer.transactionRequestsReceived.size());

        server.close();
    }

    private static class TestCryptoService extends CryptoServiceGrpc.CryptoServiceImplBase implements TestService {
        public Buffer buffer = new Buffer();

        @Override
        public Buffer getBuffer() {
            return buffer;
        }

        @Override
        public void createAccount(Transaction request, StreamObserver<TransactionResponse> responseObserver) {
            respondToTransactionFromQueue(request, responseObserver);
        }

        @Override
        public void getTransactionReceipts(Query request, StreamObserver<Response> responseObserver) {
            respondToQueryFromQueue(request, responseObserver);
        }

        @Override
        public void getTxRecordByTxID(Query request, StreamObserver<Response> responseObserver) {
            respondToQueryFromQueue(request, responseObserver);
        }
    }
}
