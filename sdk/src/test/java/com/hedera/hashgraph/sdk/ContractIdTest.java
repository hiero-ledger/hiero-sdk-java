// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

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
    void shouldHandleHieroAccountAddressFormat() {
        // Create a contract address that represents a Hedera contract (0.0.5)
        ContractId contractId = new ContractId(0, 0, 5);
        String solAddress = contractId.toSolidityAddress();

        // Convert it back using fromEvmAddress
        ContractId result = ContractId.fromEvmAddress(0, 0, solAddress);

        // Should reconstruct the original contract ID
        assertThat(result.shard).isEqualTo(0);
        assertThat(result.realm).isEqualTo(0);
        assertThat(result.num).isEqualTo(5);
        // Check if solidity address conversion works correctly
        assertThat(result.toSolidityAddress()).isEqualTo(solAddress);
    }

    @Test
    void shouldHandleNativeEvmAddresses() {
        String evmAddress = "742d35Cc6634C0532925a3b844Bc454e4438f44e";
        long shard = 1;
        long realm = 2;

        ContractId result = ContractId.fromEvmAddress(shard, realm, evmAddress);

        // For EVM addresses, should store shard/realm and the decoded EVM address
        assertThat(result.shard).isEqualTo(shard);
        assertThat(result.realm).isEqualTo(realm);

        // Verify the EVM address was properly stored (test depends on implementation details)
        assertThat(result.toSolidityAddress().toLowerCase()).contains(evmAddress.toLowerCase());
    }

    @Test
    void shouldHandleNonZeroShardAndRealmWithHieroAccountFormat() {
        // Create a contract address that represents a Hedera contract (1.2.5)
        ContractId contractId = new ContractId(1, 2, 5);
        String solAddress = contractId.toSolidityAddress();

        // Convert it back using fromEvmAddress with matching shard and realm
        ContractId result = ContractId.fromEvmAddress(1, 2, solAddress);

        // Should reconstruct the original contract ID
        assertThat(result.shard).isEqualTo(1);
        assertThat(result.realm).isEqualTo(2);
        assertThat(result.num).isEqualTo(5);
    }

    @Test
    void shouldHandleAddressesWith0xPrefix() {
        String evmAddress = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e";
        String evmAddressWithout0x = "742d35Cc6634C0532925a3b844Bc454e4438f44e";

        ContractId result1 = ContractId.fromEvmAddress(1, 2, evmAddress);
        ContractId result2 = ContractId.fromEvmAddress(1, 2, evmAddressWithout0x);

        // Both should produce equivalent results
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void shouldHandleLargeContractNumbersInHieroAccountFormat() {
        // Create a contract with a large number
        ContractId contractId = new ContractId(0, 0, 123456789L);
        String solAddress = contractId.toSolidityAddress();

        ContractId result = ContractId.fromEvmAddress(0, 0, solAddress);

        assertThat(result.num).isEqualTo(123456789L);
    }

    @Test
    void shouldHandleNegativeShardRealmValues() {
        String evmAddress = "742d35Cc6634C0532925a3b844Bc454e4438f44e";

        // Test negative shard
        assertThrows(IllegalArgumentException.class, () -> ContractId.fromEvmAddress(-1, 0, evmAddress));

        // Test negative realm
        assertThrows(IllegalArgumentException.class, () -> ContractId.fromEvmAddress(0, -1, evmAddress));
    }

    @Test
    void shouldHandleNonZeroShardAndRealmInSolidityAddress() {
        // Create a contract with non-zero shard and realm
        ContractId contractId = new ContractId(5, 1, 123);
        String solAddress = contractId.toSolidityAddress();

        // Use fromEvmAddress instead of deprecated fromSolidityAddress
        ContractId result = ContractId.fromEvmAddress(5, 1, solAddress);

        // Should reconstruct the original contract ID with proper shard and realm
        assertThat(result).isEqualTo(contractId);
        assertThat(result.shard).isEqualTo(5);
        assertThat(result.realm).isEqualTo(1);
        assertThat(result.num).isEqualTo(123);
    }

    @Test
    void shouldCorrectlyIdentifyHieroAccountAddress() {
        // Create a Hedera account ID solidity address
        ContractId contractId = new ContractId(0, 0, 5);
        String solAddress = contractId.toSolidityAddress();
        byte[] decodedAddress = Hex.decode(solAddress);

        // Should identify as a Hedera account address
        assertThat(EntityIdHelper.isHieroAccountAddress(decodedAddress)).isTrue();

        // Regular Ethereum address shouldn't be identified as Hedera account
        String ethAddress = "742d35Cc6634C0532925a3b844Bc454e4438f44e";
        byte[] ethDecodedAddress = Hex.decode(ethAddress);

        assertThat(EntityIdHelper.isHieroAccountAddress(ethDecodedAddress)).isFalse();
    }

    @Test
    void shouldUseSameLogicAsfromEvmAddressInsteadOfFromSolidityAddress() {
        // Create a contract with non-zero shard and realm
        ContractId contractId = new ContractId(5, 1, 123);
        String solAddress = contractId.toSolidityAddress();

        // Test that fromEvmAddress with proper shard/realm parameters works as expected
        ContractId fromEvmResult = ContractId.fromEvmAddress(5, 1, solAddress);

        // This test ensures that developers are using the new pattern
        assertThat(fromEvmResult.shard).isEqualTo(5);
        assertThat(fromEvmResult.realm).isEqualTo(1);
        assertThat(fromEvmResult.num).isEqualTo(123);
    }
}
