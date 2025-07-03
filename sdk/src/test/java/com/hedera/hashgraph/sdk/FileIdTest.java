// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FileIdTest {
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
        SnapshotMatcher.expect(FileId.fromString("0.0.5005").toString()).toMatchSnapshot();
    }

    @Test
    void toBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(Hex.toHexString(new FileId(0, 0, 5005).toBytes()))
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(
                        FileId.fromBytes(new FileId(0, 0, 5005).toBytes()).toString())
                .toMatchSnapshot();
    }

    @Test
    void fromSolidityAddress() {
        SnapshotMatcher.expect(FileId.fromSolidityAddress("000000000000000000000000000000000000138D")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toSolidityAddress() {
        SnapshotMatcher.expect(new FileId(0, 0, 5005).toSolidityAddress()).toMatchSnapshot();
    }

    @Test
    void getAddressBookFileIdForReturnsCorrectFileId() {
        FileId defaultAddressBook = FileId.getAddressBookFileIdFor(0, 0);
        assertNotNull(defaultAddressBook);
        assertEquals(0, defaultAddressBook.shard);
        assertEquals(0, defaultAddressBook.realm);
        assertEquals(102, defaultAddressBook.num);

        long testShard = 5;
        long testRealm = 10;
        FileId customAddressBook = FileId.getAddressBookFileIdFor(testShard, testRealm);
        assertNotNull(customAddressBook);
        assertEquals(testShard, customAddressBook.shard);
        assertEquals(testRealm, customAddressBook.realm);
        assertEquals(102, customAddressBook.num);

        assertEquals("5.10.102", customAddressBook.toString());

        SnapshotMatcher.expect(customAddressBook.toString()).toMatchSnapshot();
    }

    @Test
    void getFeeScheduleFileIdForReturnsCorrectFileId() {
        FileId defaultFeeSchedule = FileId.getFeeScheduleFileIdFor(0, 0);
        assertNotNull(defaultFeeSchedule);
        assertEquals(0, defaultFeeSchedule.shard);
        assertEquals(0, defaultFeeSchedule.realm);
        assertEquals(111, defaultFeeSchedule.num);

        long testShard = 7;
        long testRealm = 12;
        FileId customFeeSchedule = FileId.getFeeScheduleFileIdFor(testShard, testRealm);
        assertNotNull(customFeeSchedule);
        assertEquals(testShard, customFeeSchedule.shard);
        assertEquals(testRealm, customFeeSchedule.realm);
        assertEquals(111, customFeeSchedule.num);

        assertEquals("7.12.111", customFeeSchedule.toString());

        SnapshotMatcher.expect(customFeeSchedule.toString()).toMatchSnapshot();
    }

    @Test
    void getExchangeRatesFileIdForReturnsCorrectFileId() {
        FileId defaultExchangeRates = FileId.getExchangeRatesFileIdFor(0, 0);
        assertNotNull(defaultExchangeRates);
        assertEquals(0, defaultExchangeRates.shard);
        assertEquals(0, defaultExchangeRates.realm);
        assertEquals(112, defaultExchangeRates.num);

        long testShard = 3;
        long testRealm = 9;
        FileId customExchangeRates = FileId.getExchangeRatesFileIdFor(testShard, testRealm);
        assertNotNull(customExchangeRates);
        assertEquals(testShard, customExchangeRates.shard);
        assertEquals(testRealm, customExchangeRates.realm);
        assertEquals(112, customExchangeRates.num);

        assertEquals("3.9.112", customExchangeRates.toString());

        SnapshotMatcher.expect(customExchangeRates.toString()).toMatchSnapshot();
    }

    @Test
    void testFileIdFromEvmAddressIncorrectAddress() {
        // Test with an EVM address that's too short
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            FileId.fromEvmAddress(0, 0, "abc123");
        });
        assertTrue(exception.getMessage().contains("Solidity addresses must be 20 bytes or 40 hex chars"));

        // Test with an EVM address that's too long
        exception = assertThrows(IllegalArgumentException.class, () -> {
            FileId.fromEvmAddress(0, 0, "0123456789abcdef0123456789abcdef0123456789abcdef");
        });
        assertTrue(exception.getMessage().contains("Solidity addresses must be 20 bytes or 40 hex chars"));

        // Test with a 0x prefix that gets removed but then is too short
        exception = assertThrows(IllegalArgumentException.class, () -> {
            FileId.fromEvmAddress(0, 0, "0xabc123");
        });
        assertTrue(exception.getMessage().contains("Solidity addresses must be 20 bytes or 40 hex chars"));

        // Test with non-long-zero address
        exception = assertThrows(IllegalArgumentException.class, () -> {
            FileId.fromEvmAddress(0, 0, "742d35Cc6634C0532925a3b844Bc454e4438f44e");
        });
        assertTrue(exception.getMessage().contains("EVM address is not a correct long zero address"));
    }

    @Test
    void testFileIdFromEvmAddress() {
        // Test with a long zero address representing file 1234
        String evmAddress = "00000000000000000000000000000000000004d2";
        FileId id = FileId.fromEvmAddress(0, 0, evmAddress);

        assertEquals(0, id.shard);
        assertEquals(0, id.realm);
        assertEquals(1234, id.num);

        // Test with a different shard and realm
        id = FileId.fromEvmAddress(1, 1, evmAddress);

        assertEquals(1, id.shard);
        assertEquals(1, id.realm);
        assertEquals(1234, id.num);
    }

    @Test
    void testFileIdToEvmAddress() {
        // Test with a normal file ID
        FileId id = new FileId(0, 0, 123);
        assertEquals("000000000000000000000000000000000000007b", id.toEvmAddress());

        // Test with a different shard and realm
        id = new FileId(1, 1, 123);
        assertEquals("000000000000000000000000000000000000007b", id.toEvmAddress());
    }
}
