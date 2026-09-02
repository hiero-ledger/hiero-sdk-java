// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

/**
 * Represent the setup response.
 *
 * @param message the message to return
 * @param status the status of the submitted transaction
 */
public record SetupResponse(String message, String status) {
    public SetupResponse(String message) {
        this(message == null || message.isEmpty() ? "" : message, "SUCCESS");
    }
}
