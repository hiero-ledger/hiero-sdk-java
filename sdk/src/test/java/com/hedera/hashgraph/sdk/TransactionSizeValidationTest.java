// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionSizeValidationTest {
    private static final AccountId NODE_ACCOUNT_ID = AccountId.fromString("0.0.3");
    private static final AccountId RECEIVER_ACCOUNT_ID = AccountId.fromString("0.0.1002");

    @Test
    void privilegedPayersAllowExtendedTransactionSize() {
        var transaction = createLargeSignedTransfer(AccountId.fromString("0.0.2"));

        assertDoesNotThrow(transaction::makeRequest);
        assertThat(transaction.getTransactionSize()).isGreaterThan(6 * 1024);
    }

    @Test
    void systemAdminAllowsExtendedTransactionSize() {
        var transaction = createLargeSignedTransfer(AccountId.fromString("0.0.50"));

        assertDoesNotThrow(transaction::makeRequest);
        assertThat(transaction.getTransactionSize()).isGreaterThan(6 * 1024);
    }

    @Test
    void standardPayersRespectDefaultSizeLimit() {
        var transaction = createLargeSignedTransfer(AccountId.fromString("0.0.7006"));

        assertThrows(IllegalStateException.class, transaction::makeRequest);
    }

    private TransferTransaction createLargeSignedTransfer(AccountId payer) {
        var transaction = new TransferTransaction()
                .addHbarTransfer(payer, new Hbar(-1))
                .addHbarTransfer(RECEIVER_ACCOUNT_ID, new Hbar(1))
                .setTransactionId(TransactionId.withValidStart(payer, Instant.ofEpochSecond(1L)))
                .setNodeAccountIds(List.of(NODE_ACCOUNT_ID))
                .freeze();

        for (int i = 0; i < 80; i++) {
            transaction.sign(PrivateKey.generateED25519());
        }

        return transaction;
    }
}
