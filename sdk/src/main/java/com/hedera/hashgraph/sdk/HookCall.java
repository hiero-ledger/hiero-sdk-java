// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import java.util.Objects;

/**
 * Specifies a call to a hook from within a transaction.
 * <p>
 * Often the hook's entity is implied by the nature of the call site. For example, when using an account allowance hook
 * inside a crypto transfer, the hook's entity is necessarily the account whose authorization is required.
 * <p>
 * For future extension points where the hook owner is not forced by the context, we include the option to fully
 * specify the hook id for the call.
 */
public abstract class HookCall {
    private final Long hookId;
    private final EvmHookCall evmHookCall;

    /**
     * Create a HookCall with a numeric hook ID.
     *
     * @param hookId the numeric ID of the hook to call
     * @param evmHookCall the EVM hook call details
     */
    protected HookCall(long hookId, EvmHookCall evmHookCall) {
        this.hookId = hookId;
        this.evmHookCall = Objects.requireNonNull(evmHookCall, "evmHookCall cannot be null");
    }

    /**
     * Get the numeric hook ID.
     *
     * @return the numeric hook ID, or null if using full hook ID
     */
    public Long getHookId() {
        return hookId;
    }

    /**
     * Get the EVM hook call details.
     *
     * @return the EVM hook call details
     */
    public EvmHookCall getEvmHookCall() {
        return evmHookCall;
    }

    /**
     * Convert this HookCall to a protobuf message.
     *
     * @return the protobuf HookCall
     */
    com.hedera.hashgraph.sdk.proto.HookCall toProtobuf() {
        var builder = com.hedera.hashgraph.sdk.proto.HookCall.newBuilder();

        if (hookId != null) {
            builder.setHookId(hookId);
        }

        builder.setEvmHookCall(evmHookCall.toProtobuf());

        return builder.build();
    }

    /**
     * Create a HookCall from a protobuf message.
     *
     * @param proto the protobuf HookCall
     * @return a new HookCall instance
     */
    static HookCall fromProtobuf(com.hedera.hashgraph.sdk.proto.HookCall proto) {
        return new HookCall(proto.getHookId(), EvmHookCall.fromProtobuf(proto.getEvmHookCall())) {};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HookCall hookCall = (HookCall) o;
        return Objects.equals(hookId, hookCall.hookId) && evmHookCall.equals(hookCall.evmHookCall);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(hookId);
        result = 31 * result + evmHookCall.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hookId", hookId)
                .add("evmHookCall", evmHookCall)
                .toString();
    }
}
