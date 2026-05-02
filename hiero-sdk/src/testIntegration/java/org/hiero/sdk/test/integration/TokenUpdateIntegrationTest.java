// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Objects;
import org.hiero.sdk.KeyList;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.PublicKey;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.Status;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenInfoQuery;
import org.hiero.sdk.TokenKeyValidation;
import org.hiero.sdk.TokenType;
import org.hiero.sdk.TokenUpdateTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenUpdateIntegrationTest {

    @Test
    @DisplayName("Can update token")
    void canUpdateToken() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setFreezeKey(testEnv.operatorKey)
                    .setWipeKey(testEnv.operatorKey)
                    .setKycKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setPauseKey(testEnv.operatorKey)
                    .setMetadataKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
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
            assertThat(info.adminKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.freezeKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.wipeKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.kycKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.supplyKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.pauseKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.metadataKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.defaultFreezeStatus).isNotNull().isFalse();
            assertThat(info.defaultKycStatus).isNotNull().isFalse();

            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenName("aaaa")
                    .setTokenSymbol("A")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            info = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(info.tokenId).isEqualTo(tokenId);
            assertThat(info.name).isEqualTo("aaaa");
            assertThat(info.symbol).isEqualTo("A");
            assertThat(info.decimals).isEqualTo(3);
            assertThat(info.treasuryAccountId).isEqualTo(testEnv.operatorId);
            assertThat(info.adminKey).isNotNull();
            assertThat(info.freezeKey).isNotNull();
            assertThat(info.wipeKey).isNotNull();
            assertThat(info.kycKey).isNotNull();
            assertThat(info.supplyKey).isNotNull();
            assertThat(info.adminKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.freezeKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.wipeKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.kycKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.supplyKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.pauseKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.metadataKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.defaultFreezeStatus).isNotNull();
            assertThat(info.defaultFreezeStatus).isFalse();
            assertThat(info.defaultKycStatus).isNotNull();
            assertThat(info.defaultKycStatus).isFalse();
        }
    }

    @Test
    @DisplayName("Cannot update immutable token")
    void cannotUpdateImmutableToken() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setFreezeDefault(false)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setTokenName("aaaa")
                                .setTokenSymbol("A")
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_IMMUTABLE.toString());
        }
    }

    /**
     * @notice E2E-HIP-646
     * @url https://hips.hedera.com/hip/hip-646
     */
    @Test
    @DisplayName("Can update a fungible token's metadata")
    void canUpdateFungibleTokenMetadata() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};
            var updatedTokenMetadata = new byte[] {2, 2, 2, 2, 2};

            // create a fungible token with metadata
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.FUNGIBLE_COMMON)
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoAfterCreation =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterCreation.metadata).isEqualTo(initialTokenMetadata);

            // update token's metadata
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenMetadata(updatedTokenMetadata)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterMetadataUpdate =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterMetadataUpdate.metadata).isEqualTo(updatedTokenMetadata);
        }
    }

    /**
     * @notice E2E-HIP-765
     * @url https://hips.hedera.com/hip/hip-765
     */
    @Test
    @DisplayName("Can update a non fungible token's metadata")
    void canUpdateNonFungibleTokenMetadata() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};
            var updatedTokenMetadata = new byte[] {2, 2, 2, 2, 2};

            // create a non fungible token with metadata
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoAfterCreation =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterCreation.metadata).isEqualTo(initialTokenMetadata);

            // update token's metadata
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenMetadata(updatedTokenMetadata)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterMetadataUpdate =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterMetadataUpdate.metadata).isEqualTo(updatedTokenMetadata);
        }
    }

    /**
     * @notice E2E-HIP-646
     * @url https://hips.hedera.com/hip/hip-646
     */
    @Test
    @DisplayName("Can update an immutable fungible token's metadata")
    void canUpdateImmutableFungibleTokenMetadata() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};
            var updatedTokenMetadata = new byte[] {2, 2, 2, 2, 2};
            var metadataKey = PrivateKey.generateED25519();

            // create a fungible token with metadata and metadata key
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.FUNGIBLE_COMMON)
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setMetadataKey(metadataKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoAfterCreation =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterCreation.metadata).isEqualTo(initialTokenMetadata);
            assertThat(tokenInfoAfterCreation.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // update token's metadata
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenMetadata(updatedTokenMetadata)
                    .freezeWith(testEnv.client)
                    .sign(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterMetadataUpdate =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterMetadataUpdate.metadata).isEqualTo(updatedTokenMetadata);
        }
    }

    /**
     * @notice E2E-HIP-765
     * @url https://hips.hedera.com/hip/hip-765
     */
    @Test
    @DisplayName("Can update an immutable non fungible token's metadata")
    void canUpdateImmutableNonFungibleTokenMetadata() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};
            var updatedTokenMetadata = new byte[] {2, 2, 2, 2, 2};
            var metadataKey = PrivateKey.generateED25519();

            // create a non fungible token with metadata and metadata key
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setSupplyKey(testEnv.operatorKey)
                    .setMetadataKey(metadataKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoAfterCreation =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterCreation.metadata).isEqualTo(initialTokenMetadata);
            assertThat(tokenInfoAfterCreation.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // update token's metadata
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenMetadata(updatedTokenMetadata)
                    .freezeWith(testEnv.client)
                    .sign(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterMetadataUpdate =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterMetadataUpdate.metadata).isEqualTo(updatedTokenMetadata);
        }
    }

    /**
     * @notice E2E-HIP-646
     * @url https://hips.hedera.com/hip/hip-646
     */
    @Test
    @DisplayName("Cannot update a fungible token with metadata when it is not set")
    void cannotUpdateFungibleTokenMetadataWhenItsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};

            // create a fungible token with metadata
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.FUNGIBLE_COMMON)
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoAfterCreation =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterCreation.metadata).isEqualTo(initialTokenMetadata);

            // update token, but don't update metadata
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenMemo("abc")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterMemoUpdate =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterMemoUpdate.metadata).isEqualTo(initialTokenMetadata);
        }
    }

    /**
     * @notice E2E-HIP-765
     * @url https://hips.hedera.com/hip/hip-765
     */
    @Test
    @DisplayName("Cannot update a non fungible token with metadata when it is not set")
    void cannotUpdateNonFungibleTokenMetadataWhenItsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};

            // create a non fungible token with metadata
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoAfterCreation =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterCreation.metadata).isEqualTo(initialTokenMetadata);

            // update token, but don't update metadata
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenMemo("abc")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterMemoUpdate =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterMemoUpdate.metadata).isEqualTo(initialTokenMetadata);
        }
    }

    /**
     * @notice E2E-HIP-646
     * @url https://hips.hedera.com/hip/hip-646
     */
    @Test
    @DisplayName("Can erase fungible token metadata")
    void canEraseFungibleTokenMetadata() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};
            var emptyTokenMetadata = new byte[] {};

            // create a fungible token with metadata
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.FUNGIBLE_COMMON)
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoAfterCreation =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterCreation.metadata).isEqualTo(initialTokenMetadata);

            // erase token metadata (update token with empty metadata)
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenMetadata(emptyTokenMetadata)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterSettingEmptyMetadata =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterSettingEmptyMetadata.metadata).isEqualTo(emptyTokenMetadata);
        }
    }

    /**
     * @notice E2E-HIP-765
     * @url https://hips.hedera.com/hip/hip-765
     */
    @Test
    @DisplayName("Can erase non fungible token metadata")
    void canEraseNonFungibleTokenMetadata() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};
            var emptyTokenMetadata = new byte[] {};

            // create a non fungible token with metadata
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoAfterCreation =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterCreation.metadata).isEqualTo(initialTokenMetadata);

            // erase token metadata (update token with empty metadata)
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenMetadata(emptyTokenMetadata)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterSettingEmptyMetadata =
                    new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterSettingEmptyMetadata.metadata).isEqualTo(emptyTokenMetadata);
        }
    }

    /**
     * @notice E2E-HIP-646
     * @url https://hips.hedera.com/hip/hip-646
     */
    @Test
    @DisplayName(
            "Cannot update a fungible token with metadata when transaction is not signed with an admin or a metadata key")
    void cannotUpdateFungibleTokenMetadataWhenTransactionIsNotSignedWithMetadataKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};
            var updatedTokenMetadata = new byte[] {2, 2, 2, 2, 2};
            var adminKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // create a fungible token with metadata and metadata key
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.FUNGIBLE_COMMON)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setAdminKey(adminKey)
                    .setMetadataKey(metadataKey)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setTokenMetadata(updatedTokenMetadata)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    /**
     * @notice E2E-HIP-765
     * @url https://hips.hedera.com/hip/hip-765
     */
    @Test
    @DisplayName(
            "Cannot update a non fungible token with metadata when transaction is not signed with an admin or a metadata key")
    void cannotUpdateNonFungibleTokenMetadataWhenTransactionIsNotSignedWithMetadataKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};
            var updatedTokenMetadata = new byte[] {2, 2, 2, 2, 2};
            var adminKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // create a non fungible token with metadata and metadata key
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(adminKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setMetadataKey(metadataKey)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setTokenMetadata(updatedTokenMetadata)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    /**
     * @notice E2E-HIP-646
     * @url https://hips.hedera.com/hip/hip-646
     */
    @Test
    @DisplayName("Cannot update a fungible token with metadata when admin and metadata keys are not set")
    void cannotUpdateFungibleTokenMetadataWhenMetadataKeyNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};
            var updatedTokenMetadata = new byte[] {2, 2, 2, 2, 2};

            // create a fungible token with metadata and without a metadata key and admin key
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.FUNGIBLE_COMMON)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setTokenMetadata(updatedTokenMetadata)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_IMMUTABLE.toString());
        }
    }

    /**
     * @notice E2E-HIP-765
     * @url https://hips.hedera.com/hip/hip-765
     */
    @Test
    @DisplayName("Cannot update a non fungible token with metadata when admin and metadata keys are not set")
    void cannotUpdateNonFungibleTokenMetadataWhenMetadataKeyNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};
            var updatedTokenMetadata = new byte[] {2, 2, 2, 2, 2};

            // create a non fungible token with metadata and without a metadata key and admin key
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenMetadata(initialTokenMetadata)
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setSupplyKey(testEnv.operatorKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setTokenMetadata(updatedTokenMetadata)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_IMMUTABLE.toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Can make a token immutable when updating keys to an empty KeyList, signing with an Admin Key, and setting the key verification mode to NO_VALIDATION")
    void canMakeTokenImmutableWhenUpdatingKeysToEmptyKeyListSigningWithAdminKeyWithKeyVerificationSetToNoValidation()
            throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Admin, Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var adminKey = PrivateKey.generateED25519();
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(adminKey.getPublicKey())
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.adminKey.toString())
                    .isEqualTo(adminKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            var emptyKeyList = new KeyList();

            // Make a token immutable by removing all of its keys when updating them to an empty KeyList,
            // signing with an Admin Key, and setting the key verification mode to NO_VALIDATION
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setWipeKey(emptyKeyList)
                    .setKycKey(emptyKeyList)
                    .setFreezeKey(emptyKeyList)
                    .setPauseKey(emptyKeyList)
                    .setSupplyKey(emptyKeyList)
                    .setFeeScheduleKey(emptyKeyList)
                    .setMetadataKey(emptyKeyList)
                    .setAdminKey(emptyKeyList)
                    .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterUpdate.adminKey).isNull();
            assertThat(tokenInfoAfterUpdate.wipeKey).isNull();
            assertThat(tokenInfoAfterUpdate.kycKey).isNull();
            assertThat(tokenInfoAfterUpdate.freezeKey).isNull();
            assertThat(tokenInfoAfterUpdate.pauseKey).isNull();
            assertThat(tokenInfoAfterUpdate.supplyKey).isNull();
            assertThat(tokenInfoAfterUpdate.feeScheduleKey).isNull();
            assertThat(tokenInfoAfterUpdate.metadataKey).isNull();
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Can remove all of token’s lower-privilege keys when updating keys to an empty KeyList, signing with an Admin Key, and setting the key verification mode to FULL_VALIDATION")
    void
            canRemoveAllLowerPrivilegeKeysWhenUpdatingKeysToEmptyKeyListSigningWithAdminKeyWithKeyVerificationSetToFullValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Admin, Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var adminKey = PrivateKey.generateED25519();
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(adminKey.getPublicKey())
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.adminKey.toString())
                    .isEqualTo(adminKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            var emptyKeyList = new KeyList();

            // Remove all of token’s lower-privilege keys when updating them to an empty KeyList,
            // signing with an Admin Key, and setting the key verification mode to FULL_VALIDATION
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setWipeKey(emptyKeyList)
                    .setKycKey(emptyKeyList)
                    .setFreezeKey(emptyKeyList)
                    .setPauseKey(emptyKeyList)
                    .setSupplyKey(emptyKeyList)
                    .setFeeScheduleKey(emptyKeyList)
                    .setMetadataKey(emptyKeyList)
                    .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterUpdate.wipeKey).isNull();
            assertThat(tokenInfoAfterUpdate.kycKey).isNull();
            assertThat(tokenInfoAfterUpdate.freezeKey).isNull();
            assertThat(tokenInfoAfterUpdate.pauseKey).isNull();
            assertThat(tokenInfoAfterUpdate.supplyKey).isNull();
            assertThat(tokenInfoAfterUpdate.feeScheduleKey).isNull();
            assertThat(tokenInfoAfterUpdate.metadataKey).isNull();
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Can update all of token’s lower-privilege keys to an unusable key (i.e. all-zeros key), when signing with an Admin Key, and setting the key verification mode to FULL_VALIDATION, and then revert previous keys")
    void
            canUpdateAllLowerPrivilegeKeysToUnusableKeyWhenSigningWithAdminKeyWithKeyVerificationSetToFullValidationAndThenRevertPreviousKeys()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Admin, Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var adminKey = PrivateKey.generateED25519();
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(adminKey.getPublicKey())
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.adminKey.toString())
                    .isEqualTo(adminKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // Update all of token’s lower-privilege keys to an unusable key (i.e., all-zeros key),
            // signing with an Admin Key, and setting the key verification mode to FULL_VALIDATION
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setWipeKey(PublicKey.unusableKey())
                    .setKycKey(PublicKey.unusableKey())
                    .setFreezeKey(PublicKey.unusableKey())
                    .setPauseKey(PublicKey.unusableKey())
                    .setSupplyKey(PublicKey.unusableKey())
                    .setFeeScheduleKey(PublicKey.unusableKey())
                    .setMetadataKey(PublicKey.unusableKey())
                    .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterUpdate.wipeKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.kycKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.freezeKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.pauseKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.supplyKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.feeScheduleKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.metadataKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());

            // Set all lower-privilege keys back by signing with an Admin Key,
            // and setting key verification mode to NO_VALIDATION
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterRevert = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterRevert.adminKey.toString())
                    .isEqualTo(adminKey.getPublicKey().toString());
            assertThat(tokenInfoAfterRevert.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoAfterRevert.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoAfterRevert.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoAfterRevert.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoAfterRevert.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoAfterRevert.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoAfterRevert.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Can update all of token’s lower-privilege keys when signing with an Admin Key and new respective lower-privilege key, and setting key verification mode to FULL_VALIDATION")
    void
            canUpdateAllLowerPrivilegeKeysWhenSigningWithAdminKeyAndNewLowerPrivilegeKeyWithKeyVerificationSetToFullValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Admin, Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var adminKey = PrivateKey.generateED25519();
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // New Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var newWipeKey = PrivateKey.generateED25519();
            var newKycKey = PrivateKey.generateED25519();
            var newFreezeKey = PrivateKey.generateED25519();
            var newPauseKey = PrivateKey.generateED25519();
            var newSupplyKey = PrivateKey.generateED25519();
            var newFeeScheduleKey = PrivateKey.generateED25519();
            var newMetadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(adminKey.getPublicKey())
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.adminKey.toString())
                    .isEqualTo(adminKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // Update all of token’s lower-privilege keys when signing with an Admin Key and new respective
            // lower-privilege key,
            // and setting key verification mode to FULL_VALIDATION
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setWipeKey(newWipeKey.getPublicKey())
                    .setKycKey(newKycKey.getPublicKey())
                    .setFreezeKey(newFreezeKey.getPublicKey())
                    .setPauseKey(newPauseKey.getPublicKey())
                    .setSupplyKey(newSupplyKey.getPublicKey())
                    .setFeeScheduleKey(newFeeScheduleKey.getPublicKey())
                    .setMetadataKey(newMetadataKey.getPublicKey())
                    .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .sign(newWipeKey)
                    .sign(newKycKey)
                    .sign(newFreezeKey)
                    .sign(newPauseKey)
                    .sign(newSupplyKey)
                    .sign(newFeeScheduleKey)
                    .sign(newMetadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterUpdate.wipeKey.toString())
                    .isEqualTo(newWipeKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.kycKey.toString())
                    .isEqualTo(newKycKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.freezeKey.toString())
                    .isEqualTo(newFreezeKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.pauseKey.toString())
                    .isEqualTo(newPauseKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.supplyKey.toString())
                    .isEqualTo(newSupplyKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.feeScheduleKey.toString())
                    .isEqualTo(newFeeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.metadataKey.toString())
                    .isEqualTo(newMetadataKey.getPublicKey().toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Cannot make a token immutable when updating keys to an empty KeyList, signing with a key that is different from an Admin Key, and setting the key verification mode to NO_VALIDATION")
    void
            cannotMakeTokenImmutableWhenUpdatingKeysToEmptyKeyListSigningWithDifferentKeyWithKeyVerificationSetToNoValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Admin, Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var adminKey = PrivateKey.generateED25519();
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(adminKey.getPublicKey())
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.adminKey.toString())
                    .isEqualTo(adminKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            var emptyKeyList = new KeyList();

            // Make the token immutable when updating all of its keys to an empty KeyList
            // (trying to remove keys one by one to check all errors),
            // signing with a key that is different from an Admin Key (implicitly with an operator key),
            // and setting the key verification mode to NO_VALIDATION
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setWipeKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setKycKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFreezeKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setPauseKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setSupplyKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFeeScheduleKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setMetadataKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setAdminKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Cannot make a token immutable when updating keys to an unusable key (i.e. all-zeros key), signing with a key that is different from an Admin Key, and setting the key verification mode to NO_VALIDATION")
    void
            cannotMakeTokenImmutableWhenUpdatingKeysToUnusableKeySigningWithDifferentKeyWithKeyVerificationSetToNoValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Admin, Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var adminKey = PrivateKey.generateED25519();
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(adminKey.getPublicKey())
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.adminKey.toString())
                    .isEqualTo(adminKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // Make the token immutable when updating all of its keys to an unusable key (i.e. all-zeros key)
            // (trying to remove keys one by one to check all errors),
            // signing with a key that is different from an Admin Key (implicitly with an operator key),
            // and setting the key verification mode to NO_VALIDATION
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setWipeKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setKycKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFreezeKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setPauseKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setSupplyKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFeeScheduleKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setMetadataKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setAdminKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Cannot update the Admin Key to an unusable key (i.e. all-zeros key), signing with an Admin Key, and setting the key verification mode to NO_VALIDATION")
    void cannotUpdateAdminKeyToUnusableKeySigningWithAdminKeyWithKeyVerificationSetToNoValidation() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Admin and supply keys
            var adminKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(adminKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.adminKey.toString())
                    .isEqualTo(adminKey.getPublicKey().toString());

            // Update the Admin Key to an unusable key (i.e., all-zeros key),
            // signing with an Admin Key, and setting the key verification mode to NO_VALIDATION
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setAdminKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(adminKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Can update all of token’s lower-privilege keys to an unusable key (i.e. all-zeros key), when signing with a respective lower-privilege key, and setting the key verification mode to NO_VALIDATION")
    void
            canUpdateAllLowerPrivilegeKeysToUnusableKeyWhenSigningWithRespectiveLowerPrivilegeKeyWithKeyVerificationSetToNoValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // Update all of token’s lower-privilege keys to an unusable key (i.e., all-zeros key),
            // when signing with a respective lower-privilege key,
            // and setting the key verification mode to NO_VALIDATION
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setWipeKey(PublicKey.unusableKey())
                    .setKycKey(PublicKey.unusableKey())
                    .setFreezeKey(PublicKey.unusableKey())
                    .setPauseKey(PublicKey.unusableKey())
                    .setSupplyKey(PublicKey.unusableKey())
                    .setFeeScheduleKey(PublicKey.unusableKey())
                    .setMetadataKey(PublicKey.unusableKey())
                    .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                    .freezeWith(testEnv.client)
                    .sign(wipeKey)
                    .sign(kycKey)
                    .sign(freezeKey)
                    .sign(pauseKey)
                    .sign(supplyKey)
                    .sign(feeScheduleKey)
                    .sign(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterUpdate.wipeKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.kycKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.freezeKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.pauseKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.supplyKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.feeScheduleKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
            assertThat(tokenInfoAfterUpdate.metadataKey.toString())
                    .isEqualTo(PublicKey.unusableKey().toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Can update all of token’s lower-privilege keys when signing with an old lower-privilege key and with a new lower-privilege key, and setting key verification mode to FULL_VALIDATION")
    void
            canUpdateAllLowerPrivilegeKeysWhenSigningWithOldLowerPrivilegeKeyAndNewLowerPrivilegeKeyWithKeyVerificationSetToFulValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // New Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var newWipeKey = PrivateKey.generateED25519();
            var newKycKey = PrivateKey.generateED25519();
            var newFreezeKey = PrivateKey.generateED25519();
            var newPauseKey = PrivateKey.generateED25519();
            var newSupplyKey = PrivateKey.generateED25519();
            var newFeeScheduleKey = PrivateKey.generateED25519();
            var newMetadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // Update all of token’s lower-privilege keys when signing with an old respective lower-privilege key,
            // and setting key verification mode to NO_VALIDATION
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setWipeKey(newWipeKey.getPublicKey())
                    .setKycKey(newKycKey.getPublicKey())
                    .setFreezeKey(newFreezeKey.getPublicKey())
                    .setPauseKey(newPauseKey.getPublicKey())
                    .setSupplyKey(newSupplyKey.getPublicKey())
                    .setFeeScheduleKey(newFeeScheduleKey.getPublicKey())
                    .setMetadataKey(newMetadataKey.getPublicKey())
                    .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                    .freezeWith(testEnv.client)
                    .sign(wipeKey)
                    .sign(newWipeKey)
                    .sign(kycKey)
                    .sign(newKycKey)
                    .sign(freezeKey)
                    .sign(newFreezeKey)
                    .sign(pauseKey)
                    .sign(newPauseKey)
                    .sign(supplyKey)
                    .sign(newSupplyKey)
                    .sign(feeScheduleKey)
                    .sign(newFeeScheduleKey)
                    .sign(metadataKey)
                    .sign(newMetadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterUpdate.wipeKey.toString())
                    .isEqualTo(newWipeKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.kycKey.toString())
                    .isEqualTo(newKycKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.freezeKey.toString())
                    .isEqualTo(newFreezeKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.pauseKey.toString())
                    .isEqualTo(newPauseKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.supplyKey.toString())
                    .isEqualTo(newSupplyKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.feeScheduleKey.toString())
                    .isEqualTo(newFeeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.metadataKey.toString())
                    .isEqualTo(newMetadataKey.getPublicKey().toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Can update all of token’s lower-privilege keys when signing ONLY with an old lower-privilege key, and setting key verification mode to NO_VALIDATION")
    void canUpdateAllLowerPrivilegeKeysWhenSigningOnlyWithOldLowerPrivilegeKeyWithKeyVerificationSetToNoValidation()
            throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // New Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var newWipeKey = PrivateKey.generateED25519();
            var newKycKey = PrivateKey.generateED25519();
            var newFreezeKey = PrivateKey.generateED25519();
            var newPauseKey = PrivateKey.generateED25519();
            var newSupplyKey = PrivateKey.generateED25519();
            var newFeeScheduleKey = PrivateKey.generateED25519();
            var newMetadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // Update all of token’s lower-privilege keys when signing with an old respective lower-privilege key,
            // and setting key verification mode to NO_VALIDATION
            new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setWipeKey(newWipeKey.getPublicKey())
                    .setKycKey(newKycKey.getPublicKey())
                    .setFreezeKey(newFreezeKey.getPublicKey())
                    .setPauseKey(newPauseKey.getPublicKey())
                    .setSupplyKey(newSupplyKey.getPublicKey())
                    .setFeeScheduleKey(newFeeScheduleKey.getPublicKey())
                    .setMetadataKey(newMetadataKey.getPublicKey())
                    .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                    .freezeWith(testEnv.client)
                    .sign(wipeKey)
                    .sign(kycKey)
                    .sign(freezeKey)
                    .sign(pauseKey)
                    .sign(supplyKey)
                    .sign(feeScheduleKey)
                    .sign(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var tokenInfoAfterUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoAfterUpdate.wipeKey.toString())
                    .isEqualTo(newWipeKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.kycKey.toString())
                    .isEqualTo(newKycKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.freezeKey.toString())
                    .isEqualTo(newFreezeKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.pauseKey.toString())
                    .isEqualTo(newPauseKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.supplyKey.toString())
                    .isEqualTo(newSupplyKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.feeScheduleKey.toString())
                    .isEqualTo(newFeeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoAfterUpdate.metadataKey.toString())
                    .isEqualTo(newMetadataKey.getPublicKey().toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Cannot remove all of token’s lower-privilege keys when updating them to an empty KeyList, signing with a respective lower-privilege key, and setting the key verification mode to NO_VALIDATION")
    void
            cannotRemoveAllLowerPrivilegeKeysWhenUpdatingKeysToEmptyKeyListSigningWithRespectiveLowerPrivilegeKeyWithKeyVerificationSetToNoValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            var emptyKeyList = new KeyList();

            // Remove all of token’s lower-privilege keys
            // when updating them to an empty KeyList (trying to remove keys one by one to check all errors),
            // signing with a respective lower-privilege key,
            // and setting the key verification mode to NO_VALIDATION
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setWipeKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(wipeKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_IMMUTABLE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setKycKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(kycKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_IMMUTABLE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFreezeKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(freezeKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_IMMUTABLE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setPauseKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(pauseKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_IMMUTABLE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setSupplyKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(supplyKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_IMMUTABLE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFeeScheduleKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(feeScheduleKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_IMMUTABLE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setMetadataKey(emptyKeyList)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(metadataKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_IMMUTABLE.toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Cannot update all of token’s lower-privilege keys to an unusable key (i.e. all-zeros key), when signing with a key that is different from a respective lower-privilege key, and setting the key verification mode to NO_VALIDATION")
    void cannotUpdateAllLowerPrivilegeKeysToUnusableKeyWhenSigningWithDifferentKeyWithKeyVerificationSetToNoValidation()
            throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // Update all of token’s lower-privilege keys to an unusable key (i.e. all-zeros key)
            // (trying to remove keys one by one to check all errors),
            // signing with a key that is different from a respective lower-privilege key (implicitly with an operator
            // key),
            // and setting the key verification mode to NO_VALIDATION
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setWipeKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setKycKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFreezeKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setPauseKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setSupplyKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFeeScheduleKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setMetadataKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Cannot update all of token’s lower-privilege keys to an unusable key (i.e. all-zeros key), when signing ONLY with an old respective lower-privilege key, and setting the key verification mode to FULL_VALIDATION")
    void
            cannotUpdateAllLowerPrivilegeKeysToUnusableKeyWhenSigningOnlyWithOldRespectiveLowerPrivilegeKeyWithKeyVerificationSetToFullValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // Update all of token’s lower-privilege keys to an unusable key (i.e., all-zeros key)
            // (trying to remove keys one by one to check all errors),
            // signing ONLY with an old respective lower-privilege key,
            // and setting the key verification mode to FULL_VALIDATION
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setWipeKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(wipeKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setKycKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(kycKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFreezeKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(freezeKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setPauseKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(pauseKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setSupplyKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(supplyKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFeeScheduleKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(feeScheduleKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setMetadataKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(metadataKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Cannot update all of token’s lower-privilege keys to an unusable key (i.e. all-zeros key), when signing with an old respective lower-privilege key and new respective lower-privilege key, and setting the key verification mode to FULL_VALIDATION")
    void
            cannotUpdateAllLowerPrivilegeKeysToUnusableKeyWhenSigningWithOldRespectiveLowerPrivilegeKeyAndNewRespectiveLowerPrivilegeKeyWithKeyVerificationSetToFullValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // New Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var newWipeKey = PrivateKey.generateED25519();
            var newKycKey = PrivateKey.generateED25519();
            var newFreezeKey = PrivateKey.generateED25519();
            var newPauseKey = PrivateKey.generateED25519();
            var newSupplyKey = PrivateKey.generateED25519();
            var newFeeScheduleKey = PrivateKey.generateED25519();
            var newMetadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // Update all of token’s lower-privilege keys to an unusable key (i.e., all-zeros key)
            // (trying to remove keys one by one to check all errors),
            // signing with an old respective lower-privilege key and new respective lower-privilege key,
            // and setting the key verification mode to FULL_VALIDATION
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setWipeKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(wipeKey)
                                .sign(newWipeKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setKycKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(kycKey)
                                .sign(newKycKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFreezeKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(freezeKey)
                                .sign(newFreezeKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setPauseKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(pauseKey)
                                .sign(newPauseKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setSupplyKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(supplyKey)
                                .sign(newSupplyKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFeeScheduleKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(feeScheduleKey)
                                .sign(newFeeScheduleKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setMetadataKey(PublicKey.unusableKey())
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(metadataKey)
                                .sign(newMetadataKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Cannot update all of token’s lower-privilege keys, when signing ONLY with an old respective lower-privilege key, and setting the key verification mode to FULL_VALIDATION")
    void
            cannotUpdateAllLowerPrivilegeKeysWhenSigningOnlyWithOldRespectiveLowerPrivilegeKeyWithKeyVerificationSetToFullValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // New Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var newWipeKey = PrivateKey.generateED25519();
            var newKycKey = PrivateKey.generateED25519();
            var newFreezeKey = PrivateKey.generateED25519();
            var newPauseKey = PrivateKey.generateED25519();
            var newSupplyKey = PrivateKey.generateED25519();
            var newFeeScheduleKey = PrivateKey.generateED25519();
            var newMetadataKey = PrivateKey.generateED25519();

            // Create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // Update all of token’s lower-privilege keys
            // (trying to update keys one by one to check all errors),
            // signing ONLY with an old respective lower-privilege key,
            // and setting the key verification mode to FULL_VALIDATION
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setWipeKey(newWipeKey)
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(wipeKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setKycKey(newKycKey)
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(kycKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFreezeKey(newFreezeKey)
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(freezeKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setPauseKey(newPauseKey)
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(pauseKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setSupplyKey(newSupplyKey)
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(supplyKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFeeScheduleKey(newFeeScheduleKey)
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(feeScheduleKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setMetadataKey(newMetadataKey)
                                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(metadataKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    /**
     * @notice E2E-HIP-540
     * @url https://hips.hedera.com/hip/hip-540
     */
    @Test
    @DisplayName(
            "Cannot update all of token’s lower-privilege keys when updating them to a keys with an invalid structure and signing with an old respective lower-privilege and setting key verification mode to NO_VALIDATION")
    void
            cannotUpdateAllLowerPrivilegeKeysWhenUpdatingKeysToStructurallyInvalidKeysSigningOnlyWithOldRespectiveLowerPrivilegeKeyWithKeyVerificationSetToNoValidation()
                    throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata keys
            var wipeKey = PrivateKey.generateED25519();
            var kycKey = PrivateKey.generateED25519();
            var freezeKey = PrivateKey.generateED25519();
            var pauseKey = PrivateKey.generateED25519();
            var supplyKey = PrivateKey.generateED25519();
            var feeScheduleKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();

            // create a non-fungible token
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setWipeKey(wipeKey.getPublicKey())
                    .setKycKey(kycKey.getPublicKey())
                    .setFreezeKey(freezeKey.getPublicKey())
                    .setPauseKey(pauseKey.getPublicKey())
                    .setSupplyKey(supplyKey.getPublicKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                    .setMetadataKey(metadataKey.getPublicKey())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfoBeforeUpdate = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfoBeforeUpdate.wipeKey.toString())
                    .isEqualTo(wipeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.kycKey.toString())
                    .isEqualTo(kycKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.freezeKey.toString())
                    .isEqualTo(freezeKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.pauseKey.toString())
                    .isEqualTo(pauseKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.supplyKey.toString())
                    .isEqualTo(supplyKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.feeScheduleKey.toString())
                    .isEqualTo(feeScheduleKey.getPublicKey().toString());
            assertThat(tokenInfoBeforeUpdate.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // invalid ecdsa key
            var ecdsaKey = PublicKey.fromBytesECDSA(new byte[33]);

            // update all of token’s lower-privilege keys
            // to a structurally invalid key (trying to update keys one by one to check all errors),
            // signing with an old respective lower-privilege
            // and setting key verification mode to NO_VALIDATION
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setWipeKey(ecdsaKey)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(wipeKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_WIPE_KEY.toString());

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setKycKey(ecdsaKey)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(kycKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_KYC_KEY.toString());

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFreezeKey(ecdsaKey)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(freezeKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_FREEZE_KEY.toString());

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setPauseKey(ecdsaKey)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(pauseKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_PAUSE_KEY.toString());

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setSupplyKey(ecdsaKey)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(supplyKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SUPPLY_KEY.toString());

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setFeeScheduleKey(ecdsaKey)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(feeScheduleKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_CUSTOM_FEE_SCHEDULE_KEY.toString());

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateTransaction()
                                .setTokenId(tokenId)
                                .setMetadataKey(ecdsaKey)
                                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                                .freezeWith(testEnv.client)
                                .sign(metadataKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_METADATA_KEY.toString());
        }
    }
}
