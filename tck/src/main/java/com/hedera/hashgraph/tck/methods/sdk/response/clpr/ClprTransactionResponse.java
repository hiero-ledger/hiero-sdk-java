// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.clpr;

import com.hedera.hashgraph.sdk.Status;

/**
 * Response for the CLPR transactions.
 *
 * @param status the receipt status
 */
public record ClprTransactionResponse(Status status) {}
