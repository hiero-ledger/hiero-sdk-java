// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ExtendWith(RetryTestExtension.class)
public @interface RetryTest {
    /**
     * Maximum number of retry attempts
     */
    int maxAttempts() default 3;

    /**
     * Initial delay between retries in milliseconds
     */
    long initialDelayMs() default 1000;

    /**
     * Maximum delay between retries in milliseconds
     */
    long maxDelayMs() default 10000;
}
