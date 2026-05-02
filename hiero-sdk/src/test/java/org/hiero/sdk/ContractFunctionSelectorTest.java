// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ContractFunctionSelectorTest {
    @Test
    @DisplayName("Can add all types")
    void selector() {
        var signature = new ContractFunctionSelector("testFunction")
                .addAddress()
                .addAddressArray()
                .addBool()
                .addBytes()
                .addBytes32()
                .addBytes32Array()
                .addBytesArray()
                .addFunction()
                .addInt8()
                .addInt8Array()
                .addInt32()
                .addInt32Array()
                .addInt64()
                .addInt64Array()
                .addInt256()
                .addInt256Array()
                .addUint8()
                .addUint8Array()
                .addUint32()
                .addUint32Array()
                .addUint64()
                .addUint64Array()
                .addUint256()
                .addUint256Array()
                .addString()
                .addStringArray()
                .finish();

        assertThat(Hex.toHexString(signature)).isEqualTo("4438e4ce");
    }

    @Test
    @DisplayName("Throws in adding after finished")
    void selectorError() {
        var signature = new ContractFunctionSelector("testFunction").addAddress();
        signature.finish();

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(signature::addStringArray);
        assertThatNoException().isThrownBy(signature::finish);
    }
}
