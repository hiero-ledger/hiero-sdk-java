// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ClprCompleteConnectorTransactionBody;
import com.hedera.hashgraph.sdk.proto.ClprServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Completes a CLPR Connector registration by revealing its ownership proof.
 * <p>
 * First register the commitment with {@link ClprRegisterConnectorTransaction}. The reveal
 * checks that the Connector ID matches the Channel ID, ownership public key and salt
 * before signing {@code connectorId || CLPR service address}. The ownership private key
 * is used only during {@link #setOwnershipProof(PrivateKey)} and is never retained.
 * Changing the Channel ID, Connector ID or salt clears the proof; identical bytes keep it.
 * <p>
 * The required admin key keeps deregistration possible and may be any SDK {@link Key},
 * including a threshold key or key list. The payer funds the locked stake, which defaults
 * to zero and must not be negative. The network enforces its configured minimum stake and
 * contract requirements.
 * CLPR is off by default and requires the network setting {@code clpr.enabled}.
 * This transaction cannot be scheduled.
 */
public class ClprCompleteConnectorTransaction extends ClprTransaction<ClprCompleteConnectorTransaction> {
    private byte @Nullable [] channelId;
    private byte @Nullable [] connectorId;
    private byte @Nullable [] salt;
    private @Nullable ContractId connectorContract;
    private @Nullable Key adminKey;
    private Hbar lockedStake = Hbar.ZERO;
    private final OwnershipProofState ownershipProof = new OwnershipProofState();

    /**
     * Constructor.
     */
    public ClprCompleteConnectorTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    ClprCompleteConnectorTransaction(
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
    ClprCompleteConnectorTransaction(TransactionBody txBody) {
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
     * Sets and copies the 32-byte Channel ID. Only different bytes clear the ownership proof.
     *
     * @param channelId the 32-byte Channel ID
     * @return {@code this}
     */
    public ClprCompleteConnectorTransaction setChannelId(byte[] channelId) {
        requireNotFrozen();
        var bytes = ClprOwnership.requireBytes32(channelId, "channelId");
        ownershipProof.clearIfChanged(this.channelId, bytes);
        this.channelId = bytes;
        return this;
    }

    /**
     * Returns a copy of the Connector ID.
     *
     * @return the Connector ID, or null when it has not been set
     */
    public byte @Nullable [] getConnectorId() {
        return connectorId == null ? null : connectorId.clone();
    }

    /**
     * Sets and copies the 32-byte Connector ID. Only different bytes clear the ownership proof.
     *
     * @param connectorId the 32-byte Connector ID
     * @return {@code this}
     */
    public ClprCompleteConnectorTransaction setConnectorId(byte[] connectorId) {
        requireNotFrozen();
        var bytes = ClprOwnership.requireBytes32(connectorId, "connectorId");
        ownershipProof.clearIfChanged(this.connectorId, bytes);
        this.connectorId = bytes;
        return this;
    }

    /**
     * Returns a copy of the salt.
     *
     * @return the salt, or null when it has not been set
     */
    public byte @Nullable [] getSalt() {
        return salt == null ? null : salt.clone();
    }

    /**
     * Sets and copies the 32-byte salt. Only different bytes clear the ownership proof.
     *
     * @param salt the 32-byte salt
     * @return {@code this}
     */
    public ClprCompleteConnectorTransaction setSalt(byte[] salt) {
        requireNotFrozen();
        var bytes = ClprOwnership.requireBytes32(salt, "salt");
        ownershipProof.clearIfChanged(this.salt, bytes);
        this.salt = bytes;
        return this;
    }

    /**
     * Checks the Connector ID, then signs {@code connectorId || CLPR service address} with the
     * ownership key and stores the resulting proof. Set the Channel ID, Connector ID and salt first.
     * The private key is never stored.
     *
     * @param privateKey the ECDSA secp256k1 or Ed25519 ownership private key
     * @return {@code this}
     * @throws IllegalArgumentException if the Connector ID does not match the Channel ID, key and salt
     */
    public ClprCompleteConnectorTransaction setOwnershipProof(PrivateKey privateKey) {
        requireNotFrozen();
        var fields = new LinkedHashMap<String, @Nullable Object>();
        fields.put("channelId", channelId);
        fields.put("connectorId", connectorId);
        fields.put("salt", salt);
        requireFields(fields, "calling setOwnershipProof()");
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        var id = Objects.requireNonNull(connectorId);
        var expectedId = ClprOwnership.computeConnectorId(
                Objects.requireNonNull(channelId), privateKey.getPublicKey(), Objects.requireNonNull(salt));
        if (!Arrays.equals(id, expectedId)) {
            throw new IllegalArgumentException(
                    "ClprCompleteConnectorTransaction: 'connectorId' does not match channelId, ownership public key and salt.");
        }
        var preimage = Arrays.copyOf(id, id.length + ClprOwnership.CLPR_SERVICE_ADDRESS.length);
        System.arraycopy(
                ClprOwnership.CLPR_SERVICE_ADDRESS, 0, preimage, id.length, ClprOwnership.CLPR_SERVICE_ADDRESS.length);
        ownershipProof.sign(privateKey, preimage);
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
     * Returns the Connector authorization contract.
     *
     * @return the Connector contract, or null when it has not been set
     */
    public @Nullable ContractId getConnectorContract() {
        return connectorContract;
    }

    /**
     * Sets the local Connector authorization contract.
     *
     * @param connectorContract the Connector contract
     * @return {@code this}
     */
    public ClprCompleteConnectorTransaction setConnectorContract(ContractId connectorContract) {
        requireNotFrozen();
        this.connectorContract = Objects.requireNonNull(connectorContract, "connectorContract must not be null");
        return this;
    }

    /**
     * Returns the administrative key.
     *
     * @return the admin key, or null when it has not been set
     */
    public @Nullable Key getAdminKey() {
        return adminKey;
    }

    /**
     * Sets the administrative key required for future deregistration.
     * Supports every SDK key, including threshold keys and key lists.
     *
     * @param adminKey the admin key
     * @return {@code this}
     */
    public ClprCompleteConnectorTransaction setAdminKey(Key adminKey) {
        requireNotFrozen();
        this.adminKey = Objects.requireNonNull(adminKey, "adminKey must not be null");
        return this;
    }

    /**
     * Returns the locked stake, which defaults to zero.
     *
     * @return the locked stake
     */
    public Hbar getLockedStake() {
        return lockedStake;
    }

    /**
     * Sets the payer-funded stake.
     *
     * @param lockedStake the locked stake, in 0..2^63-1 tinybars
     * @return {@code this}
     * @throws IllegalArgumentException if the stake is negative
     */
    public ClprCompleteConnectorTransaction setLockedStake(Hbar lockedStake) {
        requireNotFrozen();
        Objects.requireNonNull(lockedStake, "lockedStake must not be null");
        if (lockedStake.toTinybars() < 0) {
            throw new IllegalArgumentException(
                    "ClprCompleteConnectorTransaction: 'lockedStake' must be an integer in 0..2^63-1 tinybars.");
        }
        this.lockedStake = lockedStake;
        return this;
    }

    ClprCompleteConnectorTransactionBody.Builder build() {
        var builder = ClprCompleteConnectorTransactionBody.newBuilder().setLockedStake(lockedStake.toTinybars());
        if (channelId != null) {
            builder.setChannelId(ByteString.copyFrom(channelId));
        }
        if (connectorId != null) {
            builder.setConnectorId(ByteString.copyFrom(connectorId));
        }
        if (salt != null) {
            builder.setSalt(ByteString.copyFrom(salt));
        }
        if (connectorContract != null) {
            builder.setConnectorContract(connectorContract.toProtobuf());
        }
        if (adminKey != null) {
            builder.setAdminKey(adminKey.toProtobufKey());
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
        var body = sourceTransactionBody.getClprCompleteConnector();
        if (!body.getChannelId().isEmpty()) {
            channelId = body.getChannelId().toByteArray();
        }
        if (!body.getConnectorId().isEmpty()) {
            connectorId = body.getConnectorId().toByteArray();
        }
        if (!body.getSalt().isEmpty()) {
            salt = body.getSalt().toByteArray();
        }
        if (body.hasConnectorContract()) {
            connectorContract = ContractId.fromProtobuf(body.getConnectorContract());
        }
        if (body.hasAdminKey()) {
            adminKey = Key.fromProtobufKey(body.getAdminKey());
        }
        // Wire values keep their raw uint64 bits, independently of builder limits.
        lockedStake = Hbar.fromTinybars(body.getLockedStake());
        ownershipProof.restore(body.getPublicKey(), body.getSignature(), body.getSignatureScheme());
    }

    @Override
    Map<String, @Nullable Object> getRequiredFields() {
        var fields = new LinkedHashMap<String, @Nullable Object>();
        fields.put("channelId", channelId);
        fields.put("connectorId", connectorId);
        fields.put("salt", salt);
        fields.put("adminKey", adminKey);
        fields.put("ownershipProof", ownershipProof);
        return fields;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (connectorContract != null) {
            connectorContract.validateChecksum(client);
        }
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setClprCompleteConnector(build());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ClprServiceGrpc.getCompleteConnectorMethod();
    }
}
