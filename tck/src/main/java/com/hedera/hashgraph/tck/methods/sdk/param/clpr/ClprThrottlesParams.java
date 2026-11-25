// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.clpr;

import java.util.Map;

/**
 * The seven CLPR throttle limits. The uint32 limits are JSON numbers; the uint64 limits
 * ({@code maxGasPerMessage} and {@code maxSyncBytes}) are unsigned decimal strings.
 */
public record ClprThrottlesParams(
        long maxMessagesPerBundle,
        long maxMessagePayloadBytes,
        String maxGasPerMessage,
        long maxQueueDepth,
        String maxSyncBytes,
        long maxLocalEndpoints,
        long maxPeerEndpoints) {
    public static ClprThrottlesParams parse(Map<String, Object> params) {
        return new ClprThrottlesParams(
                number(params, "maxMessagesPerBundle"),
                number(params, "maxMessagePayloadBytes"),
                string(params, "maxGasPerMessage"),
                number(params, "maxQueueDepth"),
                string(params, "maxSyncBytes"),
                number(params, "maxLocalEndpoints"),
                number(params, "maxPeerEndpoints"));
    }

    private static long number(Map<String, Object> params, String name) {
        if (params.get(name) instanceof Number value) {
            return value.longValue();
        }
        throw new IllegalArgumentException(name + " must be a number");
    }

    private static String string(Map<String, Object> params, String name) {
        if (params.get(name) instanceof String value) {
            return value;
        }
        throw new IllegalArgumentException(name + " must be an unsigned decimal string");
    }
}
