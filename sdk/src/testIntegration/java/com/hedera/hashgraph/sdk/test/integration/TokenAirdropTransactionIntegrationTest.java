// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static com.hedera.hashgraph.sdk.test.integration.EntityHelper.fungibleInitialBalance;
import static com.hedera.hashgraph.sdk.test.integration.EntityHelper.mitedNfts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.hedera.hashgraph.sdk.AccountAllowanceApproveTransaction;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.CustomFixedFee;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenAirdropTransaction;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenMintTransaction;
import com.hedera.hashgraph.sdk.TokenSupplyType;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransferTransaction;
import java.util.Collections;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenAirdropTransactionIntegrationTest {

    private final int amount = 100;

    @Test
    @DisplayName("Transfers tokens when the account is associated")
    void canAirdropAssociatedTokens() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible and nf token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);
            var nftID = EntityHelper.createNft(testEnv);
            // mint some NFTs
            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftID)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
            var nftSerials = mintReceipt.serials;

            // create receiver with unlimited auto associations and receiverSig = false
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, -1);

            // airdrop the tokens
            new TokenAirdropTransaction()
                    .addNftTransfer(nftID.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftID.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the receiver holds the tokens via query
            var receiverAccountBalance =
                    new AccountBalanceQuery().setAccountId(receiverAccountId).execute(testEnv.client);
            assertEquals(amount, receiverAccountBalance.tokens.get(tokenID));
            assertEquals(2, receiverAccountBalance.tokens.get(nftID));

            // verify the operator does not hold the tokens
            var operatorBalance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).execute(testEnv.client);
            assertEquals(fungibleInitialBalance - amount, operatorBalance.tokens.get(tokenID));
            assertEquals(mitedNfts - 2, operatorBalance.tokens.get(nftID));
        }
    }

    @Test
    @DisplayName("Tokens are in pending state when the account is not associated")
    void canAirdropNonAssociatedTokens() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible and nf token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);
            var nftID = EntityHelper.createNft(testEnv);
            // mint some NFTs
            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftID)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
            var nftSerials = mintReceipt.serials;

            // create receiver with 0 auto associations and receiverSig = false
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 0);

            // airdrop the tokens
            var txn = new TokenAirdropTransaction()
                    .addNftTransfer(nftID.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftID.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client);
            txn.setValidateStatus(true).getReceipt(testEnv.client);
            var record = txn.getRecord(testEnv.client);

            // verify in the transaction record the pending airdrops
            assertThat(record.pendingAirdropRecords).isNotNull();
            assertFalse(record.pendingAirdropRecords.isEmpty());

            // verify the receiver does not hold the tokens via query
            var receiverAccountBalance =
                    new AccountBalanceQuery().setAccountId(receiverAccountId).execute(testEnv.client);
            assertNull(receiverAccountBalance.tokens.get(tokenID));
            assertNull(receiverAccountBalance.tokens.get(nftID));

            // verify the operator does hold the tokens
            var operatorBalance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).execute(testEnv.client);
            assertEquals(fungibleInitialBalance, operatorBalance.tokens.get(tokenID));
            assertEquals(mitedNfts, operatorBalance.tokens.get(nftID));
        }
    }

    @Test
    @DisplayName("Airdrop creates a hollow account and transfers the tokens")
    void canAirdropToAlias() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible and nf token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);
            var nftID = EntityHelper.createNft(testEnv);
            // mint some NFTs
            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftID)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
            var nftSerials = mintReceipt.serials;

            // airdrop the tokens to an alias
            PrivateKey privateKey = PrivateKey.generateED25519();
            PublicKey publicKey = privateKey.getPublicKey();

            AccountId aliasAccountId = publicKey.toAccountId(0, 0);

            // should lazy-create and transfer the tokens
            new TokenAirdropTransaction()
                    .addNftTransfer(nftID.nft(nftSerials.get(0)), testEnv.operatorId, aliasAccountId)
                    .addNftTransfer(nftID.nft(nftSerials.get(1)), testEnv.operatorId, aliasAccountId)
                    .addTokenTransfer(tokenID, aliasAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the receiver holds the tokens via query
            var receiverAccountBalance =
                    new AccountBalanceQuery().setAccountId(aliasAccountId).execute(testEnv.client);
            assertEquals(amount, receiverAccountBalance.tokens.get(tokenID));
            assertEquals(2, receiverAccountBalance.tokens.get(nftID));

            // verify the operator does not hold the tokens
            var operatorBalance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).execute(testEnv.client);
            assertEquals(fungibleInitialBalance - amount, operatorBalance.tokens.get(tokenID));
            assertEquals(mitedNfts - 2, operatorBalance.tokens.get(nftID));
        }
    }

    @Test
    @DisplayName("Can airdrop with custom fees")
    void canAirdropWithCustomFee() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create receiver unlimited auto associations and receiverSig = false
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, -1);

            // create fungible token with custom fee another token
            var customFeeTokenID = EntityHelper.createFungibleToken(testEnv, 3);

            // make the custom fee to be paid by the sender and the fee collector to be the operator account
            CustomFixedFee fee = new CustomFixedFee()
                    .setFeeCollectorAccountId(testEnv.operatorId)
                    .setDenominatingTokenId(customFeeTokenID)
                    .setAmount(1)
                    .setAllCollectorsAreExempt(true);

            var tokenID = new TokenCreateTransaction()
                    .setTokenName("Test Fungible Token")
                    .setTokenSymbol("TFT")
                    .setTokenMemo("I was created for integration tests")
                    .setDecimals(3)
                    .setInitialSupply(fungibleInitialBalance)
                    .setMaxSupply(fungibleInitialBalance)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setSupplyType(TokenSupplyType.FINITE)
                    .setAdminKey(testEnv.operatorKey)
                    .setFreezeKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setMetadataKey(testEnv.operatorKey)
                    .setPauseKey(testEnv.operatorKey)
                    .setCustomFees(Collections.singletonList(fee))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            // create sender account with unlimited associations and send some tokens to it
            var senderKey = PrivateKey.generateED25519();
            var senderAccountID = EntityHelper.createAccount(testEnv, senderKey, -1);

            // associate the token to the sender
            new TokenAssociateTransaction()
                    .setAccountId(senderAccountID)
                    .setTokenIds(Collections.singletonList(customFeeTokenID))
                    .freezeWith(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // send tokens to the sender
            new TransferTransaction()
                    .addTokenTransfer(customFeeTokenID, testEnv.operatorId, -amount)
                    .addTokenTransfer(customFeeTokenID, senderAccountID, amount)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TransferTransaction()
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .addTokenTransfer(tokenID, senderAccountID, amount)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // airdrop the tokens from the sender to the receiver
            new TokenAirdropTransaction()
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, senderAccountID, -amount)
                    .freezeWith(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // verify the custom fee has been paid by the sender to the collector
            var receiverAccountBalance =
                    new AccountBalanceQuery().setAccountId(receiverAccountId).execute(testEnv.client);
            assertEquals(amount, receiverAccountBalance.tokens.get(tokenID));

            var senderAccountBalance =
                    new AccountBalanceQuery().setAccountId(senderAccountID).execute(testEnv.client);
            assertEquals(0, senderAccountBalance.tokens.get(tokenID));
            assertEquals(amount - 1, senderAccountBalance.tokens.get(customFeeTokenID));

            var operatorBalance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).execute(testEnv.client);
            assertEquals(fungibleInitialBalance - amount + 1, operatorBalance.tokens.get(customFeeTokenID));
            assertEquals(fungibleInitialBalance - amount, operatorBalance.tokens.get(tokenID));
        }
    }

    @Test
    @DisplayName("Can airdrop ft with receiverSig=true")
    void canAirdropTokensWithReceiverSigRequiredFungible() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);

            // create receiver with unlimited auto associations and receiverSig = true
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(receiverAccountKey)
                    .setInitialBalance(new Hbar(1))
                    .setReceiverSignatureRequired(true)
                    .setMaxAutomaticTokenAssociations(-1)
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // airdrop the tokens
            new TokenAirdropTransaction()
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can airdrop nft with receiverSig=true")
    void canAirdropTokensWithReceiverSigRequiredNFT() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create nft
            var nftID = EntityHelper.createNft(testEnv);
            // mint some NFTs
            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftID)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
            var nftSerials = mintReceipt.serials;

            // create receiver with unlimited auto associations and receiverSig = true
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(receiverAccountKey)
                    .setInitialBalance(new Hbar(1))
                    .setReceiverSignatureRequired(true)
                    .setMaxAutomaticTokenAssociations(-1)
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // airdrop the tokens
            new TokenAirdropTransaction()
                    .addNftTransfer(nftID.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftID.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot airdrop ft with no balance")
    void cannotAirdropTokensWithAllowanceAndWithoutBalanceFungible() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);

            // create spender and approve to it some tokens
            var spenderKey = PrivateKey.generateED25519();
            var spenderAccountID = EntityHelper.createAccount(testEnv, spenderKey, -1);

            // create sender
            var senderKey = PrivateKey.generateED25519();
            var senderAccountID = EntityHelper.createAccount(testEnv, senderKey, -1);

            // transfer ft to sender
            new TransferTransaction()
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .addTokenTransfer(tokenID, senderAccountID, amount)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // approve allowance to the spender
            new AccountAllowanceApproveTransaction()
                    .approveTokenAllowance(tokenID, senderAccountID, spenderAccountID, amount)
                    .freezeWith(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // airdrop the tokens from the sender to the spender via approval
            // fails with NOT_SUPPORTED
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenAirdropTransaction()
                                .addTokenTransfer(tokenID, spenderAccountID, amount)
                                .addApprovedTokenTransfer(tokenID, spenderAccountID, -amount)
                                .setTransactionId(TransactionId.generate(spenderAccountID))
                                .freezeWith(testEnv.client)
                                .sign(spenderKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.NOT_SUPPORTED.toString());
        }
    }

    @Test
    @DisplayName("Cannot airdrop nft with no balance")
    void cannotAirdropTokensWithAllowanceAndWithoutBalanceNFT() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create nft
            var nftID = EntityHelper.createNft(testEnv);
            // mint some NFTs
            var mintReceipt = new TokenMintTransaction()
                    .setTokenId(nftID)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
            var nftSerials = mintReceipt.serials;

            // create spender and approve to it some tokens
            var spenderKey = PrivateKey.generateED25519();
            var spenderAccountID = EntityHelper.createAccount(testEnv, spenderKey, -1);

            // create sender
            var senderKey = PrivateKey.generateED25519();
            var senderAccountID = EntityHelper.createAccount(testEnv, senderKey, -1);

            // transfer ft to sender
            new TransferTransaction()
                    .addNftTransfer(nftID.nft(nftSerials.get(0)), testEnv.operatorId, senderAccountID)
                    .addNftTransfer(nftID.nft(nftSerials.get(1)), testEnv.operatorId, senderAccountID)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // approve allowance to the spender
            new AccountAllowanceApproveTransaction()
                    .approveTokenNftAllowance(nftID.nft(nftSerials.get(0)), senderAccountID, spenderAccountID)
                    .approveTokenNftAllowance(nftID.nft(nftSerials.get(1)), senderAccountID, spenderAccountID)
                    .freezeWith(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // airdrop the tokens from the sender to the spender via approval
            // fails with NOT_SUPPORTED
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenAirdropTransaction()
                                .addApprovedNftTransfer(nftID.nft(nftSerials.get(0)), senderAccountID, spenderAccountID)
                                .addApprovedNftTransfer(nftID.nft(nftSerials.get(1)), senderAccountID, spenderAccountID)
                                .setTransactionId(TransactionId.generate(spenderAccountID))
                                .freezeWith(testEnv.client)
                                .sign(spenderKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.NOT_SUPPORTED.toString());
        }
    }

    @Test
    @DisplayName("Cannot airdrop with invalid body")
    @Disabled
    void cannotAirdropTokensWithInvalidBody() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // airdrop with no tokenID or NftID
            // fails with EMPTY_TOKEN_TRANSFER_BODY
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenAirdropTransaction().execute(testEnv.client).getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.EMPTY_TOKEN_TRANSFER_BODY.toString());

            // create fungible token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);

            // airdrop with invalid transfers
            // fails with INVALID_TRANSACTION_BODY
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenAirdropTransaction()
                                .addTokenTransfer(tokenID, testEnv.operatorId, 100)
                                .addTokenTransfer(tokenID, testEnv.operatorId, 100)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_TRANSACTION_BODY.toString());
        }
    }
}
