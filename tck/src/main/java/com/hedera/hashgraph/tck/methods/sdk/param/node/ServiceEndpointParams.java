// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.node;

import com.hedera.hashgraph.sdk.Endpoint;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bouncycastle.util.encoders.Hex;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceEndpointParams {
    private Optional<String> ipAddressV4;
    private Optional<String> domainName;
    private Optional<Integer> port;

    public static ServiceEndpointParams parse(Map<String, Object> params) {
        var parsedIp = Optional.ofNullable((String) params.get("ipAddressV4"));
        var parsedDomain = Optional.ofNullable((String) params.get("domainName"));
        Integer parsedPort = null;
        Object portObj = params.get("port");
        if (portObj instanceof Number n) {
            parsedPort = n.intValue();
        }
        return new ServiceEndpointParams(parsedIp, parsedDomain, Optional.ofNullable(parsedPort));
    }

    public Endpoint toSdkEndpoint() {
        var ep = new Endpoint();
        domainName.ifPresent(ep::setDomainName);
        ipAddressV4.ifPresent(hex -> ep.setAddress(Hex.decode(hex)));
        port.ifPresent(ep::setPort);
        return ep;
    }
}
