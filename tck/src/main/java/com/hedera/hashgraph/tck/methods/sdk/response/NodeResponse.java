// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;

/**
 * Represent the response for all the node related transaction.
 *
 * @param nodeId the ID of the node
 * @param status the status of the submitted transaction
 */
public record NodeResponse(String nodeId, Status status) {}
