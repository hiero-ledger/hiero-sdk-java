// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancellationTest {

    @Test
    void noneIsNeverCancelled() {
        var cancellation = Cancellation.none();

        assertThat(cancellation.isCancelled()).isFalse();
        // Registering on it is legal and releasing the registration is a no-op.
        cancellation.onCancel(() -> {}).release();
        assertThat(cancellation.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("isCancelled() is already true when an observer runs")
    void observersSeeTheCancelledState() {
        var source = CancellationSource.create();
        var observed = new AtomicBoolean(false);

        source.getCancellation()
                .onCancel(() -> observed.set(source.getCancellation().isCancelled()));
        source.cancel();

        assertThat(observed).isTrue();
    }

    @Test
    void cancelIsIdempotent() {
        var source = CancellationSource.create();
        var runs = new AtomicInteger();

        source.getCancellation().onCancel(runs::incrementAndGet);

        source.cancel();
        source.cancel();

        assertThat(runs).hasValue(1);
    }

    @Test
    @DisplayName("A throwing observer stops neither the others nor the caller of cancel()")
    void aThrowingObserverIsContained() {
        var source = CancellationSource.create();
        var ran = new ArrayList<String>();

        source.getCancellation().onCancel(() -> {
            throw new IllegalStateException("boom");
        });
        source.getCancellation().onCancel(() -> ran.add("second"));

        source.cancel();

        assertThat(ran).containsExactly("second");
    }

    @Test
    void registeringAfterCancellationRunsImmediately() {
        var source = CancellationSource.create();
        source.cancel();

        var ran = new AtomicBoolean(false);
        source.getCancellation().onCancel(() -> ran.set(true));

        assertThat(ran).isTrue();
    }

    @Test
    @DisplayName("A released registration does not fire, so a long-lived source accumulates nothing")
    void releasedRegistrationsDoNotFire() {
        var source = CancellationSource.create();
        var runs = new AtomicInteger();

        // One source across many exchanges, each releasing when its exchange ends.
        for (var i = 0; i < 100; i++) {
            source.getCancellation().onCancel(runs::incrementAndGet).release();
        }

        source.cancel();

        assertThat(runs).hasValue(0);
    }
}
