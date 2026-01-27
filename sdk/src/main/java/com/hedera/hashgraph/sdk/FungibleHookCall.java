// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.Objects;

/**
 * A typed hook call for fungible (HBAR and FT) transfers.
 */
public class FungibleHookCall extends HookCall {
    private final FungibleHookType type;

    public FungibleHookCall(long hookId, EvmHookCall evmHookCall, FungibleHookType type) {
        super(hookId, evmHookCall);
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    public FungibleHookType getType() {
        return type;
    }
}
