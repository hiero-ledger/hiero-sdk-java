// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.response;

import java.util.Map;
import javax.annotation.Nonnegative;
import lombok.Data;
import org.hiero.sdk.TokenId;

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
