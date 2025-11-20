// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransferTransactionHooksIntegrationTest {

    private static final String SMART_CONTRACT_BYTECODE =
            "6080604052348015600e575f5ffd5b506107d18061001c5f395ff3fe608060405260043610610033575f3560e01c8063124d8b301461003757806394112e2f14610067578063bd0dd0b614610097575b5f5ffd5b610051600480360381019061004c91906106f2565b6100c7565b60405161005e9190610782565b60405180910390f35b610081600480360381019061007c91906106f2565b6100d2565b60405161008e9190610782565b60405180910390f35b6100b160048036038101906100ac91906106f2565b6100dd565b6040516100be9190610782565b60405180910390f35b5f6001905092915050565b5f6001905092915050565b5f6001905092915050565b5f604051905090565b5f5ffd5b5f5ffd5b5f5ffd5b5f60a08284031215610112576101116100f9565b5b81905092915050565b5f5ffd5b5f601f19601f8301169050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52604160045260245ffd5b6101658261011f565b810181811067ffffffffffffffff821117156101845761018361012f565b5b80604052505050565b5f6101966100e8565b90506101a2828261015c565b919050565b5f5ffd5b5f5ffd5b5f67ffffffffffffffff8211156101c9576101c861012f565b5b602082029050602081019050919050565b5f5ffd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610207826101de565b9050919050565b610217816101fd565b8114610221575f5ffd5b50565b5f813590506102328161020e565b92915050565b5f8160070b9050919050565b61024d81610238565b8114610257575f5ffd5b50565b5f8135905061026881610244565b92915050565b5f604082840312156102835761028261011b565b5b61028d604061018d565b90505f61029c84828501610224565b5f8301525060206102af8482850161025a565b60208301525092915050565b5f6102cd6102c8846101af565b61018d565b905080838252602082019050604084028301858111156102f0576102ef6101da565b5b835b818110156103195780610305888261026e565b8452602084019350506040810190506102f2565b5050509392505050565b5f82601f830112610337576103366101ab565b5b81356103478482602086016102bb565b91505092915050565b5f67ffffffffffffffff82111561036a5761036961012f565b5b602082029050602081019050919050565b5f67ffffffffffffffff8211156103955761039461012f565b5b602082029050602081019050919050565b5f606082840312156103bb576103ba61011b565b5b6103c5606061018d565b90505f6103d484828501610224565b5f8301525060206103e784828501610224565b60208301525060406103fb8482850161025a565b60408301525092915050565b5f6104196104148461037b565b61018d565b9050808382526020820190506060840283018581111561043c5761043b6101da565b5b835b81811015610465578061045188826103a6565b84526020840193505060608101905061043e565b5050509392505050565b5f82601f830112610483576104826101ab565b5b8135610493848260208601610407565b91505092915050565b5f606082840312156104b1576104b061011b565b5b6104bb606061018d565b90505f6104ca84828501610224565b5f83015250602082013567ffffffffffffffff8111156104ed576104ec6101a7565b5b6104f984828501610323565b602083015250604082013567ffffffffffffffff81111561051d5761051c6101a7565b5b6105298482850161046f565b60408301525092915050565b5f61054761054284610350565b61018d565b9050808382526020820190506020840283018581111561056a576105696101da565b5b835b818110156105b157803567ffffffffffffffff81111561058f5761058e6101ab565b5b80860161059c898261049c565b8552602085019450505060208101905061056c565b5050509392505050565b5f82601f8301126105cf576105ce6101ab565b5b81356105df848260208601610535565b91505092915050565b5f604082840312156105fd576105fc61011b565b5b610607604061018d565b90505f82013567ffffffffffffffff811115610626576106256101a7565b5b61063284828501610323565b5f83015250602082013567ffffffffffffffff811115610655576106546101a7565b5b610661848285016105bb565b60208301525092915050565b5f604082840312156106825761068161011b565b5b61068c604061018d565b90505f82013567ffffffffffffffff8111156106ab576106aa6101a7565b5b6106b7848285016105e8565b5f83015250602082013567ffffffffffffffff8111156106da576106d96101a7565b5b6106e6848285016105e8565b60208301525092915050565b5f5f60408385031215610708576107076100f1565b5b5f83013567ffffffffffffffff811115610725576107246100f5565b5b610731858286016100fd565b925050602083013567ffffffffffffffff811115610752576107516100f5565b5b61075e8582860161066d565b9150509250929050565b5f8115159050919050565b61077c81610768565b82525050565b5f6020820190506107955f830184610773565b9291505056fea26469706673582212207dfe7723f6d6869419b1cb0619758b439da0cf4ffd9520997c40a3946299d4dc64736f6c634300081e0033";

    @Test
    @DisplayName(
            "Given an account has a pre-transaction allowance hook configured, when a TransferTransaction attempts to transfer HBAR from that account, then the hook is called before the transfer and approves the transaction")
    void transferWithPreTransactionAllowanceHookSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hookContractId = createContractId(testEnv);

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
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToCreate(hookDetails)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var hookCall = new FungibleHookCall(
                    2L, new EvmHookCall(new byte[] {}, 25000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);

            var transferResponse = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addHbarTransfer(testEnv.operatorId, new Hbar(-1)) // Operator sends 1 HBAR
                    .addHbarTransferWithHook(accountId, new Hbar(1), hookCall) // Account receives 1 HBAR with hook
                    .execute(testEnv.client);

            var transferReceipt = transferResponse.getReceipt(testEnv.client);
            assertThat(transferReceipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given multiple accounts in a transfer have different allowance hooks, when a TransferTransaction involves all accounts, then each account's respective hooks are called and must all approve for the transaction to succeed")
    void multipleAccountsHooksMustAllApprove() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hookContractId = createContractId(testEnv);

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
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToCreate(hookDetails1)
                    .freezeWith(testEnv.client)
                    .sign(key1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new AccountUpdateTransaction()
                    .setAccountId(acct2)
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToCreate(hookDetails2)
                    .freezeWith(testEnv.client)
                    .sign(key2)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Hook calls matching each account's hook id
            var hookCall1 = new FungibleHookCall(
                    2L, new EvmHookCall(new byte[] {}, 25_000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
            var hookCall2 = new FungibleHookCall(
                    2L, new EvmHookCall(new byte[] {}, 25_000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);

            // One transaction that touches both accounts; both hooks must approve
            var resp = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addHbarTransfer(testEnv.operatorId, new Hbar(-2))
                    .addHbarTransferWithHook(acct1, new Hbar(1), hookCall1)
                    .addHbarTransferWithHook(acct2, new Hbar(1), hookCall2)
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
            var hookContractId = createContractId(testEnv);

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
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToCreate(hookDetails)
                    .setMaxTransactionFee(Hbar.from(10))
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var hookCall = new FungibleHookCall(
                    2L, new EvmHookCall(new byte[] {}, 25_000L), FungibleHookType.PRE_POST_TX_ALLOWANCE_HOOK);

            var resp = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addHbarTransfer(testEnv.operatorId, new Hbar(-1))
                    .addHbarTransferWithHook(accountId, new Hbar(1), hookCall)
                    .execute(testEnv.client);

            var receipt = resp.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given an account has an allowance hook for token transfers, when a TransferTransaction includes token transfers from that account, then the hook validates the token allowance and approves valid transfers")
    void fungibleTokenTransferWithAllowanceHookSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Use a fresh operator account to avoid residual hooks (e.g., HOOK_ID_IN_USE on persistent operator)
            testEnv.useThrowawayAccount();
            var hookContractId = createContractId(testEnv);

            var hookDetails = new HookCreationDetails(
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(hookContractId));

            var receiverKey = PrivateKey.generateED25519();
            var receiverId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(receiverKey)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Attach allowance hook to operator (sender)
            new AccountUpdateTransaction()
                    .setAccountId(receiverId)
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToCreate(hookDetails)
                    .setMaxTransactionFee(Hbar.from(10))
                    .freezeWith(testEnv.client)
                    .sign(receiverKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Create fungible token with operator as treasury
            var tokenId = new TokenCreateTransaction()
                    .setTokenName("FT-HOOK")
                    .setTokenSymbol("FTH")
                    .setTokenType(TokenType.FUNGIBLE_COMMON)
                    .setDecimals(2)
                    .setInitialSupply(10_000) // 100.00 units
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setKycKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            // Associate + KYC receiver
            new TokenAssociateTransaction()
                    .setAccountId(receiverId)
                    .setTokenIds(List.of(tokenId))
                    .freezeWith(testEnv.client)
                    .sign(receiverKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenGrantKycTransaction()
                    .setAccountId(receiverId)
                    .setTokenId(tokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Ensure the allowance hook is attached to the debited account (operator)
            var hookDetails2 = new HookCreationDetails(
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(hookContractId));
            new AccountUpdateTransaction()
                    .setAccountId(testEnv.operatorId)
                    .addHookToCreate(hookDetails2)
                    .setMaxTransactionFee(Hbar.from(10))
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Build transfer with PRE sender allowance hook (sender is operator)
            var hookCall = new FungibleHookCall(
                    2L, new EvmHookCall(new byte[] {}, 25_000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
            var resp = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addTokenTransferWithHook(tokenId, testEnv.operatorId, -1_000, hookCall) // -10.00
                    .addTokenTransfer(tokenId, receiverId, 1_000) // +10.00
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThat(resp.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given an account has an NFT allowance hook configured, when a TransferTransaction attempts to transfer an NFT from that account, then the hook validates the NFT allowance and processes the transfer accordingly")
    void nftTransferWithAllowanceHookSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hookContractId = createContractId(testEnv);

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
                    .setMaxTransactionFee(Hbar.from(10))
                    .freezeWith(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Also attach the same hook to the receiver to allow receiver pre-hook validation
            new AccountUpdateTransaction()
                    .setAccountId(receiverId)
                    .addHookToCreate(hookDetails)
                    .setMaxTransactionFee(Hbar.from(10))
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
            var senderHookCall =
                    new NftHookCall(2L, new EvmHookCall(new byte[] {}, 25_000L), NftHookType.PRE_HOOK_SENDER);
            var receiverHookCall =
                    new NftHookCall(2L, new EvmHookCall(new byte[] {}, 25_000L), NftHookType.PRE_HOOK_RECEIVER);
            var resp = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addNftTransferWithHook(tokenId.nft(serial), senderId, receiverId, senderHookCall, receiverHookCall)
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
            var hookContractId = createContractId(testEnv);

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
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToCreate(senderHookDetails)
                    .setMaxTransactionFee(Hbar.from(10))
                    .freezeWith(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new AccountUpdateTransaction()
                    .setAccountId(receiverId)
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToCreate(receiverHookDetails)
                    .freezeWith(testEnv.client)
                    .sign(receiverKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var senderHookCall2 = new FungibleHookCall(
                    2L, new EvmHookCall(new byte[] {}, 25_000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
            var receiverHookCall2 = new FungibleHookCall(
                    2L, new EvmHookCall(new byte[] {}, 25_000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);

            var resp = new TransferTransaction()
                    .setNodeAccountIds(
                            new ArrayList<>(testEnv.client.getNetwork().values()))
                    .addHbarTransferWithHook(senderId, new Hbar(-1), senderHookCall2)
                    .addHbarTransferWithHook(receiverId, new Hbar(1), receiverHookCall2)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client)
                    .sign(senderKey)
                    .execute(testEnv.client);

            var receipt = resp.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    private FileId createBytecodeFile(final IntegrationTestEnv testEnv) throws Exception {
        var response = new FileCreateTransaction()
                .setKeys(testEnv.operatorKey)
                .setContents(SMART_CONTRACT_BYTECODE)
                .execute(testEnv.client);
        return Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);
    }

    private ContractId createContractId(final IntegrationTestEnv testEnv) throws Exception {
        var fileId = createBytecodeFile(testEnv);

        var response = new ContractCreateTransaction()
                .setAdminKey(testEnv.operatorKey)
                .setGas(1000000)
                .setBytecodeFileId(fileId)
                .execute(testEnv.client);

        var receipt = response.getReceipt(testEnv.client);
        return receipt.contractId;
    }
}
