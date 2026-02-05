// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.TokenId;
import java.util.Map;
import javax.annotation.Nonnegative;
import lombok.Data;

@Data
public class AccountBalanceResponse {

    /**
     * The Hbar balance of the account
     */
    @Nonnegative
    public final String hbars;

    public final Map<TokenId, Long> tokenBalances;

    @Nonnegative
    public final Map<TokenId, Integer> tokenDecimals;
}
