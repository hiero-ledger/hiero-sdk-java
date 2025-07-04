// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TokenIdTest {
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
        SnapshotMatcher.expect(TokenId.fromString("0.0.5005").toString()).toMatchSnapshot();
    }

    @Test
    void toBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(Hex.toHexString(new TokenId(0, 0, 5005).toBytes()))
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(
                        TokenId.fromBytes(new TokenId(0, 0, 5005).toBytes()).toString())
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
    void unitTokenIdFromString() {
        TokenId tokenId = new TokenId(1, 2, 3);
        TokenId tokenIdFromString = TokenId.fromString(tokenId.toString());
        assertEquals(tokenId, tokenIdFromString);
    }

    @Test
    void unitTokenIdChecksumFromString() throws Exception {
        TokenId tokenId = TokenId.fromString("0.0.123");
        Client client = Client.forTestnet();

        tokenId.toStringWithChecksum(client);
        String sol = tokenId.toSolidityAddress();
        TokenId.fromEvmAddress(0,0, sol);
        tokenId.validate(client);

        // Test protobuf conversion
        var pb = tokenId.toProtobuf();
        TokenId.fromProtobuf(pb);

        // Test bytes conversion
        byte[] idBytes = tokenId.toBytes();
        TokenId.fromBytes(idBytes);

        // Test comparison
        tokenId.compareTo(new TokenId(0, 0, 32));

        assertEquals(123, tokenId.num);
    }

    @Test
    void unitTokenIdChecksumToString() {
        TokenId id = new TokenId(50, 150, 520);
        assertEquals("50.150.520", id.toString());
    }

    @Test
    void unitTokenIdFromStringEVM() {
        TokenId id = TokenId.fromString("0.0.434");
        assertEquals("0.0.434", id.toString());
    }

    @Test
    void unitTokenIdProtobuf() throws InvalidProtocolBufferException {
        TokenId tokenId = TokenId.fromString("0.0.434");
        var pb = tokenId.toProtobuf();

        assertEquals(0, pb.getShardNum());
        assertEquals(0, pb.getRealmNum());
        assertEquals(434, pb.getTokenNum());

        TokenId pbFrom = TokenId.fromProtobuf(pb);
        assertEquals(tokenId, pbFrom);
    }

    @Test
    void testTokenIdFromEvmAddressIncorrectAddress() {
        // Test with an EVM address that's too short
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            TokenId.fromEvmAddress(0, 0, "abc123");
        });
        assertTrue(exception.getMessage().contains("Solidity addresses must be 20 bytes or 40 hex chars"));

        // Test with an EVM address that's too long
        exception = assertThrows(IllegalArgumentException.class, () -> {
            TokenId.fromEvmAddress(0, 0, "0123456789abcdef0123456789abcdef0123456789abcdef");
        });
        assertTrue(exception.getMessage().contains("Solidity addresses must be 20 bytes or 40 hex chars"));

        // Test with a 0x prefix that gets removed but then is too short
        exception = assertThrows(IllegalArgumentException.class, () -> {
            TokenId.fromEvmAddress(0, 0, "0xabc123");
        });
        assertTrue(exception.getMessage().contains("Solidity addresses must be 20 bytes or 40 hex chars"));

        // Test with non-long-zero address
        exception = assertThrows(IllegalArgumentException.class, () -> {
            TokenId.fromEvmAddress(0, 0, "742d35Cc6634C0532925a3b844Bc454e4438f44e");
        });
        assertTrue(exception.getMessage().contains("EVM address is not a correct long zero address"));
    }

    @Test
    void testTokenIdFromEvmAddress() {
        // Test with a long zero address representing token 1234
        String evmAddress = "00000000000000000000000000000000000004d2";
        TokenId tokenId = TokenId.fromEvmAddress(0, 0, evmAddress);

        assertEquals(0, tokenId.shard);
        assertEquals(0, tokenId.realm);
        assertEquals(1234, tokenId.num);

        // Test with a different shard and realm
        tokenId = TokenId.fromEvmAddress(1, 1, evmAddress);

        assertEquals(1, tokenId.shard);
        assertEquals(1, tokenId.realm);
        assertEquals(1234, tokenId.num);
    }

    @Test
    void testTokenIdToEvmAddress() {
        // Test with a normal token ID
        TokenId id = new TokenId(0, 0, 123);
        assertEquals("000000000000000000000000000000000000007b", id.toEvmAddress());

        // Test with a different shard and realm
        id = new TokenId(1, 1, 123);
        assertEquals("000000000000000000000000000000000000007b", id.toEvmAddress());
    }
}
