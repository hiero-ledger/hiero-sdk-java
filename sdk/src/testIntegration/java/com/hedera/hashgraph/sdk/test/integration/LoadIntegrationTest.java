// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.junit.jupiter.api.Assertions.fail;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoadIntegrationTest {

    @Test
    @DisplayName("Load test with multiple clients and single executor")
    void loadTest() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var operatorPrivateKey = PrivateKey.fromString(System.getProperty("OPERATOR_KEY"));
            var operatorId = AccountId.fromString(System.getProperty("OPERATOR_ID"));

            int nThreads = 10;
            var clientExecutor = Executors.newFixedThreadPool(16);

            var threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads);

            long startTime = System.currentTimeMillis();

            System.out.println("Finished executing tasks:");
            for (int i = 0; i < nThreads; i++) {
                int finalI = i;
                threadPoolExecutor.submit(() -> {
                    try (var client = Client.forNetwork(testEnv.client.getNetwork(), clientExecutor); ) {
                        client.setOperator(operatorId, operatorPrivateKey);
                        client.setMaxAttempts(10);
                        new AccountCreateTransaction()
                                .setKeyWithoutAlias(PrivateKey.generateED25519())
                                .execute(client)
                                .getReceipt(client);
                        System.out.println(finalI);
                    } catch (Exception e) {
                        fail("AccountCreateTransaction failed, " + e);
                    }
                });
            }

            threadPoolExecutor.shutdown();

            // Wait for all tasks to finish
            try {
                if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.out.println();
                    System.out.println("Forcing shutdown");
                    threadPoolExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPoolExecutor.shutdownNow();
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.println("All tasks have finished execution in " + executionTime + "ms");
            clientExecutor.shutdownNow();
        }
    }
}
