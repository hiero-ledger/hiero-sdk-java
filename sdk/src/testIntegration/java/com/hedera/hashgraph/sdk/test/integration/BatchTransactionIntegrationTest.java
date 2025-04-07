// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.hedera.hashgraph.sdk.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BatchTransactionIntegrationTest {

    @Test
    @DisplayName("Can create batch transaction")
    void canCreateBatchTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var key = PrivateKey.generateECDSA();
            var tx1 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, testEnv.operatorKey);

            // create new Batch Transaction
            BatchTransaction batchTransaction = new BatchTransaction().addTransaction(tx1);

            var status = batchTransaction
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .status;

            assertThat(status).isEqualTo(Status.SUCCESS);
        }
    }
}
