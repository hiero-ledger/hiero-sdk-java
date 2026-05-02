// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.hiero.sdk.test.integration.EntityHelper.fungibleInitialBalance;
import static org.hiero.sdk.test.integration.EntityHelper.mitedNfts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import org.hiero.sdk.AccountBalanceQuery;
import org.hiero.sdk.PendingAirdropId;
import org.hiero.sdk.PendingAirdropRecord;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.Status;
import org.hiero.sdk.TokenAirdropTransaction;
import org.hiero.sdk.TokenAssociateTransaction;
import org.hiero.sdk.TokenCancelAirdropTransaction;
import org.hiero.sdk.TokenDeleteTransaction;
import org.hiero.sdk.TokenFreezeTransaction;
import org.hiero.sdk.TokenMintTransaction;
import org.hiero.sdk.TokenPauseTransaction;
import org.hiero.sdk.TransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenAirdropCancelIntegrationTest {

    private final int amount = 100;

    @Test
    @DisplayName("Cancels the tokens when they are in pending state")
    void canCancelTokens() throws Exception {
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

            // create receiver with 0 auto associations
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 0);

            // airdrop the tokens
            var record = new TokenAirdropTransaction()
                    .addNftTransfer(nftID.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .addNftTransfer(nftID.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // sender cancels the tokens
            record = new TokenCancelAirdropTransaction()
                    .addPendingAirdrop(record.pendingAirdropRecords.get(0).getPendingAirdropId())
                    .addPendingAirdrop(record.pendingAirdropRecords.get(1).getPendingAirdropId())
                    .addPendingAirdrop(record.pendingAirdropRecords.get(2).getPendingAirdropId())
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // verify in the transaction record the pending airdrop ids for nft and ft - should no longer exist
            assertEquals(0, record.pendingAirdropRecords.size());

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
    @DisplayName("Cancels the tokens when token is frozen")
    void canCancelTokensWhenTokenIsFrozen() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);

            // create receiver with 0 auto associations
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 0);

            // airdrop the tokens
            var record = new TokenAirdropTransaction()
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // associate
            new TokenAssociateTransaction()
                    .setAccountId(receiverAccountId)
                    .setTokenIds(Collections.singletonList(tokenID))
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // freeze the token
            new TokenFreezeTransaction()
                    .setAccountId(receiverAccountId)
                    .setTokenId(tokenID)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // cancel
            new TokenCancelAirdropTransaction()
                    .addPendingAirdrop(record.pendingAirdropRecords.get(0).getPendingAirdropId())
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cancels the tokens when token is paused")
    void canCancelTokensWhenTokenIsPaused() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);

            // create receiver with 0 auto associations
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 0);

            // airdrop the tokens
            var record = new TokenAirdropTransaction()
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // pause the token
            new TokenPauseTransaction()
                    .setTokenId(tokenID)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // cancel
            new TokenCancelAirdropTransaction()
                    .addPendingAirdrop(record.pendingAirdropRecords.get(0).getPendingAirdropId())
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cancels the tokens when token is deleted")
    void canCancelTokensWhenTokenIsDeleted() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);

            // create receiver with 0 auto associations
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 0);

            // airdrop the tokens
            var record = new TokenAirdropTransaction()
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // delete the token
            new TokenDeleteTransaction()
                    .setTokenId(tokenID)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // cancel
            new TokenCancelAirdropTransaction()
                    .addPendingAirdrop(record.pendingAirdropRecords.get(0).getPendingAirdropId())
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cancels the tokens when they are in pending state to multiple receivers")
    void canCancelTokensToMultipleReceivers() throws Exception {
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

            // create receiver1 with 0 auto associations
            var receiver1AccountKey = PrivateKey.generateED25519();
            var receiver1AccountId = EntityHelper.createAccount(testEnv, receiver1AccountKey, 0);

            // create receiver2 with 0 auto associations
            var receiver2AccountKey = PrivateKey.generateED25519();
            var receiver2AccountId = EntityHelper.createAccount(testEnv, receiver2AccountKey, 0);

            // airdrop the tokens to both
            var record = new TokenAirdropTransaction()
                    .addNftTransfer(nftID.nft(nftSerials.get(0)), testEnv.operatorId, receiver1AccountId)
                    .addNftTransfer(nftID.nft(nftSerials.get(1)), testEnv.operatorId, receiver1AccountId)
                    .addTokenTransfer(tokenID, receiver1AccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .addNftTransfer(nftID.nft(nftSerials.get(2)), testEnv.operatorId, receiver2AccountId)
                    .addNftTransfer(nftID.nft(nftSerials.get(3)), testEnv.operatorId, receiver2AccountId)
                    .addTokenTransfer(tokenID, receiver2AccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // verify the txn record
            assertEquals(6, record.pendingAirdropRecords.size());

            // cancel the tokens signing with receiver1 and receiver2
            var pendingAirdropIDs = record.pendingAirdropRecords.stream()
                    .map(PendingAirdropRecord::getPendingAirdropId)
                    .toList();
            record = new TokenCancelAirdropTransaction()
                    .setPendingAirdropIds(pendingAirdropIDs)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // verify in the transaction record the pending airdrop ids for nft and ft - should no longer exist
            assertEquals(0, record.pendingAirdropRecords.size());

            // verify receiver1 does not hold the tokens via query
            var receiverAccountBalance =
                    new AccountBalanceQuery().setAccountId(receiver1AccountId).execute(testEnv.client);
            assertNull(receiverAccountBalance.tokens.get(tokenID));
            assertNull(receiverAccountBalance.tokens.get(nftID));

            // verify receiver2 does not hold the tokens via query
            var receiver2AccountBalance =
                    new AccountBalanceQuery().setAccountId(receiver1AccountId).execute(testEnv.client);
            assertNull(receiver2AccountBalance.tokens.get(tokenID));
            assertNull(receiver2AccountBalance.tokens.get(nftID));

            // verify the operator does hold the tokens
            var operatorBalance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).execute(testEnv.client);
            assertEquals(fungibleInitialBalance, operatorBalance.tokens.get(tokenID));
            assertEquals(mitedNfts, operatorBalance.tokens.get(nftID));
        }
    }

    @Test
    @DisplayName("Cancels the tokens when they are in pending state from multiple airdrop transactions")
    void canCancelTokensFromMultipleAirdropTxns() throws Exception {
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

            // create receiver with 0 auto associations
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 0);

            // airdrop some of the tokens to the receiver
            var record1 = new TokenAirdropTransaction()
                    .addNftTransfer(nftID.nft(nftSerials.get(0)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);
            // airdrop some of the tokens to the receiver
            var record2 = new TokenAirdropTransaction()
                    .addNftTransfer(nftID.nft(nftSerials.get(1)), testEnv.operatorId, receiverAccountId)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);
            // airdrop some of the tokens to the receiver
            var record3 = new TokenAirdropTransaction()
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // get the PendingIds from the records
            var pendingAirdropIDs = new ArrayList<PendingAirdropId>();
            pendingAirdropIDs.add(record1.pendingAirdropRecords.get(0).getPendingAirdropId());
            pendingAirdropIDs.add(record2.pendingAirdropRecords.get(0).getPendingAirdropId());
            pendingAirdropIDs.add(record3.pendingAirdropRecords.get(0).getPendingAirdropId());

            // cancel the all the tokens with the receiver
            var record = new TokenCancelAirdropTransaction()
                    .setPendingAirdropIds(pendingAirdropIDs)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // verify in the transaction record the pending airdrop ids for nft and ft - should no longer exist
            assertEquals(0, record.pendingAirdropRecords.size());

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
    @DisplayName("Cannot cancel the tokens when they are not airdropped")
    void cannotCancelTokensForNonExistingAirdrop() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);

            // create receiver with 0 auto associations
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 0);

            // airdrop the tokens
            var record = new TokenAirdropTransaction()
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // create receiver with 0 auto associations
            var randomAccountKey = PrivateKey.generateED25519();
            var randomAccount = EntityHelper.createAccount(testEnv, randomAccountKey, 0);

            // cancel the tokens with the random account which has not created pending airdrops
            // fails with INVALID_SIGNATURE
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCancelAirdropTransaction()
                                .setTransactionId(TransactionId.generate(randomAccount))
                                .addPendingAirdrop(
                                        record.pendingAirdropRecords.get(0).getPendingAirdropId())
                                .execute(testEnv.client)
                                .getRecord(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    @Test
    @DisplayName("Cannot cancel the tokens when they are already canceled")
    void canonCancelTokensForAlreadyCanceledAirdrop() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);

            // create receiver with 0 auto associations
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 0);

            // airdrop the tokens
            var record = new TokenAirdropTransaction()
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // cancel the tokens with the receiver
            new TokenCancelAirdropTransaction()
                    .addPendingAirdrop(record.pendingAirdropRecords.get(0).getPendingAirdropId())
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // cancel the tokens with the receiver again
            // fails with INVALID_PENDING_AIRDROP_ID
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCancelAirdropTransaction()
                                .addPendingAirdrop(
                                        record.pendingAirdropRecords.get(0).getPendingAirdropId())
                                .execute(testEnv.client)
                                .getRecord(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_PENDING_AIRDROP_ID.toString());
        }
    }

    @Test
    @DisplayName("Cannot cancel the tokens with empty list")
    void canonCancelWithEmptyPendingAirdropsList() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // cancel the tokens with the receiver without setting pendingAirdropIds
            // fails with EMPTY_PENDING_AIRDROP_ID_LIST
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCancelAirdropTransaction()
                                .execute(testEnv.client)
                                .getRecord(testEnv.client);
                    })
                    .withMessageContaining(Status.EMPTY_PENDING_AIRDROP_ID_LIST.toString());
        }
    }

    @Test
    @DisplayName("Cannot cancel the tokens with duplicate entries")
    void cannotCancelTokensWithDuplicateEntries() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // create fungible token
            var tokenID = EntityHelper.createFungibleToken(testEnv, 3);

            // create receiver with 0 auto associations
            var receiverAccountKey = PrivateKey.generateED25519();
            var receiverAccountId = EntityHelper.createAccount(testEnv, receiverAccountKey, 0);

            // airdrop the tokens
            var record = new TokenAirdropTransaction()
                    .addTokenTransfer(tokenID, receiverAccountId, amount)
                    .addTokenTransfer(tokenID, testEnv.operatorId, -amount)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // cancel the tokens with duplicate pending airdrop token ids
            // fails with PENDING_AIRDROP_ID_REPEATED
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCancelAirdropTransaction()
                                .addPendingAirdrop(
                                        record.pendingAirdropRecords.get(0).getPendingAirdropId())
                                .addPendingAirdrop(
                                        record.pendingAirdropRecords.get(0).getPendingAirdropId())
                                .execute(testEnv.client)
                                .getRecord(testEnv.client);
                    })
                    .withMessageContaining(Status.PENDING_AIRDROP_ID_REPEATED.toString());
        }
    }
}
