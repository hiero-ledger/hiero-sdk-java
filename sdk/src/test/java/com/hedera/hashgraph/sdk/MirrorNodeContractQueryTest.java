// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MirrorNodeContractQueryTest {

    private MirrorNodeContractEstimateGasQuery mirrorNodeContractEstimateGasQuery;
    private MirrorNodeContractCallQuery mirrorNodeContractCallQuery;
    private ContractId mockContractId;

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @BeforeEach
    void setUp() {
        mirrorNodeContractEstimateGasQuery = new MirrorNodeContractEstimateGasQuery();
        mirrorNodeContractCallQuery = new MirrorNodeContractCallQuery();
        mockContractId = Mockito.mock(ContractId.class);
    }

    @Test
    void testSetAndGetContractId() {
        mirrorNodeContractEstimateGasQuery.setContractId(mockContractId);
        assertEquals(mockContractId, mirrorNodeContractEstimateGasQuery.getContractId());

        mirrorNodeContractCallQuery.setContractId(mockContractId);
        assertEquals(mockContractId, mirrorNodeContractCallQuery.getContractId());
    }

    @Test
    void testSetContractIdWithNullThrowsException() {
        assertThrows(NullPointerException.class, () -> mirrorNodeContractEstimateGasQuery.setContractId(null));
        assertThrows(NullPointerException.class, () -> mirrorNodeContractCallQuery.setContractId(null));
    }

    @Test
    void testSetAndGetContractEvmAddress() {
        String evmAddress = "0x1234567890abcdef1234567890abcdef12345678";
        mirrorNodeContractEstimateGasQuery.setContractEvmAddress(evmAddress);
        assertEquals(evmAddress, mirrorNodeContractEstimateGasQuery.getContractEvmAddress());
        assertNull(mirrorNodeContractEstimateGasQuery.getContractId());

        mirrorNodeContractCallQuery.setContractEvmAddress(evmAddress);
        assertEquals(evmAddress, mirrorNodeContractCallQuery.getContractEvmAddress());
        assertNull(mirrorNodeContractCallQuery.getContractId());
    }

    @Test
    void testSetContractEvmAddressWithNullThrowsException() {
        assertThrows(NullPointerException.class, () -> mirrorNodeContractEstimateGasQuery.setContractEvmAddress(null));
        assertThrows(NullPointerException.class, () -> mirrorNodeContractCallQuery.setContractEvmAddress(null));
    }

    @Test
    void testSetAndGetcallData() {
        ByteString params = ByteString.copyFromUtf8("test");
        mirrorNodeContractEstimateGasQuery.setFunctionParameters(params);
        assertArrayEquals(params.toByteArray(), mirrorNodeContractEstimateGasQuery.getCallData());

        mirrorNodeContractCallQuery.setFunctionParameters(params);
        assertArrayEquals(params.toByteArray(), mirrorNodeContractCallQuery.getCallData());
    }

    @Test
    void testSetFunctionWithoutParameters() {
        mirrorNodeContractEstimateGasQuery.setFunction("myFunction");
        assertNotNull(mirrorNodeContractEstimateGasQuery.getCallData());
    }

    @Test
    void testSetAndGetBlockNumber() {
        long blockNumber = 123456;
        mirrorNodeContractEstimateGasQuery.setBlockNumber(blockNumber);
        assertEquals(blockNumber, mirrorNodeContractEstimateGasQuery.getBlockNumber());

        mirrorNodeContractCallQuery.setBlockNumber(blockNumber);
        assertEquals(blockNumber, mirrorNodeContractCallQuery.getBlockNumber());
    }

    @Test
    void testSetAndGetValue() {
        long value = 1000;
        mirrorNodeContractEstimateGasQuery.setValue(value);
        assertEquals(value, mirrorNodeContractEstimateGasQuery.getValue());

        mirrorNodeContractCallQuery.setValue(value);
        assertEquals(value, mirrorNodeContractCallQuery.getValue());
    }

    @Test
    void testSetAndGetGas() {
        long gas = 50000;
        mirrorNodeContractEstimateGasQuery.setGasLimit(gas);
        assertEquals(gas, mirrorNodeContractEstimateGasQuery.getGasLimit());

        mirrorNodeContractCallQuery.setGasLimit(gas);
        assertEquals(gas, mirrorNodeContractCallQuery.getGasLimit());
    }

    @Test
    void testSetAndGetGasPrice() {
        long gasPrice = 200;
        mirrorNodeContractEstimateGasQuery.setGasPrice(gasPrice);
        assertEquals(gasPrice, mirrorNodeContractEstimateGasQuery.getGasPrice());

        mirrorNodeContractCallQuery.setGasPrice(gasPrice);
        assertEquals(gasPrice, mirrorNodeContractCallQuery.getGasPrice());
    }

    @Test
    void testEstimateGasWithMissingContractIdOrEvmAddressThrowsException() {
        ByteString params = ByteString.copyFromUtf8("gasParams");
        mirrorNodeContractEstimateGasQuery.setFunctionParameters(params);
        assertThrows(NullPointerException.class, () -> mirrorNodeContractEstimateGasQuery.estimate(null));

        mirrorNodeContractCallQuery.setFunctionParameters(params);
        assertThrows(NullPointerException.class, () -> mirrorNodeContractCallQuery.estimate(null));
    }

    @Test
    void testCreateJsonPayloadAllFieldsSet() {
        byte[] data = "testData".getBytes();
        String senderAddress = "0x1234567890abcdef1234567890abcdef12345678";
        String contractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdef";
        long gas = 50000;
        long gasPrice = 2000;
        long value = 1000;
        Long blockNumber = 0L;
        boolean estimate = true;

        String jsonPayload = MirrorNodeContractQuery.createJsonPayload(
                data, senderAddress, contractAddress, gas, gasPrice, value, blockNumber, estimate);

        JsonObject expectedJson = new JsonObject();
        expectedJson.addProperty("data", "7465737444617461");
        expectedJson.addProperty("to", contractAddress);
        expectedJson.addProperty("estimate", estimate);
        expectedJson.addProperty("block", blockNumber);
        expectedJson.addProperty("from", senderAddress);
        expectedJson.addProperty("gas", gas);
        expectedJson.addProperty("gasPrice", gasPrice);
        expectedJson.addProperty("value", value);

        assertEquals(expectedJson.toString(), jsonPayload);
    }

    @Test
    void testCreateJsonPayloadOnlyRequiredFieldsSet() {
        byte[] data = "testData".getBytes();
        String senderAddress = "";
        String contractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdef";
        long gas = 0;
        long gasPrice = 0;
        long value = 0;
        Long block = null;
        boolean estimate = true;

        String jsonPayload = MirrorNodeContractQuery.createJsonPayload(
                data, senderAddress, contractAddress, gas, gasPrice, value, block, estimate);

        JsonObject expectedJson = new JsonObject();
        expectedJson.addProperty("data", "7465737444617461");
        expectedJson.addProperty("to", contractAddress);
        expectedJson.addProperty("estimate", estimate);

        assertEquals(expectedJson.toString(), jsonPayload);
    }

    @Test
    void testCreateJsonPayloadSomeOptionalFieldsSet() {
        byte[] data = "testData".getBytes();
        String senderAddress = "0x1234567890abcdef1234567890abcdef12345678";
        String contractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdef";
        long gas = 50000;
        long gasPrice = 0;
        long value = 1000;
        Long block = 0L;
        boolean estimate = false;

        String jsonPayload = MirrorNodeContractQuery.createJsonPayload(
                data, senderAddress, contractAddress, gas, gasPrice, value, block, estimate);

        JsonObject expectedJson = new JsonObject();
        expectedJson.addProperty("data", "7465737444617461");
        expectedJson.addProperty("to", contractAddress);
        expectedJson.addProperty("estimate", estimate);
        expectedJson.addProperty("block", block);
        expectedJson.addProperty("from", senderAddress);
        expectedJson.addProperty("gas", gas);
        expectedJson.addProperty("value", value);

        assertEquals(expectedJson.toString(), jsonPayload);
    }

    @Test
    void testCreateJsonPayloadAllOptionalFieldsDefault() {
        byte[] data = "testData".getBytes();
        String contractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdef";
        String senderAddress = "";
        long gas = 0;
        long gasPrice = 0;
        long value = 0;
        Long block = 0L;
        boolean estimate = false;

        String jsonPayload = MirrorNodeContractQuery.createJsonPayload(
                data, senderAddress, contractAddress, gas, gasPrice, value, block, estimate);

        JsonObject expectedJson = new JsonObject();
        expectedJson.addProperty("data", "7465737444617461");
        expectedJson.addProperty("to", contractAddress);
        expectedJson.addProperty("estimate", estimate);
        expectedJson.addProperty("block", block);

        assertEquals(expectedJson.toString(), jsonPayload);
    }

    @Test
    void testParseHexEstimateToLong() {
        String responseBody = "{\"result\": \"0x1234\"}";
        long parsedResult = MirrorNodeContractQuery.parseHexEstimateToLong(responseBody);
        assertEquals(0x1234, parsedResult);
    }

    @Test
    void testParseContractCallResult() {
        String responseBody = "{\"result\": \"0x1234abcdef\"}";
        String parsedResult = MirrorNodeContractQuery.parseContractCallResult(responseBody);
        assertEquals("0x1234abcdef", parsedResult);
    }

    @Test
    void shouldSerialize() {
        ContractId testContractId = new ContractId(0, 0, 1234);
        String testEvmAddress = "0x1234567890abcdef1234567890abcdef12345678";
        AccountId testSenderId = new AccountId(0, 0, 5678);
        String testSenderEvmAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdef";
        ByteString testCallData = ByteString.copyFromUtf8("testData");
        String testFunctionName = "myFunction";
        ContractFunctionParameters testParams = new ContractFunctionParameters().addString("param1");

        long testValue = 1000L;
        long testGasLimit = 500000L;
        long testGasPrice = 20L;
        long testBlockNumber = 123456L;

        var mirrorNodeContractEstimateGasQuery = new MirrorNodeContractEstimateGasQuery()
                .setContractId(testContractId)
                .setContractEvmAddress(testEvmAddress)
                .setSender(testSenderId)
                .setSenderEvmAddress(testSenderEvmAddress)
                .setFunction(testFunctionName, testParams)
                .setFunctionParameters(testCallData)
                .setValue(testValue)
                .setGasLimit(testGasLimit)
                .setGasPrice(testGasPrice)
                .setBlockNumber(testBlockNumber);

        var mirrorNodeContractCallQuery = new MirrorNodeContractCallQuery()
                .setContractId(testContractId)
                .setContractEvmAddress(testEvmAddress)
                .setSender(testSenderId)
                .setSenderEvmAddress(testSenderEvmAddress)
                .setFunction(testFunctionName, testParams)
                .setFunctionParameters(testCallData)
                .setValue(testValue)
                .setGasLimit(testGasLimit)
                .setGasPrice(testGasPrice)
                .setBlockNumber(testBlockNumber);

        SnapshotMatcher.expect(mirrorNodeContractEstimateGasQuery.toString() + mirrorNodeContractCallQuery.toString())
                .toMatchSnapshot();
    }
}
