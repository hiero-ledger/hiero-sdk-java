// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ClprCompleteChannelTransactionBody;
import com.hedera.hashgraph.sdk.proto.ClprServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Completes a CLPR Channel registration by revealing its ownership proof.
 * <p>
 * Any payer can submit this reveal. The ownership private key is used only during
 * {@link #setOwnershipProof(PrivateKey)} and is never retained. Changing the Channel ID
 * clears the proof; setting the same bytes keeps it.
 * <p>
 * The verifier contract and its configuration and endpoint manifest proofs are opaque
 * to the SDK. They are optional locally because verifier families differ. Only the
 * Channel ID and a complete ownership proof are required at freeze.
 * CLPR is off by default and requires the network setting {@code clpr.enabled}.
 * This transaction cannot be scheduled.
 */
public class ClprCompleteChannelTransaction extends ClprTransaction<ClprCompleteChannelTransaction> {
    private byte @Nullable [] channelId;
    private @Nullable ContractId verifierContract;
    private byte[] configProofBytes = new byte[0];
    private byte[] endpointManifestProofBytes = new byte[0];
    private final OwnershipProofState ownershipProof = new OwnershipProofState();

    /**
     * Constructor.
     */
    public ClprCompleteChannelTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    ClprCompleteChannelTransaction(
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
    ClprCompleteChannelTransaction(TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Returns a copy of the Channel ID.
     *
     * @return the Channel ID, or null when it has not been set
     */
    public byte @Nullable [] getChannelId() {
        return channelId == null ? null : channelId.clone();
    }

    /**
     * Sets and copies the 32-byte Channel ID. A different ID clears the ownership proof;
     * setting identical bytes keeps it.
     *
     * @param channelId the 32-byte Channel ID
     * @return {@code this}
     */
    public ClprCompleteChannelTransaction setChannelId(byte[] channelId) {
        requireNotFrozen();
        var bytes = ClprOwnership.requireBytes32(channelId, "channelId");
        ownershipProof.clearIfChanged(this.channelId, bytes);
        this.channelId = bytes;
        return this;
    }

    /**
     * Signs the Channel ID with the ownership key and stores the resulting proof.
     * The private key is never stored. Set the Channel ID before calling this method.
     *
     * @param privateKey the ECDSA secp256k1 or Ed25519 ownership private key
     * @return {@code this}
     */
    public ClprCompleteChannelTransaction setOwnershipProof(PrivateKey privateKey) {
        requireNotFrozen();
        var fields = new LinkedHashMap<String, @Nullable Object>();
        fields.put("channelId", channelId);
        requireFields(fields, "calling setOwnershipProof()");
        ownershipProof.sign(privateKey, Objects.requireNonNull(channelId));
        return this;
    }

    /**
     * Returns a copy of the canonical ownership public key.
     *
     * @return the public key, or null without a proof
     */
    public byte @Nullable [] getPublicKey() {
        return ownershipProof.getPublicKey();
    }

    /**
     * Returns a copy of the ownership signature.
     *
     * @return the signature, or null without a proof
     */
    public byte @Nullable [] getSignature() {
        return ownershipProof.getSignature();
    }

    /**
     * Returns the proof's signature scheme.
     *
     * @return the signature scheme, or null without a proof
     */
    public @Nullable ClprSignatureScheme getSignatureScheme() {
        return ownershipProof.getSignatureScheme();
    }

    /**
     * Returns the verifier contract.
     *
     * @return the verifier contract, or null when it has not been set
     */
    public @Nullable ContractId getVerifierContract() {
        return verifierContract;
    }

    /**
     * Sets the verifier contract. The SDK does not interpret its proof format.
     *
     * @param verifierContract the verifier contract
     * @return {@code this}
     */
    public ClprCompleteChannelTransaction setVerifierContract(ContractId verifierContract) {
        requireNotFrozen();
        this.verifierContract = Objects.requireNonNull(verifierContract, "verifierContract must not be null");
        return this;
    }

    /**
     * Returns a copy of the opaque configuration proof bytes.
     *
     * @return the configuration proof bytes
     */
    public byte[] getConfigProofBytes() {
        return configProofBytes.clone();
    }

    /**
     * Sets and copies the opaque configuration proof bytes.
     *
     * @param configProofBytes the configuration proof bytes
     * @return {@code this}
     */
    public ClprCompleteChannelTransaction setConfigProofBytes(byte[] configProofBytes) {
        requireNotFrozen();
        this.configProofBytes = Objects.requireNonNull(configProofBytes, "configProofBytes must not be null")
                .clone();
        return this;
    }

    /**
     * Returns a copy of the opaque endpoint manifest proof bytes.
     *
     * @return the endpoint manifest proof bytes
     */
    public byte[] getEndpointManifestProofBytes() {
        return endpointManifestProofBytes.clone();
    }

    /**
     * Sets and copies the opaque endpoint manifest proof bytes.
     *
     * @param endpointManifestProofBytes the endpoint manifest proof bytes
     * @return {@code this}
     */
    public ClprCompleteChannelTransaction setEndpointManifestProofBytes(byte[] endpointManifestProofBytes) {
        requireNotFrozen();
        this.endpointManifestProofBytes = Objects.requireNonNull(
                        endpointManifestProofBytes, "endpointManifestProofBytes must not be null")
                .clone();
        return this;
    }

    ClprCompleteChannelTransactionBody.Builder build() {
        var builder = ClprCompleteChannelTransactionBody.newBuilder()
                .setConfigProofBytes(ByteString.copyFrom(configProofBytes))
                .setEndpointManifestProofBytes(ByteString.copyFrom(endpointManifestProofBytes));
        if (channelId != null) {
            builder.setChannelId(ByteString.copyFrom(channelId));
        }
        if (verifierContract != null) {
            builder.setVerifierContract(verifierContract.toProtobuf());
        }
        var publicKey = ownershipProof.getPublicKey();
        var signature = ownershipProof.getSignature();
        var scheme = ownershipProof.getSignatureScheme();
        if (publicKey != null && signature != null && scheme != null) {
            builder.setPublicKey(ByteString.copyFrom(publicKey))
                    .setSignature(ByteString.copyFrom(signature))
                    .setSignatureScheme(scheme.toProtobuf());
        }
        return builder;
    }

    void initFromTransactionBody() {
        var body = sourceTransactionBody.getClprCompleteChannel();
        if (!body.getChannelId().isEmpty()) {
            channelId = body.getChannelId().toByteArray();
        }
        if (body.hasVerifierContract()) {
            verifierContract = ContractId.fromProtobuf(body.getVerifierContract());
        }
        configProofBytes = body.getConfigProofBytes().toByteArray();
        endpointManifestProofBytes = body.getEndpointManifestProofBytes().toByteArray();
        ownershipProof.restore(body.getPublicKey(), body.getSignature(), body.getSignatureScheme());
    }

    @Override
    Map<String, @Nullable Object> getRequiredFields() {
        var fields = new LinkedHashMap<String, @Nullable Object>();
        fields.put("channelId", channelId);
        fields.put("ownershipProof", ownershipProof);
        return fields;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (verifierContract != null) {
            verifierContract.validateChecksum(client);
        }
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setClprCompleteChannel(build());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ClprServiceGrpc.getCompleteChannelMethod();
    }
}
