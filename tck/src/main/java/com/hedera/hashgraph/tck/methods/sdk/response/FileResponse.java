// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;
import lombok.Data;

@Data
public class FileResponse {
    private final String fileId;
    private final Status status;
}
