// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;

/**
 * Represent the response for file related transactions.
 *
 * @param fileId the ID of the file
 * @param status the status of the submitted transaction
 */
public record FileResponse(String fileId, Status status) {}
