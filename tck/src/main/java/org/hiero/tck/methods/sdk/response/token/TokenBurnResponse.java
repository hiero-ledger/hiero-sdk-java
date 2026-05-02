// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.response.token;

import org.hiero.sdk.Status;

public class TokenBurnResponse extends TokenResponse {

    private final String newTotalSupply;

    public TokenBurnResponse(String tokenId, Status status, String newTotalSupply) {
        super(tokenId, status);
        this.newTotalSupply = newTotalSupply;
    }

    public String getNewTotalSupply() {
        return newTotalSupply;
    }
}
