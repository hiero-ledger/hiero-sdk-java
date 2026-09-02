// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;

/**
 * Represent response for topic related transaction.
 *
 * @param topicId the ID of the topic
 * @param status the status of the submitted transaction
 */
public record TopicResponse(String topicId, Status status) {}
