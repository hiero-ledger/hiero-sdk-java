// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AddressBookResponseTest {
    @Test
    void shouldSerializeAddressBookResponseWithAllFields() {
        var nodeAddress = new AddressBookResponse.NodeAddress(
                "publicKey",
                "0.0.1",
                1L,
                "certHex",
                List.of(new AddressBookResponse.Endpoint("127.0.0.1", 8080, "test.co.in")),
                "Test Node",
                1L);
        var response = new AddressBookResponse();
        response.addNodeAddress(nodeAddress);
        var json = serializeToJson(response);

        Assertions.assertTrue(
                json.contains(
                        "\"nodeAddresses\":[{\"publicKey\":\"publicKey\",\"accountId\":\"0.0.1\",\"nodeId\":1,\"certHash\":\"certHex\",\"serviceEndpoints\":[{\"address\":\"127.0.0.1\",\"port\":8080,\"domainName\":\"test.co.in\"}],\"description\":\"Test Node\",\"stake\":1}]"));
    }

    @Test
    void shouldSerializeAddressBookResponseWithEmptyServiceEndpoint() {
        var nodeAddress =
                new AddressBookResponse.NodeAddress("publicKey", "0.0.1", 1L, "certHex", List.of(), "Test Node", 1L);
        var response = new AddressBookResponse();
        response.addNodeAddress(nodeAddress);
        var json = serializeToJson(response);

        Assertions.assertTrue(
                json.contains(
                        "\"nodeAddresses\":[{\"publicKey\":\"publicKey\",\"accountId\":\"0.0.1\",\"nodeId\":1,\"certHash\":\"certHex\",\"serviceEndpoints\":[],\"description\":\"Test Node\",\"stake\":1}]"));
    }

    @Test
    void shouldSerializeAddressBookResponseWithEmptyNodeAddress() {
        var response = new AddressBookResponse();
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"nodeAddresses\":[]"));
    }
}
