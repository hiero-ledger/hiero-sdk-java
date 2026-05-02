// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;
import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.LiveHashAddTransaction;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LiveHashAddIntegrationTest {
    private static final byte[] HASH = Hex.decode(
            "100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002");

    @Test
    @DisplayName("Cannot create live hash because it's not supported")
    void cannotCreateLiveHashBecauseItsNotSupported() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateED25519();

            var response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client);

            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new LiveHashAddTransaction()
                                .setAccountId(accountId)
                                .setDuration(Duration.ofDays(30))
                                .setHash(HASH)
                                .setKeys(key)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.NOT_SUPPORTED.toString());
        }
    }
}
