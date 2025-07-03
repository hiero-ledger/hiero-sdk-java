// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DelegateContractIdTest {
    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void fromString() {
        SnapshotMatcher.expect(DelegateContractId.fromString("0.0.5005").toString())
                .toMatchSnapshot();
    }

    @Test
    void fromSolidityAddress() {
        SnapshotMatcher.expect(DelegateContractId.fromSolidityAddress("000000000000000000000000000000000000138D")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void fromSolidityAddressWith0x() {
        SnapshotMatcher.expect(DelegateContractId.fromSolidityAddress("0x000000000000000000000000000000000000138D")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(Hex.toHexString(new DelegateContractId(0, 0, 5005).toBytes()))
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(DelegateContractId.fromBytes(new DelegateContractId(0, 0, 5005).toBytes())
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toSolidityAddress() {
        SnapshotMatcher.expect(new DelegateContractId(0, 0, 5005).toSolidityAddress())
                .toMatchSnapshot();
    }

    @Test
    void fromEvmAddressIncorrectSizeTooShort() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    DelegateContractId.fromEvmAddress(0, 0, "abc123");
                })
                .withMessageContaining("Solidity addresses must be 20 bytes or 40 hex chars");
    }

    @Test
    void fromEvmAddressIncorrectSizeTooLong() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    DelegateContractId.fromEvmAddress(0, 0, "0123456789abcdef0123456789abcdef0123456789abcdef");
                })
                .withMessageContaining("Solidity addresses must be 20 bytes or 40 hex chars");
    }

    @Test
    void fromEvmAddressIncorrectSizeWith0xPrefix() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    DelegateContractId.fromEvmAddress(0, 0, "0xabc123");
                })
                .withMessageContaining("Solidity addresses must be 20 bytes or 40 hex chars");
    }

    @Test
    void fromEvmAddressCorrectSize() {
        String correctAddress = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e";
        DelegateContractId id = DelegateContractId.fromEvmAddress(0, 0, correctAddress);

        assertThat(id.evmAddress).isNotNull();
        assertThat(Hex.toHexString(id.evmAddress)).isEqualTo("742d35cc6634c0532925a3b844bc454e4438f44e");
    }

    @Test
    void toEvmAddressNormalContractId() {
        DelegateContractId id = new DelegateContractId(0, 0, 123);

        assertThat(id.toEvmAddress()).isEqualTo("000000000000000000000000000000000000007b");
    }

    @Test
    void toEvmAddressWithDifferentShardAndRealm() {
        DelegateContractId id = new DelegateContractId(1, 1, 123);

        assertThat(id.toEvmAddress()).isEqualTo("000000000000000000000000000000000000007b");
    }

    @Test
    void toEvmAddressLongZeroAddress() {
        String longZeroAddress = "00000000000000000000000000000000000004d2";
        DelegateContractId id = DelegateContractId.fromEvmAddress(1, 1, longZeroAddress);

        assertThat(id.toEvmAddress()).isEqualTo(longZeroAddress.toLowerCase());
    }

    @Test
    void toEvmAddressNormalEvmAddress() {
        String evmAddress = "742d35Cc6634C0532925a3b844Bc454e4438f44e";
        DelegateContractId id = DelegateContractId.fromEvmAddress(0, 0, evmAddress);
        String expected = evmAddress.toLowerCase();

        assertThat(id.toEvmAddress()).isEqualTo(expected);
    }

    @Test
    void toEvmAddressNormalEvmAddressWithShardAndRealm() {
        String evmAddress = "742d35Cc6634C0532925a3b844Bc454e4438f44e";
        DelegateContractId id = DelegateContractId.fromEvmAddress(1, 1, evmAddress);
        String expected = evmAddress.toLowerCase();

        assertThat(id.toEvmAddress()).isEqualTo(expected);
    }
}
