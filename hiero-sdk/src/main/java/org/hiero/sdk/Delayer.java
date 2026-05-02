// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used internally by the sdk.
 */
final class Delayer {
    private static final Logger logger = LoggerFactory.getLogger(Delayer.class);

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private static final Duration MIN_DELAY = Duration.ofMillis(500);

    /**
     * Constructor.
     */
    private Delayer() {}

    /**
     * Set the delay backoff attempts.
     *
     * @param attempt                   the attempts
     * @param executor                  the executor
     * @return                          the updated future
     */
    static CompletableFuture<Void> delayBackOff(int attempt, Executor executor) {
        var interval = MIN_DELAY.multipliedBy(ThreadLocalRandom.current().nextLong(1L << attempt));

        return delayFor(interval.toMillis(), executor);
    }

    /**
     * Set the delay backoff milliseconds.
     *
     * @param milliseconds              the milliseconds
     * @param executor                  the executor
     * @return                          the updated future
     */
    static CompletableFuture<Void> delayFor(long milliseconds, Executor executor) {
        logger.trace("waiting for {} seconds before trying again", (double) milliseconds / 1000.0);

        return CompletableFuture.runAsync(() -> {}, delayedExecutor(milliseconds, TimeUnit.MILLISECONDS, executor));
    }

    private static Executor delayedExecutor(long delay, TimeUnit unit, Executor executor) {
        return r -> SCHEDULER.schedule(() -> executor.execute(r), delay, unit);
    }
}
