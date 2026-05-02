// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class ConsumerHelper {
    static <T> void biConsumer(CompletableFuture<T> future, BiConsumer<T, Throwable> consumer) {
        future.whenComplete(consumer);
    }

    static <T> void twoConsumers(CompletableFuture<T> future, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        future.whenComplete((output, error) -> {
            if (error != null) {
                onFailure.accept(error);
            } else {
                onSuccess.accept(output);
            }
        });
    }
}
