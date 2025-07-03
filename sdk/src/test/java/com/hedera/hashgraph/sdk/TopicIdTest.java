// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TopicIdTest {
    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void shouldSerializeFromString() {
        SnapshotMatcher.expect(TopicId.fromString("0.0.5005").toString()).toMatchSnapshot();
    }

    @Test
    void toBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(Hex.toHexString(new TopicId(0, 0, 5005).toBytes()))
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(
                        TopicId.fromBytes(new TopicId(0, 0, 5005).toBytes()).toString())
                .toMatchSnapshot();
    }

    @Test
    void fromSolidityAddress() {
        SnapshotMatcher.expect(TokenId.fromSolidityAddress("000000000000000000000000000000000000138D")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toSolidityAddress() {
        SnapshotMatcher.expect(new TokenId(0, 0, 5005).toSolidityAddress()).toMatchSnapshot();
    }

    @Test
    void testTopicIdFromEvmAddressIncorrectAddress() {
        // Test with an EVM address that's too short
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            TopicId.fromEvmAddress(0, 0, "abc123");
        });
        assertTrue(exception.getMessage().contains("Solidity addresses must be 20 bytes or 40 hex chars"));

        // Test with an EVM address that's too long
        exception = assertThrows(IllegalArgumentException.class, () -> {
            TopicId.fromEvmAddress(0, 0, "0123456789abcdef0123456789abcdef0123456789abcdef");
        });
        assertTrue(exception.getMessage().contains("Solidity addresses must be 20 bytes or 40 hex chars"));

        // Test with a 0x prefix that gets removed but then is too short
        exception = assertThrows(IllegalArgumentException.class, () -> {
            TopicId.fromEvmAddress(0, 0, "0xabc123");
        });
        assertTrue(exception.getMessage().contains("Solidity addresses must be 20 bytes or 40 hex chars"));

        // Test with non-long-zero address
        exception = assertThrows(IllegalArgumentException.class, () -> {
            TopicId.fromEvmAddress(0, 0, "742d35Cc6634C0532925a3b844Bc454e4438f44e");
        });
        assertTrue(exception.getMessage().contains("EVM address is not a correct long zero address"));
    }

    @Test
    void testTopicIdFromEvmAddress() {
        // Test with a long zero address representing topic 1234
        String evmAddress = "00000000000000000000000000000000000004d2";
        TopicId id = TopicId.fromEvmAddress(0, 0, evmAddress);

        assertEquals(0, id.shard);
        assertEquals(0, id.realm);
        assertEquals(1234, id.num);

        // Test with a different shard and realm
        id = TopicId.fromEvmAddress(1, 1, evmAddress);

        assertEquals(1, id.shard);
        assertEquals(1, id.realm);
        assertEquals(1234, id.num);
    }

    @Test
    void testTopicIdToEvmAddress() {
        // Test with a normal topic ID
        TopicId id = new TopicId(0, 0, 123);
        assertEquals("000000000000000000000000000000000000007b", id.toEvmAddress());

        // Test with a different shard and realm
        id = new TopicId(1, 1, 123);
        assertEquals("000000000000000000000000000000000000007b", id.toEvmAddress());
    }
}
