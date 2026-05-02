// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.response;

import org.hiero.sdk.Status;

public class TopicResponse {
    private String topicId;
    private Status status;

    public TopicResponse(String topicId, Status status) {
        this.topicId = topicId;
        this.status = status;
    }

    public String getTopicId() {
        return topicId;
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}

