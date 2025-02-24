// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.FileID;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.Transaction;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionGetReceiptResponse;
import com.hedera.hashgraph.sdk.proto.TransactionReceipt;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EthereumFlowMockTest {
    static ByteString ETHEREUM_DATA = ByteString.fromHex(
            "f864012f83018000947e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc18180827653820277a0f9fbff985d374be4a55f296915002eec11ac96f1ce2df183adf992baa9390b2fa00c1e867cc960d9c74ec2e6a662b7908ec4c8cc9f3091e886bcefbeb2290fb792");

    static ByteString LONG_CALL_DATA = ByteString.fromHex("00".repeat(5121));

    @Test
    void dontTruncateEthereumDataUnnecessarily()
            throws PrecheckStatusException, TimeoutException, InterruptedException, ReceiptStatusException {
        List<Object> responses1 = List.of(
                (Function<Object, Object>) o -> {
                    var signedTransaction = SignedTransaction.parseFrom(((Transaction) o).getSignedTransactionBytes());
                    var transactionBody = TransactionBody.parseFrom(signedTransaction.getBodyBytes());
                    assertThat(transactionBody.getDataCase())
                            .isEqualByComparingTo(TransactionBody.DataCase.ETHEREUMTRANSACTION);
                    assertThat(transactionBody.hasEthereumTransaction()).isTrue();
                    assertThat(transactionBody.getEthereumTransaction().getEthereumData())
                            .isEqualTo(ETHEREUM_DATA);
                    return TransactionResponse.newBuilder()
                            .setNodeTransactionPrecheckCodeValue(0)
                            .build();
                },
                Response.newBuilder()
                        .setTransactionGetReceipt(TransactionGetReceiptResponse.newBuilder()
                                .setReceipt(
                                        TransactionReceipt.newBuilder().setStatusValue(ResponseCodeEnum.SUCCESS_VALUE)))
                        .build());

        var responses = List.of(responses1);

        try (var mocker = Mocker.withResponses(responses)) {
            new EthereumFlow()
                    .setEthereumData(ETHEREUM_DATA.toByteArray())
                    .execute(mocker.client)
                    .getReceipt(mocker.client);
        }
    }

    @ParameterizedTest(name = "[{0}] ContractCreateFlow functions")
    @CsvSource({"sync", "async"})
    void extractsCallData(String versionToTest)
            throws PrecheckStatusException, TimeoutException, InterruptedException, ReceiptStatusException,
                    ExecutionException {
        List<Object> responses1 = List.of(
                (Function<Object, Object>) o -> {
                    var signedTransaction = SignedTransaction.parseFrom(((Transaction) o).getSignedTransactionBytes());
                    var transactionBody = TransactionBody.parseFrom(signedTransaction.getBodyBytes());
                    assertThat(transactionBody.getDataCase()).isEqualByComparingTo(TransactionBody.DataCase.FILECREATE);
                    assertThat(transactionBody.hasFileCreate()).isTrue();
                    assertThat(transactionBody.getFileCreate().getContents().size())
                            .isEqualTo(4096);
                    return TransactionResponse.newBuilder()
                            .setNodeTransactionPrecheckCodeValue(0)
                            .build();
                },
                Response.newBuilder()
                        .setTransactionGetReceipt(TransactionGetReceiptResponse.newBuilder()
                                .setReceipt(TransactionReceipt.newBuilder()
                                        .setStatusValue(ResponseCodeEnum.SUCCESS_VALUE)
                                        .setFileID(FileID.newBuilder().setFileNum(1))))
                        .build(),
                (Function<Object, Object>) o -> {
                    var signedTransaction = SignedTransaction.parseFrom(((Transaction) o).getSignedTransactionBytes());
                    var transactionBody = TransactionBody.parseFrom(signedTransaction.getBodyBytes());
                    assertThat(transactionBody.getDataCase()).isEqualByComparingTo(TransactionBody.DataCase.FILEAPPEND);
                    assertThat(transactionBody.hasFileAppend()).isTrue();
                    assertThat(transactionBody.getFileAppend().hasFileID()).isTrue();
                    assertThat(transactionBody.getFileAppend().getFileID().getFileNum())
                            .isEqualTo(1);
                    assertThat(transactionBody.getFileAppend().getContents()).isEqualTo(LONG_CALL_DATA.substring(4096));
                    return TransactionResponse.newBuilder()
                            .setNodeTransactionPrecheckCodeValue(0)
                            .build();
                },
                Response.newBuilder()
                        .setTransactionGetReceipt(TransactionGetReceiptResponse.newBuilder()
                                .setReceipt(
                                        TransactionReceipt.newBuilder().setStatusValue(ResponseCodeEnum.SUCCESS_VALUE)))
                        .build(),
                Response.newBuilder()
                        .setTransactionGetReceipt(TransactionGetReceiptResponse.newBuilder()
                                .setReceipt(
                                        TransactionReceipt.newBuilder().setStatusValue(ResponseCodeEnum.SUCCESS_VALUE)))
                        .build(),
                (Function<Object, Object>) o -> {
                    var signedTransaction = SignedTransaction.parseFrom(((Transaction) o).getSignedTransactionBytes());
                    var transactionBody = TransactionBody.parseFrom(signedTransaction.getBodyBytes());
                    assertThat(transactionBody.getDataCase())
                            .isEqualByComparingTo(TransactionBody.DataCase.ETHEREUMTRANSACTION);
                    assertThat(transactionBody.hasEthereumTransaction()).isTrue();
                    assertThat(EthereumTransactionData.fromBytes(transactionBody
                                            .getEthereumTransaction()
                                            .getEthereumData()
                                            .toByteArray())
                                    .callData)
                            .isEqualTo(LONG_CALL_DATA.toByteArray());
                    return TransactionResponse.newBuilder()
                            .setNodeTransactionPrecheckCodeValue(0)
                            .build();
                },
                Response.newBuilder()
                        .setTransactionGetReceipt(TransactionGetReceiptResponse.newBuilder()
                                .setReceipt(
                                        TransactionReceipt.newBuilder().setStatusValue(ResponseCodeEnum.SUCCESS_VALUE)))
                        .build());

        var responses = List.of(responses1);

        EthereumFlow ethereumFlow;
        try (var mocker = Mocker.withResponses(responses)) {
            var ethereumData = EthereumTransactionData.fromBytes(ETHEREUM_DATA.toByteArray());
            ethereumData.callData = LONG_CALL_DATA.toByteArray();

            if (versionToTest.equals("sync")) {
                ethereumFlow = new EthereumFlow()
                        .setMaxGasAllowance(Hbar.fromTinybars(25))
                        .setEthereumData(ethereumData.toBytes());

                ethereumFlow.execute(mocker.client).getReceipt(mocker.client);
            } else {
                ethereumFlow = new EthereumFlow()
                        .setMaxGasAllowance(Hbar.fromTinybars(25))
                        .setEthereumData(ethereumData.toBytes());

                ethereumFlow
                        .executeAsync(mocker.client)
                        .thenCompose(response -> response.getReceiptAsync(mocker.client))
                        .get();
            }

            assertThat(ethereumFlow.getEthereumData()).isNotNull();
            assertThat(ethereumFlow.getMaxGasAllowance().toTinybars()).isEqualTo(25);
        }
    }
}
