// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Collections;
import java.util.List;
import org.hiero.sdk.AccountAllowanceApproveTransaction;
import org.hiero.sdk.AccountBalanceQuery;
import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.TokenAssociateTransaction;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenDeleteTransaction;
import org.hiero.sdk.TokenFreezeTransaction;
import org.hiero.sdk.TokenMintTransaction;
import org.hiero.sdk.TokenNftInfoQuery;
import org.hiero.sdk.TokenPauseTransaction;
import org.hiero.sdk.TokenRejectTransaction;
import org.hiero.sdk.TokenSupplyType;
import org.hiero.sdk.TokenType;
import org.hiero.sdk.TransactionId;
import org.hiero.sdk.TransferTransaction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TokenRejectIntegrationTest {

    @Test
    @DisplayName("Can execute TokenReject transaction for Fungible Token")
    void canExecuteTokenRejectTransactionForFungibleToken() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenId1 = EntityHelper.createFungibleToken(testEnv, 3);
            var tokenId2 = EntityHelper.createFungibleToken(testEnv, 3);
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 100);

            // transfer fts to the receiver
            new TransferTransaction()
                    .addTokenTransfer(tokenId1, testEnv.operatorId, -10)
                    .addTokenTransfer(tokenId1, receiverAccountId, 10)
                    .addTokenTransfer(tokenId2, testEnv.operatorId, -10)
                    .addTokenTransfer(tokenId2, receiverAccountId, 10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token
            new TokenRejectTransaction()
                    .setOwnerId(receiverAccountId)
                    .setTokenIds(List.of(tokenId1, tokenId2))
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the balance of the receiver is 0
            var receiverAccountBalance =
                    new AccountBalanceQuery().setAccountId(receiverAccountId).execute(testEnv.client);

            assertThat(receiverAccountBalance.tokens.get(tokenId1)).isEqualTo(0);
            assertThat(receiverAccountBalance.tokens.get(tokenId2)).isEqualTo(0);

            // verify the tokens are transferred back to the treasury
            var treasuryAccountBalance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).execute(testEnv.client);

            assertThat(treasuryAccountBalance.tokens.get(tokenId1)).isEqualTo(1_000_000);
            assertThat(treasuryAccountBalance.tokens.get(tokenId2)).isEqualTo(1_000_000);

            new TokenDeleteTransaction()
                    .setTokenId(tokenId1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(tokenId2)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can execute TokenReject transaction for NFT")
    void canExecuteTokenRejectTransactionForNft() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenId1 = EntityHelper.createNft(testEnv);
            var tokenId2 = EntityHelper.createNft(testEnv);
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 100);

            var mintReceiptToken1 = new TokenMintTransaction()
                    .setTokenId(tokenId1)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var mintReceiptToken2 = new TokenMintTransaction()
                    .setTokenId(tokenId2)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceiptToken2.serials;

            // transfer nfts to the receiver
            new TransferTransaction()
                    .addNftTransfer(tokenId1.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(tokenId1.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(tokenId2.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(tokenId2.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject one of the nfts
            new TokenRejectTransaction()
                    .setOwnerId(receiverAccountId)
                    .setNftIds(List.of(tokenId1.nft(nftSerials.get(1)), tokenId2.nft(nftSerials.get(1))))
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the balance is decremented by 1
            var receiverAccountBalance =
                    new AccountBalanceQuery().setAccountId(receiverAccountId).execute(testEnv.client);

            assertThat(receiverAccountBalance.tokens.get(tokenId1)).isEqualTo(1);
            assertThat(receiverAccountBalance.tokens.get(tokenId2)).isEqualTo(1);

            // verify the token is transferred back to the treasury
            var tokenId1NftInfo = new TokenNftInfoQuery()
                    .setNftId(tokenId1.nft(nftSerials.get(1)))
                    .execute(testEnv.client);

            assertThat(tokenId1NftInfo.get(0).accountId).isEqualTo(testEnv.operatorId);

            var tokenId2NftInfo = new TokenNftInfoQuery()
                    .setNftId(tokenId2.nft(nftSerials.get(1)))
                    .execute(testEnv.client);

            assertThat(tokenId2NftInfo.get(0).accountId).isEqualTo(testEnv.operatorId);

            new TokenDeleteTransaction()
                    .setTokenId(tokenId1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(tokenId2)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can execute TokenReject transaction for FT and NFT in One Tx")
    void canExecuteTokenRejectTransactionForFtAndNftInOneTx() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var ftTokenId1 = EntityHelper.createFungibleToken(testEnv, 3);
            var ftTokenId2 = EntityHelper.createFungibleToken(testEnv, 3);
            var nftTokenId1 = EntityHelper.createNft(testEnv);
            var nftTokenId2 = EntityHelper.createNft(testEnv);
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 100);

            var mintReceiptNftToken1 = new TokenMintTransaction()
                    .setTokenId(nftTokenId1)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var mintReceiptNftToken2 = new TokenMintTransaction()
                    .setTokenId(nftTokenId2)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceiptNftToken2.serials;

            // transfer fts to the receiver
            new TransferTransaction()
                    .addTokenTransfer(ftTokenId1, testEnv.operatorId, -10)
                    .addTokenTransfer(ftTokenId1, receiverAccountId, 10)
                    .addTokenTransfer(ftTokenId2, testEnv.operatorId, -10)
                    .addTokenTransfer(ftTokenId2, receiverAccountId, 10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // transfer nfts to the receiver
            new TransferTransaction()
                    .addNftTransfer(nftTokenId1.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId1.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId2.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId2.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token
            new TokenRejectTransaction()
                    .setOwnerId(receiverAccountId)
                    .setTokenIds(List.of(ftTokenId1, ftTokenId2))
                    .setNftIds(List.of(nftTokenId1.nft(nftSerials.get(1)), nftTokenId2.nft(nftSerials.get(1))))
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the balance of the receiver is 0
            var receiverAccountBalance =
                    new AccountBalanceQuery().setAccountId(receiverAccountId).execute(testEnv.client);

            assertThat(receiverAccountBalance.tokens.get(ftTokenId1)).isEqualTo(0);
            assertThat(receiverAccountBalance.tokens.get(ftTokenId2)).isEqualTo(0);
            assertThat(receiverAccountBalance.tokens.get(nftTokenId1)).isEqualTo(1);
            assertThat(receiverAccountBalance.tokens.get(nftTokenId2)).isEqualTo(1);

            // verify the tokens are transferred back to the treasury
            var treasuryAccountBalance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).execute(testEnv.client);

            assertThat(treasuryAccountBalance.tokens.get(ftTokenId1)).isEqualTo(1_000_000);
            assertThat(treasuryAccountBalance.tokens.get(ftTokenId2)).isEqualTo(1_000_000);

            var tokenId1NftInfo = new TokenNftInfoQuery()
                    .setNftId(nftTokenId1.nft(nftSerials.get(1)))
                    .execute(testEnv.client);

            assertThat(tokenId1NftInfo.get(0).accountId).isEqualTo(testEnv.operatorId);

            var tokenId2NftInfo = new TokenNftInfoQuery()
                    .setNftId(nftTokenId2.nft(nftSerials.get(1)))
                    .execute(testEnv.client);

            assertThat(tokenId2NftInfo.get(0).accountId).isEqualTo(testEnv.operatorId);

            new TokenDeleteTransaction()
                    .setTokenId(ftTokenId1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(ftTokenId2)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(nftTokenId1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(nftTokenId2)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can execute TokenReject transaction for FT and NFT when Treasury receiverSigRequired is Enabled")
    void canExecuteTokenRejectTransactionForFtAndNftWhenTreasuryReceiverSigRequiredIsEnabled() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 100);

            var treasuryAccountKey = PrivateKey.generateED25519();
            var treasuryAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(treasuryAccountKey)
                    .setInitialBalance(new Hbar(0))
                    .setReceiverSignatureRequired(true)
                    .setMaxAutomaticTokenAssociations(100)
                    .freezeWith(testEnv.client)
                    .sign(treasuryAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var ftTokenId = new TokenCreateTransaction()
                    .setTokenName("Test Fungible Token")
                    .setTokenSymbol("TFT")
                    .setTokenMemo("I was created for integration tests")
                    .setDecimals(18)
                    .setInitialSupply(1_000_000)
                    .setMaxSupply(1_000_000)
                    .setTreasuryAccountId(treasuryAccountId)
                    .setSupplyType(TokenSupplyType.FINITE)
                    .setAdminKey(testEnv.operatorKey)
                    .setFreezeKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setMetadataKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .sign(treasuryAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            // transfer fts to the receiver
            new TransferTransaction()
                    .addTokenTransfer(ftTokenId, treasuryAccountId, -10)
                    .addTokenTransfer(ftTokenId, receiverAccountId, 10)
                    .freezeWith(testEnv.client)
                    .sign(treasuryAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token
            new TokenRejectTransaction()
                    .setOwnerId(receiverAccountId)
                    .addTokenId(ftTokenId)
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the balance of the receiver is 0
            var receiverAccountBalanceFt =
                    new AccountBalanceQuery().setAccountId(receiverAccountId).execute(testEnv.client);

            assertThat(receiverAccountBalanceFt.tokens.get(ftTokenId)).isEqualTo(0);

            // verify the tokens are transferred back to the treasury
            var treasuryAccountBalance =
                    new AccountBalanceQuery().setAccountId(treasuryAccountId).execute(testEnv.client);

            assertThat(treasuryAccountBalance.tokens.get(ftTokenId)).isEqualTo(1_000_000);

            // same test for nft

            var nftTokenId = new TokenCreateTransaction()
                    .setTokenName("Test NFT")
                    .setTokenSymbol("TNFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(treasuryAccountId)
                    .setSupplyType(TokenSupplyType.FINITE)
                    .setMaxSupply(10)
                    .setAdminKey(testEnv.operatorKey)
                    .setFreezeKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setMetadataKey(testEnv.operatorKey)
                    .setWipeKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .sign(treasuryAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            var mintReceiptNftToken = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceiptNftToken.serials;

            // transfer nfts to the receiver
            new TransferTransaction()
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(0)), treasuryAccountId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(1)), treasuryAccountId, receiverAccountId)
                    .freezeWith(testEnv.client)
                    .sign(treasuryAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token
            new TokenRejectTransaction()
                    .setOwnerId(receiverAccountId)
                    .addNftId(nftTokenId.nft(nftSerials.get(1)))
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the balance is decremented by 1
            var receiverAccountBalanceNft =
                    new AccountBalanceQuery().setAccountId(receiverAccountId).execute(testEnv.client);

            assertThat(receiverAccountBalanceNft.tokens.get(nftTokenId)).isEqualTo(1);

            // verify the token is transferred back to the treasury
            var nftTokenIdInfo = new TokenNftInfoQuery()
                    .setNftId(nftTokenId.nft(nftSerials.get(1)))
                    .execute(testEnv.client);

            assertThat(nftTokenIdInfo.get(0).accountId).isEqualTo(treasuryAccountId);

            new TokenDeleteTransaction()
                    .setTokenId(ftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(nftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot execute TokenReject transaction for FT and NFT when Token is Frozen")
    void canExecuteTokenRejectTransactionForFtAndNftWhenTokenIsFrozen() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var ftTokenId = EntityHelper.createFungibleToken(testEnv, 18);
            var nftTokenId = EntityHelper.createNft(testEnv);
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 100);

            // transfer fts to the receiver
            new TransferTransaction()
                    .addTokenTransfer(ftTokenId, testEnv.operatorId, -10)
                    .addTokenTransfer(ftTokenId, receiverAccountId, 10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // freeze ft
            new TokenFreezeTransaction()
                    .setTokenId(ftTokenId)
                    .setAccountId(receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token - should fail with ACCOUNT_FROZEN_FOR_TOKEN
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .addTokenId(ftTokenId)
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("ACCOUNT_FROZEN_FOR_TOKEN");

            // same test for nft

            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceipt.serials;

            // transfer nfts to the receiver
            new TransferTransaction()
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // freeze nft
            new TokenFreezeTransaction()
                    .setTokenId(nftTokenId)
                    .setAccountId(receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token - should fail with ACCOUNT_FROZEN_FOR_TOKEN
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .addNftId(nftTokenId.nft(nftSerials.get(1)))
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("ACCOUNT_FROZEN_FOR_TOKEN");

            new TokenDeleteTransaction()
                    .setTokenId(ftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(nftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot execute TokenReject transaction for FT and NFT when Token is Paused")
    void canExecuteTokenRejectTransactionForFtAndNftWhenTokenIsPaused() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var ftTokenId = EntityHelper.createFungibleToken(testEnv, 18);
            var nftTokenId = EntityHelper.createNft(testEnv);
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 100);

            // transfer fts to the receiver
            new TransferTransaction()
                    .addTokenTransfer(ftTokenId, testEnv.operatorId, -10)
                    .addTokenTransfer(ftTokenId, receiverAccountId, 10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // pause ft
            new TokenPauseTransaction()
                    .setTokenId(ftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token - should fail with TOKEN_IS_PAUSED
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .addTokenId(ftTokenId)
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("TOKEN_IS_PAUSED");

            // same test for nft

            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceipt.serials;

            // transfer nfts to the receiver
            new TransferTransaction()
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // pause nft
            new TokenPauseTransaction()
                    .setTokenId(nftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token - should fail with TOKEN_IS_PAUSED
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .addNftId(nftTokenId.nft(nftSerials.get(1)))
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("TOKEN_IS_PAUSED");
        }
    }

    @Test
    @Disabled // temp disabled till issue re nfts will be resolved on services side
    @DisplayName("Can remove allowance when executing TokenReject transaction for FT and NFT")
    void canRemoveAllowanceWhenExecutingTokenRejectForFtAndNft() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var ftTokenId = EntityHelper.createFungibleToken(testEnv, 3);
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, -1);
            var spenderAccountKey = PrivateKey.generateED25519();
            var spenderAccountId = EntityHelper.createAccount(testEnv, spenderAccountKey, -1);

            // transfer ft to the receiver
            new TransferTransaction()
                    .addTokenTransfer(ftTokenId, testEnv.operatorId, -10)
                    .addTokenTransfer(ftTokenId, receiverAccountId, 10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // approve allowance to the spender
            new AccountAllowanceApproveTransaction()
                    .approveTokenAllowance(ftTokenId, receiverAccountId, spenderAccountId, 10)
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the spender has allowance
            new TransferTransaction()
                    .addApprovedTokenTransfer(ftTokenId, receiverAccountId, -5)
                    .addTokenTransfer(ftTokenId, spenderAccountId, 5)
                    .setTransactionId(TransactionId.generate(spenderAccountId))
                    .freezeWith(testEnv.client)
                    .sign(spenderAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token
            new TokenRejectTransaction()
                    .setOwnerId(receiverAccountId)
                    .addTokenId(ftTokenId)
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the allowance - should be 0 , because the receiver is no longer the owner
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TransferTransaction()
                                .addApprovedTokenTransfer(ftTokenId, receiverAccountId, -5)
                                .addTokenTransfer(ftTokenId, spenderAccountId, 5)
                                .setTransactionId(TransactionId.generate(spenderAccountId))
                                .freezeWith(testEnv.client)
                                .sign(spenderAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("SPENDER_DOES_NOT_HAVE_ALLOWANCE");

            // same test for nft

            var nftTokenId = EntityHelper.createNft(testEnv);

            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceipt.serials;

            // transfer nfts to the receiver
            new TransferTransaction()
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(2)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(3)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // approve allowance to the spender
            new AccountAllowanceApproveTransaction()
                    .approveTokenNftAllowance(nftTokenId.nft(nftSerials.get(0)), receiverAccountId, spenderAccountId)
                    .approveTokenNftAllowance(nftTokenId.nft(nftSerials.get(1)), receiverAccountId, spenderAccountId)
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the spender has allowance
            new TransferTransaction()
                    .addApprovedNftTransfer(nftTokenId.nft(nftSerials.get(0)), receiverAccountId, spenderAccountId)
                    .setTransactionId(TransactionId.generate(spenderAccountId))
                    .freezeWith(testEnv.client)
                    .sign(spenderAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token
            new TokenRejectTransaction()
                    .setOwnerId(receiverAccountId)
                    .setNftIds(List.of(nftTokenId.nft(nftSerials.get(1)), nftTokenId.nft(nftSerials.get(2))))
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the allowance - should be 0 , because the receiver is no longer the owner
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TransferTransaction()
                                .addApprovedNftTransfer(
                                        nftTokenId.nft(nftSerials.get(1)), receiverAccountId, spenderAccountId)
                                .addApprovedNftTransfer(
                                        nftTokenId.nft(nftSerials.get(2)), receiverAccountId, spenderAccountId)
                                .setTransactionId(TransactionId.generate(spenderAccountId))
                                .freezeWith(testEnv.client)
                                .sign(spenderAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("SPENDER_DOES_NOT_HAVE_ALLOWANCE");

            new TokenDeleteTransaction()
                    .setTokenId(ftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(nftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot reject NFT when executing TokenReject with Add or Set TokenId")
    void cannotRejectNftWhenUsingAddOrSetTokenId() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var nftTokenId = EntityHelper.createNft(testEnv);
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 100);

            var mintReceiptNftToken = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceiptNftToken.serials;

            // transfer nfts to the receiver
            new TransferTransaction()
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(2)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the whole collection (addTokenId) - should fail
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .addTokenId(nftTokenId)
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("ACCOUNT_AMOUNT_TRANSFERS_ONLY_ALLOWED_FOR_FUNGIBLE_COMMON");

            // reject the whole collection (setTokenIds) - should fail
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .setTokenIds(List.of(nftTokenId))
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("ACCOUNT_AMOUNT_TRANSFERS_ONLY_ALLOWED_FOR_FUNGIBLE_COMMON");

            new TokenDeleteTransaction()
                    .setTokenId(nftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot Reject a Token when executing TokenReject and Duplicating Token Reference")
    void cannotRejectTokenWhenExecutingTokenRejectAndDuplicatingTokenReference() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var ftTokenId = EntityHelper.createFungibleToken(testEnv, 3);
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 100);

            // transfer fts to the receiver
            new TransferTransaction()
                    .addTokenTransfer(ftTokenId, testEnv.operatorId, -10)
                    .addTokenTransfer(ftTokenId, receiverAccountId, 10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token with duplicate token id - should fail with TOKEN_REFERENCE_REPEATED
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .setTokenIds(List.of(ftTokenId, ftTokenId))
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("TOKEN_REFERENCE_REPEATED");

            // same test for nft

            var nftTokenId = EntityHelper.createNft(testEnv);

            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceipt.serials;

            // transfer nfts to the receiver
            new TransferTransaction()
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the nft with duplicate nft id - should fail with TOKEN_REFERENCE_REPEATED
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .setNftIds(
                                        List.of(nftTokenId.nft(nftSerials.get(0)), nftTokenId.nft(nftSerials.get(0))))
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("TOKEN_REFERENCE_REPEATED");

            new TokenDeleteTransaction()
                    .setTokenId(ftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(nftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot Reject a Token when Owner Has Empty Balance")
    void cannotRejectTokenWhenOwnerHasEmptyBalance() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var ftTokenId = EntityHelper.createFungibleToken(testEnv, 3);
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 100);

            // skip the transfer
            // associate the receiver
            new TokenAssociateTransaction()
                    .setAccountId(receiverAccountId)
                    .setTokenIds(Collections.singletonList(ftTokenId))
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token - should fail with INSUFFICIENT_TOKEN_BALANCE
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .addTokenId(ftTokenId)
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INSUFFICIENT_TOKEN_BALANCE");

            // same test for nft

            var nftTokenId = EntityHelper.createNft(testEnv);

            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceipt.serials;

            // skip the transfer
            // associate the receiver
            new TokenAssociateTransaction()
                    .setAccountId(receiverAccountId)
                    .setTokenIds(Collections.singletonList(nftTokenId))
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the nft - should fail with INVALID_OWNER_ID
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .addNftId(nftTokenId.nft(nftSerials.get(0)))
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INVALID_OWNER_ID");

            new TokenDeleteTransaction()
                    .setTokenId(ftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(nftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot Reject a Token when Treasury Rejects itself")
    void cannotRejectTokenWhenTreasuryRejects() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var ftTokenId = EntityHelper.createFungibleToken(testEnv, 3);

            // skip the transfer
            // reject the token with the treasury - should fail with ACCOUNT_IS_TREASURY
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(testEnv.operatorId)
                                .addTokenId(ftTokenId)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("ACCOUNT_IS_TREASURY");

            // same test for nft

            var nftTokenId = EntityHelper.createNft(testEnv);

            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceipt.serials;

            // skip the transfer
            // reject the nft with the treasury - should fail with ACCOUNT_IS_TREASURY
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(testEnv.operatorId)
                                .addNftId(nftTokenId.nft(nftSerials.get(0)))
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("ACCOUNT_IS_TREASURY");
        }
    }

    @Test
    @DisplayName("Cannot Reject a Token with Invalid Signature")
    void cannotRejectTokenWithInvalidSignature() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var ftTokenId = EntityHelper.createFungibleToken(testEnv, 3);
            var randomKey = PrivateKey.generateED25519();
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 100);

            // transfer fts to the receiver
            new TransferTransaction()
                    .addTokenTransfer(ftTokenId, testEnv.operatorId, -10)
                    .addTokenTransfer(ftTokenId, receiverAccountId, 10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token with different key - should fail with INVALID_SIGNATURE
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .addTokenId(ftTokenId)
                                .freezeWith(testEnv.client)
                                .sign(randomKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INVALID_SIGNATURE");

            new TokenDeleteTransaction()
                    .setTokenId(ftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot Reject a Token when Token Or NFT ID is not set")
    void cannotRejectTokenWhenTokenOrNFTIdIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // reject the token with invalid token - should fail with EMPTY_TOKEN_REFERENCE_LIST
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(testEnv.operatorId)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("EMPTY_TOKEN_REFERENCE_LIST");
        }
    }

    @Test
    @DisplayName("Cannot Reject a Token when executing TokenReject and Token Reference List Size Exceeded")
    void cannotRejectTokenWhenTokenReferenceListSizeExceeded() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var ftTokenId = EntityHelper.createFungibleToken(testEnv, 18);
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, -1);
            var nftTokenId = EntityHelper.createNft(testEnv);

            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nftSerials = mintReceipt.serials;

            // transfer the tokens to the receiver
            new TransferTransaction()
                    .addTokenTransfer(ftTokenId, testEnv.operatorId, -10)
                    .addTokenTransfer(ftTokenId, receiverAccountId, 10)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(2)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(3)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(4)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(5)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(6)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(7)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(8)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftTokenId.nft(nftSerials.get(9)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // reject the token with 11 token references - should fail with TOKEN_REFERENCE_LIST_SIZE_LIMIT_EXCEEDED
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TokenRejectTransaction()
                                .setOwnerId(receiverAccountId)
                                .addTokenId(ftTokenId)
                                .setNftIds(List.of(
                                        nftTokenId.nft(nftSerials.get(0)),
                                        nftTokenId.nft(nftSerials.get(1)),
                                        nftTokenId.nft(nftSerials.get(2)),
                                        nftTokenId.nft(nftSerials.get(3)),
                                        nftTokenId.nft(nftSerials.get(4)),
                                        nftTokenId.nft(nftSerials.get(5)),
                                        nftTokenId.nft(nftSerials.get(6)),
                                        nftTokenId.nft(nftSerials.get(7)),
                                        nftTokenId.nft(nftSerials.get(8)),
                                        nftTokenId.nft(nftSerials.get(9))))
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("TOKEN_REFERENCE_LIST_SIZE_LIMIT_EXCEEDED");

            new TokenDeleteTransaction()
                    .setTokenId(ftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(nftTokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }
}
