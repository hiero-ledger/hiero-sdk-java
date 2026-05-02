// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hiero.sdk.AccountAllowanceApproveTransaction;
import org.hiero.sdk.AccountAllowanceDeleteTransaction;
import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.NftId;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.Status;
import org.hiero.sdk.TokenAssociateTransaction;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenId;
import org.hiero.sdk.TokenMintTransaction;
import org.hiero.sdk.TokenNftInfoQuery;
import org.hiero.sdk.TokenType;
import org.hiero.sdk.TransactionId;
import org.hiero.sdk.TransferTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NftAllowancesIntegrationTest {
    @Test
    @DisplayName("Cannot transfer on behalf of `spender` account without allowance approval")
    void cannotTransferWithoutAllowanceApproval() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var spenderKey = PrivateKey.generateED25519();
            var spenderAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(spenderKey)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var receiverKey = PrivateKey.generateED25519();
            var receiverAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(receiverKey)
                    .setMaxAutomaticTokenAssociations(10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            TokenId nftTokenId = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            new TokenAssociateTransaction()
                    .setAccountId(spenderAccountId)
                    .setTokenIds(List.of(nftTokenId))
                    .freezeWith(testEnv.client)
                    .sign(spenderKey)
                    .execute(testEnv.client);

            var serials = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .addMetadata("asd".getBytes(StandardCharsets.UTF_8))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .serials;

            var nft1 = new NftId(nftTokenId, serials.get(0));

            var onBehalfOfTransactionId = TransactionId.generate(spenderAccountId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new TransferTransaction()
                            .addApprovedNftTransfer(nft1, testEnv.operatorId, receiverAccountId)
                            .setTransactionId(onBehalfOfTransactionId)
                            .freezeWith(testEnv.client)
                            .sign(spenderKey)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining(Status.SPENDER_DOES_NOT_HAVE_ALLOWANCE.toString());
        }
    }

    @Test
    @DisplayName("Cannot transfer on behalf of `spender` account after removing the allowance approval")
    void cannotTransferAfterAllowanceRemove() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var spenderKey = PrivateKey.generateED25519();
            var spenderAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(spenderKey)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var receiverKey = PrivateKey.generateED25519();
            var receiverAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(receiverKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            TokenId nftTokenId = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            new TokenAssociateTransaction()
                    .setAccountId(spenderAccountId)
                    .setTokenIds(List.of(nftTokenId))
                    .freezeWith(testEnv.client)
                    .sign(spenderKey)
                    .execute(testEnv.client);

            new TokenAssociateTransaction()
                    .setAccountId(receiverAccountId)
                    .setTokenIds(List.of(nftTokenId))
                    .freezeWith(testEnv.client)
                    .sign(receiverKey)
                    .execute(testEnv.client);

            var serials = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .addMetadata("asd1".getBytes(StandardCharsets.UTF_8))
                    .addMetadata("asd2".getBytes(StandardCharsets.UTF_8))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .serials;

            var nft1 = new NftId(nftTokenId, serials.get(0));
            var nft2 = new NftId(nftTokenId, serials.get(1));

            new AccountAllowanceApproveTransaction()
                    .approveTokenNftAllowance(nft1, testEnv.operatorId, spenderAccountId)
                    .approveTokenNftAllowance(nft2, testEnv.operatorId, spenderAccountId)
                    .execute(testEnv.client);

            new AccountAllowanceDeleteTransaction()
                    .deleteAllTokenNftAllowances(nft2, testEnv.operatorId)
                    .execute(testEnv.client);

            var onBehalfOfTransactionId = TransactionId.generate(spenderAccountId);

            new TransferTransaction()
                    .addApprovedNftTransfer(nft1, testEnv.operatorId, receiverAccountId)
                    .setTransactionId(onBehalfOfTransactionId)
                    .freezeWith(testEnv.client)
                    .sign(spenderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var info = new TokenNftInfoQuery().setNftId(nft1).execute(testEnv.client);
            assertThat(info.get(0).accountId).isEqualTo(receiverAccountId);

            var onBehalfOfTransactionId2 = TransactionId.generate(spenderAccountId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new TransferTransaction()
                            .addApprovedNftTransfer(nft2, testEnv.operatorId, receiverAccountId)
                            .setTransactionId(onBehalfOfTransactionId2)
                            .freezeWith(testEnv.client)
                            .sign(spenderKey)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining(Status.SPENDER_DOES_NOT_HAVE_ALLOWANCE.toString());
        }
    }

    @Test
    @DisplayName("Cannot remove single serial number allowance when the allowance is given for all serials at once")
    void cannotRemoveSingleSerialWhenAllowanceIsGivenForAll() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var spenderKey = PrivateKey.generateED25519();
            var spenderAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(spenderKey)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var receiverKey = PrivateKey.generateED25519();
            var receiverAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(receiverKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            TokenId nftTokenId = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            new TokenAssociateTransaction()
                    .setAccountId(spenderAccountId)
                    .setTokenIds(List.of(nftTokenId))
                    .freezeWith(testEnv.client)
                    .sign(spenderKey)
                    .execute(testEnv.client);

            new TokenAssociateTransaction()
                    .setAccountId(receiverAccountId)
                    .setTokenIds(List.of(nftTokenId))
                    .freezeWith(testEnv.client)
                    .sign(receiverKey)
                    .execute(testEnv.client);

            var serials = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .addMetadata("asd1".getBytes(StandardCharsets.UTF_8))
                    .addMetadata("asd2".getBytes(StandardCharsets.UTF_8))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .serials;

            var nft1 = new NftId(nftTokenId, serials.get(0));
            var nft2 = new NftId(nftTokenId, serials.get(1));

            new AccountAllowanceApproveTransaction()
                    .approveTokenNftAllowanceAllSerials(nftTokenId, testEnv.operatorId, spenderAccountId)
                    .execute(testEnv.client);

            var onBehalfOfTransactionId = TransactionId.generate(spenderAccountId);

            new TransferTransaction()
                    .addApprovedNftTransfer(nft1, testEnv.operatorId, receiverAccountId)
                    .setTransactionId(onBehalfOfTransactionId)
                    .freezeWith(testEnv.client)
                    .sign(spenderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // hopefully in the future this should end up with a precheck error provided from services
            new AccountAllowanceDeleteTransaction()
                    .deleteAllTokenNftAllowances(nft2, testEnv.operatorId)
                    .execute(testEnv.client);

            var onBehalfOfTransactionId2 = TransactionId.generate(spenderAccountId);

            new TransferTransaction()
                    .addApprovedNftTransfer(nft2, testEnv.operatorId, receiverAccountId)
                    .setTransactionId(onBehalfOfTransactionId2)
                    .freezeWith(testEnv.client)
                    .sign(spenderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var infoNft1 = new TokenNftInfoQuery().setNftId(nft1).execute(testEnv.client);

            var infoNft2 = new TokenNftInfoQuery().setNftId(nft2).execute(testEnv.client);

            assertThat(infoNft1.get(0).accountId).isEqualTo(receiverAccountId);
            assertThat(infoNft2.get(0).accountId).isEqualTo(receiverAccountId);
        }
    }

    @Test
    @DisplayName(
            "Account, which given the allowance for all serials at once, should be able to give allowances for single serial numbers to other accounts")
    void accountGivenAllowanceForAllShouldBeAbleToGiveAllowanceForSingle() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var delegatingSpenderKey = PrivateKey.generateED25519();
            var delegatingSpenderAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(delegatingSpenderKey)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var spenderKey = PrivateKey.generateED25519();
            var spenderAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(spenderKey)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var receiverKey = PrivateKey.generateED25519();
            var receiverAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(receiverKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            TokenId nftTokenId = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            new TokenAssociateTransaction()
                    .setAccountId(delegatingSpenderAccountId)
                    .setTokenIds(List.of(nftTokenId))
                    .freezeWith(testEnv.client)
                    .sign(spenderKey)
                    .execute(testEnv.client);

            new TokenAssociateTransaction()
                    .setAccountId(receiverAccountId)
                    .setTokenIds(List.of(nftTokenId))
                    .freezeWith(testEnv.client)
                    .sign(receiverKey)
                    .execute(testEnv.client);

            var serials = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .addMetadata("asd1".getBytes(StandardCharsets.UTF_8))
                    .addMetadata("asd2".getBytes(StandardCharsets.UTF_8))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .serials;

            var nft1 = new NftId(nftTokenId, serials.get(0));
            var nft2 = new NftId(nftTokenId, serials.get(1));

            new AccountAllowanceApproveTransaction()
                    .approveTokenNftAllowanceAllSerials(nftTokenId, testEnv.operatorId, delegatingSpenderAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new AccountAllowanceApproveTransaction()
                    .approveTokenNftAllowance(nft1, testEnv.operatorId, spenderAccountId, delegatingSpenderAccountId)
                    .freezeWith(testEnv.client)
                    .sign(delegatingSpenderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var onBehalfOfTransactionId = TransactionId.generate(spenderAccountId);

            new TransferTransaction()
                    .addApprovedNftTransfer(nft1, testEnv.operatorId, receiverAccountId)
                    .setTransactionId(onBehalfOfTransactionId)
                    .freezeWith(testEnv.client)
                    .sign(spenderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var onBehalfOfTransactionId2 = TransactionId.generate(spenderAccountId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new TransferTransaction()
                            .addApprovedNftTransfer(nft2, testEnv.operatorId, receiverAccountId)
                            .setTransactionId(onBehalfOfTransactionId2)
                            .freezeWith(testEnv.client)
                            .sign(spenderKey)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining(Status.SPENDER_DOES_NOT_HAVE_ALLOWANCE.toString());

            var infoNft1 = new TokenNftInfoQuery().setNftId(nft1).execute(testEnv.client);

            var infoNft2 = new TokenNftInfoQuery().setNftId(nft2).execute(testEnv.client);

            assertThat(infoNft1.get(0).accountId).isEqualTo(receiverAccountId);
            assertThat(infoNft2.get(0).accountId).isEqualTo(testEnv.operatorId);
        }
    }
}
