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
public class HookCall {
    private final HookId fullHookId;
    private final Long hookId;
    private final EvmHookCall evmHookCall;

    /**
     * Create a HookCall with a full hook ID.
     *
     * @param fullHookId the full ID of the hook to call
     * @param evmHookCall the EVM hook call details
     */
    public HookCall(HookId fullHookId, EvmHookCall evmHookCall) {
        this.fullHookId = Objects.requireNonNull(fullHookId, "fullHookId cannot be null");
        this.hookId = null;
        this.evmHookCall = Objects.requireNonNull(evmHookCall, "evmHookCall cannot be null");
    }

    /**
     * Create a HookCall with a numeric hook ID.
     *
     * @param hookId the numeric ID of the hook to call
     * @param evmHookCall the EVM hook call details
     */
    public HookCall(long hookId, EvmHookCall evmHookCall) {
        this.fullHookId = null;
        this.hookId = hookId;
        this.evmHookCall = Objects.requireNonNull(evmHookCall, "evmHookCall cannot be null");
    }

    /**
     * Get the full hook ID.
     *
     * @return the full hook ID, or null if using numeric hook ID
     */
    public HookId getFullHookId() {
        return fullHookId;
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
     * Check if this hook call uses a full hook ID.
     *
     * @return true if using full hook ID, false if using numeric hook ID
     */
    public boolean hasFullHookId() {
        return fullHookId != null;
    }

    /**
     * Convert this HookCall to a protobuf message.
     *
     * @return the protobuf HookCall
     */
    com.hedera.hashgraph.sdk.proto.HookCall toProtobuf() {
        var builder = com.hedera.hashgraph.sdk.proto.HookCall.newBuilder();

        if (fullHookId != null) {
            builder.setFullHookId(fullHookId.toProtobuf());
        } else {
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
        if (proto.hasFullHookId()) {
            return new HookCall(
                    HookId.fromProtobuf(proto.getFullHookId()), EvmHookCall.fromProtobuf(proto.getEvmHookCall()));
        } else {
            return new HookCall(proto.getHookId(), EvmHookCall.fromProtobuf(proto.getEvmHookCall()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HookCall hookCall = (HookCall) o;
        return Objects.equals(fullHookId, hookCall.fullHookId)
                && Objects.equals(hookId, hookCall.hookId)
                && evmHookCall.equals(hookCall.evmHookCall);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(fullHookId);
        result = 31 * result + Objects.hashCode(hookId);
        result = 31 * result + evmHookCall.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fullHookId", fullHookId)
                .add("hookId", hookId)
                .add("evmHookCall", evmHookCall)
                .toString();
    }
}
