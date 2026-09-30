// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.MirrorNodeTokenBalanceQuery;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenMintTransaction;
import com.hedera.hashgraph.sdk.TransferTransaction;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration coverage for {@link MirrorNodeTokenBalanceQuery}, the token-balance counterpart to
 * {@link com.hedera.hashgraph.sdk.MirrorNodeAccountBalanceQuery}.
 *
 * <p>The mirror node is eventually consistent, so every assertion on a balance a transaction in the
 * same test produced goes through {@link IntegrationTestEnv#awaitMirrorTokenBalance}, and every
 * assertion that something did <i>not</i> happen goes through
 * {@link IntegrationTestEnv#assertTokenNotAssociated}.
 */
class MirrorNodeTokenBalanceQueryIntegrationTest {

    @Test
    @DisplayName("Reports the treasury's whole supply of a fungible token, with its decimals")
    void canFetchAFungibleBalance() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenId = EntityHelper.createFungibleToken(testEnv, 3);

            var balance = IntegrationTestEnv.awaitMirrorTokenBalance(
                    testEnv.client, testEnv.operatorId, tokenId, b -> b.isAssociated() && b.balance > 0);

            assertThat(balance.getTokenId()).isEqualTo(tokenId);
            assertThat(balance.getBalance()).isEqualTo(1_000_000);
            assertThat(balance.getDecimals()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("For a non-fungible token the balance is the number of NFTs held")
    void canFetchAnNftCount() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenId = EntityHelper.createNft(testEnv);

            new TokenMintTransaction()
                    .setTokenId(tokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 3))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            IntegrationTestEnv.assertTokenBalance(testEnv.client, testEnv.operatorId, tokenId, 3);
        }
    }

    @Test
    @DisplayName("An account that was never associated reports isAssociated() == false, not an error")
    void reportsNotAssociated() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenId = EntityHelper.createFungibleToken(testEnv, 0);
            var accountId = EntityHelper.createAccount(testEnv, PrivateKey.generateED25519(), 0);

            IntegrationTestEnv.assertTokenNotAssociated(testEnv.client, accountId, tokenId);
        }
    }

    @Test
    @DisplayName("An associated account with a zero balance is distinguishable from one never associated")
    void distinguishesAssociatedFromZero() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenId = EntityHelper.createFungibleToken(testEnv, 0);
            var accountKey = PrivateKey.generateED25519();
            var accountId = EntityHelper.createAccount(testEnv, accountKey, 0);

            new TokenAssociateTransaction()
                    .setAccountId(accountId)
                    .setTokenIds(Collections.singletonList(tokenId))
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var balance = IntegrationTestEnv.awaitMirrorTokenBalance(
                    testEnv.client, accountId, tokenId, b -> b.isAssociated());

            assertThat(balance.getBalance()).isZero();
            assertThat(balance.isAssociated()).isTrue();
        }
    }

    @Test
    @DisplayName("A transferred balance is reported once the mirror node catches up")
    void reportsATransferredBalance() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenId = EntityHelper.createFungibleToken(testEnv, 0);
            var accountId = EntityHelper.createAccount(testEnv, PrivateKey.generateED25519(), -1);

            new TransferTransaction()
                    .addTokenTransfer(tokenId, testEnv.operatorId, -10)
                    .addTokenTransfer(tokenId, accountId, 10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            IntegrationTestEnv.assertTokenBalance(testEnv.client, accountId, tokenId, 10);
        }
    }

    @Test
    @DisplayName("An account addressed by its EVM address resolves on the mirror node")
    void canFetchByEvmAddress() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenId = EntityHelper.createFungibleToken(testEnv, 0);
            var accountId = EntityHelper.createAccount(testEnv, PrivateKey.generateECDSA(), -1);

            new TransferTransaction()
                    .addTokenTransfer(tokenId, testEnv.operatorId, -5)
                    .addTokenTransfer(tokenId, accountId, 5)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var evmAddressAccountId = AccountId.fromEvmAddress("0x" + accountId.toEvmAddress());

            IntegrationTestEnv.assertTokenBalance(testEnv.client, evmAddressAccountId, tokenId, 5);
        }
    }

    @Test
    @DisplayName("An entity the mirror node does not know reads as 'no relationship' rather than failing")
    void reportsNotAssociatedForAnUnknownEntity() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenId = EntityHelper.createFungibleToken(testEnv, 0);

            var balance = new MirrorNodeTokenBalanceQuery()
                    .setAccountId(new AccountId(0, 0, 999_999_999L))
                    .setTokenId(tokenId)
                    .execute(testEnv.client);

            assertThat(balance.isAssociated()).isFalse();
            assertThat(balance.getBalance()).isZero();
        }
    }

    @Test
    @DisplayName("Both ids are required, and the query fails before any network call without them")
    void requiresBothIds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenId = EntityHelper.createFungibleToken(testEnv, 0);

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> new MirrorNodeTokenBalanceQuery()
                            .setTokenId(tokenId)
                            .execute(testEnv.client))
                    .withMessageContaining("accountId must be set");

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> new MirrorNodeTokenBalanceQuery()
                            .setAccountId(testEnv.operatorId)
                            .execute(testEnv.client))
                    .withMessageContaining("tokenId must be set");
        }
    }

    @Test
    @DisplayName("Reading several tokens for one account means one query each")
    void readsOneTokenAtATime() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var first = EntityHelper.createFungibleToken(testEnv, 0);
            var second = EntityHelper.createFungibleToken(testEnv, 0);

            for (TokenId tokenId : List.of(first, second)) {
                IntegrationTestEnv.assertTokenBalance(testEnv.client, testEnv.operatorId, tokenId, 1_000_000);
            }
        }
    }
}
