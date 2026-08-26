// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import com.hedera.hashgraph.sdk.Status;
import java.util.List;

public class TokenMintResponse extends TokenResponse {
    private final String newTotalSupply;
    private final List<String> serialNumbers;

    public TokenMintResponse(String tokenId, Status status, String newTotalSupply, List<String> serialNumbers) {
        super(tokenId, status);
        this.newTotalSupply = newTotalSupply;
        this.serialNumbers = serialNumbers;
    }
}
