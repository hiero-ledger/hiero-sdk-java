// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import java.util.Objects;

/**
 * Specifies details of a call to an EVM hook.
 * <p>
 * This class represents the evm_hook_call field in the HookCall protobuf message.
 * It contains the call data and gas limit for executing an EVM hook.
 */
public class EvmHookCall {
    private final byte[] data;
    private final long gasLimit;

    /**
     * Create a new EvmHookCall.
     *
     * @param data the call data to pass to the hook via the IHieroHook.HookContext#data field
     * @param gasLimit the gas limit to use for the hook execution
     */
    public EvmHookCall(byte[] data, long gasLimit) {
        this.data = Objects.requireNonNull(data, "data cannot be null");
        this.gasLimit = gasLimit;
    }

    /**
     * Get the call data for this hook call.
     *
     * @return the call data as a byte array
     */
    public byte[] getData() {
        return data.clone(); // Return a copy to prevent external modification
    }

    /**
     * Get the gas limit for this hook call.
     *
     * @return the gas limit
     */
    public long getGasLimit() {
        return gasLimit;
    }

    /**
     * Convert this EvmHookCall to a protobuf message.
     *
     * @return the protobuf EvmHookCall
     */
    com.hedera.hashgraph.sdk.proto.EvmHookCall toProtobuf() {
        return com.hedera.hashgraph.sdk.proto.EvmHookCall.newBuilder()
                .setData(com.google.protobuf.ByteString.copyFrom(data))
                .setGasLimit(gasLimit)
                .build();
    }

    /**
     * Create an EvmHookCall from a protobuf message.
     *
     * @param proto the protobuf EvmHookCall
     * @return a new EvmHookCall instance
     */
    static EvmHookCall fromProtobuf(com.hedera.hashgraph.sdk.proto.EvmHookCall proto) {
        return new EvmHookCall(proto.getData().toByteArray(), proto.getGasLimit());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EvmHookCall that = (EvmHookCall) o;
        return gasLimit == that.gasLimit && java.util.Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = java.util.Arrays.hashCode(data);
        result = 31 * result + Long.hashCode(gasLimit);
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("data", data)
                .add("gasLimit", gasLimit)
                .toString();
    }
}
