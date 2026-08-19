// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import com.hedera.hashgraph.sdk.Status;

public class TokenBurnResponse extends TokenResponse {

    private final String newTotalSupply;

    public TokenBurnResponse(String tokenId, Status status, String newTotalSupply) {
        super(tokenId, status);
        this.newTotalSupply = newTotalSupply;
    }
}
