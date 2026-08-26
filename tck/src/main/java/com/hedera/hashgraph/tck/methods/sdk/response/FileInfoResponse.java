// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.List;

public record FileInfoResponse(
        String fileId,
        String size,
        String expirationTime,
        Boolean isDeleted,
        String memo,
        String ledgerId,
        List<String> keys) {}
