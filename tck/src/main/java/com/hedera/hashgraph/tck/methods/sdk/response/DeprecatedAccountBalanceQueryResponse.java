// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import javax.annotation.Nullable;

/**
 * Represent the AccountBalanceQuery deprecation response.
 *
 * @param constructionWarning the warning the SDK logged while constructing the query, or null if none
 * @param executionError the message of the error the operation raised, or null if it returned normally
 */
public record DeprecatedAccountBalanceQueryResponse(
        @Nullable String constructionWarning, @Nullable String executionError) {}
