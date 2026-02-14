// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileInfoResponse {
    private final String fileId;
    private final String size;
    private final String expirationTime;
    private final Boolean isDeleted;
    private final String memo;
    private final String ledgerId;
    private final List<String> keys;
}
