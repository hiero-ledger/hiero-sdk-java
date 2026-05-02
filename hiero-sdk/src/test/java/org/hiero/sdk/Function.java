// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

public interface Function<T, R> {
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R apply(T t) throws Throwable;
}
