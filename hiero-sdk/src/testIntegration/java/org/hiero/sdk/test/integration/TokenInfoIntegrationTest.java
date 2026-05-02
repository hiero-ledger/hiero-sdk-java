// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Objects;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.MaxQueryPaymentExceededException;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenDeleteTransaction;
import org.hiero.sdk.TokenInfoQuery;
import org.hiero.sdk.TokenMintTransaction;
import org.hiero.sdk.TokenSupplyType;
import org.hiero.sdk.TokenType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenInfoIntegrationTest {

    @Test
    @DisplayName("Can query token info when all keys are different")
    void canQueryTokenInfoWhenAllKeysAreDifferent() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var key1 = PrivateKey.generateED25519();
            var key2 = PrivateKey.generateED25519();
            var key3 = PrivateKey.generateED25519();
            var key4 = PrivateKey.generateED25519();
            var key5 = PrivateKey.generateED25519();
            var key6 = PrivateKey.generateED25519();
            var key7 = PrivateKey.generateED25519();

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(key1)
                    .setFreezeKey(key2)
                    .setWipeKey(key3)
                    .setKycKey(key4)
                    .setSupplyKey(key5)
                    .setPauseKey(key6)
                    .setMetadataKey(key7)
                    .setFreezeDefault(false)
                    .freezeWith(testEnv.client)
                    .sign(key1)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            var info = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(info.tokenId).isEqualTo(tokenId);
            assertThat(info.name).isEqualTo("ffff");
            assertThat(info.symbol).isEqualTo("F");
            assertThat(info.decimals).isEqualTo(3);
            assertThat(info.treasuryAccountId).isEqualTo(testEnv.operatorId);
            assertThat(info.adminKey).isNotNull();
            assertThat(info.freezeKey).isNotNull();
            assertThat(info.wipeKey).isNotNull();
            assertThat(info.kycKey).isNotNull();
            assertThat(info.supplyKey).isNotNull();
            assertThat(info.pauseKey).isNotNull();
            assertThat(info.metadataKey).isNotNull();
            assertThat(info.adminKey.toString()).isEqualTo(key1.getPublicKey().toString());
            assertThat(info.freezeKey.toString()).isEqualTo(key2.getPublicKey().toString());
            assertThat(info.wipeKey.toString()).isEqualTo(key3.getPublicKey().toString());
            assertThat(info.kycKey.toString()).isEqualTo(key4.getPublicKey().toString());
            assertThat(info.supplyKey.toString()).isEqualTo(key5.getPublicKey().toString());
            assertThat(info.pauseKey.toString()).isEqualTo(key6.getPublicKey().toString());
            assertThat(info.metadataKey.toString())
                    .isEqualTo(key7.getPublicKey().toString());
            assertThat(info.defaultFreezeStatus).isNotNull();
            assertThat(info.defaultFreezeStatus).isFalse();
            assertThat(info.defaultKycStatus).isNotNull();
            assertThat(info.defaultKycStatus).isFalse();
            assertThat(info.tokenType).isEqualTo(TokenType.FUNGIBLE_COMMON);
            assertThat(info.supplyType).isEqualTo(TokenSupplyType.INFINITE);

            new TokenDeleteTransaction()
                    .setTokenId(tokenId)
                    .freezeWith(testEnv.client)
                    .sign(key1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can query token with minimal properties")
    void canQueryTokenInfoWhenTokenIsCreatedWithMinimalProperties() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            var info = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(info.tokenId).isEqualTo(tokenId);
            assertThat(info.name).isEqualTo("ffff");
            assertThat(info.symbol).isEqualTo("F");
            assertThat(info.decimals).isEqualTo(0);
            assertThat(info.totalSupply).isEqualTo(0);
            assertThat(info.treasuryAccountId).isEqualTo(testEnv.operatorId);
            assertThat(info.adminKey).isNull();
            assertThat(info.freezeKey).isNull();
            assertThat(info.wipeKey).isNull();
            assertThat(info.kycKey).isNull();
            assertThat(info.supplyKey).isNull();
            assertThat(info.pauseKey).isNull();
            assertThat(info.metadataKey).isNull();
            assertThat(info.defaultFreezeStatus).isNull();
            assertThat(info.defaultKycStatus).isNull();
            assertThat(info.tokenType).isEqualTo(TokenType.FUNGIBLE_COMMON);
            assertThat(info.supplyType).isEqualTo(TokenSupplyType.INFINITE);
        }
    }

    @Test
    @DisplayName("Can query NFT")
    void canQueryNfts() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setSupplyType(TokenSupplyType.FINITE)
                    .setMaxSupply(5000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(tokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThat(mintReceipt.serials.size()).isEqualTo(10);

            var info = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(info.tokenId).isEqualTo(tokenId);
            assertThat(info.name).isEqualTo("ffff");
            assertThat(info.symbol).isEqualTo("F");
            assertThat(info.decimals).isEqualTo(0);
            assertThat(info.totalSupply).isEqualTo(10);
            assertThat(testEnv.operatorId).isEqualTo(info.treasuryAccountId);
            assertThat(info.adminKey).isNotNull();
            assertThat(info.freezeKey).isNull();
            assertThat(info.wipeKey).isNull();
            assertThat(info.kycKey).isNull();
            assertThat(info.supplyKey).isNotNull();
            assertThat(info.pauseKey).isNull();
            assertThat(info.metadataKey).isNull();
            assertThat(info.defaultFreezeStatus).isNull();
            assertThat(info.defaultKycStatus).isNull();
            assertThat(info.tokenType).isEqualTo(TokenType.NON_FUNGIBLE_UNIQUE);
            assertThat(info.supplyType).isEqualTo(TokenSupplyType.FINITE);
            assertThat(info.maxSupply).isEqualTo(5000);
        }
    }

    @Test
    @DisplayName("Get cost of token info query")
    void getCostQueryTokenInfo() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            var infoQuery = new TokenInfoQuery().setTokenId(tokenId);

            var cost = infoQuery.getCost(testEnv.client);

            infoQuery.setQueryPayment(cost).execute(testEnv.client);
        }
    }

    @Test
    @DisplayName("Get cost of token info query, with big max")
    void getCostBigMaxQueryTokenInfo() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            var infoQuery = new TokenInfoQuery().setTokenId(tokenId).setMaxQueryPayment(new Hbar(1000));

            var cost = infoQuery.getCost(testEnv.client);

            infoQuery.setQueryPayment(cost).execute(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can query token info when all keys are different")
    void getCostSmallMaxTokenInfo() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            var infoQuery = new TokenInfoQuery().setTokenId(tokenId).setMaxQueryPayment(Hbar.fromTinybars(1));

            assertThatExceptionOfType(MaxQueryPaymentExceededException.class).isThrownBy(() -> {
                infoQuery.execute(testEnv.client);
            });
        }
    }

    @Test
    @DisplayName("Throws insufficient transaction fee error")
    void getCostInsufficientTxFeeQueryTokenInfo() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            var infoQuery = new TokenInfoQuery().setTokenId(tokenId).setMaxQueryPayment(new Hbar(1000));

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        infoQuery.setQueryPayment(Hbar.fromTinybars(1)).execute(testEnv.client);
                    })
                    .satisfies(error -> assertThat(error.status.toString()).isEqualTo("INSUFFICIENT_TX_FEE"));
        }
    }
}
