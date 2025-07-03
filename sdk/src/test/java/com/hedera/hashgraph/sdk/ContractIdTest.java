// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ContractIdTest {
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
        SnapshotMatcher.expect(ContractId.fromString("0.0.5005").toString()).toMatchSnapshot();
    }

    @Test
    void fromSolidityAddress() {
        SnapshotMatcher.expect(ContractId.fromSolidityAddress("000000000000000000000000000000000000138D")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void fromSolidityAddressWith0x() {
        SnapshotMatcher.expect(ContractId.fromSolidityAddress("0x000000000000000000000000000000000000138D")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void fromEvmAddress() {
        SnapshotMatcher.expect(ContractId.fromEvmAddress(1, 2, "98329e006610472e6B372C080833f6D79ED833cf")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void fromEvmAddressWith0x() {
        SnapshotMatcher.expect(ContractId.fromEvmAddress(1, 2, "0x98329e006610472e6B372C080833f6D79ED833cf")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void fromStringWithEvmAddress() {
        SnapshotMatcher.expect(ContractId.fromString("1.2.98329e006610472e6B372C080833f6D79ED833cf")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toFromBytes() throws InvalidProtocolBufferException {
        ContractId a = ContractId.fromString("1.2.3");
        assertThat(ContractId.fromBytes(a.toBytes())).isEqualTo(a);
        ContractId b = ContractId.fromEvmAddress(1, 2, "0x98329e006610472e6B372C080833f6D79ED833cf");
        assertThat(ContractId.fromBytes(b.toBytes())).isEqualTo(b);
    }

    @Test
    void toBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(Hex.toHexString(new ContractId(0, 0, 5005).toBytes()))
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(ContractId.fromBytes(new ContractId(0, 0, 5005).toBytes())
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toSolidityAddress() {
        SnapshotMatcher.expect(new ContractId(0, 0, 5005).toSolidityAddress()).toMatchSnapshot();
    }

    @Test
    void toSolidityAddress2() {
        SnapshotMatcher.expect(ContractId.fromEvmAddress(1, 2, "0x98329e006610472e6B372C080833f6D79ED833cf")
                        .toSolidityAddress())
                .toMatchSnapshot();
    }

    @Test
    void fromEvmAddressIncorrectSizeTooShort() {
        // Test with an EVM address that's too short
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    ContractId.fromEvmAddress(0, 0, "abc123");
                })
                .withMessageContaining("Solidity addresses must be 20 bytes or 40 hex chars");
    }

    @Test
    void fromEvmAddressIncorrectSizeTooLong() {
        // Test with an EVM address that's too long
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    ContractId.fromEvmAddress(0, 0, "0123456789abcdef0123456789abcdef0123456789abcdef");
                })
                .withMessageContaining("Solidity addresses must be 20 bytes or 40 hex chars");
    }

    @Test
    void fromEvmAddressIncorrectSizeWith0xPrefix() {
        // Test with a 0x prefix that gets removed but then is too short
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    ContractId.fromEvmAddress(0, 0, "0xabc123");
                })
                .withMessageContaining("Solidity addresses must be 20 bytes or 40 hex chars");
    }

    @Test
    void fromEvmAddressCorrectSize() {
        // Verify a correct length works
        String correctAddress = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e";
        ContractId id = ContractId.fromEvmAddress(0, 0, correctAddress);

        assertThat(id.evmAddress).isNotNull();
        assertThat(Hex.toHexString(id.evmAddress)).isEqualTo("742d35cc6634c0532925a3b844bc454e4438f44e");
    }

    @Test
    void fromEvmAddressNormalAddress() {
        // Test with a normal EVM address
        String evmAddress = "742d35Cc6634C0532925a3b844Bc454e4438f44e";
        byte[] expectedBytes = Hex.decode(evmAddress);

        ContractId id = ContractId.fromEvmAddress(0, 0, evmAddress);

        assertThat(id.shard).isEqualTo(0);
        assertThat(id.realm).isEqualTo(0);
        assertThat(id.num).isEqualTo(0);
        assertThat(id.evmAddress).isEqualTo(expectedBytes);
    }

    @Test
    void fromEvmAddressWithDifferentShardAndRealm() {
        // Test with a different shard and realm
        String evmAddress = "742d35Cc6634C0532925a3b844Bc454e4438f44e";
        byte[] expectedBytes = Hex.decode(evmAddress);

        ContractId id = ContractId.fromEvmAddress(1, 1, evmAddress);

        assertThat(id.shard).isEqualTo(1);
        assertThat(id.realm).isEqualTo(1);
        assertThat(id.num).isEqualTo(0);
        assertThat(id.evmAddress).isEqualTo(expectedBytes);
    }

    @Test
    void fromEvmAddressLongZeroAddress() {
        // Test with a long zero address
        String evmAddress = "00000000000000000000000000000000000004d2";
        byte[] expectedBytes = Hex.decode(evmAddress);

        ContractId id = ContractId.fromEvmAddress(0, 0, evmAddress);

        assertThat(id.shard).isEqualTo(0);
        assertThat(id.realm).isEqualTo(0);
        assertThat(id.num).isEqualTo(0);
        assertThat(id.evmAddress).isEqualTo(expectedBytes);
    }

    @Test
    void fromEvmAddressLongZeroAddressWithShardAndRealm() {
        // Test with a long zero address and different shard and realm
        String evmAddress = "00000000000000000000000000000000000004d2";
        byte[] expectedBytes = Hex.decode(evmAddress);

        ContractId id = ContractId.fromEvmAddress(1, 1, evmAddress);

        assertThat(id.shard).isEqualTo(1);
        assertThat(id.realm).isEqualTo(1);
        assertThat(id.num).isEqualTo(0);
        assertThat(id.evmAddress).isEqualTo(expectedBytes);
    }

    @Test
    void toEvmAddressNormalContractId() {
        // Test with a normal contract ID
        ContractId id = new ContractId(0, 0, 123);

        assertThat(id.toEvmAddress()).isEqualTo("000000000000000000000000000000000000007b");
    }

    @Test
    void toEvmAddressWithDifferentShardAndRealm() {
        // Test with a different shard and realm
        ContractId id = new ContractId(1, 1, 123);

        assertThat(id.toEvmAddress()).isEqualTo("000000000000000000000000000000000000007b");
    }

    @Test
    void toEvmAddressLongZeroAddress() {
        // Test with a long zero address
        String longZeroAddress = "00000000000000000000000000000000000004d2";
        ContractId id = ContractId.fromEvmAddress(1, 1, longZeroAddress);

        assertThat(id.toEvmAddress()).isEqualTo(longZeroAddress.toLowerCase());
    }

    @Test
    void toEvmAddressNormalEvmAddress() {
        // Test with a normal EVM address
        String evmAddress = "742d35Cc6634C0532925a3b844Bc454e4438f44e";
        ContractId id = ContractId.fromEvmAddress(0, 0, evmAddress);
        String expected = evmAddress.toLowerCase();

        assertThat(id.toEvmAddress()).isEqualTo(expected);
    }

    @Test
    void toEvmAddressNormalEvmAddressWithShardAndRealm() {
        // Test with normal EVM address and different shard and realm
        String evmAddress = "742d35Cc6634C0532925a3b844Bc454e4438f44e";
        ContractId id = ContractId.fromEvmAddress(1, 1, evmAddress);
        String expected = evmAddress.toLowerCase();

        assertThat(id.toEvmAddress()).isEqualTo(expected);
    }
}
