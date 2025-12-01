// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EvmHookCallTest {
    @Test
    void roundTripProtoAndGettersAndEquality() {
        byte[] data = new byte[] {1, 2, 3};
        long gas = 25000L;
        var call = new EvmHookCall(data, gas);

        // getters
        assertThat(call.getGasLimit()).isEqualTo(gas);
        assertThat(call.getData()).containsExactly(1, 2, 3);

        // immutability of data
        var returned = call.getData();
        returned[0] = 9;
        assertThat(call.getData()).containsExactly(1, 2, 3);

        // proto round-trip
        var proto = call.toProtobuf();
        var parsed = EvmHookCall.fromProtobuf(proto);
        assertThat(parsed).isEqualTo(call);
        assertThat(parsed.hashCode()).isEqualTo(call.hashCode());
    }

    @Test
    void nullDataThrows() {
        assertThatThrownBy(() -> new EvmHookCall(null, 1L))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("data cannot be null");
    }
}
