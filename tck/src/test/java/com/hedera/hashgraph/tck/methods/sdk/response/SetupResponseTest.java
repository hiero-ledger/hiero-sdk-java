// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SetupResponseTest {

    @Test
    void testConstructorWithMessage() {
        // Given
        String message = "Test message";

        // When
        SetupResponse setupResponse = new SetupResponse(message);

        // Then
        assertNotNull(setupResponse);
        assertEquals(message, setupResponse.message());
        assertEquals("SUCCESS", setupResponse.status());
    }

    @Test
    void testConstructorWithNullMessage() {
        // Given
        String message = null;

        // When
        SetupResponse setupResponse = new SetupResponse(message);

        // Then
        assertNotNull(setupResponse);
        assertEquals("", setupResponse.message()); // message should default to empty string
        assertEquals("SUCCESS", setupResponse.status());
    }

    @Test
    void testConstructorWithEmptyMessage() {
        // Given
        String message = "";

        // When
        SetupResponse setupResponse = new SetupResponse(message);

        // Then
        assertNotNull(setupResponse);
        assertEquals("", setupResponse.message());
        assertEquals("SUCCESS", setupResponse.status());
    }

    @Test
    void shouldSerializeSetupResponseWithAllFields() {
        var response = new SetupResponse("testMessage");
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"message\":\"testMessage\""));
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
    }
}
