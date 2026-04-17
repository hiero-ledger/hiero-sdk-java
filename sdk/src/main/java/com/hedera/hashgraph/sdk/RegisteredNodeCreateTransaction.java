// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.AddressBookServiceGrpc;
import com.hedera.hashgraph.sdk.proto.RegisteredNodeCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A transaction to create a new registered node in the network
 * address book.
 *<p>
 * This transaction, once complete, SHALL add a new registered node to the
 * network state.
 * The new registered node SHALL be visible and discoverable upon
 * completion of this transaction.
 */
public class RegisteredNodeCreateTransaction extends Transaction<RegisteredNodeCreateTransaction> {
    private Key adminKey;
    private String description = "";
    private List<RegisteredServiceEndpoint> serviceEndpoints = new ArrayList<>();

    /**
     * Constructor.
     */
    public RegisteredNodeCreateTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    RegisteredNodeCreateTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    RegisteredNodeCreateTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Get administrative key controlled by the node operator.
     * @return {@code Key} the admin key
     */
    public Key getAdminKey() {
        return adminKey;
    }

    /**
     * Set administrative key controlled by the node operator.
     * <p>
     * This key MUST sign this transaction.<br/>
     * This key MUST sign each transaction to update this node.<br/>
     * This field MUST contain a valid `Key` value.<br/>
     * This field is REQUIRED.
     *
     * @param adminKey the admin key for the registered node.
     * @return  {@code this}
     */
    public RegisteredNodeCreateTransaction setAdminKey(Key adminKey) {
        this.requireNotFrozen();
        this.adminKey = adminKey;
        return this;
    }

    /**
     * Get short description of the node.
     * @return {@code String} the node's description
     */
    public String getDescription() {
        return description;
    }

    /**
     * A short description of the node.
     * <p>
     * This value, if set, MUST NOT exceed 100 bytes when encoded as UTF-8.<br/>
     * This field is OPTIONAL.
     *
     * @param description The string to be set as description for the node.
     * @return {@code this}
     */
    public RegisteredNodeCreateTransaction setDescription(@Nullable String description) {
        this.requireNotFrozen();
        if (description == null) {
            this.description = "";
            return this;
        }

        if (description.getBytes(StandardCharsets.UTF_8).length > 100) {
            throw new IllegalArgumentException("description must not exceed 100 bytes when UTF-8 encoded");
        }
        this.description = description;
        return this;
    }

    /**
     * Get list of service endpoints for client calls.
     * @return {@code List<RegisterServiceEndpoint>} list of service endpoints
     */
    public List<RegisteredServiceEndpoint> getServiceEndpoints() {
        return serviceEndpoints;
    }

    /**
     * A list of service endpoints for client calls.
     * <p>
     * These endpoints SHALL represent the published endpoints to which
     * clients may submit requests.<br/>
     * Endpoints in this list MAY supply either IP address or FQDN, but MUST
     * NOT supply both values for the same endpoint.<br/>
     * Multiple endpoints in this list MAY resolve to the same interface.<br/>
     * One Registered Node MAY expose endpoints for multiple service types.<br/>
     * This list MUST NOT be empty.<br/>
     * This list MUST NOT contain more than `50` entries.
     *
     * @param serviceEndpoints the list of service endpoints for the client calls.
     * @return {@code this}
     */
    public RegisteredNodeCreateTransaction setServiceEndpoints(List<RegisteredServiceEndpoint> serviceEndpoints) {
        this.requireNotFrozen();
        Objects.requireNonNull(serviceEndpoints, "serviceEndpoints cannot be null");

        if (serviceEndpoints.isEmpty()) {
            throw new IllegalArgumentException("serviceEndpoints list must not be empty.");
        }
        if (serviceEndpoints.size() > 50) {
            throw new IllegalArgumentException("serviceEndpoints must not contain more than 50 entries");
        }

        for (RegisteredServiceEndpoint serviceEndpoint : serviceEndpoints) {
            RegisteredServiceEndpoint.validateNoIpAndDomain(serviceEndpoint);
        }

        this.serviceEndpoints = new ArrayList<>(serviceEndpoints);
        return this;
    }

    /**
     * Add a service endpoint for the client calls.
     * @param serviceEndpoint the service endpoint
     * @return {@code this}
     */
    public RegisteredNodeCreateTransaction addServiceEndpoint(RegisteredServiceEndpoint serviceEndpoint) {
        requireNotFrozen();
        if (serviceEndpoints.size() >= 50) {
            throw new IllegalArgumentException("serviceEndpoints must not contain more than 50 entries");
        }

        RegisteredServiceEndpoint.validateNoIpAndDomain(serviceEndpoint);
        serviceEndpoints.add(serviceEndpoint);
        return this;
    }

    /**
     * Build the transaction body.
     * @return {@link com.hedera.hashgraph.sdk.proto.RegisteredNodeCreateTransactionBody}
     */
    RegisteredNodeCreateTransactionBody.Builder build() {
        var builder = RegisteredNodeCreateTransactionBody.newBuilder().setDescription(description);

        if (adminKey != null) {
            builder.setAdminKey(adminKey.toProtobufKey());
        }

        for (RegisteredServiceEndpoint serviceEndpoint : serviceEndpoints) {
            builder.addServiceEndpoint(serviceEndpoint.toProtobuf());
        }

        return builder;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getRegisteredNodeCreate();

        if (body.hasAdminKey()) {
            adminKey = Key.fromProtobufKey(body.getAdminKey());
        }

        description = body.getDescription();

        serviceEndpoints.clear();
        for (com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint serviceEndpoint : body.getServiceEndpointList()) {
            serviceEndpoints.add(RegisteredServiceEndpoint.fromProtobuf(serviceEndpoint));
        }
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {}

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return AddressBookServiceGrpc.getCreateRegisteredNodeMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setRegisteredNodeCreate(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setRegisteredNodeCreate(build());
    }

    /**
     * Freeze this transaction with the given client.
     *
     * @param client the client to freeze with
     * @return this transaction
     * @throws IllegalStateException if adminKey is not set
     */
    @Override
    public RegisteredNodeCreateTransaction freezeWith(@Nullable Client client) {
        if (adminKey == null) {
            throw new IllegalStateException(
                    "RegisteredNodeCreateTransaction: 'adminKey' must be explicitly set before calling freeze().");
        }
        return super.freezeWith(client);
    }
}
