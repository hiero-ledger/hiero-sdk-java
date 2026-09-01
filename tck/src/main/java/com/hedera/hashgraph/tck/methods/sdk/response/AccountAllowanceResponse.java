// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;

/**
 * Represents the accountAllowance response.
 *
 * @param status the status of the submitted transaction
 */
public record AccountAllowanceResponse(Status status) {}
