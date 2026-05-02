// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hiero.sdk.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Disabled("Temporarily disabled. Will be enabled with PR for refactoring")
class FeeEstimateQueryIntegrationTest {

    private static final long MIRROR_SYNC_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(2);

    private IntegrationTestEnv createFeeEstimateTestEnv() throws Exception {
        var testEnv = new IntegrationTestEnv(1);

        if ("localhost".equals(System.getProperty("HEDERA_NETWORK"))) {
            testEnv.client.setMirrorNetwork(List.of("127.0.0.1:8084"));
        }

        return testEnv;
    }

    @Test
    @DisplayName("Given a TokenCreateTransaction, when fee estimate is requested, "
            + "then response includes service fees for token creation and network fees")
    void tokenCreateTransactionFeeEstimate() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {

            // Given: A TokenCreateTransaction is created
            var transaction = new TokenCreateTransaction()
                    .setTokenName("Test Token")
                    .setTokenSymbol("TEST")
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            waitForMirrorNodeSync();

            // When: A fee estimate is requested
            FeeEstimateResponse response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.STATE)
                    .execute(testEnv.client);

            // Then: The response includes appropriate fees
            assertFeeComponentsPresent(response);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName(
            "Given a TransferTransaction, when fee estimate is requested in STATE mode, then all components are returned")
    void transferTransactionStateModeFeeEstimate() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, Hbar.fromTinybars(-1))
                    .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.fromTinybars(1))
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.STATE)
                    .execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName(
            "Given a TransferTransaction, when fee estimate is requested in INTRINSIC mode, then components are returned without state dependencies")
    void transferTransactionIntrinsicModeFeeEstimate() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, Hbar.fromTinybars(-1))
                    .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.fromTinybars(1))
                    .freezeWith(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.INTRINSIC)
                    .execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName(
            "Given a TransferTransaction without explicit mode, when fee estimate is requested, then INTRINSIC mode is used by default")
    void transferTransactionDefaultModeIsIntrinsic() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, Hbar.fromTinybars(-1))
                    .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.fromTinybars(1))
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery().setTransaction(transaction).execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName(
            "Given a TransferTransaction with high volume throttle, when fee estimate is requested, then high volume multiplier is returned")
    void feeEstimateQueryWithHighVolumeThrottle() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, Hbar.fromTinybars(-1))
                    .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.fromTinybars(1))
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setHighVolumeThrottle(5000)
                    .execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertThat(response.getHighVolumeMultiplier()).isGreaterThanOrEqualTo(1);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName("Given a TokenMintTransaction, when fee estimate is requested, then extras are returned for minting")
    void tokenMintTransactionFeeEstimate() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new TokenMintTransaction()
                    .setTokenId(TokenId.fromString("0.0.1234"))
                    .setAmount(10)
                    .freezeWith(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.INTRINSIC)
                    .execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertThat(response.getNode().getExtras()).isNotNull();
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName("Given a TopicCreateTransaction, when fee estimate is requested, then service fees are included")
    void topicCreateTransactionFeeEstimate() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new TopicCreateTransaction()
                    .setTopicMemo("integration test topic")
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.STATE)
                    .execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName("Given a ContractCreateTransaction, when fee estimate is requested, then execution fees are returned")
    void contractCreateTransactionFeeEstimate() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new ContractCreateTransaction()
                    .setBytecode(new byte[] {1, 2, 3})
                    .setGas(1000)
                    .setAdminKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.STATE)
                    .execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName("Given a FileCreateTransaction, when fee estimate is requested, then storage fees are included")
    void fileCreateTransactionFeeEstimate() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("integration test file")
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.STATE)
                    .execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName(
            "Given a FileAppendTransaction spanning multiple chunks, when fee estimate is requested, then aggregated totals are returned")
    void fileAppendTransactionFeeEstimateAggregatesChunks() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new FileAppendTransaction()
                    .setFileId(FileId.fromString("0.0.1234"))
                    .setContents(new byte[5000])
                    .freezeWith(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.INTRINSIC)
                    .execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName(
            "Given a TopicMessageSubmitTransaction smaller than a chunk, when fee estimate is requested, then a single chunk estimate is returned")
    void topicMessageSubmitSingleChunkFeeEstimate() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new TopicMessageSubmitTransaction()
                    .setTopicId(TopicId.fromString("0.0.1234"))
                    .setMessage(new byte[128])
                    .freezeWith(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.INTRINSIC)
                    .execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName(
            "Given a TopicMessageSubmitTransaction larger than a chunk, when fee estimate is requested, then multi-chunk totals are aggregated")
    void topicMessageSubmitMultipleChunkFeeEstimate() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            var transaction = new TopicMessageSubmitTransaction()
                    .setTopicId(TopicId.fromString("0.0.1234"))
                    .setMessage(new byte[5000])
                    .freezeWith(testEnv.client);

            waitForMirrorNodeSync();

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.INTRINSIC)
                    .execute(testEnv.client);

            assertFeeComponentsPresent(response);
            assertComponentTotalsConsistent(response);
        }
    }

    @Test
    @DisplayName(
            "Given a FeeEstimateQuery with a malformed transaction, when the query is executed, then it returns an INVALID_ARGUMENT error and does not retry")
    void malformedTransactionReturnsInvalidArgumentError() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {

            // Given: A malformed transaction payload (invalid signed bytes)
            ByteString invalidBytes = ByteString.copyFrom(new byte[] {0x00, 0x01, 0x02, 0x03});
            var malformedTransaction = org.hiero.sdk.proto.Transaction.newBuilder()
                    .setSignedTransactionBytes(invalidBytes)
                    .build();

            waitForMirrorNodeSync();

            // When/Then: Executing the fee estimate query should throw INVALID_ARGUMENT
            assertThatThrownBy(() -> new FeeEstimateQuery()
                            .setTransaction(malformedTransaction)
                            .setMode(FeeEstimateMode.STATE)
                            .execute(testEnv.client))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("HTTP status");
        }
    }

    @Test
    @DisplayName("Given a FeeEstimateQuery without a transaction, when executed, then it throws an error")
    void queryWithoutTransactionThrowsError() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            assertThatThrownBy(() -> new FeeEstimateQuery()
                            .setMode(FeeEstimateMode.STATE)
                            .execute(testEnv.client))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("transaction must be set");
        }
    }

    @Test
    @Disabled
    @DisplayName(
            "Given a fee estimate is obtained, when transaction is executed, then actual fees are within reasonable range")
    void actualFeesMatchEstimateWithinTolerance() throws Throwable {
        try (var testEnv = createFeeEstimateTestEnv()) {
            // Create and freeze transaction
            var transaction = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, Hbar.fromTinybars(-1000))
                    .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.fromTinybars(1000))
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            // Get estimate
            var estimate = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.STATE)
                    .execute(testEnv.client);

            // Execute transaction
            var response = transaction.execute(testEnv.client);
            var receipt = response.getReceipt(testEnv.client);
            var record = response.getRecord(testEnv.client);

            long actualFee = record.transactionFee.toTinybars();
            long estimatedFee = estimate.getTotal();

            // Define tolerance (e.g., 20%)
            double tolerance = 0.20;
            long lowerBound = (long) (estimatedFee * (1 - tolerance));
            long upperBound = (long) (estimatedFee * (1 + tolerance));

            assertThat(actualFee)
                    .as("Actual fee should be within ±20%% of estimate")
                    .isBetween(lowerBound, upperBound);
        }
    }

    private static void waitForMirrorNodeSync() throws InterruptedException {
        Thread.sleep(MIRROR_SYNC_DELAY_MILLIS);
    }

    private static long subtotal(FeeEstimate estimate) {
        return estimate.getBase()
                + estimate.getExtras().stream().mapToLong(FeeExtra::getSubtotal).sum();
    }

    private static void assertFeeComponentsPresent(FeeEstimateResponse response) {
        // TODO adjust when NetworkService.getFeeEstimate has actual implementation
        assertThat(response).isNotNull();

        // Network fee validations
        assertThat(response.getNetwork()).isNotNull();
        assertThat(response.getNetwork().getMultiplier()).isGreaterThan(0);
        assertThat(response.getNetwork().getSubtotal()).isGreaterThanOrEqualTo(0);

        // Node fee validations
        assertThat(response.getNode()).isNotNull();
        assertThat(response.getNode().getBase()).isGreaterThanOrEqualTo(0);
        assertThat(response.getNode().getExtras()).isNotNull();

        // Service fee validations
        assertThat(response.getService()).isNotNull();
        assertThat(response.getService().getBase()).isGreaterThanOrEqualTo(0);
        assertThat(response.getService().getExtras()).isNotNull();

        // High volume multiplier and total
        assertThat(response.getHighVolumeMultiplier()).isGreaterThanOrEqualTo(1);
        assertThat(response.getTotal()).isGreaterThan(0);
    }

    private static void assertComponentTotalsConsistent(FeeEstimateResponse response) {
        // TODO adjust when NetworkService.getFeeEstimate has actual implementation
        var network = response.getNetwork();
        var node = response.getNode();
        var service = response.getService();

        var nodeSubtotal = subtotal(node);
        var serviceSubtotal = subtotal(service);

        assertThat(network.getSubtotal()).isEqualTo(nodeSubtotal * network.getMultiplier());
        assertThat(response.getTotal()).isEqualTo(network.getSubtotal() + nodeSubtotal + serviceSubtotal);
    }
}
