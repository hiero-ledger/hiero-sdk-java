// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertEquals(message, setupResponse.getMessage());
        assertEquals("SUCCESS", setupResponse.getStatus());
    }

    @Test
    void testConstructorWithNullMessage() {
        // Given
        String message = null;

        // When
        SetupResponse setupResponse = new SetupResponse(message);

        // Then
        assertNotNull(setupResponse);
        assertEquals("", setupResponse.getMessage()); // message should default to empty string
        assertEquals("SUCCESS", setupResponse.getStatus());
    }

    @Test
    void testConstructorWithEmptyMessage() {
        // Given
        String message = "";

        // When
        SetupResponse setupResponse = new SetupResponse(message);

        // Then
        assertNotNull(setupResponse);
        assertEquals("", setupResponse.getMessage());
        assertEquals("SUCCESS", setupResponse.getStatus());
    }
}
