// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.BlockNodeEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BlockNodeApiTest {
    @Test
    @DisplayName("BlockNodeApi can be constructed for BlockNodeEndpoint API")
    void blockNodeApiCodeToBlockNodeApi() {
        for (BlockNodeEndpoint.BlockNodeApi code : BlockNodeEndpoint.BlockNodeApi.values()) {
            if (code == BlockNodeEndpoint.BlockNodeApi.UNRECOGNIZED) {
                continue;
            }

            BlockNodeApi blockNodeApi = BlockNodeApi.valueOf(code);
            assertThat(code.getNumber()).isEqualTo(blockNodeApi.code.getNumber());
        }
    }

    @Test
    @DisplayName("BlockNodeApi throws on Unrecognized")
    void blockNodeApiUnrecognized() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> BlockNodeApi.valueOf(BlockNodeEndpoint.BlockNodeApi.UNRECOGNIZED))
                .withMessage("Unhandled BlockNodeApi code");
    }
}
