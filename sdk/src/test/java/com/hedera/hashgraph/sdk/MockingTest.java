// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.AccountID;
import com.hedera.hashgraph.sdk.proto.CryptoGetAccountBalanceResponse;
import com.hedera.hashgraph.sdk.proto.CryptoGetInfoResponse;
import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.FileServiceGrpc;
import com.hedera.hashgraph.sdk.proto.Query;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.SmartContractServiceGrpc;
import com.hedera.hashgraph.sdk.proto.Transaction;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionGetReceiptResponse;
import com.hedera.hashgraph.sdk.proto.TransactionGetRecordResponse;
import com.hedera.hashgraph.sdk.proto.TransactionReceipt;
import com.hedera.hashgraph.sdk.proto.TransactionRecord;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class MockingTest {
    @Test
    void testSucceedsWithCorrectHbars() throws PrecheckStatusException, TimeoutException, InterruptedException {
        List<Object> responses1 = List.of(
                Status.Code.UNAVAILABLE.toStatus().asRuntimeException(),
                (Function<Object, Object>)
                        o -> Status.Code.UNAVAILABLE.toStatus().asRuntimeException(),
                Response.newBuilder()
                        .setCryptogetAccountBalance(CryptoGetAccountBalanceResponse.newBuilder()
                                .setHeader(ResponseHeader.newBuilder()
                                        .setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)
                                        .build())
                                .setAccountID(
                                        AccountID.newBuilder().setAccountNum(10).build())
                                .setBalance(100)
                                .build())
                        .build());

        var responses = List.of(responses1);

        try (var mocker = Mocker.withResponses(responses)) {
            var balance = new AccountBalanceQuery()
                    .setAccountId(new AccountId(0, 0, 10))
                    .execute(mocker.client);

            Assertions.assertEquals(balance.hbars, Hbar.fromTinybars(100));
        }
    }

    String makeBigString(int size) {
        char[] chars = new char[size];
        Arrays.fill(chars, 'A');
        return new String(chars);
    }

    @ParameterizedTest(name = "[{0}] Executable retries on gRPC error with PLATFORM_NOT_ACTIVE when getting receipt")
    @CsvSource({"sync", "async"})
    void shouldRetryExceptionallyFunctionsCorrectlyForPlatformNotActiveGetReceipt(String sync) throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("getReceiptRetry" + sync, service);
        server.client.setMaxAttempts(3);

        service.buffer.enqueueResponse(TestResponse.transaction(com.hedera.hashgraph.sdk.Status.PLATFORM_NOT_ACTIVE));
        service.buffer.enqueueResponse(TestResponse.transactionOk());

        com.hedera.hashgraph.sdk.TransactionResponse transactionResponse;
        if (sync.equals("sync")) {
            transactionResponse = new AccountCreateTransaction().execute(server.client);
        } else {
            transactionResponse =
                    new AccountCreateTransaction().executeAsync(server.client).get();
        }

        service.buffer
                .enqueueResponse(TestResponse.query(Response.newBuilder()
                        .setTransactionGetReceipt(TransactionGetReceiptResponse.newBuilder()
                                .setHeader(ResponseHeader.newBuilder()
                                        .setNodeTransactionPrecheckCode(ResponseCodeEnum.PLATFORM_NOT_ACTIVE))
                                .setReceipt(TransactionReceipt.newBuilder()
                                        .setStatus(ResponseCodeEnum.SUCCESS)
                                        .build())
                                .build())
                        .build()))
                .enqueueResponse(TestResponse.receipt(com.hedera.hashgraph.sdk.Status.PLATFORM_NOT_ACTIVE))
                .enqueueResponse(TestResponse.successfulReceipt());

        if (sync.equals("sync")) {
            transactionResponse.getReceipt(server.client);
        } else {
            transactionResponse.getReceiptAsync(server.client).get();
        }
    }

    @ParameterizedTest(name = "[{0}] Executable retries on gRPC error with PLATFORM_NOT_ACTIVE when getting record")
    @CsvSource({"sync", "async"})
    void shouldRetryExceptionallyFunctionsCorrectlyForPlatformNotActiveGetRecord(String sync) throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("getRecordRetry" + sync, service);
        server.client.setMaxAttempts(3);

        service.buffer.enqueueResponse(TestResponse.transaction(com.hedera.hashgraph.sdk.Status.PLATFORM_NOT_ACTIVE));
        service.buffer.enqueueResponse(TestResponse.transactionOk());

        com.hedera.hashgraph.sdk.TransactionResponse transactionResponse;
        if (sync.equals("sync")) {
            transactionResponse = new AccountCreateTransaction().execute(server.client);
        } else {
            transactionResponse =
                    new AccountCreateTransaction().executeAsync(server.client).get();
        }

        service.buffer
                .enqueueResponse(TestResponse.successfulReceipt()) // for inner getReceipt
                .enqueueResponse(TestResponse.successfulReceipt()) // for inner getCost
                .enqueueResponse(TestResponse.query(Response.newBuilder()
                        .setTransactionGetRecord(TransactionGetRecordResponse.newBuilder()
                                .setHeader(ResponseHeader.newBuilder()
                                        .setNodeTransactionPrecheckCode(ResponseCodeEnum.PLATFORM_NOT_ACTIVE)
                                        .build())
                                .setTransactionRecord(TransactionRecord.newBuilder()
                                        .setReceipt(TransactionReceipt.newBuilder()
                                                .setStatus(ResponseCodeEnum.SUCCESS)
                                                .build())
                                        .build())
                                .build())
                        .build()))
                .enqueueResponse(TestResponse.query(Response.newBuilder()
                        .setTransactionGetRecord(TransactionGetRecordResponse.newBuilder()
                                .setTransactionRecord(TransactionRecord.newBuilder()
                                        .setReceipt(TransactionReceipt.newBuilder()
                                                .setStatus(ResponseCodeEnum.PLATFORM_NOT_ACTIVE)
                                                .build())
                                        .build())
                                .build())
                        .build()))
                .enqueueResponse(TestResponse.query(Response.newBuilder()
                        .setTransactionGetRecord(TransactionGetRecordResponse.newBuilder()
                                .setTransactionRecord(TransactionRecord.newBuilder()
                                        .setReceipt(TransactionReceipt.newBuilder()
                                                .setStatus(ResponseCodeEnum.SUCCESS)
                                                .build())
                                        .build())
                                .build())
                        .build()));

        if (sync.equals("sync")) {
            transactionResponse.getRecord(server.client);
        } else {
            transactionResponse.getRecordAsync(server.client).get();
        }
    }

    @ParameterizedTest(name = "[{0}, {1}] ContractCreateFlow functions")
    @CsvSource({
        "sync, stakedNode",
        "sync, stakedAccount",
        "async, stakedNode",
        "async, stakedAccount",
    })
    void contractCreateFlowFunctions(String versionToTest, String stakeType) throws Throwable {
        var BIG_BYTECODE = makeBigString(ContractCreateFlow.FILE_CREATE_MAX_BYTES + 1000);
        var adminKey = PrivateKey.generateED25519().getPublicKey();

        var cryptoService = new TestCryptoService();
        var fileService = new TestFileService();
        var contractService = new TestContractService();
        var server = new TestServer(
                "contractCreateFlow" + versionToTest + stakeType, cryptoService, fileService, contractService);

        var fileId = FileId.fromString("1.2.3");
        var maxAutomaticTokenAssociations = 101;
        var stakedAccountId = AccountId.fromString("4.3.2");
        var stakedNode = 13L;
        var declineStakingReward = true;

        cryptoService
                .buffer
                .enqueueResponse(TestResponse.query(Response.newBuilder()
                        .setTransactionGetReceipt(TransactionGetReceiptResponse.newBuilder()
                                .setReceipt(TransactionReceipt.newBuilder()
                                        .setFileID(fileId.toProtobuf())
                                        .setStatus(ResponseCodeEnum.SUCCESS)
                                        .build())
                                .build())
                        .build()))
                .enqueueResponse(TestResponse.successfulReceipt())
                .enqueueResponse(TestResponse.successfulReceipt());
        fileService
                .buffer
                .enqueueResponse(TestResponse.transactionOk())
                .enqueueResponse(TestResponse.transactionOk())
                .enqueueResponse(TestResponse.transactionOk());

        contractService.buffer.enqueueResponse(TestResponse.transactionOk());

        var flow = new ContractCreateFlow()
                .setBytecode(BIG_BYTECODE)
                .setContractMemo("memo goes here")
                .setConstructorParameters(new byte[] {1, 2, 3})
                .setAutoRenewPeriod(Duration.ofMinutes(1))
                .setAdminKey(adminKey)
                .setGas(100)
                .setInitialBalance(new Hbar(3))
                .setMaxAutomaticTokenAssociations(maxAutomaticTokenAssociations)
                .setDeclineStakingReward(declineStakingReward);

        if (stakeType.equals("stakedAccount")) {
            flow.setStakedAccountId(stakedAccountId);
        } else {
            flow.setStakedNodeId(stakedNode);
        }

        if (versionToTest.equals("sync")) {
            flow.execute(server.client);
        } else {
            flow.executeAsync(server.client).get();
        }

        Thread.sleep(1000);

        Assertions.assertEquals(3, cryptoService.buffer.queryRequestsReceived.size());
        Assertions.assertEquals(3, fileService.buffer.transactionRequestsReceived.size());
        Assertions.assertEquals(1, contractService.buffer.transactionRequestsReceived.size());
        var transactions = new ArrayList<com.hedera.hashgraph.sdk.Transaction<?>>();
        for (var request : fileService.buffer.transactionRequestsReceived) {
            transactions.add(com.hedera.hashgraph.sdk.Transaction.fromBytes(request.toByteArray()));
        }
        transactions.add(com.hedera.hashgraph.sdk.Transaction.fromBytes(
                contractService.buffer.transactionRequestsReceived.get(0).toByteArray()));

        Assertions.assertInstanceOf(FileCreateTransaction.class, transactions.get(0));
        Assertions.assertEquals(
                ContractCreateFlow.FILE_CREATE_MAX_BYTES,
                ((FileCreateTransaction) transactions.get(0)).getContents().size());

        Assertions.assertTrue(cryptoService.buffer.queryRequestsReceived.get(0).hasTransactionGetReceipt());

        Assertions.assertInstanceOf(FileAppendTransaction.class, transactions.get(1));
        var fileAppendTx = (FileAppendTransaction) transactions.get(1);
        Assertions.assertEquals(fileId, fileAppendTx.getFileId());
        Assertions.assertEquals(
                BIG_BYTECODE.length() - ContractCreateFlow.FILE_CREATE_MAX_BYTES,
                fileAppendTx.getContents().size());

        Assertions.assertInstanceOf(ContractCreateTransaction.class, transactions.get(3));
        var contractCreateTx = (ContractCreateTransaction) transactions.get(3);
        Assertions.assertEquals("memo goes here", contractCreateTx.getContractMemo());
        Assertions.assertEquals(fileId, contractCreateTx.getBytecodeFileId());
        Assertions.assertEquals(ByteString.copyFrom(new byte[] {1, 2, 3}), contractCreateTx.getConstructorParameters());
        Assertions.assertEquals(Duration.ofMinutes(1), contractCreateTx.getAutoRenewPeriod());
        Assertions.assertEquals(adminKey, contractCreateTx.getAdminKey());
        Assertions.assertEquals(100, contractCreateTx.getGas());
        Assertions.assertEquals(new Hbar(3), contractCreateTx.getInitialBalance());
        Assertions.assertEquals(maxAutomaticTokenAssociations, contractCreateTx.getMaxAutomaticTokenAssociations());
        Assertions.assertEquals(declineStakingReward, contractCreateTx.getDeclineStakingReward());

        if (stakeType.equals("stakedAccount")) {
            Assertions.assertEquals(stakedAccountId, contractCreateTx.getStakedAccountId());
        } else {
            Assertions.assertEquals(stakedNode, contractCreateTx.getStakedNodeId());
        }

        Assertions.assertInstanceOf(FileDeleteTransaction.class, transactions.get(2));

        server.close();
    }

    @Test
    void accountInfoFlowFunctions() throws Throwable {
        var BIG_BYTES = makeBigString(1000).getBytes(StandardCharsets.UTF_8);
        var privateKey = PrivateKey.generateED25519();
        var otherPrivateKey = PrivateKey.generateED25519();
        var accountId = AccountId.fromString("1.2.3");
        var cost = Hbar.from(1);

        Supplier<TokenMintTransaction> makeTx = () -> new TokenMintTransaction()
                .setTokenId(TokenId.fromString("1.2.3"))
                .setAmount(5)
                .setTransactionId(TransactionId.generate(accountId))
                .setNodeAccountIds(List.of(AccountId.fromString("0.0.3")))
                .freeze();

        var properlySignedTx = makeTx.get().sign(privateKey);
        var improperlySignedTx = makeTx.get().sign(otherPrivateKey);
        var properBigBytesSignature = privateKey.sign(BIG_BYTES);
        var improperBigBytesSignature = otherPrivateKey.sign(BIG_BYTES);

        var cryptoService = new TestCryptoService();
        var server = new TestServer("accountInfoFlow", cryptoService);

        for (int i = 0; i < 8; i++) {
            cryptoService.buffer.enqueueResponse(TestResponse.query(Response.newBuilder()
                    .setCryptoGetInfo(CryptoGetInfoResponse.newBuilder()
                            .setHeader(ResponseHeader.newBuilder()
                                    .setCost(cost.toTinybars())
                                    .build())
                            .build())
                    .build()));
            cryptoService.buffer.enqueueResponse(TestResponse.query(Response.newBuilder()
                    .setCryptoGetInfo(CryptoGetInfoResponse.newBuilder()
                            .setAccountInfo(CryptoGetInfoResponse.AccountInfo.newBuilder()
                                    .setKey(privateKey.getPublicKey().toProtobufKey())
                                    .build())
                            .build())
                    .build()));
        }

        Assertions.assertTrue(AccountInfoFlow.verifyTransactionSignature(server.client, accountId, properlySignedTx));
        Assertions.assertFalse(
                AccountInfoFlow.verifyTransactionSignature(server.client, accountId, improperlySignedTx));
        Assertions.assertTrue(
                AccountInfoFlow.verifySignature(server.client, accountId, BIG_BYTES, properBigBytesSignature));
        Assertions.assertFalse(
                AccountInfoFlow.verifySignature(server.client, accountId, BIG_BYTES, improperBigBytesSignature));
        Assertions.assertTrue(
                AccountInfoFlow.verifyTransactionSignatureAsync(server.client, accountId, properlySignedTx)
                        .get());
        Assertions.assertFalse(
                AccountInfoFlow.verifyTransactionSignatureAsync(server.client, accountId, improperlySignedTx)
                        .get());
        Assertions.assertTrue(
                AccountInfoFlow.verifySignatureAsync(server.client, accountId, BIG_BYTES, properBigBytesSignature)
                        .get());
        Assertions.assertFalse(
                AccountInfoFlow.verifySignatureAsync(server.client, accountId, BIG_BYTES, improperBigBytesSignature)
                        .get());

        Assertions.assertEquals(16, cryptoService.buffer.queryRequestsReceived.size());
        for (int i = 0; i < 16; i += 2) {
            var costQueryRequest = cryptoService.buffer.queryRequestsReceived.get(i);
            var queryRequest = cryptoService.buffer.queryRequestsReceived.get(i + 1);

            Assertions.assertTrue(costQueryRequest.hasCryptoGetInfo());
            Assertions.assertTrue(costQueryRequest.getCryptoGetInfo().hasHeader());
            Assertions.assertTrue(
                    costQueryRequest.getCryptoGetInfo().getHeader().hasPayment());

            Assertions.assertTrue(queryRequest.hasCryptoGetInfo());
            Assertions.assertTrue(queryRequest.getCryptoGetInfo().hasAccountID());
            Assertions.assertEquals(
                    accountId,
                    AccountId.fromProtobuf(queryRequest.getCryptoGetInfo().getAccountID()));
        }
        server.close();
    }

    @Test
    void exitOnAborted() throws PrecheckStatusException, TimeoutException, InterruptedException {
        List<Object> responses1 = List.of();

        var responses = List.of(responses1);

        try (var mocker = Mocker.withResponses(responses)) {
            Assertions.assertThrows(RuntimeException.class, () -> new AccountBalanceQuery()
                    .setAccountId(new AccountId(0, 0, 10))
                    .execute(mocker.client));
        }
    }

    @ParameterizedTest(name = "[{2}] Executable retries on gRPC error with status {0} and description {1}")
    @CsvSource({
        "INTERNAL, internal RST_STREAM error, sync",
        "INTERNAL, rst stream, sync",
        "RESOURCE_EXHAUSTED, , sync",
        "UNAVAILABLE, , sync",
        "INTERNAL, internal RST_STREAM error, async",
        "INTERNAL, rst stream, async",
        "RESOURCE_EXHAUSTED, , async",
        "UNAVAILABLE, , async"
    })
    void shouldRetryExceptionallyFunctionsCorrectly(Status.Code code, String description, String sync)
            throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer(
                "executableRetry" + code + (description != null ? description.replace(" ", "") : "NULL") + sync,
                service);

        var exception = Status.fromCode(code).withDescription(description).asRuntimeException();

        service.buffer.enqueueResponse(TestResponse.error(exception)).enqueueResponse(TestResponse.transactionOk());

        if (sync.equals("sync")) {
            new AccountCreateTransaction()
                    .setNodeAccountIds(List.of(AccountId.fromString("1.1.1"), AccountId.fromString("2.2.2")))
                    .execute(server.client);
        } else {
            new AccountCreateTransaction()
                    .setNodeAccountIds(List.of(AccountId.fromString("1.1.1"), AccountId.fromString("2.2.2")))
                    .executeAsync(server.client)
                    .get();
        }

        Assertions.assertEquals(2, service.buffer.transactionRequestsReceived.size());
        assertFirstTwoRequestsNotDirectedAtSameNode(service);

        server.close();
    }

    @ParameterizedTest(name = "[{2}] Executable should make max {1} attempts when there are {0} errors, and error")
    @CsvSource({"2, 2, sync", "2, 2, async"})
    void hitsTxMaxAttemptsCorrectly(Integer numberOfErrors, Integer maxAttempts, String sync) throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("executableMaxAttemptsSync" + numberOfErrors + maxAttempts + sync, service);

        var exception = Status.UNAVAILABLE.asRuntimeException();

        for (var i = 0; i < numberOfErrors; i++) {
            service.buffer.enqueueResponse(TestResponse.error(exception));
        }

        service.buffer.enqueueResponse(TestResponse.transactionOk());

        if (sync.equals("sync")) {
            Assertions.assertThrows(MaxAttemptsExceededException.class, () -> {
                new AccountCreateTransaction()
                        .setMaxAttempts(maxAttempts)
                        .setNodeAccountIds(List.of(AccountId.fromString("1.1.1"), AccountId.fromString("2.2.2")))
                        .execute(server.client);
            });
        } else {
            new AccountCreateTransaction()
                    .setMaxAttempts(maxAttempts)
                    .setNodeAccountIds(List.of(AccountId.fromString("1.1.1"), AccountId.fromString("2.2.2")))
                    .executeAsync(server.client)
                    .handle((response, error) -> {
                        Assertions.assertNotNull(error);
                        System.out.println(error);
                        Assertions.assertTrue(error.getCause() instanceof MaxAttemptsExceededException);

                        return null;
                    })
                    .get();
        }

        Assertions.assertEquals(2, service.buffer.transactionRequestsReceived.size());

        server.close();
    }

    @ParameterizedTest(name = "[{2}] Executable retries on {1} Hedera status error(s) {0}")
    @CsvSource({
        "BUSY, 1, sync",
        "PLATFORM_TRANSACTION_NOT_CREATED, 1, sync",
        "TRANSACTION_EXPIRED, 1, sync",
        "BUSY, 3, sync",
        "PLATFORM_TRANSACTION_NOT_CREATED, 3, sync",
        "TRANSACTION_EXPIRED, 3, sync",
        "BUSY, 1, async",
        "PLATFORM_TRANSACTION_NOT_CREATED, 1, async",
        "TRANSACTION_EXPIRED, 1, async",
        "BUSY, 3, async",
        "PLATFORM_TRANSACTION_NOT_CREATED, 3, async",
        "TRANSACTION_EXPIRED, 3, async"
    })
    void shouldRetryFunctionsCorrectly(com.hedera.hashgraph.sdk.Status status, int numberOfErrors, String sync)
            throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("shouldRetryFunctionsCorrectly" + status + numberOfErrors + sync, service);

        for (var i = 0; i < numberOfErrors; i++) {
            service.buffer.enqueueResponse(TestResponse.transaction(status));
        }

        service.buffer.enqueueResponse(TestResponse.transactionOk());

        server.client.setMaxAttempts(4);

        if (sync.equals("sync")) {
            new AccountCreateTransaction()
                    .setNodeAccountIds(List.of(AccountId.fromString("1.1.1"), AccountId.fromString("2.2.2")))
                    .execute(server.client);
        } else {
            new AccountCreateTransaction()
                    .setNodeAccountIds(List.of(AccountId.fromString("1.1.1"), AccountId.fromString("2.2.2")))
                    .executeAsync(server.client)
                    .get();
        }

        Assertions.assertEquals(numberOfErrors + 1, service.buffer.transactionRequestsReceived.size());
        assertFirstTwoRequestsNotDirectedAtSameNode(service);

        server.close();
    }

    @ParameterizedTest(name = "[{2}] Executable retries on {1} Hedera status error(s) {0}")
    @CsvSource({
        "BUSY, sync",
        "PLATFORM_TRANSACTION_NOT_CREATED, sync",
        "PLATFORM_NOT_ACTIVE, sync",
        "BUSY, async",
        "PLATFORM_TRANSACTION_NOT_CREATED, async",
        "PLATFORM_NOT_ACTIVE, async",
    })
    void hitsClientMaxAttemptsCorrectly(com.hedera.hashgraph.sdk.Status status, String sync) throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("shouldRetryFunctionsCorrectly" + status + sync, service);

        for (var i = 0; i < 2; i++) {
            service.buffer.enqueueResponse(TestResponse.transaction(status));
        }

        server.client.setMaxAttempts(2);

        if (sync.equals("sync")) {
            Assertions.assertThrows(MaxAttemptsExceededException.class, () -> {
                new AccountCreateTransaction()
                        .setNodeAccountIds(List.of(AccountId.fromString("1.1.1"), AccountId.fromString("2.2.2")))
                        .execute(server.client);
            });
        } else {
            new AccountCreateTransaction()
                    .setNodeAccountIds(List.of(AccountId.fromString("1.1.1"), AccountId.fromString("2.2.2")))
                    .executeAsync(server.client)
                    .handle((response, error) -> {
                        Assertions.assertNotNull(error);
                        Assertions.assertTrue(error.getCause() instanceof MaxAttemptsExceededException);

                        return null;
                    })
                    .get();
        }
        Assertions.assertEquals(2, service.buffer.transactionRequestsReceived.size());
        assertFirstTwoRequestsNotDirectedAtSameNode(service);

        server.close();
    }

    private static void assertFirstTwoRequestsNotDirectedAtSameNode(TestCryptoService service)
            throws InvalidProtocolBufferException {
        var requests = service.buffer.transactionRequestsReceived;
        var signedTx0 = SignedTransaction.parseFrom(requests.get(0).getSignedTransactionBytes());
        var signedTx1 = SignedTransaction.parseFrom(requests.get(1).getSignedTransactionBytes());
        var txBody0 = TransactionBody.parseFrom(signedTx0.getBodyBytes());
        var txBody1 = TransactionBody.parseFrom(signedTx1.getBodyBytes());
        Assertions.assertNotEquals(txBody0.getNodeAccountID(), txBody1.getNodeAccountID());
    }

    @Test
    @DisplayName("Client.setDefaultMaxTransactionFee() functions correctly")
    void defaultMaxTransactionFeeTest() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("maxTransactionFee", service);

        service.buffer
                .enqueueResponse(TestResponse.transactionOk())
                .enqueueResponse(TestResponse.transactionOk())
                .enqueueResponse(TestResponse.transactionOk())
                .enqueueResponse(TestResponse.transactionOk());

        new AccountDeleteTransaction().execute(server.client);

        new AccountDeleteTransaction().setMaxTransactionFee(new Hbar(5)).execute(server.client);

        server.client.setDefaultMaxTransactionFee(new Hbar(1));

        new AccountDeleteTransaction().execute(server.client);

        new AccountDeleteTransaction().setMaxTransactionFee(new Hbar(3)).execute(server.client);

        Assertions.assertEquals(4, service.buffer.transactionRequestsReceived.size());
        var transactions = new ArrayList<com.hedera.hashgraph.sdk.Transaction<?>>();
        for (var request : service.buffer.transactionRequestsReceived) {
            transactions.add(com.hedera.hashgraph.sdk.Transaction.fromBytes(request.toByteArray()));
        }
        Assertions.assertEquals(new Hbar(2), transactions.get(0).getMaxTransactionFee());
        Assertions.assertEquals(new Hbar(5), transactions.get(1).getMaxTransactionFee());
        Assertions.assertEquals(new Hbar(1), transactions.get(2).getMaxTransactionFee());
        Assertions.assertEquals(new Hbar(3), transactions.get(3).getMaxTransactionFee());

        server.close();
    }

    @Disabled
    @Test
    @DisplayName("Client.setDefaultMaxQueryPayment() functions correctly")
    void defaultMaxQueryPaymentTest() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("queryPayment", service);

        var response = Response.newBuilder()
                .setCryptogetAccountBalance(
                        new AccountBalance(new Hbar(0), new HashMap<TokenId, Long>(), new HashMap<TokenId, Integer>())
                                .toProtobuf())
                .build();

        service.buffer
                .enqueueResponse(TestResponse.query(response))
                .enqueueResponse(TestResponse.query(response))
                .enqueueResponse(TestResponse.query(response));

        // TODO: this will take some work, since I have to contend with Query's getCost behavior
        // TODO: actually, because AccountBalanceQuery is free, I'll need some other query type to test this.
        //       Perhaps getAccountInfo?

        server.close();
    }

    @Test
    @DisplayName("Signer is prevented from signing twice")
    void signerDoesNotSignTwice() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("signerDoesNotSignTwice", service);

        service.buffer.enqueueResponse(TestResponse.transactionOk());

        var aliceKey = PrivateKey.generateED25519();

        var transaction = new AccountCreateTransaction()
                .setTransactionId(TransactionId.generate(Objects.requireNonNull(server.client.getOperatorAccountId())))
                .setNodeAccountIds(server.client.network.getNodeAccountIdsForExecute())
                .freeze()
                .sign(aliceKey);

        // This will cause the SDK Transaction to populate the sigPairLists list
        transaction.getTransactionHashPerNode();

        // This will clear the outerTransactions list while keeping the sigPairLists list
        transaction.signWithOperator(server.client);

        // If Transaction.signTransaction() is not programmed correctly, it will add Alice's signature to the
        // sigPairList a second time here.
        transaction.execute(server.client);

        // Now we must go through the laborious process of digging info out of the response.  =(
        Assertions.assertEquals(1, service.buffer.transactionRequestsReceived.size());
        var request = service.buffer.transactionRequestsReceived.get(0);
        var sigPairList = SignedTransaction.parseFrom(request.getSignedTransactionBytes())
                .getSigMap()
                .getSigPairList();
        Assertions.assertEquals(2, sigPairList.size());
        Assertions.assertNotEquals(
                sigPairList.get(0).getEd25519().toString(),
                sigPairList.get(1).getEd25519().toString());

        server.close();
    }

    @Test
    @DisplayName("Can cancel executeAsync()")
    void canCancelExecuteAsync() throws Exception {
        var service = new TestCryptoService();
        var server = new TestServer("canCancelExecuteAsync", service);

        server.client.setMaxBackoff(Duration.ofSeconds(8));
        server.client.setMinBackoff(Duration.ofSeconds(1));

        var noReceiptResponse = TestResponse.query(Response.newBuilder()
                .setTransactionGetReceipt(TransactionGetReceiptResponse.newBuilder()
                        .setHeader(ResponseHeader.newBuilder()
                                .setNodeTransactionPrecheckCode(
                                        com.hedera.hashgraph.sdk.Status.RECEIPT_NOT_FOUND.code)))
                .build());

        service.buffer.enqueueResponse(noReceiptResponse);
        service.buffer.enqueueResponse(noReceiptResponse);
        service.buffer.enqueueResponse(noReceiptResponse);

        var future = new TransactionReceiptQuery().executeAsync(server.client);
        Thread.sleep(1500);
        future.cancel(true);
        Thread.sleep(5000);

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
        public void cryptoDelete(Transaction request, StreamObserver<TransactionResponse> responseObserver) {
            respondToTransactionFromQueue(request, responseObserver);
        }

        @Override
        public void cryptoGetBalance(Query request, StreamObserver<Response> responseObserver) {
            respondToQueryFromQueue(request, responseObserver);
        }

        @Override
        public void getTransactionReceipts(Query request, StreamObserver<Response> responseObserver) {
            respondToQueryFromQueue(request, responseObserver);
        }

        @Override
        public void getTxRecordByTxID(Query request, StreamObserver<Response> responseObserver) {
            respondToQueryFromQueue(request, responseObserver);
        }

        @Override
        public void getAccountInfo(Query request, StreamObserver<Response> responseObserver) {
            respondToQueryFromQueue(request, responseObserver);
        }
    }

    private static class TestFileService extends FileServiceGrpc.FileServiceImplBase implements TestService {
        public Buffer buffer = new Buffer();

        @Override
        public Buffer getBuffer() {
            return buffer;
        }

        @Override
        public void createFile(Transaction request, StreamObserver<TransactionResponse> responseObserver) {
            respondToTransactionFromQueue(request, responseObserver);
        }

        @Override
        public void appendContent(Transaction request, StreamObserver<TransactionResponse> responseObserver) {
            respondToTransactionFromQueue(request, responseObserver);
        }

        @Override
        public void deleteFile(Transaction request, StreamObserver<TransactionResponse> responseObserver) {
            respondToTransactionFromQueue(request, responseObserver);
        }
    }

    private static class TestContractService extends SmartContractServiceGrpc.SmartContractServiceImplBase
            implements TestService {
        public Buffer buffer = new Buffer();

        @Override
        public Buffer getBuffer() {
            return buffer;
        }

        @Override
        public void createContract(Transaction request, StreamObserver<TransactionResponse> responseObserver) {
            respondToTransactionFromQueue(request, responseObserver);
        }
    }
}
