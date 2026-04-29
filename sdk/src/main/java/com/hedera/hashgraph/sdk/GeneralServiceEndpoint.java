// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Represents a general-purpose service endpoint.
 */
public class GeneralServiceEndpoint extends RegisteredServiceEndpointBase<GeneralServiceEndpoint> {
    /**
     * A short description of the service provided.
     */
    @Nullable
    private String description;

    /**
     * Constructor.
     */
    public GeneralServiceEndpoint() {}

    /**
     * Returns the description of the service provided by this endpoint.
     *
     * @return the service description, or null if not set
     */
    public @Nullable String getDescription() {
        return this.description;
    }

    /**
     * Sets a short description of the service provided.
     * This value MUST NOT exceed 100 bytes when encoded as UTF-8.
     *
     * @param description a short description of the service
     * @return {@code this}
     */
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

    /**
     * Parses GeneralServiceEndpoint from the type-specific JSON object the MirrorNode.
     *
     * @param json the json containing general service specific data
     * @return {@code this}
     */
    static GeneralServiceEndpoint fromJson(JsonObject json) {
        Objects.requireNonNull(json, "json must not be null");

        String description = json.has("description") && !json.get("description").isJsonNull()
                ? json.get("description").getAsString()
                : null;

        return new GeneralServiceEndpoint().setDescription(description);
    }

    @Override
    com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint toProtobuf() {
        if (ipAddress == null && domainName == null) {
            throw new IllegalArgumentException(
                    "RegisterServiceEndpoint must define either an ipAddress or  domainName");
        }

        var generalService = com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.GeneralServiceEndpoint.newBuilder();
        if (description != null) {
            generalService.setDescription(description);
        }

        var registeredServiceEndpoint = com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.newBuilder()
                .setPort(port)
                .setRequiresTls(requiresTls)
                .setGeneralService(generalService);


        if (ipAddress != null) {
            registeredServiceEndpoint.setIpAddress(ByteString.copyFrom(this.ipAddress));
        }

        if (domainName != null) {
            registeredServiceEndpoint.setDomainName(this.domainName);
        }

        return registeredServiceEndpoint.build();
    }

    @Override
    public String toString() {
        return toStringHelper().add("description", description).toString();
    }
}
