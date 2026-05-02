// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import org.hiero.sdk.proto.CryptoApproveAllowanceTransactionBody;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AccountAllowanceApproveTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final TokenId testTokenId = TokenId.fromString("1.2.3");
    private static final AccountId testOwnerAccountId = AccountId.fromString("4.5.7");
    private static final AccountId testSpenderAccountId = AccountId.fromString("8.9.0");

    final Instant validStart = Instant.ofEpochSecond(1554158542);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    AccountAllowanceApproveTransaction spawnTestTransaction() {
        var ownerId = AccountId.fromString("5.6.7");
        return new AccountAllowanceApproveTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .addHbarAllowance(AccountId.fromString("1.1.1"), new Hbar(3))
                .addTokenAllowance(TokenId.fromString("2.2.2"), AccountId.fromString("3.3.3"), 6)
                .addTokenNftAllowance(TokenId.fromString("4.4.4").nft(123), AccountId.fromString("5.5.5"))
                .addTokenNftAllowance(TokenId.fromString("4.4.4").nft(456), AccountId.fromString("5.5.5"))
                .addTokenNftAllowance(TokenId.fromString("8.8.8").nft(456), AccountId.fromString("5.5.5"))
                .addTokenNftAllowance(TokenId.fromString("4.4.4").nft(789), AccountId.fromString("9.9.9"))
                .addAllTokenNftAllowance(TokenId.fromString("6.6.6"), AccountId.fromString("7.7.7"))
                .approveHbarAllowance(ownerId, AccountId.fromString("1.1.1"), new Hbar(3))
                .approveTokenAllowance(TokenId.fromString("2.2.2"), ownerId, AccountId.fromString("3.3.3"), 6)
                .approveTokenNftAllowance(TokenId.fromString("4.4.4").nft(123), ownerId, AccountId.fromString("5.5.5"))
                .approveTokenNftAllowance(TokenId.fromString("4.4.4").nft(456), ownerId, AccountId.fromString("5.5.5"))
                .approveTokenNftAllowance(TokenId.fromString("8.8.8").nft(456), ownerId, AccountId.fromString("5.5.5"))
                .approveTokenNftAllowance(TokenId.fromString("4.4.4").nft(789), ownerId, AccountId.fromString("9.9.9"))
                .approveTokenNftAllowanceAllSerials(TokenId.fromString("6.6.6"), ownerId, AccountId.fromString("7.7.7"))
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = AccountAllowanceApproveTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void propertiesTest() {
        var tx = spawnTestTransaction();

        assertThat(tx.getHbarAllowances()).isNotEmpty();
        assertThat(tx.getHbarApprovals()).isNotEmpty();
        assertThat(tx.getTokenAllowances()).isNotEmpty();
        assertThat(tx.getTokenApprovals()).isNotEmpty();
        assertThat(tx.getTokenNftAllowances()).isNotEmpty();
        assertThat(tx.getTokenNftApprovals()).isNotEmpty();
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new AccountAllowanceApproveTransaction();
        var tx2 = AccountAllowanceApproveTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setCryptoApproveAllowance(
                        CryptoApproveAllowanceTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(AccountAllowanceApproveTransaction.class);
    }

    @Test
    void deleteNftAllowanceAllSerials() {
        var accountAllowanceApproveTransaction = new AccountAllowanceApproveTransaction()
                .deleteTokenNftAllowanceAllSerials(testTokenId, testOwnerAccountId, testSpenderAccountId);

        assertThat(accountAllowanceApproveTransaction.getTokenNftApprovals().size())
                .isEqualTo(1);
        assertThat(accountAllowanceApproveTransaction.getTokenNftApprovals().get(0).tokenId)
                .isEqualTo(testTokenId);
        assertThat(accountAllowanceApproveTransaction.getTokenNftApprovals().get(0).ownerAccountId)
                .isEqualTo(testOwnerAccountId);
        assertThat(accountAllowanceApproveTransaction.getTokenNftApprovals().get(0).spenderAccountId)
                .isEqualTo(testSpenderAccountId);
        assertTrue(accountAllowanceApproveTransaction
                .getTokenNftApprovals()
                .get(0)
                .serialNumbers
                .isEmpty());
        assertFalse(accountAllowanceApproveTransaction.getTokenNftApprovals().get(0).allSerials);
        assertNull(accountAllowanceApproveTransaction.getTokenNftApprovals().get(0).delegatingSpender);
    }

    @Test
    void deleteNftAllowanceAllSerialsFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(
                IllegalStateException.class,
                () -> tx.deleteTokenNftAllowanceAllSerials(testTokenId, testOwnerAccountId, testSpenderAccountId));
    }
}
