// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.TokenId;
import java.util.Map;

/**
 * Represent accountBalance response.
 *
 * @param hbars the hbar balance of the account in tinybars, non-negative
 * @param tokenBalances a map of token IDs to the balances
 * @param tokenDecimals a map of token IDs to the decimal places, every value non-negative
 */
public record AccountBalanceResponse(
        String hbars, Map<TokenId, Long> tokenBalances, Map<TokenId, Integer> tokenDecimals) {}
