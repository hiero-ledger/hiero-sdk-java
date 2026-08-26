// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import com.hedera.hashgraph.sdk.Status;
import java.util.List;

public record TokenMintResponse(String tokenId, Status status, String newTotalSupply, List<String> serialNumbers) {}
