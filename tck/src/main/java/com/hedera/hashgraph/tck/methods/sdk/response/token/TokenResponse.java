// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import com.hedera.hashgraph.sdk.Status;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TokenResponse {
    private String tokenId;
    private Status status;
}
