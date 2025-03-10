// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.junit.jupiter.api.Assertions.fail;

import com.hedera.hashgraph.sdk.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoadIntegrationTest {

    @Test
    @DisplayName("Load test with multiple clients and single executor")
    void compareThreadingModels() throws Exception {
        // Test parameters, could be adjusted for more deep investigation, limited to 100 due to test speed considerations
        int[] concurrencyLevels = {10, 100, 1000};

        try (var testEnv = new IntegrationTestEnv(1)) {
            var operatorPrivateKey = PrivateKey.fromString(System.getProperty("OPERATOR_KEY"));
            var operatorId = AccountId.fromString(System.getProperty("OPERATOR_ID"));

            // Run tests for each concurrency level
            for (int nTasks : concurrencyLevels) {
                System.out.println("\n========================================");
                System.out.println("Testing with " + nTasks + " concurrent tasks");
                System.out.println("========================================");

                long fixedPoolTime = runLoadTest(testEnv, operatorId, operatorPrivateKey, nTasks, false);

                long virtualThreadTime = runLoadTest(testEnv, operatorId, operatorPrivateKey, nTasks, true);

                double speedup = (double) fixedPoolTime / virtualThreadTime;
                System.out.println("\nPERFORMANCE COMPARISON:");
                System.out.println("Fixed Thread Pool: " + fixedPoolTime + "ms");
                System.out.println("Virtual Threads: " + virtualThreadTime + "ms");
                System.out.println("Speedup: " + String.format("%.2fx", speedup));
            }
        }
    }

    long runLoadTest(IntegrationTestEnv testEnv, AccountId operatorId, PrivateKey operatorPrivateKey,
                     int nTasks, boolean useVirtualThreads) throws Exception {

        String model = useVirtualThreads ? "Virtual Threads" : "Fixed Thread Pool";
        System.out.println("\nRunning test with " + model);

        ExecutorService executor;
        ExecutorService clientExecutor;
        Semaphore semaphore;

        if (useVirtualThreads) {
            // Limit actual concurrent task count to 5000 for virtual threads
            int maxConcurrentTasks = Math.min(nTasks, 5000);
            executor = Executors.newVirtualThreadPerTaskExecutor();
            clientExecutor = Executors.newVirtualThreadPerTaskExecutor();
            System.out.println("Using virtual threads with max concurrency: " + maxConcurrentTasks);

            // Use a semaphore to limit concurrency
            semaphore = new Semaphore(maxConcurrentTasks);
        } else {
            int poolSize = Math.min(Runtime.getRuntime().availableProcessors() + 1, 16);
            executor = Executors.newFixedThreadPool(poolSize);
            clientExecutor = Executors.newFixedThreadPool(poolSize);
            System.out.println("Using thread pool size: " + poolSize);

            // No need for semaphore with fixed thread pool
            semaphore = null;
        }

        CountDownLatch latch = new CountDownLatch(nTasks);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < nTasks; i++) {
            int taskId = i;
            executor.submit(() -> {
                try {
                    if (semaphore != null) {
                        semaphore.acquire();
                    }

                    int attempts = 0;
                    boolean success = false;
                    Exception lastException = null;

                    while (attempts < 3 && !success) {
                        attempts++;

                        if (attempts > 1) {
                            try {
                                Thread.sleep(100 * (attempts - 1));
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }

                        try (var client = Client.forNetwork(testEnv.client.getNetwork(), clientExecutor)) {
                            client.setOperator(operatorId, operatorPrivateKey);
                            client.setMaxAttempts(10);

                            new AccountCreateTransaction()
                                .setKeyWithoutAlias(PrivateKey.generateED25519())
                                .execute(client)
                                .getReceipt(client);

                            success = true;
                            successCount.incrementAndGet();

                            if (taskId % 100 == 0) {
                                System.out.println("Completed task " + taskId + " (attempt " + attempts + ")");
                            }
                        } catch (Exception e) {
                            lastException = e;
                            if (taskId % 100 == 0 && attempts == 3) {
                                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                                System.err.println("Task " + taskId + " failed on attempt " + attempts + ": " + errorMsg);
                            }
                        }
                    }

                    if (!success) {
                        failureCount.incrementAndGet();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failureCount.incrementAndGet();
                } finally {
                    // Release semaphore if we acquired it
                    if (semaphore != null) {
                        semaphore.release();
                    }

                    // Always count down the latch
                    latch.countDown();
                }
            });
        }

        // Wait for completion with reasonable timeout
        int timeoutMinutes = nTasks > 1000 ? 10 : 5;
        boolean completed = latch.await(timeoutMinutes, TimeUnit.MINUTES);

        // Calculate execution time
        long executionTime = System.currentTimeMillis() - startTime;

        System.out.println("\nTest results for " + model + ":");
        System.out.println("Completed: " + (completed ? "All tasks completed" : "Timed out"));
        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("Successful tasks: " + successCount.get() + "/" + nTasks);
        System.out.println("Failed tasks: " + failureCount.get());

        // Clean up executors
        executor.shutdown();
        clientExecutor.shutdown();

        return executionTime;
    }

    @Test
    @DisplayName("Comparison of transaction throughput: Virtual Threads vs Fixed Thread Pool")
    void compareExecutorTypesSequential() throws Exception {
        int[] taskCounts = {10, 100, 1000};

        try (var testEnv = new IntegrationTestEnv(1)) {
            // Use the default client (with virtual threads)
            Client virtualThreadClient = testEnv.client;

            // Create a client with fixed thread pool
            int poolSize = Math.min(Runtime.getRuntime().availableProcessors() + 1, 16);
            ExecutorService fixedExecutor = Executors.newFixedThreadPool(poolSize);
            Client fixedPoolClient = Client.forNetwork(testEnv.client.getNetwork(), fixedExecutor);

            var operatorPrivateKey = PrivateKey.fromString(System.getProperty("OPERATOR_KEY"));
            var operatorId = AccountId.fromString(System.getProperty("OPERATOR_ID"));

            fixedPoolClient.setOperator(operatorId,
                operatorPrivateKey
            );

            try {
                for (int nTasks : taskCounts) {
                    System.out.println("\n========================================");
                    System.out.println("Executing " + nTasks + " transactions sequentially");
                    System.out.println("========================================");

                    System.out.println("\nTesting with Virtual Thread Executor:");
                    long virtualThreadTime = runSequentialTransactions(virtualThreadClient, nTasks);

                    System.out.println("\nTesting with Fixed Thread Pool Executor (size: " + poolSize + "):");
                    long fixedPoolTime = runSequentialTransactions(fixedPoolClient, nTasks);

                    double speedup = (double) fixedPoolTime / virtualThreadTime;
                    System.out.println("\nPERFORMANCE COMPARISON:");
                    System.out.println("Fixed Thread Pool: " + fixedPoolTime + "ms");
                    System.out.println("Virtual Threads: " + virtualThreadTime + "ms");
                    System.out.println("Speedup: " + String.format("%.2fx", speedup));
                }
            } finally {
                fixedExecutor.shutdown();
            }
        }
    }

    private long runSequentialTransactions(Client client, int nTasks) {
        long startTime = System.currentTimeMillis();

        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < nTasks; i++) {
            try {
                // Execute transaction and get receipt
                TransactionResponse response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(PrivateKey.generateED25519())
                    .setInitialBalance(Hbar.fromTinybars(100))
                    .execute(client);

                TransactionReceipt receipt = response.getReceipt(client);

                successCount++;
                if (i % 100 == 0) {
                    System.out.println("Completed transaction " + i);
                }
            } catch (Exception e) {
                failureCount++;
                System.err.println("Transaction " + i + " failed: " + e.getMessage());
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;

        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("Successful transactions: " + successCount + "/" + nTasks);
        System.out.println("Failed transactions: " + failureCount);
        System.out.println("Transactions per second: " + (successCount * 1000.0 / executionTime));

        return executionTime;
    }


    //TODO comparison test between newly created one and async -> compare performance
}
