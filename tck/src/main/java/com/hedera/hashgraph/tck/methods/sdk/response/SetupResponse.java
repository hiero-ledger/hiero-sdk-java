// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

public record SetupResponse(String message, String status) {
    public SetupResponse(String message) {
        this(message == null || message.isEmpty() ? "" : message, "SUCCESS");
    }
}
