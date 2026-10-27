// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RegisteredNodeAddressBookQueryTest {
    private RegisteredNodeAddressBookQuery registeredNodeAddressBookQuery;

    @BeforeEach
    void setup() {
        registeredNodeAddressBookQuery = new RegisteredNodeAddressBookQuery();
    }

    @Test
    void testSetAndGetRegisteredNodeId() {
        var nodeId = 1;
        registeredNodeAddressBookQuery.setRegisteredNodeId(nodeId);
        assertThat(registeredNodeAddressBookQuery.getRegisteredNodeId()).isEqualTo(nodeId);
    }

    @Test
    void testSetAndGetLimit() {
        var limit = 10;
        registeredNodeAddressBookQuery.setLimit(limit);
        assertThat(registeredNodeAddressBookQuery.getLimit()).isEqualTo(limit);
    }

    @Test
    void testSetAndGetMaxAttempts() {
        var maxAttempts = 2;
        registeredNodeAddressBookQuery.setMaxAttempts(maxAttempts);
        assertThat(registeredNodeAddressBookQuery.getMaxAttempts()).isEqualTo(maxAttempts);
    }

    @Test
    void testSetAndGetMaxBackoff() {
        var backoff = Duration.ofSeconds(10);
        registeredNodeAddressBookQuery.setMaxBackoff(backoff);
        assertThat(registeredNodeAddressBookQuery.getMaxBackoff()).isEqualTo(backoff);
    }

    @Test
    void testSetMaxBackoffLessThan500Ms() {
        var backoff = Duration.ofMillis(499);
        assertThatThrownBy(() -> registeredNodeAddressBookQuery.setMaxBackoff(backoff))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxBackoff must be at least 500 ms");
    }
}
