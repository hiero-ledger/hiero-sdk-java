// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import com.hedera.hashgraph.sdk.Status;

public record TokenBurnResponse(String tokenId, Status status, String newTotalSupply) {}
