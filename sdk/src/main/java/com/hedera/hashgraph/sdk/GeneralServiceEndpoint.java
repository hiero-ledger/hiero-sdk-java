// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import java.util.Objects;
import javax.annotation.Nullable;

public class GeneralServiceEndpoint extends RegisteredServiceEndpointBase<GeneralServiceEndpoint> {
    @Nullable
    private String description;

    public GeneralServiceEndpoint() {}

    public GeneralServiceEndpoint setDescription(String description) {
        this.description = description;
        return this;
    }

    static GeneralServiceEndpoint fromProtobuf(
            com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint serviceEndpoint) {
        Objects.requireNonNull(serviceEndpoint, "serviceEndpoint must not be null");

        var generalEndpoint = new GeneralServiceEndpoint()
                .setPort(serviceEndpoint.getPort())
                .setRequiresTls(serviceEndpoint.getRequiresTls())
                .setDescription(serviceEndpoint.getGeneralService().getDescription());

        if (serviceEndpoint.hasIpAddress()) {
            generalEndpoint.setIpAddress(serviceEndpoint.getIpAddress().toByteArray());
        }

        if (serviceEndpoint.hasDomainName()) {
            generalEndpoint.setDomainName(serviceEndpoint.getDomainName());
        }

        return generalEndpoint;
    }

    @Override
    com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint toProtobuf() {
        if (ipAddress == null && domainName == null) {
            throw new IllegalArgumentException(
                    "RegisterServiceEndpoint must define either an ipAddress or  domainName");
        }

        var registeredServiceEndpoint = com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.newBuilder()
                .setPort(port)
                .setRequiresTls(requiresTls)
                .setGeneralService(
                        com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.GeneralServiceEndpoint.newBuilder()
                                .setDescription(this.description)
                                .build());

        if (ipAddress != null) {
            registeredServiceEndpoint.setIpAddress(ByteString.copyFrom(this.ipAddress));
        }

        if (domainName != null) {
            registeredServiceEndpoint.setDomainName(this.domainName);
        }

        return registeredServiceEndpoint.build();
    }
}
