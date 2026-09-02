// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.TokenId;
import java.util.Map;
import javax.annotation.Nonnegative;

/**
 * Represent accountBalance response.
 *
 * @param hbars the hbar balance of the account in tinybars
 * @param tokenBalances a map of token IDs to the balances
 * @param tokenDecimals a map of token IDs to the decimal places
 */
public record AccountBalanceResponse(
        @Nonnegative String hbars,
        Map<TokenId, Long> tokenBalances,
        @Nonnegative Map<TokenId, Integer> tokenDecimals) {}
