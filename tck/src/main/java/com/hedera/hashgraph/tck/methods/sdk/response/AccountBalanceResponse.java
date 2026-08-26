// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.TokenId;
import java.util.Map;
import javax.annotation.Nonnegative;

public record AccountBalanceResponse(
        @Nonnegative String hbars,
        Map<TokenId, Long> tokenBalances,
        @Nonnegative Map<TokenId, Integer> tokenDecimals) {}
