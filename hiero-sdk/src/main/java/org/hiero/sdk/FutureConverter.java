// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

// Converts between ListenableFuture (Guava) and CompletableFuture (StreamSupport).
// https://github.com/lukas-krecan/future-converter/blob/master/java8-guava/src/main/java/net/javacrumbs/futureconverter/java8guava/FutureConverter.java#L28
final class FutureConverter {
    private FutureConverter() {}

    /**
     * Generate a T object from a listenable future.
     *
     * @param listenableFuture          the T object generator
     * @return                          the T type object
     */
    static <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> listenableFuture) {
        return Java8FutureUtils.createCompletableFuture(GuavaFutureUtils.createValueSourceFuture(listenableFuture));
    }

    // https://github.com/lukas-krecan/future-converter/blob/master/common/src/main/java/net/javacrumbs/futureconverter/common/internal/ValueSource.java
    private interface ValueSource<T> {
        void addCallbacks(Consumer<T> successCallback, Consumer<Throwable> failureCallback);

        boolean cancel(boolean mayInterruptIfRunning);
    }

    // https://github.com/lukas-krecan/future-converter/blob/master/common/src/main/java/net/javacrumbs/futureconverter/common/internal/ValueSourceFuture.java
    private abstract static class ValueSourceFuture<T> extends FutureWrapper<T> implements ValueSource<T> {
        ValueSourceFuture(Future<T> wrappedFuture) {
            super(wrappedFuture);
        }
    }

    // https://github.com/lukas-krecan/future-converter/blob/652b845824de90b075cf5ddbbda6fdf440f3ed0a/common/src/main/java/net/javacrumbs/futureconverter/common/internal/FutureWrapper.java
    private static class FutureWrapper<T> implements Future<T> {
        private final Future<T> wrappedFuture;

        FutureWrapper(Future<T> wrappedFuture) {
            this.wrappedFuture = wrappedFuture;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return wrappedFuture.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return wrappedFuture.isCancelled();
        }

        @Override
        public boolean isDone() {
            return wrappedFuture.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return wrappedFuture.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return wrappedFuture.get(timeout, unit);
        }

        Future<T> getWrappedFuture() {
            return wrappedFuture;
        }
    }

    // https://github.com/lukas-krecan/future-converter/blob/master/guava-common/src/main/java/net/javacrumbs/futureconverter/guavacommon/GuavaFutureUtils.java
    private static class GuavaFutureUtils {
        public static <T> ValueSourceFuture<T> createValueSourceFuture(ListenableFuture<T> listenableFuture) {
            if (listenableFuture instanceof ValueSourceFutureBackedListenableFuture) {
                return ((ValueSourceFutureBackedListenableFuture<T>) listenableFuture).getWrappedFuture();
            } else {
                return new ListenableFutureBackedValueSourceFuture<>(listenableFuture);
            }
        }

        private static class ValueSourceFutureBackedListenableFuture<T> extends FutureWrapper<T>
                implements ListenableFuture<T> {
            ValueSourceFutureBackedListenableFuture(ValueSourceFuture<T> valueSourceFuture) {
                super(valueSourceFuture);
            }

            @Override
            ValueSourceFuture<T> getWrappedFuture() {
                return (ValueSourceFuture<T>) super.getWrappedFuture();
            }

            @Override
            public void addListener(Runnable listener, Executor executor) {
                getWrappedFuture().addCallbacks(value -> executor.execute(listener), ex -> executor.execute(listener));
            }
        }

        private static class ListenableFutureBackedValueSourceFuture<T> extends ValueSourceFuture<T> {
            private ListenableFutureBackedValueSourceFuture(ListenableFuture<T> wrappedFuture) {
                super(wrappedFuture);
            }

            @Override
            public void addCallbacks(Consumer<T> successCallback, Consumer<Throwable> failureCallback) {
                Futures.addCallback(
                        getWrappedFuture(),
                        new FutureCallback<T>() {
                            @Override
                            public void onSuccess(T result) {
                                successCallback.accept(result);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                failureCallback.accept(t);
                            }
                        },
                        MoreExecutors.directExecutor());
            }

            @Override
            ListenableFuture<T> getWrappedFuture() {
                return (ListenableFuture<T>) super.getWrappedFuture();
            }
        }
    }

    // https://github.com/lukas-krecan/future-converter/blob/master/java8-common/src/main/java/net/javacrumbs/futureconverter/java8common/Java8FutureUtils.java
    private static class Java8FutureUtils {
        public static <T> CompletableFuture<T> createCompletableFuture(ValueSource<T> valueSource) {
            if (valueSource instanceof CompletableFutureBackedValueSource) {
                return ((CompletableFutureBackedValueSource<T>) valueSource).getWrappedFuture();
            } else {
                return new ValueSourceBackedCompletableFuture<T>(valueSource);
            }
        }

        private static final class ValueSourceBackedCompletableFuture<T> extends CompletableFuture<T> {
            private final ValueSource<T> valueSource;

            @SuppressWarnings("ConstructorLeaksThis")
            private ValueSourceBackedCompletableFuture(ValueSource<T> valueSource) {
                this.valueSource = valueSource;
                valueSource.addCallbacks(this::complete, this::completeExceptionally);
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (isDone()) {
                    return false;
                }
                boolean result = valueSource.cancel(mayInterruptIfRunning);
                super.cancel(mayInterruptIfRunning);
                return result;
            }
        }

        private static final class CompletableFutureBackedValueSource<T> extends ValueSourceFuture<T> {
            private CompletableFutureBackedValueSource(CompletableFuture<T> completableFuture) {
                super(completableFuture);
            }

            @Override
            public void addCallbacks(Consumer<T> successCallback, Consumer<Throwable> failureCallback) {
                getWrappedFuture().whenComplete((v, t) -> {
                    if (t == null) {
                        successCallback.accept(v);
                    } else {
                        failureCallback.accept(t);
                    }
                });
            }

            @Override
            CompletableFuture<T> getWrappedFuture() {
                return (CompletableFuture<T>) super.getWrappedFuture();
            }
        }
    }
}
