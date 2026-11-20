// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hedera.hashgraph.sdk.ClprGetEndpointManifestQuery;
import com.hedera.hashgraph.sdk.ClprGetLedgerConfigurationQuery;
import com.hedera.hashgraph.sdk.ClprRegisterChannelTransaction;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import java.security.SecureRandom;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HIP-1535 CLPR integration tests.
 * <p>
 * CLPR is off by default, so these tests are gated by the {@code CLPR_ENABLED} Gradle property.
 * Unset skips them all. {@code -PCLPR_ENABLED=true} runs the enabled tests against a node with
 * {@code clpr.enabled=true}. {@code -PCLPR_ENABLED=false} checks {@code CLPR_NOT_ENABLED} against a node
 * with {@code clpr.enabled=false}. The Channel and Connector reveal steps need real peer proof material,
 * contracts and keys, so they are covered by the CLPR lifecycle examples rather than here.
 */
class ClprIntegrationTest {
    private static final String CLPR_ENABLED = System.getProperty("CLPR_ENABLED", "");

    private static void assumeClprEnabled(boolean expected) {
        Assumptions.assumeTrue(
                String.valueOf(expected).equals(CLPR_ENABLED),
                "Set -PCLPR_ENABLED=" + expected + " to run this CLPR test");
    }

    private static byte[] randomChannelId() {
        var channelId = new byte[32];
        new SecureRandom().nextBytes(channelId);
        return channelId;
    }

    @Test
    @DisplayName("Can query the CLPR ledger configuration")
    void canQueryLedgerConfiguration() throws Exception {
        assumeClprEnabled(true);
        try (var testEnv = new IntegrationTestEnv(1)) {
            var info = new ClprGetLedgerConfigurationQuery().execute(testEnv.client);

            assertThat(info.getChainId()).isNotEmpty();
            assertThat(info.getThrottles().getMaxMessagesPerBundle()).isPositive();
        }
    }

    @Test
    @DisplayName("Can query the CLPR endpoint manifest")
    void canQueryEndpointManifest() throws Exception {
        assumeClprEnabled(true);
        try (var testEnv = new IntegrationTestEnv(1)) {
            var info = new ClprGetEndpointManifestQuery().execute(testEnv.client);

            assertThat(info.getEndpoints()).isNotNull();
            assertThat(info.getManifestStateProof()).isNotNull();
        }
    }

    @Test
    @DisplayName("Can register a CLPR Channel commitment")
    void canRegisterChannel() throws Exception {
        assumeClprEnabled(true);
        try (var testEnv = new IntegrationTestEnv(1)) {
            var commitment = ClprRegisterChannelTransaction.computeOwnershipCommitment(
                    randomChannelId(), PrivateKey.generateECDSA().getPublicKey());

            var receipt = new ClprRegisterChannelTransaction()
                    .setOwnershipCommitment(commitment)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName("Cannot register the same CLPR Channel commitment twice")
    void cannotRegisterChannelCommitmentTwice() throws Exception {
        assumeClprEnabled(true);
        try (var testEnv = new IntegrationTestEnv(1)) {
            var commitment = ClprRegisterChannelTransaction.computeOwnershipCommitment(
                    randomChannelId(), PrivateKey.generateED25519().getPublicKey());
            new ClprRegisterChannelTransaction()
                    .setOwnershipCommitment(commitment)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThatThrownBy(() -> new ClprRegisterChannelTransaction()
                            .setOwnershipCommitment(commitment)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .isInstanceOf(ReceiptStatusException.class)
                    .hasMessageContaining(Status.CLPR_CHANNEL_ALREADY_EXISTS.toString());
        }
    }

    @Test
    @DisplayName("CLPR queries fail with CLPR_NOT_ENABLED when CLPR is disabled")
    void queryFailsWhenClprIsDisabled() throws Exception {
        assumeClprEnabled(false);
        try (var testEnv = new IntegrationTestEnv(1)) {
            assertThatThrownBy(() -> new ClprGetLedgerConfigurationQuery().execute(testEnv.client))
                    .isInstanceOf(PrecheckStatusException.class)
                    .hasMessageContaining(Status.CLPR_NOT_ENABLED.toString());
        }
    }

    @Test
    @DisplayName("CLPR transactions fail with CLPR_NOT_ENABLED when CLPR is disabled")
    void transactionFailsWhenClprIsDisabled() throws Exception {
        assumeClprEnabled(false);
        try (var testEnv = new IntegrationTestEnv(1)) {
            var commitment = ClprRegisterChannelTransaction.computeOwnershipCommitment(
                    randomChannelId(), PrivateKey.generateECDSA().getPublicKey());

            // The node may reject the disabled service at precheck or at handle time.
            assertThatThrownBy(() -> new ClprRegisterChannelTransaction()
                            .setOwnershipCommitment(commitment)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .isInstanceOfAny(PrecheckStatusException.class, ReceiptStatusException.class)
                    .hasMessageContaining(Status.CLPR_NOT_ENABLED.toString());
        }
    }
}
