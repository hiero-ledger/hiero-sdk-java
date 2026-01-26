// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * Hook type for fungible (HBAR and FT) transfers.
 */
public enum FungibleHookType {
    PRE_TX_ALLOWANCE_HOOK,
    PRE_POST_TX_ALLOWANCE_HOOK
}
