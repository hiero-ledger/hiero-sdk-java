// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import org.jspecify.annotations.Nullable;

/**
 * Utility exception class.
 */
public class MaxAttemptsExceededException extends IllegalStateException {
    MaxAttemptsExceededException(@Nullable Throwable e) {
        super("exceeded maximum attempts for request with last exception being", e);
    }
}
