// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hiero.sdk.BaseNodeAddress.PORT_MIRROR_TLS;
import static org.hiero.sdk.BaseNodeAddress.PORT_NODE_PLAIN;
import static org.hiero.sdk.BaseNodeAddress.PORT_NODE_TLS;

import org.junit.jupiter.api.Test;

public class BaseNodeAddressTest {
    @Test
    void fromString() {
        var ipAddress = BaseNodeAddress.fromString("35.237.200.180:50211");
        assertThat(ipAddress.getName()).isNull();
        assertThat(ipAddress.getAddress()).isEqualTo("35.237.200.180");
        assertThat(ipAddress.getPort()).isEqualTo(PORT_NODE_PLAIN);
        assertThat(ipAddress).hasToString("35.237.200.180:50211");

        var ipAddressSecure = ipAddress.toSecure();
        assertThat(ipAddressSecure.getName()).isNull();
        assertThat(ipAddressSecure.getAddress()).isEqualTo("35.237.200.180");
        assertThat(ipAddressSecure.getPort()).isEqualTo(PORT_NODE_TLS);
        assertThat(ipAddressSecure).hasToString("35.237.200.180:50212");

        var ipAddressInsecure = ipAddressSecure.toInsecure();
        assertThat(ipAddressInsecure.getName()).isNull();
        assertThat(ipAddressInsecure.getAddress()).isEqualTo("35.237.200.180");
        assertThat(ipAddressInsecure.getPort()).isEqualTo(PORT_NODE_PLAIN);
        assertThat(ipAddressInsecure).hasToString("35.237.200.180:50211");

        var urlAddress = BaseNodeAddress.fromString("0.testnet.hedera.com:50211");
        assertThat(urlAddress.getName()).isNull();
        assertThat(urlAddress.getAddress()).isEqualTo("0.testnet.hedera.com");
        assertThat(urlAddress.getPort()).isEqualTo(PORT_NODE_PLAIN);
        assertThat(urlAddress).hasToString("0.testnet.hedera.com:50211");

        var urlAddressSecure = urlAddress.toSecure();
        assertThat(urlAddressSecure.getName()).isNull();
        assertThat(urlAddressSecure.getAddress()).isEqualTo("0.testnet.hedera.com");
        assertThat(urlAddressSecure.getPort()).isEqualTo(PORT_NODE_TLS);
        assertThat(urlAddressSecure).hasToString("0.testnet.hedera.com:50212");

        var urlAddressInsecure = urlAddressSecure.toInsecure();
        assertThat(urlAddressInsecure.getName()).isNull();
        assertThat(urlAddressInsecure.getAddress()).isEqualTo("0.testnet.hedera.com");
        assertThat(urlAddressInsecure.getPort()).isEqualTo(PORT_NODE_PLAIN);
        assertThat(urlAddressInsecure).hasToString("0.testnet.hedera.com:50211");

        var processAddress = BaseNodeAddress.fromString("in-process:testingProcess");
        assertThat(processAddress.getName()).isEqualTo("testingProcess");
        assertThat(processAddress.getAddress()).isNull();
        assertThat(processAddress.getPort()).isEqualTo(0);
        assertThat(processAddress).hasToString("testingProcess");

        var processAddressSecure = processAddress.toSecure();
        assertThat(processAddressSecure.getName()).isEqualTo("testingProcess");
        assertThat(processAddressSecure.getAddress()).isNull();
        assertThat(processAddressSecure.getPort()).isEqualTo(0);
        assertThat(processAddressSecure).hasToString("testingProcess");

        var processAddressInsecure = processAddressSecure.toInsecure();
        assertThat(processAddressInsecure.getName()).isEqualTo("testingProcess");
        assertThat(processAddressInsecure.getAddress()).isNull();
        assertThat(processAddressInsecure.getPort()).isEqualTo(0);
        assertThat(processAddressInsecure).hasToString("testingProcess");

        var mirrorNodeAddress = BaseNodeAddress.fromString("mainnet-public.mirrornode.hedera.com:443");
        assertThat(mirrorNodeAddress.getName()).isNull();
        assertThat(mirrorNodeAddress.getAddress()).isEqualTo("mainnet-public.mirrornode.hedera.com");
        assertThat(mirrorNodeAddress.getPort()).isEqualTo(PORT_MIRROR_TLS);
        assertThat(mirrorNodeAddress).hasToString("mainnet-public.mirrornode.hedera.com:443");

        var mirrorNodeAddressSecure = mirrorNodeAddress.toSecure();
        assertThat(mirrorNodeAddressSecure.getName()).isNull();
        assertThat(mirrorNodeAddressSecure.getAddress()).isEqualTo("mainnet-public.mirrornode.hedera.com");
        assertThat(mirrorNodeAddressSecure.getPort()).isEqualTo(PORT_MIRROR_TLS);
        assertThat(mirrorNodeAddressSecure).hasToString("mainnet-public.mirrornode.hedera.com:443");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> BaseNodeAddress.fromString("this is a random string with spaces:443"));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> BaseNodeAddress.fromString("mainnet-public.mirrornode.hedera.com:notarealport"));
    }
}
