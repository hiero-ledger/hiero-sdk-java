// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.List;

/**
 * Represent the fileInfo response.
 *
 * @param fileId the file ID
 * @param size the current file size in bytes
 * @param expirationTime the time at which this file is set to expire
 * @param isDeleted if true, then this file has been deleted
 * @param memo the memo associated with the file
 * @param ledgerId the ID of the ledger from which the response was returned
 * @param keys the keys required to modify the file
 */
public record FileInfoResponse(
        String fileId,
        String size,
        String expirationTime,
        Boolean isDeleted,
        String memo,
        String ledgerId,
        List<String> keys) {}
