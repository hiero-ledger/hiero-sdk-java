// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EndpointTest {

    @Test
    @DisplayName("validateNoIpAndDomain allows only IP")
    void validateAllowsOnlyIp() {
        var ep = new Endpoint().setAddress(new byte[] {127, 0, 0, 1}).setPort(50211);
        assertThatCode(() -> Endpoint.validateNoIpAndDomain(ep)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateNoIpAndDomain allows only domain")
    void validateAllowsOnlyDomain() {
        var ep = new Endpoint().setDomainName("node1.test.local").setPort(50211);
        assertThatCode(() -> Endpoint.validateNoIpAndDomain(ep)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateNoIpAndDomain throws when both IP and domain are set")
    void validateThrowsOnIpAndDomain() {
        var ep = new Endpoint()
                .setAddress(new byte[] {127, 0, 0, 1})
                .setDomainName("node1.test.local")
                .setPort(50211);
        assertThrows(IllegalArgumentException.class, () -> Endpoint.validateNoIpAndDomain(ep));
    }

    @Test
    @DisplayName("validateNoIpAndDomain is no-op for null endpoint")
    void validateNoOpOnNull() {
        assertThatCode(() -> Endpoint.validateNoIpAndDomain(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateNoIpAndDomain allows empty domain with IP")
    void validateAllowsEmptyDomainWithIp() {
        var ep = new Endpoint()
                .setAddress(new byte[] {10, 0, 0, 1})
                .setDomainName("")
                .setPort(50211);
        assertThatCode(() -> Endpoint.validateNoIpAndDomain(ep)).doesNotThrowAnyException();
    }
}
