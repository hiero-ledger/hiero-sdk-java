// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * Enum for the fee estimate mode.
 * <p>
 * Determines how the fee estimate is calculated for a transaction.
 */
public enum FeeEstimateMode {
    /**
     * Default mode: uses latest known state.
     * <p>
     * This mode calculates fees based on the current state of the network,
     * taking into account all state-dependent factors such as current
     * exchange rates, gas prices, and network congestion.
     */
    STATE(0),

    /**
     * Intrinsic mode: ignores state-dependent factors.
     * <p>
     * This mode calculates fees based only on the intrinsic properties of
     * the transaction itself, ignoring dynamic network conditions. This
     * provides a baseline estimate that doesn't fluctuate with network state.
     */
    INTRINSIC(1);

    final int code;

    FeeEstimateMode(int code) {
        this.code = code;
    }

    /**
     * Convert a protobuf-encoded fee estimate mode value to the corresponding enum.
     *
     * @param code the protobuf-encoded value
     * @return the corresponding FeeEstimateMode
     * @throws IllegalArgumentException if the code is not recognized
     */
    static FeeEstimateMode valueOf(int code) {
        return switch (code) {
            case 0 -> STATE;
            case 1 -> INTRINSIC;
            default -> throw new IllegalArgumentException("(BUG) unhandled FeeEstimateMode code: " + code);
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case STATE -> "STATE";
            case INTRINSIC -> "INTRINSIC";
        };
    }
}
