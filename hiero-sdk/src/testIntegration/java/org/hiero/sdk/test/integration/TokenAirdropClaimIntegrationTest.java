// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import org.hiero.sdk.TokenClaimAirdropTransaction;
import org.hiero.sdk.TokenDeleteTransaction;
import org.hiero.sdk.TokenFreezeTransaction;
import org.hiero.sdk.TokenMintTransaction;
import org.hiero.sdk.TokenPauseTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenAirdropClaimIntegrationTest {

    private final int amount = 100;

    @Test
    @DisplayName("Claims the tokens when they are in pending state")
    void canClaimTokens() throws Exception {
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

            // verify the txn record
            assertEquals(3, record.pendingAirdropRecords.size());

            assertEquals(100, record.pendingAirdropRecords.get(0).getPendingAirdropAmount());
            assertEquals(
                    tokenID,
                    record.pendingAirdropRecords.get(0).getPendingAirdropId().getTokenId());
            assertNull(record.pendingAirdropRecords.get(0).getPendingAirdropId().getNftId());

            assertEquals(0, record.pendingAirdropRecords.get(1).getPendingAirdropAmount());
            assertEquals(
                    nftID.nft(1),
                    record.pendingAirdropRecords.get(1).getPendingAirdropId().getNftId());
            assertNull(record.pendingAirdropRecords.get(1).getPendingAirdropId().getTokenId());

            assertEquals(0, record.pendingAirdropRecords.get(2).getPendingAirdropAmount());
            assertEquals(
                    nftID.nft(2),
                    record.pendingAirdropRecords.get(2).getPendingAirdropId().getNftId());
            assertNull(record.pendingAirdropRecords.get(2).getPendingAirdropId().getTokenId());

            // claim the tokens with the receiver
            record = new TokenClaimAirdropTransaction()
                    .addPendingAirdrop(record.pendingAirdropRecords.get(0).getPendingAirdropId())
                    .addPendingAirdrop(record.pendingAirdropRecords.get(1).getPendingAirdropId())
                    .addPendingAirdrop(record.pendingAirdropRecords.get(2).getPendingAirdropId())
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // verify in the transaction record the pending airdrop ids for nft and ft - should no longer exist
            assertEquals(0, record.pendingAirdropRecords.size());

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
    @DisplayName("Claims the tokens when they are in pending state to multiple receivers")
    void canClaimTokensToMultipleReceivers() throws Exception {
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

            // claim the tokens signing with receiver1 and receiver2
            var pendingAirdropIDs = record.pendingAirdropRecords.stream()
                    .map(PendingAirdropRecord::getPendingAirdropId)
                    .toList();
            record = new TokenClaimAirdropTransaction()
                    .setPendingAirdropIds(pendingAirdropIDs)
                    .freezeWith(testEnv.client)
                    .sign(receiver1AccountKey)
                    .sign(receiver2AccountKey)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // verify in the transaction record the pending airdrop ids for nft and ft - should no longer exist
            assertEquals(0, record.pendingAirdropRecords.size());
            // verify receiver1 holds the tokens via query
            var receiverAccountBalance =
                    new AccountBalanceQuery().setAccountId(receiver1AccountId).execute(testEnv.client);
            assertEquals(amount, receiverAccountBalance.tokens.get(tokenID));
            assertEquals(2, receiverAccountBalance.tokens.get(nftID));

            // verify receiver2 holds the tokens via query
            var receiver2AccountBalance =
                    new AccountBalanceQuery().setAccountId(receiver1AccountId).execute(testEnv.client);
            assertEquals(amount, receiver2AccountBalance.tokens.get(tokenID));
            assertEquals(2, receiver2AccountBalance.tokens.get(nftID));

            // verify the operator does not hold the tokens
            var operatorBalance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).execute(testEnv.client);
            assertEquals(fungibleInitialBalance - amount * 2, operatorBalance.tokens.get(tokenID));
            assertEquals(mitedNfts - 4, operatorBalance.tokens.get(nftID));
        }
    }

    @Test
    @DisplayName("Claims the tokens when they are in pending state from multiple airdrop transactions")
    void canClaimTokensFromMultipleAirdropTxns() throws Exception {
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

            // claim the all the tokens with the receiver
            var record = new TokenClaimAirdropTransaction()
                    .setPendingAirdropIds(pendingAirdropIDs)
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // verify in the transaction record the pending airdrop ids for nft and ft - should no longer exist
            assertEquals(0, record.pendingAirdropRecords.size());

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
    @DisplayName("Cannot claim the tokens when they are not airdropped")
    void cannotClaimTokensForNonExistingAirdrop() throws Exception {
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

            // claim the tokens with the operator which does not have pending airdrops
            // fails with INVALID_SIGNATURE
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenClaimAirdropTransaction()
                                .addPendingAirdrop(
                                        record.pendingAirdropRecords.get(0).getPendingAirdropId())
                                .execute(testEnv.client)
                                .getRecord(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    @Test
    @DisplayName("Cannot claim the tokens when they are already claimed")
    void cannotClaimTokensForAlreadyClaimedAirdrop() throws Exception {
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

            // claim the tokens with the receiver
            new TokenClaimAirdropTransaction()
                    .addPendingAirdrop(record.pendingAirdropRecords.get(0).getPendingAirdropId())
                    .freezeWith(testEnv.client)
                    .sign(receiverAccountKey)
                    .execute(testEnv.client)
                    .getRecord(testEnv.client);

            // claim the tokens with the receiver again
            // fails with INVALID_PENDING_AIRDROP_ID
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenClaimAirdropTransaction()
                                .addPendingAirdrop(
                                        record.pendingAirdropRecords.get(0).getPendingAirdropId())
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getRecord(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_PENDING_AIRDROP_ID.toString());
        }
    }

    @Test
    @DisplayName("Cannot claim the tokens with empty list")
    void cannotClaimWithEmptyPendingAirdropsList() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            // claim the tokens with the receiver without setting pendingAirdropIds
            // fails with EMPTY_PENDING_AIRDROP_ID_LIST
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenClaimAirdropTransaction()
                                .execute(testEnv.client)
                                .getRecord(testEnv.client);
                    })
                    .withMessageContaining(Status.EMPTY_PENDING_AIRDROP_ID_LIST.toString());
        }
    }

    @Test
    @DisplayName("Cannot claim the tokens with duplicate entries")
    void cannotClaimTokensWithDuplicateEntries() throws Exception {
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

            // claim the tokens with duplicate pending airdrop token ids
            // fails with PENDING_AIRDROP_ID_REPEATED
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenClaimAirdropTransaction()
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

    @Test
    @DisplayName("Cannot claim the tokens when token is paused")
    void cannotClaimTokensWhenTokenIsPaused() throws Exception {
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

            // claim the tokens with receiver
            // fails with TOKEN_IS_PAUSED
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenClaimAirdropTransaction()
                                .addPendingAirdrop(
                                        record.pendingAirdropRecords.get(0).getPendingAirdropId())
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getRecord(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_IS_PAUSED.toString());
        }
    }

    @Test
    @DisplayName("Cannot claim the tokens when token is deleted")
    void cannotClaimTokensWhenTokenIsDeleted() throws Exception {
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

            // claim the tokens with receiver
            // fails with TOKEN_IS_DELETED
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenClaimAirdropTransaction()
                                .addPendingAirdrop(
                                        record.pendingAirdropRecords.get(0).getPendingAirdropId())
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getRecord(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_WAS_DELETED.toString());
        }
    }

    @Test
    @DisplayName("Cannot claim the tokens when token is frozen")
    void cannotClaimTokensWhenTokenIsFrozen() throws Exception {
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

            // claim the tokens with receiver
            // fails with ACCOUNT_FROZEN_FOR_TOKEN
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenClaimAirdropTransaction()
                                .addPendingAirdrop(
                                        record.pendingAirdropRecords.get(0).getPendingAirdropId())
                                .freezeWith(testEnv.client)
                                .sign(receiverAccountKey)
                                .execute(testEnv.client)
                                .getRecord(testEnv.client);
                    })
                    .withMessageContaining(Status.ACCOUNT_FROZEN_FOR_TOKEN.toString());
        }
    }
}
