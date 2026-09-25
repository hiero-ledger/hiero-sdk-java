// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * Creates a {@link Cancellation} and is the only thing that can fire it.
 *
 * <p>Java has no standard cancellation type, so the SDK supplies one. Hand the {@link #getCancellation()}
 * to a transport and keep the source; a transport that could cancel would be able to cancel the
 * caller's own operation.
 */
public final class CancellationSource {
    private final CancellationState state = new CancellationState();
    private final Cancellation cancellation = new Cancellation(state);

    private CancellationSource() {}

    /**
     * Create a new, uncancelled source.
     *
     * @return the new source
     */
    public static CancellationSource create() {
        return new CancellationSource();
    }

    /**
     * Extract the observer side, safe to hand to a transport.
     *
     * @return the cancellation this source fires
     */
    public Cancellation getCancellation() {
        return cancellation;
    }

    /**
     * Fire the cancellation.
     *
     * <p>Idempotent: a second call is a no-op, as is cancelling after the exchange has finished.
     * {@link Cancellation#isCancelled()} returns true before any observer runs, observers run
     * synchronously on this thread, and one that throws neither stops the others nor propagates here.
     */
    public void cancel() {
        state.cancel();
    }
}
