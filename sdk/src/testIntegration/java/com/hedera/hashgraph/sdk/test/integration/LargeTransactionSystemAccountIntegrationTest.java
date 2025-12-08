// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.*;

import java.util.Arrays;
import java.util.Objects;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LargeTransactionSystemAccountIntegrationTest {

    private static final int LARGE_CONTENT_SIZE_BYTES = 100 * 1024;
    private static final int SIZE_THRESHOLD_BYTES = 6 * 1024;
    private static final int EXTENDED_SIZE_LIMIT_BYTES = 130 * 1024;

    @Test
    @DisplayName("Privileged system payer can create files larger than 6kb")
    void privilegedSystemAccountCanCreateLargeFile() throws Exception {
        try (var testEnv = createSystemAccountTestEnv()) {
            var largeContents = new byte[LARGE_CONTENT_SIZE_BYTES];
            Arrays.fill(largeContents, (byte) 1);

            var transaction = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(largeContents)
                    .setTransactionMemo("HIP-1300 create large file")
                    .setMaxTransactionFee(new Hbar(20));

            transaction.freezeWith(testEnv.client);

            var serialized = transaction.toBytes();
            assertThat(serialized.length).isGreaterThan(SIZE_THRESHOLD_BYTES);
            assertThat(serialized.length).isLessThan(EXTENDED_SIZE_LIMIT_BYTES);

            var receipt = transaction.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(receipt.fileId).isNotNull();

            new FileDeleteTransaction()
                    .setFileId(Objects.requireNonNull(receipt.fileId))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Privileged system payer can update files with contents larger than 6kb")
    void privilegedSystemAccountCanUpdateLargeFile() throws Exception {
        try (var testEnv = createSystemAccountTestEnv()) {
            var initialContents = "test".getBytes();
            var fileId = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(initialContents)
                    .setTransactionMemo("HIP-1300 initial file")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .fileId;

            Objects.requireNonNull(fileId);

            var updatedContents = new byte[LARGE_CONTENT_SIZE_BYTES];
            Arrays.fill(updatedContents, (byte) 2);

            var transaction = new FileUpdateTransaction()
                    .setFileId(fileId)
                    .setContents(updatedContents)
                    .setTransactionMemo("HIP-1300 update large file")
                    .setMaxTransactionFee(new Hbar(20));

            transaction.freezeWith(testEnv.client);

            var serialized = transaction.toBytes();
            assertThat(serialized.length).isGreaterThan(SIZE_THRESHOLD_BYTES);
            assertThat(serialized.length).isLessThan(EXTENDED_SIZE_LIMIT_BYTES);

            transaction.execute(testEnv.client).getReceipt(testEnv.client);

            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Privileged system payer can append to file making it larger than 6kb")
    void privilegedSystemAccountCanAppendLargeFile() throws Exception {
        try (var testEnv = createSystemAccountTestEnv()) {
            // 1.  Create a small file
            var fileId = new FileCreateTransaction()
                .setKeys(testEnv.operatorKey)
                .setContents("start".getBytes())
                .execute(testEnv.client)
                .getReceipt(testEnv.client)
                .fileId;

            Objects.requireNonNull(fileId);

            // 2.  Append large content - need to set max chunks for content over default limit
            var largeContents = new byte[LARGE_CONTENT_SIZE_BYTES];
            Arrays.fill(largeContents, (byte) 3);

            var transaction = new FileAppendTransaction()
                .setFileId(fileId)
                .setContents(largeContents)
                .setMaxChunks(100)
                .setMaxTransactionFee(new Hbar(20))
                .setTransactionMemo("HIP-1300 append large file");

            transaction.freezeWith(testEnv.client);
            assertThat(transaction.toBytes().length).isLessThan(EXTENDED_SIZE_LIMIT_BYTES);
            assertThat(transaction.toBytes().length).isGreaterThan(SIZE_THRESHOLD_BYTES);
            transaction.execute(testEnv.client).getReceipt(testEnv.client);

            new FileDeleteTransaction()
                .setFileId(fileId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        }
    }


    @Test
    @DisplayName("Non-privileged account cannot create files larger than 6kb")
    void nonPrivilegedAccountCannotCreateLargeFile() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount(new Hbar(50))) {
            // useThrowawayAccount creates a non-privileged account for testing
            // This ensures the test runs even if OPERATOR_ID is 0.0.2 or 0.0.50

            var largeContents = new byte[LARGE_CONTENT_SIZE_BYTES];
            Arrays.fill(largeContents, (byte) 1);

            var transaction = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(largeContents)
                    .setTransactionMemo("Should fail - too large for non-privileged")
                    .setMaxTransactionFee(new Hbar(20));

            var exception = assertThrows(PrecheckStatusException.class, () -> {
                transaction.freezeWith(testEnv.client);
                transaction.execute(testEnv.client);
            });

            assertThat(exception.status).isEqualTo(Status.TRANSACTION_OVERSIZE);
        }
    }

    @Test
    @DisplayName("Non-privileged account cannot create account with large KeyList larger than 6kb")
    void nonPrivilegedAccountCannotCreateAccountWithLargeKeyList() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount(new Hbar(50))) {
            // useThrowawayAccount creates a non-privileged account for testing
            // This ensures the test runs even if OPERATOR_ID is 0.0.2 or 0.0.50

            // Generate 180 key pairs to ensure transaction size exceeds 6KB
            var numberOfKeys = 180;
            var publicKeys = new PublicKey[numberOfKeys];

            for (int i = 0; i < numberOfKeys; i++) {
                var key = PrivateKey.generateED25519();
                publicKeys[i] = key.getPublicKey();
            }

            // Create a KeyList with all public keys
            var keyList = new KeyList();
            for (var publicKey : publicKeys) {
                keyList.add(publicKey);
            }

            var transaction = new AccountCreateTransaction()
                    .setKeyWithoutAlias(keyList)
                    .setInitialBalance(new Hbar(1))
                    .setTransactionMemo("Should fail - too large for non-privileged")
                    .setMaxTransactionFee(new Hbar(20));

            var exception = assertThrows(PrecheckStatusException.class, () -> {
                transaction.freezeWith(testEnv.client);
                transaction.execute(testEnv.client);
            });

            assertThat(exception.status).isEqualTo(Status.TRANSACTION_OVERSIZE);
        }
    }

    @Test
    @DisplayName("Privileged account can create file just under 130kb limit")
    void privilegedAccountAtNear130KBLimit() throws Exception {
        try (var testEnv = createSystemAccountTestEnv()) {
            // Create content that will result in transaction just under 130KB
            var contentSize = 128 * 1024; // 128KB content
            var largeContents = new byte[contentSize];
            Arrays.fill(largeContents, (byte) 1);

            var transaction = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(largeContents)
                    .setTransactionMemo("HIP-1300 near limit test")
                    .setMaxTransactionFee(new Hbar(20));

            transaction.freezeWith(testEnv.client);

            var serialized = transaction.toBytes();
            // Should be less than 130KB total
            assertThat(serialized.length).isLessThan(EXTENDED_SIZE_LIMIT_BYTES);
            // But definitely over 6KB
            assertThat(serialized.length).isGreaterThan(SIZE_THRESHOLD_BYTES);

            var receipt = transaction.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(receipt.fileId).isNotNull();

            new FileDeleteTransaction()
                    .setFileId(Objects.requireNonNull(receipt.fileId))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Non-privileged account can create file under 6kb limit")
    void nonPrivilegedAccountCanCreateSmallFile() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount(new Hbar(50))) {
            // useThrowawayAccount creates a non-privileged account for testing
            // This ensures the test runs even if OPERATOR_ID is 0.0.2 or 0.0.50

            // Small content that stays well under 6KB
            var smallContents = new byte[2 * 1024]; // 2KB
            Arrays.fill(smallContents, (byte) 1);

            var transaction = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(smallContents)
                    .setTransactionMemo("Small file test")
                    .setMaxTransactionFee(new Hbar(5));

            transaction.freezeWith(testEnv.client);

            var serialized = transaction.toBytes();
            assertThat(serialized.length).isLessThan(SIZE_THRESHOLD_BYTES);

            var receipt = transaction.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(receipt.fileId).isNotNull();

            new FileDeleteTransaction()
                    .setFileId(Objects.requireNonNull(receipt.fileId))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Treasury account (0.0.2) can create large files")
    void treasuryAccountCanCreateLargeFile() throws Exception {
        try (var testEnv = createSystemAccountTestEnv()) {
            // This test specifically validates 0.0.2 if that's the operator
            Assumptions.assumeTrue(testEnv.operatorId.num == 2, "Test requires treasury account 0.0.2");

            var largeContents = new byte[LARGE_CONTENT_SIZE_BYTES];
            Arrays.fill(largeContents, (byte) 1);

            var transaction = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(largeContents)
                    .setTransactionMemo("HIP-1300 test with 0.0.2")
                    .setMaxTransactionFee(new Hbar(20));

            transaction.freezeWith(testEnv.client);

            var serialized = transaction.toBytes();
            assertThat(serialized.length).isGreaterThan(SIZE_THRESHOLD_BYTES);
            assertThat(serialized.length).isLessThan(EXTENDED_SIZE_LIMIT_BYTES);

            var receipt = transaction.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(receipt.fileId).isNotNull();

            new FileDeleteTransaction()
                    .setFileId(Objects.requireNonNull(receipt.fileId))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Privileged system payer can create account with large KeyList (180 keys)")
    void privilegedSystemAccountCanCreateAccountWithLargeKeyList() throws Exception {
        try (var testEnv = createSystemAccountTestEnv()) {
            // Generate 180 key pairs to ensure transaction size exceeds 6KB
            // With 100 keys we got ~3709 bytes, so 180 keys should give us ~6676 bytes
            var numberOfKeys = 180;
            var privateKeys = new PrivateKey[numberOfKeys];
            var publicKeys = new PublicKey[numberOfKeys];

            for (int i = 0; i < numberOfKeys; i++) {
                var key = PrivateKey.generateED25519();
                privateKeys[i] = key;
                publicKeys[i] = key.getPublicKey();
            }

            // Create a KeyList with all public keys
            var keyList = new KeyList();
            for (var publicKey : publicKeys) {
                keyList.add(publicKey);
            }

            var transaction = new AccountCreateTransaction()
                    .setKeyWithoutAlias(keyList)
                    .setInitialBalance(new Hbar(1))
                    .setTransactionMemo("HIP-1300 create account with large KeyList")
                    .setMaxTransactionFee(new Hbar(20));

            transaction.freezeWith(testEnv.client);

            var serialized = transaction.toBytes();
            assertThat(serialized.length).isGreaterThan(SIZE_THRESHOLD_BYTES);
            assertThat(serialized.length).isLessThan(EXTENDED_SIZE_LIMIT_BYTES);

            var receipt = transaction.execute(testEnv.client).getReceipt(testEnv.client);
            var accountId = Objects.requireNonNull(receipt.accountId);
            assertThat(accountId).isNotNull();

            // Cleanup: Delete the account (requires all keys in the KeyList to sign)
            var deleteTransaction = new AccountDeleteTransaction()
                    .setAccountId(accountId)
                    .setTransferAccountId(testEnv.operatorId)
                    .freezeWith(testEnv.client);

            // Sign with all private keys (required for simple KeyList)
            for (var privateKey : privateKeys) {
                deleteTransaction.sign(privateKey);
            }

            deleteTransaction.execute(testEnv.client).getReceipt(testEnv.client);
        }
    }

    private IntegrationTestEnv createSystemAccountTestEnv() throws Exception {
        var testEnv = new IntegrationTestEnv(1);

        if (!isPrivilegedSystemAccount(testEnv.operatorId)) {
            var systemAccountId = System.getProperty("SYSTEM_ACCOUNT_ID");
            var systemAccountKey = System.getProperty("SYSTEM_ACCOUNT_KEY");

            if (systemAccountId != null
                    && !systemAccountId.isBlank()
                    && systemAccountKey != null
                    && !systemAccountKey.isBlank()) {
                var accountId = AccountId.fromString(systemAccountId);
                var privateKey = PrivateKey.fromString(systemAccountKey);
                testEnv.client.setOperator(accountId, privateKey);
                testEnv.operatorId = accountId;
                testEnv.operatorKey = privateKey.getPublicKey();
            }
        }

        Assumptions.assumeTrue(
                isPrivilegedSystemAccount(testEnv.operatorId),
                "System account credentials (0.0.2 or 0.0.50) are required for large transaction tests");

        return testEnv;
    }

    private boolean isPrivilegedSystemAccount(AccountId accountId) {
        return accountId != null
                && accountId.shard == 0
                && accountId.realm == 0
                && (accountId.num == 2 || accountId.num == 50);
    }
}
