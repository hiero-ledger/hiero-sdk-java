// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

public class EthereumEncodingTest {

    @Test
    public void uint64Encoding() {
        assertThat(Hex.toHexString(EthereumEncoding.uint64ToEthBytes(0))).isEqualTo("");
        assertThat(Hex.toHexString(EthereumEncoding.uint64ToEthBytes(1))).isEqualTo("01");
        assertThat(Hex.toHexString(EthereumEncoding.uint64ToEthBytes(256))).isEqualTo("0100");
        assertThat(Hex.toHexString(EthereumEncoding.uint64ToEthBytes(0xDEADBEEFL)))
                .isEqualTo("deadbeef");
    }

    @Test
    public void uint64RoundTrip() {
        long[] values = {0L, 1L, 255L, 256L, 65535L, 0xDEADBEEFL, Long.MAX_VALUE};
        for (long value : values) {
            assertThat(EthereumEncoding.ethBytesToLong(EthereumEncoding.uint64ToEthBytes(value)))
                    .isEqualTo(value);
        }
    }

    @Test
    public void ethBytesToLongHandlesEmptyAndLong() {
        assertThat(EthereumEncoding.ethBytesToLong(new byte[] {})).isEqualTo(0L);
        // longer than 8 bytes truncates to the low 8
        byte[] nineBytes = Hex.decode("01000000000000000a");
        assertThat(EthereumEncoding.ethBytesToLong(nineBytes)).isEqualTo(10L);
    }

    @Test
    public void bigIntEncoding() {
        assertThat(Hex.toHexString(EthereumEncoding.bigIntToEthBytes(null))).isEqualTo("");
        assertThat(Hex.toHexString(EthereumEncoding.bigIntToEthBytes(BigInteger.ZERO)))
                .isEqualTo("");
        assertThat(Hex.toHexString(EthereumEncoding.bigIntToEthBytes(BigInteger.ONE)))
                .isEqualTo("01");
        // 1 ether = 10^18, big-endian minimal with no leading zero
        BigInteger oneEther = new BigInteger("1000000000000000000");
        assertThat(Hex.toHexString(EthereumEncoding.bigIntToEthBytes(oneEther))).isEqualTo("0de0b6b3a7640000");
        // a value whose high bit is set must not be padded with a leading zero
        BigInteger highBit = new BigInteger("128");
        assertThat(Hex.toHexString(EthereumEncoding.bigIntToEthBytes(highBit))).isEqualTo("80");
    }

    @Test
    public void bigIntRoundTrip() {
        BigInteger[] values = {
            BigInteger.ZERO,
            BigInteger.ONE,
            new BigInteger("255"),
            new BigInteger("1000000000000000000"),
            new BigInteger("ffffffffffffffffffffffffffffffff", 16)
        };
        for (BigInteger value : values) {
            assertThat(EthereumEncoding.ethBytesToBigInt(EthereumEncoding.bigIntToEthBytes(value)))
                    .isEqualTo(value);
        }
    }

    @Test
    public void typedAndBytesAccessorsAreConsistent() {
        var tx = new EthereumTransactionDataEip1559().setNonce(42).setValue(new BigInteger("1000000000000000000"));
        assertThat(tx.getNonce()).isEqualTo(42L);
        assertThat(EthereumEncoding.ethBytesToLong(tx.getNonceBytes())).isEqualTo(42L);
        assertThat(tx.getValue()).isEqualTo(new BigInteger("1000000000000000000"));
        assertThat(Hex.toHexString(tx.getValueBytes())).isEqualTo("0de0b6b3a7640000");
    }
}
