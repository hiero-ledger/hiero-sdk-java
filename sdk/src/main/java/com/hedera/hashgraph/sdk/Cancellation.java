// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A caller's request to end an exchange early, as observed by an {@link HttpTransport}.
 *
 * <p>A transport observes one; it never creates one. Only {@link CancellationSource} can fire it, so a
 * third-party transport handed a {@code Cancellation} cannot cancel the caller's own operation.
 *
 * <p>Spelled {@code Cancellation} rather than {@code CancellationSignal} to avoid colliding with
 * {@code android.os.CancellationSignal}.
 */
public final class Cancellation {
    private static final Cancellation NONE = new Cancellation(null);

    @Nullable
    private final CancellationState state;

    Cancellation(@Nullable CancellationState state) {
        this.state = state;
    }

    /**
     * The value for a call with no caller-supplied cancellation. Never null, and never fires.
     *
     * @return a cancellation that is never cancelled
     */
    public static Cancellation none() {
        return NONE;
    }

    /**
     * Whether the caller has cancelled.
     *
     * <p>Returns true before any {@link #onCancel(Runnable)} callback runs.
     *
     * @return true if cancelled
     */
    public boolean isCancelled() {
        return state != null && state.isCancelled();
    }

    /**
     * Observe the cancellation.
     *
     * <p>The callback runs synchronously on the thread that cancelled and must not block. Registering
     * on an instance that has already been cancelled runs the callback immediately rather than never.
     *
     * <p>A transport <b>must</b> release its registration when the exchange ends — see
     * {@link CancellationRegistration}.
     *
     * @param callback the observer to run when the cancellation fires
     * @return a handle that stops the observation
     */
    public CancellationRegistration onCancel(Runnable callback) {
        Objects.requireNonNull(callback, "callback must not be null");

        if (state == null) {
            return CancellationRegistration.noop();
        }

        return state.onCancel(callback);
    }
}
