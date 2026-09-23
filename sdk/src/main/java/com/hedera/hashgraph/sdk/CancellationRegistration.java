// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import javax.annotation.Nullable;

/**
 * The handle returned by {@link Cancellation#onCancel(Runnable)}, used to stop observing.
 *
 * <p>Releasing is not optional. An application that holds one {@link CancellationSource} across many
 * calls — "cancel everything on logout", which is the use the type exists for — would otherwise
 * accumulate one dead closure per exchange for the lifetime of that source. It is the
 * {@code addEventListener} without {@code removeEventListener} leak, and a single-use rule would not
 * fix it because nothing can enforce one.
 *
 * <p>Implements {@link AutoCloseable} so it can be released from a try-with-resources block;
 * {@link #close()} is exactly {@link #release()} and throws nothing.
 */
public final class CancellationRegistration implements AutoCloseable {
    private static final CancellationRegistration NOOP = new CancellationRegistration(null, null);

    @Nullable
    private final CancellationState state;

    @Nullable
    private final Runnable callback;

    private CancellationRegistration(@Nullable CancellationState state, @Nullable Runnable callback) {
        this.state = state;
        this.callback = callback;
    }

    static CancellationRegistration of(CancellationState state, Runnable callback) {
        return new CancellationRegistration(state, callback);
    }

    static CancellationRegistration noop() {
        return NOOP;
    }

    /**
     * Stop observing. Idempotent, and a no-op once the cancellation has already fired.
     */
    public void release() {
        if (state != null && callback != null) {
            state.remove(callback);
        }
    }

    @Override
    public void close() {
        release();
    }
}
