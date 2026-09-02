// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import com.hedera.hashgraph.sdk.Status;

/**
 * Represent the response for the token related transaction.
 *
 * @param tokenId the ID of the token
 * @param status the status of the submitted transaction
 */
public record TokenResponse(String tokenId, Status status) {}
