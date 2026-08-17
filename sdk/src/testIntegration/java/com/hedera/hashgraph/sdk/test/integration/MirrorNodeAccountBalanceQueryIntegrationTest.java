// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.MirrorNodeAccountBalanceQuery;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TransferTransaction;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration coverage for {@link MirrorNodeAccountBalanceQuery}, the mirror node REST replacement
 * for the removed {@code AccountBalanceQuery}.
 *
 * <p>The mirror node is eventually consistent, so every assertion on a balance that a transaction in
 * the same test produced goes through {@link IntegrationTestEnv#awaitMirrorBalance}.
 */
class MirrorNodeAccountBalanceQueryIntegrationTest {

    @Test
    @DisplayName("Can fetch the HBAR balance for the client operator")
    void canFetchBalanceForClientOperator() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var balance =
                    IntegrationTestEnv.awaitMirrorBalance(testEnv.client, testEnv.operatorId, b -> b.toTinybars() > 0);

            assertThat(balance.toTinybars()).isPositive();
        }
    }

    @Test
    @DisplayName("Can fetch the HBAR balance for an account addressed by its EVM address")
    void canFetchBalanceByEvmAddress() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateECDSA();
            var initialBalance = new Hbar(1);

            var accountId = Objects.requireNonNull(new AccountCreateTransaction()
                    .setInitialBalance(initialBalance)
                    .setKeyWithoutAlias(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId);

            var evmAddressAccountId = AccountId.fromEvmAddress("0x" + accountId.toEvmAddress());

            var balance = IntegrationTestEnv.awaitMirrorBalance(
                    testEnv.client, evmAddressAccountId, b -> b.equals(initialBalance));

            assertThat(balance).isEqualTo(initialBalance);
        }
    }

    @Test
    @DisplayName("A public key alias is not a supported form for the HBAR balance query")
    void aliasAddressedAccountIsRejected() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            var aliasAccountId = key.getPublicKey().toAccountId(0, 0);
            var initialBalance = new Hbar(1);

            // Transferring to an alias auto-creates the account.
            new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, initialBalance.negated())
                    .addHbarTransfer(aliasAccountId, initialBalance)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // The /balances endpoint takes the account as the `account.id` *query* parameter, which
            // accepts shard.realm.num and EVM addresses but rejects a public key alias with HTTP 400.
            // (MirrorNodeTokenBalanceQuery does support aliases because it puts the account in the
            // path, where the mirror node accepts idOrAliasOrEvmAddress.) Resolve the alias to a
            // number first — e.g. AccountId.populateAccountNum — if you need this lookup.
            assertThatThrownBy(() -> new MirrorNodeAccountBalanceQuery()
                            .setAccountId(aliasAccountId)
                            .setMaxAttempts(1)
                            .execute(testEnv.client))
                    .isInstanceOf(ExecutionException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("400");
        }
    }

    @Test
    @DisplayName("Can fetch the HBAR balance of a contract passed as an account id")
    void canFetchBalanceForContract() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var contractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            // The balances endpoint resolves contract IDs, so no separate setContractId is needed.
            var contractAsAccountId = new AccountId(contractId.shard, contractId.realm, contractId.num);

            // The test contract's constructor is not payable, so setInitialBalance would revert with
            // CONTRACT_REVERT_EXECUTED. Fund it with a plain crypto transfer instead.
            new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, new Hbar(1).negated())
                    .addHbarTransfer(contractAsAccountId, new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var balance =
                    IntegrationTestEnv.awaitMirrorBalance(testEnv.client, contractAsAccountId, b -> b.toTinybars() > 0);

            assertThat(balance).isEqualTo(new Hbar(1));
        }
    }

    @Test
    @DisplayName("Returns a zero balance for a non-existent account rather than failing")
    void returnsZeroForNonExistentAccount() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // The balances endpoint answers with an empty array, not a 404.
            var balance = new MirrorNodeAccountBalanceQuery()
                    .setAccountId(new AccountId(0, 0, 999_999_999L))
                    .execute(testEnv.client);

            assertThat(balance.hbars).isEqualTo(Hbar.ZERO);
        }
    }

    @Test
    @DisplayName("Fails before making a network call when no account id is set")
    void failsBeforeNetworkCallWhenAccountIdMissing() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> new MirrorNodeAccountBalanceQuery().execute(testEnv.client))
                    .withMessageContaining("accountId must be set");
        }
    }

    @Test
    @DisplayName("An id in a shard the mirror node does not serve reports zero rather than failing")
    void returnsZeroForUnservedShard() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // 1.0.3 is a well-formed entity id, so the mirror node accepts it and answers with an
            // empty balances array. Only a genuinely malformed string yields HTTP 400, which
            // AccountId.fromString rejects before a request is ever built — that path is covered by
            // MirrorNodeAccountBalanceQueryMockTest.doesNotRetryOn400 instead.
            var balance = new MirrorNodeAccountBalanceQuery()
                    .setAccountId(AccountId.fromString("1.0.3"))
                    .setMaxAttempts(1)
                    .execute(testEnv.client);

            assertThat(balance.hbars).isEqualTo(Hbar.ZERO);
        }
    }
}
