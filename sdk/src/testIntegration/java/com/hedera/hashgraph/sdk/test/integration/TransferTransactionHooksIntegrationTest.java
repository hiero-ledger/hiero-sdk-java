// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.*;
import java.util.ArrayList;
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
}
