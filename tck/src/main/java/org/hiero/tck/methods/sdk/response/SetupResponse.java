// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.response;

import lombok.Data;

@Data
public class SetupResponse {
    private String message = "";
    private String status = "";

    public SetupResponse(String message) {
        if (message != null && !message.isEmpty()) {
            this.message = message;
        }
        this.status = "SUCCESS";
    }
}
