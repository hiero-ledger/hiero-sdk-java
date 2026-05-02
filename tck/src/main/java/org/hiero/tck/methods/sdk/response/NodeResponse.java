// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.response;

import org.hiero.sdk.Status;
import lombok.Data;

@Data
public class NodeResponse {
    private final String nodeId;
    private final Status status;
}

