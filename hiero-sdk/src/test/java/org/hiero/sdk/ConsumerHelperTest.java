// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ConsumerHelperTest {
    @Test
    void biConsumer() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello");
        BiConsumer<String, Throwable> consumer = mock(BiConsumer.class);
        ConsumerHelper.biConsumer(future, consumer);
        future.join();
        verify(consumer, times(1)).accept(any(), any());
    }

    @Test
    void twoConsumersWithoutError() {
        var value = "Hello";
        CompletableFuture<String> future = CompletableFuture.completedFuture(value);

        Consumer<String> onSuccess = mock(Consumer.class);
        Consumer<Throwable> onFailure = mock(Consumer.class);

        ConsumerHelper.twoConsumers(future, onSuccess, onFailure);
        future.join();

        verify(onSuccess, times(1)).accept(value);
        verify(onFailure, times(0)).accept(any());
    }

    @Test
    void twoConsumersWithError() {
        var exception = new RuntimeException("Exception");
        CompletableFuture<String> future = CompletableFuture.failedFuture(exception);

        Consumer<String> onSuccess = mock(Consumer.class);
        Consumer<Throwable> onFailure = mock(Consumer.class);

        ConsumerHelper.twoConsumers(future, onSuccess, onFailure);
        assertThrows(RuntimeException.class, future::join);

        verify(onSuccess, times(0)).accept(any());
        verify(onFailure, times(1)).accept(exception);
    }
}
