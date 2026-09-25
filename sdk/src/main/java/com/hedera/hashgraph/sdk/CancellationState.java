// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The state a {@link CancellationSource} and the {@link Cancellation} it hands out share.
 *
 * <p>Splitting the observer from the source is the point: a {@link Cancellation} is handed to
 * third-party transports, code the SDK did not write, and if {@code cancel()} lived on it a transport
 * could cancel the caller's own operation.
 */
final class CancellationState {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Set<Runnable> callbacks = ConcurrentHashMap.newKeySet();

    boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Fire the cancellation. Idempotent; a second call is a no-op.
     *
     * <p>{@link #isCancelled()} returns true before any callback runs. Callbacks run synchronously on
     * the calling thread, and one that throws neither prevents the others from running nor propagates
     * to the caller.
     */
    void cancel() {
        if (!cancelled.compareAndSet(false, true)) {
            return;
        }

        for (var callback : callbacks) {
            if (callbacks.remove(callback)) {
                runQuietly(callback);
            }
        }
    }

    /**
     * Register an observer, running it immediately if the cancellation has already fired.
     */
    CancellationRegistration onCancel(Runnable callback) {
        if (cancelled.get()) {
            runQuietly(callback);
            return CancellationRegistration.noop();
        }

        callbacks.add(callback);

        // The cancellation may have fired between the check above and the add, in which case cancel()
        // has already walked the set and will not see this callback. Claim it back and run it here.
        if (cancelled.get() && callbacks.remove(callback)) {
            runQuietly(callback);
            return CancellationRegistration.noop();
        }

        return CancellationRegistration.of(this, callback);
    }

    void remove(Runnable callback) {
        callbacks.remove(callback);
    }

    private static void runQuietly(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException | Error ignored) {
            // A throwing observer must not prevent the others from running, and must not reach the
            // caller of cancel().
        }
    }
}
