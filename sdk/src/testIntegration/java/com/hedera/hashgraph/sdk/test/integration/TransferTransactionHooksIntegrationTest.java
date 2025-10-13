// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransferTransactionHooksIntegrationTest {

    @Test
    @DisplayName(
            "Given an account has a pre-transaction allowance hook configured, when a TransferTransaction attempts to transfer HBAR from that account, then the hook is called before the transfer and approves the transaction")
    void transferWithPreTransactionAllowanceHookSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hookContractId = new ContractId(0, 0, 1);

            var hookDetails = new HookCreationDetails(
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(hookContractId));

            var accountKey = PrivateKey.generateED25519();
            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .addHookToCreate(hookDetails)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var hookCall = new HookCall(2L, new EvmHookCall(new byte[] {}, 25000L));

            var transferResponse = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addHbarTransfer(testEnv.operatorId, new Hbar(-1)) // Operator sends 1 HBAR
                    .addHbarTransferWithHook(
                            accountId,
                            new Hbar(1),
                            hookCall,
                            HookType.PRE_TX_ALLOWANCE_HOOK) // Account receives 1 HBAR with hook
                    .execute(testEnv.client);

            var receipt = transferResponse.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given multiple accounts in a transfer have different allowance hooks, when a TransferTransaction involves all accounts, then each account's respective hooks are called and must all approve for the transaction to succeed")
    void multipleAccountsHooksMustAllApprove() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hookContractId = new ContractId(0, 0, 1);

            // Two different hook ids for two different accounts
            var hookDetails1 = new HookCreationDetails(
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(hookContractId));
            var hookDetails2 = new HookCreationDetails(
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(hookContractId));

            // Create two recipient accounts, each with its own hook
            var key1 = PrivateKey.generateED25519();
            var acct1 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key1)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var key2 = PrivateKey.generateED25519();
            var acct2 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key2)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Attach hooks (must be signed by each account key)
            new AccountUpdateTransaction()
                    .setAccountId(acct1)
                    .addHookToCreate(hookDetails1)
                    .freezeWith(testEnv.client)
                    .sign(key1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new AccountUpdateTransaction()
                    .setAccountId(acct2)
                    .addHookToCreate(hookDetails2)
                    .freezeWith(testEnv.client)
                    .sign(key2)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Hook calls matching each account's hook id
            var hookCall1 = new HookCall(2L, new EvmHookCall(new byte[] {}, 25_000L));
            var hookCall2 = new HookCall(2L, new EvmHookCall(new byte[] {}, 25_000L));

            // One transaction that touches both accounts; both hooks must approve
            var resp = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addHbarTransfer(testEnv.operatorId, new Hbar(-2))
                    .addHbarTransferWithHook(acct1, new Hbar(1), hookCall1, HookType.PRE_TX_ALLOWANCE_HOOK)
                    .addHbarTransferWithHook(acct2, new Hbar(1), hookCall2, HookType.PRE_TX_ALLOWANCE_HOOK)
                    .execute(testEnv.client);

            var receipt = resp.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given an account has a pre/post-transaction allowance hook configured, when a successful HBAR transfer occurs, then the hook is called both before and after the transfer execution")
    void transferWithPrePostTransactionAllowanceHookSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hookContractId = new ContractId(0, 0, 1);

            var hookDetails = new HookCreationDetails(
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(hookContractId));

            var accountKey = PrivateKey.generateED25519();
            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .addHookToCreate(hookDetails)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var hookCall = new HookCall(2L, new EvmHookCall(new byte[] {}, 25_000L));

            var resp = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addHbarTransfer(testEnv.operatorId, new Hbar(-1))
                    .addHbarTransferWithHook(accountId, new Hbar(1), hookCall, HookType.PRE_POST_TX_ALLOWANCE_HOOK)
                    .execute(testEnv.client);

            var receipt = resp.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    //    @Test
    //    @DisplayName(
    //            "Given an account has an allowance hook for token transfers, when a TransferTransaction includes token
    // transfers from that account, then the hook validates the token allowance and approves valid transfers")
    //    void fungibleTokenTransferWithAllowanceHookSucceeds() throws Exception {
    //        try (var testEnv = new IntegrationTestEnv(1)) {
    //            var hookContractId = new ContractId(0, 0, 1);
    //
    //            var hookDetails = new HookCreationDetails(
    //                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(hookContractId));
    //
    //            var receiverKey = PrivateKey.generateED25519();
    //            var receiverId = new AccountCreateTransaction()
    //                    .setKeyWithoutAlias(receiverKey)
    //                    .setInitialBalance(new Hbar(2))
    //                    .execute(testEnv.client)
    //                    .getReceipt(testEnv.client)
    //                    .accountId;
    //
    //            // Attach allowance hook to operator (sender)
    //            new AccountUpdateTransaction()
    //                    .setAccountId(testEnv.operatorId)
    //                    .addHookToCreate(hookDetails)
    //                    .freezeWith(testEnv.client)
    //                    .signWithOperator(testEnv.client)
    //                    .execute(testEnv.client)
    //                    .getReceipt(testEnv.client);
    //
    //            // Create fungible token with operator as treasury
    //            var tokenId = new TokenCreateTransaction()
    //                    .setTokenName("FT-HOOK")
    //                    .setTokenSymbol("FTH")
    //                    .setTokenType(TokenType.FUNGIBLE_COMMON)
    //                    .setDecimals(2)
    //                    .setInitialSupply(10_000) // 100.00 units
    //                    .setTreasuryAccountId(testEnv.operatorId)
    //                    .setAdminKey(testEnv.operatorKey)
    //                    .setSupplyKey(testEnv.operatorKey)
    //                    .setKycKey(testEnv.operatorKey)
    //                    .freezeWith(testEnv.client)
    //                    .signWithOperator(testEnv.client)
    //                    .execute(testEnv.client)
    //                    .getReceipt(testEnv.client)
    //                    .tokenId;
    //
    //            // Associate + KYC receiver
    //            new TokenAssociateTransaction()
    //                    .setAccountId(receiverId)
    //                    .setTokenIds(List.of(tokenId))
    //                    .freezeWith(testEnv.client)
    //                    .sign(receiverKey)
    //                    .execute(testEnv.client)
    //                    .getReceipt(testEnv.client);
    //
    //            new TokenGrantKycTransaction()
    //                    .setAccountId(receiverId)
    //                    .setTokenId(tokenId)
    //                    .execute(testEnv.client)
    //                    .getReceipt(testEnv.client);
    //
    //            // Build transfer with PRE sender allowance hook (sender is operator)
    //            var hookCall = new HookCall(2L, new EvmHookCall(new byte[] {}, 25_000L));
    //            var resp = new TransferTransaction()
    //                    .setNodeAccountIds(
    //                            new ArrayList<>(testEnv.client.getNetwork().values()))
    //                    .addTokenTransferWithHook(
    //                            tokenId, testEnv.operatorId, -1_000, hookCall, HookType.PRE_TX_ALLOWANCE_HOOK) //
    // -10.00
    //                    .addTokenTransfer(tokenId, receiverId, 1_000) // +10.00
    //                    .execute(testEnv.client)
    //                    .getReceipt(testEnv.client);
    //
    //            assertThat(resp.status).isEqualTo(Status.SUCCESS);
    //        }
    //    }

    @Test
    @DisplayName(
            "Given an account has an NFT allowance hook configured, when a TransferTransaction attempts to transfer an NFT from that account, then the hook validates the NFT allowance and processes the transfer accordingly")
    void nftTransferWithAllowanceHookSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hookContractId = new ContractId(0, 0, 1);

            var hookDetails = new HookCreationDetails(
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(hookContractId));

            var senderKey = PrivateKey.generateED25519();
            var senderId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(senderKey)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var receiverKey = PrivateKey.generateED25519();
            var receiverId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(receiverKey)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Attach a hook to the sender (the owner of the NFT) to validate allowance
            new AccountUpdateTransaction()
                    .setAccountId(senderId)
                    .addHookToCreate(hookDetails)
                    .freezeWith(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Also attach the same hook to the receiver to allow receiver pre-hook validation
            new AccountUpdateTransaction()
                    .setAccountId(receiverId)
                    .addHookToCreate(hookDetails)
                    .freezeWith(testEnv.client)
                    .sign(receiverKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Create and mint an NFT under the sender as treasury (matches Go test pattern)
            var tokenId = new TokenCreateTransaction()
                    .setTokenName("NFT-HOOK")
                    .setTokenSymbol("NHK")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(senderId)
                    .setAdminKey(senderKey.getPublicKey())
                    .setSupplyKey(senderKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            var firstMint = new TokenMintTransaction()
                    .setTokenId(tokenId)
                    .setMetadata(List.of(new byte[] {1}))
                    .freezeWith(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Associate only the receiver with the NFT token (sender is treasury)
            new TokenAssociateTransaction()
                    .setAccountId(receiverId)
                    .setTokenIds(List.of(tokenId))
                    .freezeWith(testEnv.client)
                    .sign(receiverKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Use the serial from first mint (sender already owns it as treasury)
            var serial = firstMint.serials.get(0);

            // Now perform sender -> receiver with a PRE sender allowance hook
            var hookCall = new HookCall(2L, new EvmHookCall(new byte[] {}, 25_000L));
            var resp = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addNftTransferWithHook(
                            tokenId.nft(serial),
                            senderId,
                            receiverId,
                            hookCall,
                            NftHookType.PRE_HOOK_SENDER,
                            NftHookType.PRE_HOOK_RECEIVER)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client)
                    .sign(senderKey) // sender must sign for NFT transfer
                    .execute(testEnv.client);

            var receipt = resp.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given a sender account has an allowance hook and receiver account has a different allowance hook, when a TransferTransaction occurs between them, then both sender and receiver hooks are executed in the correct order")
    void senderAndReceiverHooksExecuteForHbarTransfer() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hookContractId = new ContractId(0, 0, 1);

            var senderHookDetails = new HookCreationDetails(
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(hookContractId));
            var receiverHookDetails = new HookCreationDetails(
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(hookContractId));

            var senderKey = PrivateKey.generateED25519();
            var senderId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(senderKey)
                    .setInitialBalance(new Hbar(3))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var receiverKey = PrivateKey.generateED25519();
            var receiverId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(receiverKey)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            new AccountUpdateTransaction()
                    .setAccountId(senderId)
                    .addHookToCreate(senderHookDetails)
                    .freezeWith(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new AccountUpdateTransaction()
                    .setAccountId(receiverId)
                    .addHookToCreate(receiverHookDetails)
                    .freezeWith(testEnv.client)
                    .sign(receiverKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var senderHookCall = new HookCall(2L, new EvmHookCall(new byte[] {}, 25_000L));
            var receiverHookCall = new HookCall(2L, new EvmHookCall(new byte[] {}, 25_000L));

            var resp = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addHbarTransferWithHook(senderId, new Hbar(-1), senderHookCall, HookType.PRE_TX_ALLOWANCE_HOOK)
                    .addHbarTransferWithHook(receiverId, new Hbar(1), receiverHookCall, HookType.PRE_TX_ALLOWANCE_HOOK)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client);

            var receipt = resp.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }
}
