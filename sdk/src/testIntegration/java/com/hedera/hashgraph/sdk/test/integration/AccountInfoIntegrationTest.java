// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountInfoFlow;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Transaction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountInfoIntegrationTest {
    @Test
    @DisplayName("Can query account info for client operator")
    void canQueryAccountInfoForClientOperator() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var info = new AccountInfoQuery().setAccountId(testEnv.operatorId).execute(testEnv.client);

            assertThat(info.accountId).isEqualTo(testEnv.operatorId);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.key).isEqualTo(testEnv.operatorKey);
            assertThat(info.balance.toTinybars()).isGreaterThan(0);
            assertThat(info.proxyAccountId).isNull();
            assertThat(info.proxyReceived).isEqualTo(Hbar.ZERO);
        }
    }

    @Test
    @DisplayName("Can get cost for account info query")
    void getCostAccountInfoForClientOperator() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var info = new AccountInfoQuery().setAccountId(testEnv.operatorId).setMaxQueryPayment(new Hbar(1));

            var cost = info.getCost(testEnv.client);

            var accInfo = info.setQueryPayment(cost).execute(testEnv.client);

            assertThat(accInfo.accountId).isEqualTo(testEnv.operatorId);
        }
    }

    @Test
    @DisplayName("Can get cost for account info query, with a bix max")
    void getCostBigMaxAccountInfoForClientOperator() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var info = new AccountInfoQuery().setAccountId(testEnv.operatorId).setMaxQueryPayment(Hbar.MAX);

            var cost = info.getCost(testEnv.client);

            var accInfo = info.setQueryPayment(cost).execute(testEnv.client);

            assertThat(accInfo.accountId).isEqualTo(testEnv.operatorId);
        }
    }

    @Test
    @Disabled
    @DisplayName("Can get cost for account info query, with a small max")
    void getCostSmallMaxAccountInfoForClientOperator() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var info = new AccountInfoQuery().setAccountId(testEnv.operatorId).setMaxQueryPayment(Hbar.fromTinybars(1));

            var cost = info.getCost(testEnv.client);

            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> {
                        info.execute(testEnv.client);
                    })
                    .withMessage(
                            "com.hedera.hashgraph.sdk.MaxQueryPaymentExceededException: cost for AccountInfoQuery, of "
                                    + cost.toString()
                                    + ", without explicit payment is greater than the maximum allowed payment of 1 tℏ");
        }
    }

    @Test
    @Disabled("Cannot run with solo action")
    @DisplayName("Insufficient tx fee error.")
    void getCostInsufficientTxFeeAccountInfoForClientOperator() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var info = new AccountInfoQuery()
                    .setAccountId(testEnv.operatorId)
                    .setMaxQueryPayment(Hbar.fromTinybars(10000));

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        info.setQueryPayment(Hbar.fromTinybars(1)).execute(testEnv.client);
                    })
                    .satisfies(error -> assertThat(error.status.toString()).isEqualTo("INSUFFICIENT_TX_FEE"));
        }
    }

    @Test
    @DisplayName("AccountInfoFlow.verify functions")
    void accountInfoFlowVerifyFunctions() throws Throwable {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var newKey = PrivateKey.generateED25519();
            var newPublicKey = newKey.getPublicKey();

            Transaction<?> signedTx = new AccountCreateTransaction()
                    .setKeyWithoutAlias(newPublicKey)
                    .setInitialBalance(Hbar.fromTinybars(1000))
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            Transaction<?> unsignedTx = new AccountCreateTransaction()
                    .setKeyWithoutAlias(newPublicKey)
                    .setInitialBalance(Hbar.fromTinybars(1000))
                    .freezeWith(testEnv.client);

            assertThat(AccountInfoFlow.verifyTransactionSignature(testEnv.client, testEnv.operatorId, signedTx))
                    .isTrue();
            assertThat(AccountInfoFlow.verifyTransactionSignature(testEnv.client, testEnv.operatorId, unsignedTx))
                    .isFalse();
        }
    }
}
