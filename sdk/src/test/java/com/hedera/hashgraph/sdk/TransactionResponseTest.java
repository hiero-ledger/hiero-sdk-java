// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.Query;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import com.hedera.hashgraph.sdk.proto.Transaction;
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
