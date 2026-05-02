// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.hiero.sdk.AccountAllowanceApproveTransaction;
import org.hiero.sdk.AccountBalanceQuery;
import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.AccountInfoQuery;
import org.hiero.sdk.AccountUpdateTransaction;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.TokenDeleteTransaction;
import org.hiero.sdk.TokenMintTransaction;
import org.hiero.sdk.TransactionId;
import org.hiero.sdk.TransferTransaction;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TokenAutomaticAssociationIntegrationTest {

    @Test
    @DisplayName("Can transfer Fungible Tokens to accounts with Limited Max Auto Associations")
    void canTransferFungibleTokensToAccountsWithLimitedMaxAutoAssociations() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenId1 = EntityHelper.createFungibleToken(testEnv, 0);
            var tokenId2 = EntityHelper.createFungibleToken(testEnv, 0);
            var accountKey = PrivateKey.generateED25519();
            var accountMaxAutomaticTokenAssociations = 1;
            var receiverAccountId =
                    EntityHelper.createAccount(testEnv, accountKey, accountMaxAutomaticTokenAssociations);

            var accountInfoBeforeTokenAssociation =
                    new AccountInfoQuery().setAccountId(receiverAccountId).execute(testEnv.client);
            assertThat(accountInfoBeforeTokenAssociation.maxAutomaticTokenAssociations)
                    .isEqualTo(1);
            assertThat(accountInfoBeforeTokenAssociation.tokenRelationships.size())
                    .isEqualTo(0);

            var transferRecord = new TransferTransaction()
                    .addTokenTransfer(tokenId1, testEnv.operatorId, -1)
                    .addTokenTransfer(tokenId1, receiverAccountId, 1)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);
            assertThat(transferRecord.automaticTokenAssociations.size()).isEqualTo(1);
            assertThat(transferRecord.automaticTokenAssociations.get(0).accountId)
                    .isEqualTo(receiverAccountId);
            assertThat(transferRecord.automaticTokenAssociations.get(0).tokenId).isEqualTo(tokenId1);

            var accountInfoAfterTokenAssociation =
                    new AccountInfoQuery().setAccountId(receiverAccountId).execute(testEnv.client);
            assertThat(accountInfoAfterTokenAssociation.tokenRelationships.size())
                    .isEqualTo(1);
            assertThat(accountInfoAfterTokenAssociation.tokenRelationships.get(tokenId1).automaticAssociation)
                    .isTrue();

            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new TransferTransaction()
                                .addTokenTransfer(tokenId2, testEnv.operatorId, -1)
                                .addTokenTransfer(tokenId2, receiverAccountId, 1)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("NO_REMAINING_AUTOMATIC_ASSOCIATIONS");

            new AccountUpdateTransaction()
                    .setAccountId(receiverAccountId)
                    .setMaxAutomaticTokenAssociations(2)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var accountInfoAfterMaxAssocUpdate =
                    new AccountInfoQuery().setAccountId(receiverAccountId).execute(testEnv.client);
            assertThat(accountInfoAfterMaxAssocUpdate.maxAutomaticTokenAssociations)
                    .isEqualTo(2);

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
    @DisplayName("Can transfer Nfts to accounts with Limited Max Auto Associations")
    void canTransferNftsToAccountsWithLimitedMaxAutoAssociations() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenId1 = EntityHelper.createNft(testEnv);
            var tokenId2 = EntityHelper.createNft(testEnv);
            var accountKey = PrivateKey.generateED25519();
            var accountMaxAutomaticTokenAssociations = 1;
            var receiverAccountId =
                    EntityHelper.createAccount(testEnv, accountKey, accountMaxAutomaticTokenAssociations);

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

            var accountInfoBeforeTokenAssociation =
                    new AccountInfoQuery().setAccountId(receiverAccountId).execute(testEnv.client);
            assertThat(accountInfoBeforeTokenAssociation.maxAutomaticTokenAssociations)
                    .isEqualTo(1);
            assertThat(accountInfoBeforeTokenAssociation.tokenRelationships.size())
                    .isEqualTo(0);

            var serialsToTransfer = new ArrayList<>(mintReceiptToken2.serials);
            var nftTransferTransaction = new TransferTransaction();
            for (var serial : serialsToTransfer) {
                nftTransferTransaction.addNftTransfer(tokenId1.nft(serial), testEnv.operatorId, receiverAccountId);
            }
            var transferRecord = nftTransferTransaction.execute(testEnv.client).getRecord(testEnv.client);

            assertThat(transferRecord.automaticTokenAssociations.size()).isEqualTo(1);
            assertThat(transferRecord.automaticTokenAssociations.get(0).accountId)
                    .isEqualTo(receiverAccountId);
            assertThat(transferRecord.automaticTokenAssociations.get(0).tokenId).isEqualTo(tokenId1);

            var accountInfoAfterTokenAssociation =
                    new AccountInfoQuery().setAccountId(receiverAccountId).execute(testEnv.client);
            assertThat(accountInfoAfterTokenAssociation.tokenRelationships.size())
                    .isEqualTo(1);
            assertThat(accountInfoAfterTokenAssociation.tokenRelationships.get(tokenId1).automaticAssociation)
                    .isTrue();

            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        var serial = mintReceiptToken2.serials.get(0);
                        new TransferTransaction()
                                .addNftTransfer(tokenId2.nft(serial), testEnv.operatorId, receiverAccountId)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("NO_REMAINING_AUTOMATIC_ASSOCIATIONS");

            new AccountUpdateTransaction()
                    .setAccountId(receiverAccountId)
                    .setMaxAutomaticTokenAssociations(2)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var accountInfoAfterMaxAssocUpdate =
                    new AccountInfoQuery().setAccountId(receiverAccountId).execute(testEnv.client);
            assertThat(accountInfoAfterMaxAssocUpdate.maxAutomaticTokenAssociations)
                    .isEqualTo(2);

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

    /**
     * @notice E2E-HIP-904
     * @url https://hips.hedera.com/hip/hip-904
     */
    @Test
    @DisplayName("Can set unlimited max auto associations for Account")
    void canSetUnlimitedMaxAutoAssociationsForAccount() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var accountKey = PrivateKey.generateED25519();
            var accountMaxAutomaticTokenAssociations = -1;
            var accountId = EntityHelper.createAccount(testEnv, accountKey, accountMaxAutomaticTokenAssociations);

            new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .setMaxAutomaticTokenAssociations(accountMaxAutomaticTokenAssociations)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var accountInfoBeforeTokenAssociation =
                    new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);
            assertThat(accountInfoBeforeTokenAssociation.maxAutomaticTokenAssociations)
                    .isEqualTo(-1);
        }
    }

    /**
     * @notice E2E-HIP-904
     * @url https://hips.hedera.com/hip/hip-904
     */
    @Test
    @DisplayName("Can transfer Fungible Tokens to accounts with Unlimited Max Auto Associations")
    void canTransferFungibleTokensToAccountsWithUnlimitedMaxAutoAssociations() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenId1 = EntityHelper.createFungibleToken(testEnv, 3);
            var tokenId2 = EntityHelper.createFungibleToken(testEnv, 3);
            var accountKey = PrivateKey.generateED25519();
            var accountId1 = EntityHelper.createAccount(testEnv, accountKey, -1);
            var accountId2 = EntityHelper.createAccount(testEnv, accountKey, 100);

            new AccountUpdateTransaction()
                    .setAccountId(accountId2)
                    .setMaxAutomaticTokenAssociations(-1)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // transfer to both receivers some token1 tokens
            new TransferTransaction()
                    .addTokenTransfer(tokenId1, testEnv.operatorId, -1000)
                    .addTokenTransfer(tokenId1, accountId1, 1000)
                    .addTokenTransfer(tokenId1, testEnv.operatorId, -1000)
                    .addTokenTransfer(tokenId1, accountId2, 1000)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // transfer to both receivers some token2 tokens
            new TransferTransaction()
                    .addTokenTransfer(tokenId2, testEnv.operatorId, -1000)
                    .addTokenTransfer(tokenId2, accountId1, 1000)
                    .addTokenTransfer(tokenId2, testEnv.operatorId, -1000)
                    .addTokenTransfer(tokenId2, accountId2, 1000)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the balance of the receivers is 1000
            var accountId1Balance =
                    new AccountBalanceQuery().setAccountId(accountId1).execute(testEnv.client);

            assertThat(accountId1Balance.tokens.get(tokenId1)).isEqualTo(1000);
            assertThat(accountId1Balance.tokens.get(tokenId2)).isEqualTo(1000);

            var accountId2Balance =
                    new AccountBalanceQuery().setAccountId(accountId2).execute(testEnv.client);

            assertThat(accountId2Balance.tokens.get(tokenId1)).isEqualTo(1000);
            assertThat(accountId2Balance.tokens.get(tokenId2)).isEqualTo(1000);

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

    /**
     * @notice E2E-HIP-904
     * @url https://hips.hedera.com/hip/hip-904
     */
    @Test
    @DisplayName("Can transfer Fungible Tokens (With Decimals) to accounts with Unlimited Max Auto Associations")
    void canTransferFungibleTokensWithDecimalsToAccountsWithUnlimitedMaxAutoAssociations() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenDecimals = 10;
            var tokenId1 = EntityHelper.createFungibleToken(testEnv, tokenDecimals);
            var tokenId2 = EntityHelper.createFungibleToken(testEnv, tokenDecimals);
            var accountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, accountKey, -1);

            new TransferTransaction()
                    .addTokenTransferWithDecimals(tokenId1, testEnv.operatorId, -1000, tokenDecimals)
                    .addTokenTransferWithDecimals(tokenId1, receiverAccountId, 1000, tokenDecimals)
                    .addTokenTransferWithDecimals(tokenId2, testEnv.operatorId, -1000, tokenDecimals)
                    .addTokenTransferWithDecimals(tokenId2, receiverAccountId, 1000, tokenDecimals)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var receiverAccountBalance =
                    new AccountBalanceQuery().setAccountId(receiverAccountId).execute(testEnv.client);

            assertThat(receiverAccountBalance.tokens.get(tokenId1)).isEqualTo(1000);
            assertThat(receiverAccountBalance.tokens.get(tokenId2)).isEqualTo(1000);

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

    /**
     * @notice E2E-HIP-904
     * @url https://hips.hedera.com/hip/hip-904
     */
    @Test
    @DisplayName("Can transfer Fungible Tokens on Behalf Of Owner to account with Unlimited Max Auto Associations")
    void canTransferFungibleTokensOnBehalfOfOwnerToAccountWithUnlimitedMaxAutoAssociations() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenId1 = EntityHelper.createFungibleToken(testEnv, 3);
            var tokenId2 = EntityHelper.createFungibleToken(testEnv, 3);
            var accountKey = PrivateKey.generateED25519();
            var accountId = EntityHelper.createAccount(testEnv, accountKey, -1);
            var spenderAccountKey = PrivateKey.generateED25519();
            var spenderAccountId = EntityHelper.createAccount(testEnv, spenderAccountKey, -1);

            new AccountAllowanceApproveTransaction()
                    .approveTokenAllowance(tokenId1, testEnv.operatorId, spenderAccountId, 2000)
                    .approveTokenAllowance(tokenId2, testEnv.operatorId, spenderAccountId, 2000)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var record = new TransferTransaction()
                    .addApprovedTokenTransfer(tokenId1, testEnv.operatorId, -1000)
                    .addTokenTransfer(tokenId1, accountId, 1000)
                    .addApprovedTokenTransfer(tokenId2, testEnv.operatorId, -1000)
                    .addTokenTransfer(tokenId2, accountId, 1000)
                    .setTransactionId(TransactionId.generate(spenderAccountId))
                    .freezeWith(testEnv.client)
                    .sign(spenderAccountKey)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            var accountBalance =
                    new AccountBalanceQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(accountBalance.tokens.get(tokenId1)).isEqualTo(1000);
            assertThat(accountBalance.tokens.get(tokenId2)).isEqualTo(1000);

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

    /**
     * @notice E2E-HIP-904
     * @url https://hips.hedera.com/hip/hip-904
     */
    @Test
    @DisplayName("Can transfer Nfts to accounts with Unlimited Max Auto Associations")
    void canTransferNftsToAccountsWithUnlimitedMaxAutoAssociations() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenId1 = EntityHelper.createNft(testEnv);
            var tokenId2 = EntityHelper.createNft(testEnv);
            var accountKey = PrivateKey.generateED25519();
            var accountId1 = EntityHelper.createAccount(testEnv, accountKey, -1);
            var accountId2 = EntityHelper.createAccount(testEnv, accountKey, 100);

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

            new AccountUpdateTransaction()
                    .setAccountId(accountId2)
                    .setMaxAutomaticTokenAssociations(-1)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // transfer nft1 to both receivers, 2 for each
            new TransferTransaction()
                    .addNftTransfer(tokenId1.nft(nftSerials.get(0)), testEnv.operatorId, accountId1)
                    .addNftTransfer(tokenId1.nft(nftSerials.get(1)), testEnv.operatorId, accountId1)
                    .addNftTransfer(tokenId1.nft(nftSerials.get(2)), testEnv.operatorId, accountId2)
                    .addNftTransfer(tokenId1.nft(nftSerials.get(3)), testEnv.operatorId, accountId2)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // transfer nft2 to both receivers, 2 for each
            new TransferTransaction()
                    .addNftTransfer(tokenId2.nft(nftSerials.get(0)), testEnv.operatorId, accountId1)
                    .addNftTransfer(tokenId2.nft(nftSerials.get(1)), testEnv.operatorId, accountId1)
                    .addNftTransfer(tokenId2.nft(nftSerials.get(2)), testEnv.operatorId, accountId2)
                    .addNftTransfer(tokenId2.nft(nftSerials.get(3)), testEnv.operatorId, accountId2)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the balance of the receivers is 2
            var accountId1Balance =
                    new AccountBalanceQuery().setAccountId(accountId1).execute(testEnv.client);

            assertThat(accountId1Balance.tokens.get(tokenId1)).isEqualTo(2);
            assertThat(accountId1Balance.tokens.get(tokenId2)).isEqualTo(2);

            var accountId2Balance =
                    new AccountBalanceQuery().setAccountId(accountId2).execute(testEnv.client);

            assertThat(accountId2Balance.tokens.get(tokenId1)).isEqualTo(2);
            assertThat(accountId2Balance.tokens.get(tokenId2)).isEqualTo(2);

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

    /**
     * @notice E2E-HIP-904
     * @url https://hips.hedera.com/hip/hip-904
     */
    @Test
    @DisplayName("Can transfer Nfts on Behalf Of Owner to account with Unlimited Max Auto Associations")
    void canTransferNftsOnBehalfOfOwnerToAccountWithUnlimitedMaxAutoAssociations() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenId1 = EntityHelper.createNft(testEnv);
            var tokenId2 = EntityHelper.createNft(testEnv);
            var accountKey = PrivateKey.generateED25519();
            var accountId = EntityHelper.createAccount(testEnv, accountKey, -1);
            var spenderAccountKey = PrivateKey.generateED25519();
            var spenderAccountId = EntityHelper.createAccount(testEnv, spenderAccountKey, -1);

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

            new AccountAllowanceApproveTransaction()
                    .approveTokenNftAllowanceAllSerials(tokenId1, testEnv.operatorId, spenderAccountId)
                    .approveTokenNftAllowanceAllSerials(tokenId2, testEnv.operatorId, spenderAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TransferTransaction()
                    .addApprovedNftTransfer(tokenId1.nft(nftSerials.get(0)), testEnv.operatorId, accountId)
                    .addApprovedNftTransfer(tokenId1.nft(nftSerials.get(1)), testEnv.operatorId, accountId)
                    .addApprovedNftTransfer(tokenId2.nft(nftSerials.get(0)), testEnv.operatorId, accountId)
                    .addApprovedNftTransfer(tokenId2.nft(nftSerials.get(1)), testEnv.operatorId, accountId)
                    .setTransactionId(TransactionId.generate(spenderAccountId))
                    .freezeWith(testEnv.client)
                    .sign(spenderAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var accountBalance =
                    new AccountBalanceQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(accountBalance.tokens.get(tokenId1)).isEqualTo(2);
            assertThat(accountBalance.tokens.get(tokenId2)).isEqualTo(2);

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

    /**
     * @notice E2E-HIP-904
     * @url https://hips.hedera.com/hip/hip-904
     */
    @Test
    @DisplayName("Cannot Set Invalid Max Auto Associations Values")
    void cannotSetInvalidMaxAutoAssociationsValues() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var accountKey = PrivateKey.generateED25519();

            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new AccountCreateTransaction()
                                .setKeyWithoutAlias(accountKey)
                                .setMaxAutomaticTokenAssociations(-2)
                                .execute(testEnv.client);
                    })
                    .withMessageContaining("INVALID_MAX_AUTO_ASSOCIATIONS");

            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new AccountCreateTransaction()
                                .setKeyWithoutAlias(accountKey)
                                .setMaxAutomaticTokenAssociations(-1000)
                                .execute(testEnv.client);
                    })
                    .withMessageContaining("INVALID_MAX_AUTO_ASSOCIATIONS");

            var accountId = EntityHelper.createAccount(testEnv, accountKey, 100);

            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new AccountUpdateTransaction()
                                .setAccountId(accountId)
                                .setMaxAutomaticTokenAssociations(-2)
                                .freezeWith(testEnv.client)
                                .sign(accountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INVALID_MAX_AUTO_ASSOCIATIONS");

            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new AccountUpdateTransaction()
                                .setAccountId(accountId)
                                .setMaxAutomaticTokenAssociations(-1000)
                                .freezeWith(testEnv.client)
                                .sign(accountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INVALID_MAX_AUTO_ASSOCIATIONS");
        }
    }
}

