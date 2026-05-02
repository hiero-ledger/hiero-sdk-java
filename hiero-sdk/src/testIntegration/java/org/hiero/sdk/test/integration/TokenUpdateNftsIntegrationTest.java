// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Objects;
import org.hiero.sdk.Client;
import org.hiero.sdk.NftId;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.Status;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenId;
import org.hiero.sdk.TokenInfoQuery;
import org.hiero.sdk.TokenMintTransaction;
import org.hiero.sdk.TokenNftInfoQuery;
import org.hiero.sdk.TokenType;
import org.hiero.sdk.TokenUpdateNftsTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @notice E2E-HIP-657
 * @url https://hips.hedera.com/hip/hip-657
 */
public class TokenUpdateNftsIntegrationTest {

    @Test
    @DisplayName("Can update the metadata of the entire NFT collection")
    void canUpdateNFTMetadataOfEntireCollection() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var metadataKey = PrivateKey.generateED25519();
            var nftCount = 4;
            var initialMetadataList = NftMetadataGenerator.generate(new byte[] {4, 2, 0}, nftCount);
            var updatedMetadata = new byte[] {6, 9};
            var updatedMetadataList = NftMetadataGenerator.generate(updatedMetadata, nftCount);

            // create a token with metadata key
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setMetadataKey(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            // mint tokens
            var tokenMintTransactionReceipt = new TokenMintTransaction()
                    .setMetadata(initialMetadataList)
                    .setTokenId(tokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // check that metadata was set correctly
            var nftSerials = tokenMintTransactionReceipt.serials;
            List<byte[]> metadataListAfterMint = getMetadataList(testEnv.client, tokenId, nftSerials);

            assertThat(metadataListAfterMint.toArray()).isEqualTo(initialMetadataList.toArray());

            // update metadata all minted NFTs
            new TokenUpdateNftsTransaction()
                    .setTokenId(tokenId)
                    .setSerials(nftSerials)
                    .setMetadata(updatedMetadata)
                    .freezeWith(testEnv.client)
                    .sign(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // check updated NFTs' metadata
            List<byte[]> metadataListAfterUpdate = getMetadataList(testEnv.client, tokenId, nftSerials);
            assertThat(metadataListAfterUpdate.toArray()).isEqualTo(updatedMetadataList.toArray());
        }
    }

    @Test
    @DisplayName("Can update the metadata of a part of the NFT collection")
    void canUpdateNFTMetadataOfPartOfCollection() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var metadataKey = PrivateKey.generateED25519();
            var nftCount = 4;
            var initialMetadataList = NftMetadataGenerator.generate(new byte[] {4, 2, 0}, nftCount);
            var updatedMetadata = new byte[] {6, 9};
            var updatedMetadataList = NftMetadataGenerator.generate(updatedMetadata, nftCount / 2);

            // create a token with metadata key
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setMetadataKey(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            // mint tokens
            var tokenMintTransactionReceipt = new TokenMintTransaction()
                    .setMetadata(initialMetadataList)
                    .setTokenId(tokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // check that metadata was set correctly
            var nftSerials = tokenMintTransactionReceipt.serials;
            List<byte[]> metadataListAfterMint = getMetadataList(testEnv.client, tokenId, nftSerials);

            assertThat(metadataListAfterMint.toArray()).isEqualTo(initialMetadataList.toArray());

            // update metadata of the first two minted NFTs
            var nftSerialsToUpdate = nftSerials.subList(0, nftCount / 2);

            new TokenUpdateNftsTransaction()
                    .setTokenId(tokenId)
                    .setSerials(nftSerialsToUpdate)
                    .setMetadata(updatedMetadata)
                    .freezeWith(testEnv.client)
                    .sign(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // check updated NFTs' metadata
            List<byte[]> metadataListAfterUpdate = getMetadataList(testEnv.client, tokenId, nftSerialsToUpdate);

            assertThat(metadataListAfterUpdate.toArray()).isEqualTo(updatedMetadataList.toArray());

            // check that remaining NFTs were not updated
            var nftSerialsSame = nftSerials.subList(nftCount / 2, nftCount);
            List<byte[]> metadataList = getMetadataList(testEnv.client, tokenId, nftSerialsSame);

            assertThat(metadataList.toArray())
                    .isEqualTo(
                            initialMetadataList.subList(nftCount / 2, nftCount).toArray());
        }
    }

    @Test
    @DisplayName("Cannot update NFTs metadata when it is not set")
    void cannotUpdateNFTMetadataWhenItsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var metadataKey = PrivateKey.generateED25519();
            var nftCount = 4;
            var initialMetadataList = NftMetadataGenerator.generate(new byte[] {4, 2, 0}, nftCount);

            // create a token with metadata key
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setMetadataKey(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            // mint tokens
            var tokenMintTransactionReceipt = new TokenMintTransaction()
                    .setMetadata(initialMetadataList)
                    .setTokenId(tokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // check that metadata was set correctly
            var nftSerials = tokenMintTransactionReceipt.serials;
            List<byte[]> metadataListAfterMint = getMetadataList(testEnv.client, tokenId, nftSerials);

            assertThat(metadataListAfterMint.toArray()).isEqualTo(initialMetadataList.toArray());

            // run `TokenUpdateNftsTransaction` without `setMetadata`
            new TokenUpdateNftsTransaction()
                    .setTokenId(tokenId)
                    .setSerials(nftSerials)
                    .freezeWith(testEnv.client)
                    .sign(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // check that NFTs' metadata was not updated
            List<byte[]> metadataListAfterUpdate = getMetadataList(testEnv.client, tokenId, nftSerials);
            assertThat(metadataListAfterUpdate.toArray()).isEqualTo(initialMetadataList.toArray());
        }
    }

    @Test
    @DisplayName("Can erase NFTs metadata")
    void canEraseNFTsMetadata() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var metadataKey = PrivateKey.generateED25519();
            var nftCount = 4;
            var initialMetadataList = NftMetadataGenerator.generate(new byte[] {4, 2, 0}, nftCount);
            var emptyMetadata = new byte[] {};
            var emptyMetadataList = NftMetadataGenerator.generate(emptyMetadata, nftCount);

            // create a token with metadata key
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setMetadataKey(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            // mint tokens
            var tokenMintTransactionReceipt = new TokenMintTransaction()
                    .setMetadata(initialMetadataList)
                    .setTokenId(tokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // check that metadata was set correctly
            var nftSerials = tokenMintTransactionReceipt.serials;
            List<byte[]> metadataListAfterMint = getMetadataList(testEnv.client, tokenId, nftSerials);

            assertThat(metadataListAfterMint.toArray()).isEqualTo(initialMetadataList.toArray());

            // erase metadata all minted NFTs (update to an empty byte array)
            new TokenUpdateNftsTransaction()
                    .setTokenId(tokenId)
                    .setSerials(nftSerials)
                    .setMetadata(emptyMetadata)
                    .freezeWith(testEnv.client)
                    .sign(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // check that NFTs' metadata was erased
            List<byte[]> metadataListAfterUpdate = getMetadataList(testEnv.client, tokenId, nftSerials);
            assertThat(metadataListAfterUpdate.toArray()).isEqualTo(emptyMetadataList.toArray());
        }
    }

    @Test
    @DisplayName("Cannot update NFT metadata when transaction is not signed with metadata key")
    void cannotUpdateNFTMetadataWhenTransactionIsNotSignedWithMetadataKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var supplyKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();
            var nftCount = 4;
            var initialMetadataList = NftMetadataGenerator.generate(new byte[] {4, 2, 0}, nftCount);
            var updatedMetadata = new byte[] {6, 9};

            // create a token with a metadata key and check it
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(supplyKey)
                    .setMetadataKey(metadataKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfo = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfo.metadataKey.toString())
                    .isEqualTo(metadataKey.getPublicKey().toString());

            // mint tokens
            var tokenMintTransactionReceipt = new TokenMintTransaction()
                    .setMetadata(initialMetadataList)
                    .setTokenId(tokenId)
                    .freezeWith(testEnv.client)
                    .sign(supplyKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = tokenMintTransactionReceipt.serials;

            // update nfts without signing
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateNftsTransaction()
                                .setTokenId(tokenId)
                                .setSerials(nftSerials)
                                .setMetadata(updatedMetadata)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    @Test
    @DisplayName("Cannot update NFT metadata when metadata key is not set")
    void cannotUpdateNFTMetadataWhenMetadataKeyNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var supplyKey = PrivateKey.generateED25519();
            var metadataKey = PrivateKey.generateED25519();
            var nftCount = 4;
            var initialMetadataList = NftMetadataGenerator.generate(new byte[] {4, 2, 0}, nftCount);
            var updatedMetadata = new byte[] {6, 9};

            // create a token without a metadata key and check it
            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(supplyKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            var tokenInfo = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfo.metadataKey).isNull();

            // mint tokens
            var tokenMintTransactionReceipt = new TokenMintTransaction()
                    .setMetadata(initialMetadataList)
                    .setTokenId(tokenId)
                    .freezeWith(testEnv.client)
                    .sign(supplyKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = tokenMintTransactionReceipt.serials;

            // check NFTs' metadata can't be updated when a metadata key is not set
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenUpdateNftsTransaction()
                                .setTokenId(tokenId)
                                .setSerials(nftSerials)
                                .setMetadata(updatedMetadata)
                                .freezeWith(testEnv.client)
                                .sign(metadataKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    /**
     * Retrieves the metadata information for a given list of NFT serials associated with a token.
     *
     * @param client The Hedera client used for executing the query.
     * @param tokenId The ID of the token.
     * @param nftSerials The list of serial numbers of the NFTs.
     * @return A list of byte arrays representing the metadata information for the NFTs.
     */
    private List<byte[]> getMetadataList(Client client, TokenId tokenId, List<Long> nftSerials) {
        return nftSerials.stream()
                .map(serial -> new NftId(tokenId, serial))
                .flatMap(nftId -> {
                    try {
                        return new TokenNftInfoQuery().setNftId(nftId).execute(client).stream();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(tokenNftInfo -> tokenNftInfo.metadata)
                .toList();
    }
}
