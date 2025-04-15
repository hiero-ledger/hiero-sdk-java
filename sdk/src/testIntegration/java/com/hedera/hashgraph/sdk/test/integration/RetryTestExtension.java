// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

public class RetryTestExtension implements TestExecutionExceptionHandler {
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        RetryTest retryTest = context.getRequiredTestMethod().getAnnotation(RetryTest.class);

        if (retryTest == null) {
            throw throwable;
        }

        int maxAttempts = retryTest.maxAttempts();
        long initialDelay = retryTest.initialDelayMs();
        long maxDelay = retryTest.maxDelayMs();
        long currentDelay = initialDelay;
        int attempt = 1;

        Throwable lastThrowable = throwable;

        while (attempt < maxAttempts) {
            System.out.println("Retrying test " + context.getDisplayName() + " after failure.");

            try {
                // Wait with exponential backoff
                Thread.sleep(currentDelay);

                // Execute the test again
                context.getRequiredTestMethod().invoke(context.getRequiredTestInstance());

                System.out.println("Test " + context.getDisplayName() + " passed on retry attempt" + attempt + 1);
                return;
            } catch (Throwable t) {
                lastThrowable = t;
                attempt++;

                // Calculate next delay with exponential backoff
                currentDelay = Math.min(currentDelay * 2, maxDelay);
            }
        }

        // If we get here, we've exhausted our retries
        System.out.println("Test " + context.getDisplayName() + " reached max attempts.");
        throw lastThrowable;
    }
}
