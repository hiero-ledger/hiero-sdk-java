// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.BlockNodeEndpoint;

/**
 * An enumeration of well-known block node endpoint APIs.
 */
public enum BlockNodeApi {
    /**
     * Any other API type associated with a block node.
     */
    OTHER(BlockNodeEndpoint.BlockNodeApi.OTHER),

    /**
     * The Block Node Status API.
     */
    STATUS(BlockNodeEndpoint.BlockNodeApi.STATUS),

    /**
     * The Block Node Publish API.
     */
    PUBLISH(BlockNodeEndpoint.BlockNodeApi.PUBLISH),

    /**
     * The Block Node Subscribe Stream API.
     */
    SUBSCRIBE_STREAM(BlockNodeEndpoint.BlockNodeApi.SUBSCRIBE_STREAM),

    /**
     * The Block Node State Proof API.
     */
    STATE_PROOF(BlockNodeEndpoint.BlockNodeApi.STATE_PROOF);

    final BlockNodeEndpoint.BlockNodeApi code;

    BlockNodeApi(BlockNodeEndpoint.BlockNodeApi code) {
        this.code = code;
    }

    static BlockNodeApi valueOf(BlockNodeEndpoint.BlockNodeApi code) {
        return switch (code) {
            case OTHER -> OTHER;
            case STATUS -> STATUS;
            case PUBLISH -> PUBLISH;
            case SUBSCRIBE_STREAM -> SUBSCRIBE_STREAM;
            case STATE_PROOF -> STATE_PROOF;
            default -> throw new IllegalArgumentException("Unhandled BlockNodeApi code");
        };
    }

    @Override
    public String toString() {
        return code.name();
    }
}
