// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TopicResponse {
    private String topicId;
    private Status status;
}
