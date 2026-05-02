// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.AccountRecordsQuery;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.TransferTransaction;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountRecordsIntegrationTest {
    @Test
    @DisplayName("Can query account records")
    void canQueryAccountRecords() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();

            var response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client);

            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);

            new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, new Hbar(1).negated())
                    .addHbarTransfer(accountId, new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, new Hbar(1))
                    .addHbarTransfer(accountId, new Hbar(1).negated())
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var records = new AccountRecordsQuery()
                    .setAccountId(testEnv.operatorId)
                    .setQueryPayment(new Hbar(10))
                    .execute(testEnv.client);

            assertThat(records.isEmpty()).isFalse();
        }
    }
}

