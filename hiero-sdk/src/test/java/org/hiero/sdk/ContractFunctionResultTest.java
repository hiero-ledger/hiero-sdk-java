// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import java.math.BigInteger;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ContractFunctionResultTest {
    static final String CALL_RESULT_HEX = "00000000000000000000000000000000000000000000000000000000ffffffff"
            + "7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
            + "00000000000000000000000011223344556677889900aabbccddeeff00112233"
            + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
            + "00000000000000000000000000000000000000000000000000000000000000c0"
            + "0000000000000000000000000000000000000000000000000000000000000100"
            + "000000000000000000000000000000000000000000000000000000000000000d"
            + "48656c6c6f2c20776f726c642100000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000014"
            + "48656c6c6f2c20776f726c642c20616761696e21000000000000000000000000";

    private static final String STRING_ARRAY_RESULT_HEX =
            "0000000000000000000000000000000000000000000000000000000000000020"
                    + "0000000000000000000000000000000000000000000000000000000000000002"
                    + "0000000000000000000000000000000000000000000000000000000000000040"
                    + "0000000000000000000000000000000000000000000000000000000000000080"
                    + "000000000000000000000000000000000000000000000000000000000000000C"
                    + "72616E646F6D2062797465730000000000000000000000000000000000000000"
                    + "000000000000000000000000000000000000000000000000000000000000000C"
                    + "72616E646F6D2062797465730000000000000000000000000000000000000000";

    private static final byte[] callResult = Hex.decode(CALL_RESULT_HEX);
    private static final byte[] stringArrayCallResult = Hex.decode(STRING_ARRAY_RESULT_HEX);

    @Test
    @DisplayName("provides results correctly")
    void providesResultsCorrectly() {
        var result = new ContractFunctionResult(org.hiero.sdk.proto.ContractFunctionResult.newBuilder()
                .setContractID(ContractId.fromString("1.2.3").toProtobuf())
                .setContractCallResult(ByteString.copyFrom(callResult))
                .setEvmAddress(BytesValue.newBuilder()
                        .setValue(ByteString.copyFrom(Hex.decode("98329e006610472e6B372C080833f6D79ED833cf")))
                        .build())
                // .addStateChanges(
                //     new ContractStateChange(
                //         ContractId.fromString("1.2.3"),
                //         Collections.singletonList(
                //             new StorageChange(
                //                 BigInteger.valueOf(555),
                //                 BigInteger.valueOf(666),
                //                 BigInteger.valueOf(777)
                //             )
                //         )
                //     ).toProtobuf())
                .setSenderId(AccountId.fromString("1.2.3").toProtobuf())
                .addContractNonces(new ContractNonceInfo(ContractId.fromString("1.2.3"), 10L).toProtobuf()));

        // interpretation varies based on width
        assertThat(result.getBool(0)).isTrue();
        assertThat(result.getInt32(0)).isEqualTo(-1);
        assertThat(result.getInt64(0)).isEqualTo((1L << 32) - 1);
        assertThat(result.getInt256(0)).isEqualTo(BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE));

        assertThat(result.getInt256(1)).isEqualTo(BigInteger.ONE.shiftLeft(255).subtract(BigInteger.ONE));

        assertThat(result.getAddress(2)).isEqualTo("11223344556677889900aabbccddeeff00112233");

        // unsigned integers (where applicable)
        assertThat(result.getUint32(3)).isEqualTo(-1);
        assertThat(result.getUint64(3)).isEqualTo(-1L);
        // BigInteger can represent the full range and so should be 2^256 - 1
        assertThat(result.getUint256(3)).isEqualTo(BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE));

        assertThat(result.getString(4)).isEqualTo("Hello, world!");
        assertThat(result.getString(5)).isEqualTo("Hello, world, again!");

        assertThat(result.senderAccountId).isEqualTo(AccountId.fromString("1.2.3"));

        assertThat(result.contractId).isEqualTo(ContractId.fromString("1.2.3"));
        assertThat(result.evmAddress)
                .isEqualTo(ContractId.fromEvmAddress(1, 2, "98329e006610472e6B372C080833f6D79ED833cf"));
        // assertThat(result.stateChanges.size()).isEqualTo(1);
        // ContractStateChange resultStateChange = result.stateChanges.get(0);
        // assertThat(resultStateChange.contractId).isEqualTo(ContractId.fromString("1.2.3"));
        // assertThat(resultStateChange.storageChanges.size()).isEqualTo(1);
        // StorageChange resultStorageChange = resultStateChange.storageChanges.get(0);
        // assertThat(resultStorageChange.slot).isEqualTo(BigInteger.valueOf(555));
        // assertThat(resultStorageChange.valueRead).isEqualTo(BigInteger.valueOf(666));
        // assertThat(resultStorageChange.valueWritten).isEqualTo(BigInteger.valueOf(777));
        assertThat(result.contractNonces).containsOnly(new ContractNonceInfo(ContractId.fromString("1.2.3"), 10L));
    }

    @Test
    @DisplayName("can get string array result")
    void canGetStringArrayResult() {
        var result = new ContractFunctionResult(org.hiero.sdk.proto.ContractFunctionResult.newBuilder()
                .setContractCallResult(ByteString.copyFrom(stringArrayCallResult)));

        var strings = result.getStringArray(0);
        assertThat(strings.get(0)).isEqualTo("random bytes");
        assertThat(strings.get(1)).isEqualTo("random bytes");
    }

    @Test
    @DisplayName("Can to/from bytes with state changes")
    void canToFromBytesStateChanges() {}
}
