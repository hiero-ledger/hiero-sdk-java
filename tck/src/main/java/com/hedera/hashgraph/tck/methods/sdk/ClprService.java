// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.ClprCloseChannelTransaction;
import com.hedera.hashgraph.sdk.ClprCompleteChannelTransaction;
import com.hedera.hashgraph.sdk.ClprCompleteConnectorTransaction;
import com.hedera.hashgraph.sdk.ClprDeregisterConnectorTransaction;
import com.hedera.hashgraph.sdk.ClprGetEndpointManifestQuery;
import com.hedera.hashgraph.sdk.ClprGetLedgerConfigurationQuery;
import com.hedera.hashgraph.sdk.ClprRegisterChannelTransaction;
import com.hedera.hashgraph.sdk.ClprRegisterConnectorTransaction;
import com.hedera.hashgraph.sdk.ClprSignatureScheme;
import com.hedera.hashgraph.sdk.ClprSubmitBundleTransaction;
import com.hedera.hashgraph.sdk.ClprThrottles;
import com.hedera.hashgraph.sdk.ClprUpdateLedgerConfigurationTransaction;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Query;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.exception.InvalidJSONRPC2RequestException;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprCloseChannelParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprCompleteChannelParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprCompleteConnectorParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprDeregisterConnectorParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprRegisterChannelParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprRegisterConnectorParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprSubmitBundleParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprThrottlesParams;
import com.hedera.hashgraph.tck.methods.sdk.param.clpr.ClprUpdateLedgerConfigurationParams;
import com.hedera.hashgraph.tck.methods.sdk.response.clpr.ClprCompletionResponse;
import com.hedera.hashgraph.tck.methods.sdk.response.clpr.ClprEndpointManifestResponse;
import com.hedera.hashgraph.tck.methods.sdk.response.clpr.ClprLedgerConfigurationResponse;
import com.hedera.hashgraph.tck.methods.sdk.response.clpr.ClprTransactionResponse;
import com.hedera.hashgraph.tck.util.KeyUtils;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;
import org.bouncycastle.util.encoders.Hex;

/**
 * HIP-1535 CLPR methods. Method names are provisional until the TCK specification settles them.
 * Hex byte strings have no {@code 0x} prefix, and uint64 values are unsigned decimal strings.
 */
@JSONRPC2Service
public class ClprService extends AbstractJSONRPC2Service {
    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);
    private static final Pattern HEX = Pattern.compile("^([0-9a-fA-F]{2})*$");
    private static final Pattern UINT = Pattern.compile("^\\d+$");

    private final SdkService sdkService;

    public ClprService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("registerClprChannel")
    public ClprTransactionResponse registerClprChannel(final ClprRegisterChannelParams params) throws Exception {
        var tx = new ClprRegisterChannelTransaction();
        if (params.ownershipCommitment().isPresent()) {
            tx.setOwnershipCommitment(decode(params.ownershipCommitment().get()));
        }
        return new ClprTransactionResponse(execute(tx, params.commonTransactionParams(), params.sessionId()));
    }

    @JSONRPC2Method("completeClprChannel")
    public ClprCompletionResponse completeClprChannel(final ClprCompleteChannelParams params) throws Exception {
        var tx = new ClprCompleteChannelTransaction();
        if (params.channelId().isPresent()) {
            tx.setChannelId(decode(params.channelId().get()));
        }
        if (params.verifierContract().isPresent()) {
            tx.setVerifierContract(
                    ContractId.fromString(params.verifierContract().get()));
        }
        if (params.configProofBytes().isPresent()) {
            tx.setConfigProofBytes(decode(params.configProofBytes().get()));
        }
        if (params.endpointManifestProofBytes().isPresent()) {
            tx.setEndpointManifestProofBytes(
                    decode(params.endpointManifestProofBytes().get()));
        }
        tx.setOwnershipProof(ownershipPrivateKey(params.ownershipPrivateKey()));
        var status = execute(tx, params.commonTransactionParams(), params.sessionId());
        return new ClprCompletionResponse(
                status, encode(tx.getPublicKey()), encode(tx.getSignature()), scheme(tx.getSignatureScheme()));
    }

    @JSONRPC2Method("closeClprChannel")
    public ClprTransactionResponse closeClprChannel(final ClprCloseChannelParams params) throws Exception {
        var tx = new ClprCloseChannelTransaction();
        if (params.channelId().isPresent()) {
            tx.setChannelId(decode(params.channelId().get()));
        }
        if (params.ownershipCommitment().isPresent()) {
            tx.setOwnershipCommitment(decode(params.ownershipCommitment().get()));
        }
        return new ClprTransactionResponse(execute(tx, params.commonTransactionParams(), params.sessionId()));
    }

    @JSONRPC2Method("submitClprBundle")
    public ClprTransactionResponse submitClprBundle(final ClprSubmitBundleParams params) throws Exception {
        var tx = new ClprSubmitBundleTransaction();
        if (params.channelId().isPresent()) {
            tx.setChannelId(decode(params.channelId().get()));
        }
        if (params.bundlePayload().isPresent()) {
            tx.setBundlePayload(decode(params.bundlePayload().get()));
        }
        return new ClprTransactionResponse(execute(tx, params.commonTransactionParams(), params.sessionId()));
    }

    @JSONRPC2Method("registerClprConnector")
    public ClprTransactionResponse registerClprConnector(final ClprRegisterConnectorParams params) throws Exception {
        var tx = new ClprRegisterConnectorTransaction();
        if (params.channelId().isPresent()) {
            tx.setChannelId(decode(params.channelId().get()));
        }
        if (params.commitment().isPresent()) {
            tx.setCommitment(decode(params.commitment().get()));
        }
        return new ClprTransactionResponse(execute(tx, params.commonTransactionParams(), params.sessionId()));
    }

    @JSONRPC2Method("completeClprConnector")
    public ClprCompletionResponse completeClprConnector(final ClprCompleteConnectorParams params) throws Exception {
        var tx = new ClprCompleteConnectorTransaction();
        if (params.channelId().isPresent()) {
            tx.setChannelId(decode(params.channelId().get()));
        }
        if (params.connectorId().isPresent()) {
            tx.setConnectorId(decode(params.connectorId().get()));
        }
        if (params.salt().isPresent()) {
            tx.setSalt(decode(params.salt().get()));
        }
        if (params.connectorContract().isPresent()) {
            tx.setConnectorContract(
                    ContractId.fromString(params.connectorContract().get()));
        }
        if (params.adminKey().isPresent()) {
            var key = parseKey(params.adminKey().get());
            if (key == null) {
                throw new InvalidJSONRPC2RequestException("adminKey must be an SDK key string");
            }
            tx.setAdminKey(key instanceof PrivateKey privateKey ? privateKey.getPublicKey() : key);
        }
        if (params.lockedStake().isPresent()) {
            var stake = uint64(params.lockedStake().get(), "lockedStake");
            if (stake < 0) {
                throw new InvalidJSONRPC2RequestException("lockedStake must be a valid tinybar amount");
            }
            tx.setLockedStake(Hbar.fromTinybars(stake));
        }
        tx.setOwnershipProof(ownershipPrivateKey(params.ownershipPrivateKey()));
        var status = execute(tx, params.commonTransactionParams(), params.sessionId());
        return new ClprCompletionResponse(
                status, encode(tx.getPublicKey()), encode(tx.getSignature()), scheme(tx.getSignatureScheme()));
    }

    @JSONRPC2Method("deregisterClprConnector")
    public ClprTransactionResponse deregisterClprConnector(final ClprDeregisterConnectorParams params)
            throws Exception {
        var tx = new ClprDeregisterConnectorTransaction();
        if (params.channelId().isPresent()) {
            tx.setChannelId(decode(params.channelId().get()));
        }
        if (params.connectorId().isPresent()) {
            tx.setConnectorId(decode(params.connectorId().get()));
        }
        if (params.stakeRecipient().isPresent()) {
            tx.setStakeRecipient(AccountId.fromString(params.stakeRecipient().get()));
        }
        return new ClprTransactionResponse(execute(tx, params.commonTransactionParams(), params.sessionId()));
    }

    @JSONRPC2Method("updateClprLedgerConfiguration")
    public ClprTransactionResponse updateClprLedgerConfiguration(final ClprUpdateLedgerConfigurationParams params)
            throws Exception {
        var tx = new ClprUpdateLedgerConfigurationTransaction();
        if (params.throttles().isPresent()) {
            var limits = params.throttles().get();
            tx.setThrottles(ClprThrottles.of(
                    limits.maxMessagesPerBundle(),
                    limits.maxMessagePayloadBytes(),
                    uint64(limits.maxGasPerMessage(), "maxGasPerMessage"),
                    limits.maxQueueDepth(),
                    uint64(limits.maxSyncBytes(), "maxSyncBytes"),
                    limits.maxLocalEndpoints(),
                    limits.maxPeerEndpoints()));
        }
        return new ClprTransactionResponse(execute(tx, params.commonTransactionParams(), params.sessionId()));
    }

    @JSONRPC2Method("getClprLedgerConfiguration")
    public ClprLedgerConfigurationResponse getClprLedgerConfiguration(final ClprQueryParams params) throws Exception {
        var query = new ClprGetLedgerConfigurationQuery().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        applyQueryPayment(query, params);
        var info = query.execute(sdkService.getClient(params.sessionId()));
        var limits = info.getThrottles();
        var timestamp = info.getTimestamp();
        return new ClprLedgerConfigurationResponse(
                Integer.toUnsignedLong(info.getProtocolVersion()),
                info.getChainId(),
                encode(info.getServiceAddress()),
                timestamp.getEpochSecond() + "." + String.format("%09d", timestamp.getNano()),
                new ClprThrottlesParams(
                        limits.getMaxMessagesPerBundle(),
                        limits.getMaxMessagePayloadBytes(),
                        Long.toUnsignedString(limits.getMaxGasPerMessage()),
                        limits.getMaxQueueDepth(),
                        Long.toUnsignedString(limits.getMaxSyncBytes()),
                        limits.getMaxLocalEndpoints(),
                        limits.getMaxPeerEndpoints()),
                encode(info.getInitialTrustAnchor()),
                encode(info.getInitialTrustAnchorId()),
                encode(info.getConfigurationStateProof()));
    }

    @JSONRPC2Method("getClprEndpointManifest")
    public ClprEndpointManifestResponse getClprEndpointManifest(final ClprQueryParams params) throws Exception {
        var query = new ClprGetEndpointManifestQuery().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        applyQueryPayment(query, params);
        var info = query.execute(sdkService.getClient(params.sessionId()));
        return new ClprEndpointManifestResponse(
                Long.toUnsignedString(info.getVersion()),
                encode(info.getServiceAddress()),
                info.getEndpoints().stream()
                        .map(endpoint -> new ClprEndpointManifestResponse.Endpoint(
                                new ClprEndpointManifestResponse.ServiceEndpoint(
                                        endpoint.getServiceEndpoint().getIpAddress(),
                                        endpoint.getServiceEndpoint().getPort()),
                                encode(endpoint.getTlsCertificate()),
                                encode(endpoint.getAccountId())))
                        .toList(),
                encode(info.getManifestStateProof()));
    }

    private com.hedera.hashgraph.sdk.Status execute(
            Transaction<?> transaction, Optional<CommonTransactionParams> common, String sessionId) throws Exception {
        var client = sdkService.getClient(sessionId);
        transaction.setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        common.ifPresent(params -> params.fillOutTransaction(transaction, client));
        return transaction.execute(client).getReceipt(client).status;
    }

    private static void applyQueryPayment(Query<?, ?> query, ClprQueryParams params) throws Exception {
        if (params.queryPayment().isPresent()) {
            query.setQueryPayment(Hbar.fromTinybars(uint64(params.queryPayment().get(), "queryPayment")));
        }
        if (params.maxQueryPayment().isPresent()) {
            query.setMaxQueryPayment(
                    Hbar.fromTinybars(uint64(params.maxQueryPayment().get(), "maxQueryPayment")));
        }
    }

    static byte[] decode(String hex) throws InvalidJSONRPC2RequestException {
        if (!HEX.matcher(hex).matches()) {
            throw new InvalidJSONRPC2RequestException("expected hex bytes without 0x: " + hex);
        }
        return Hex.decode(hex);
    }

    static String encode(byte[] bytes) {
        return bytes == null ? null : Hex.toHexString(bytes);
    }

    static String scheme(ClprSignatureScheme scheme) {
        return scheme == null ? null : scheme.name();
    }

    /**
     * Parses an unsigned uint64 decimal string. Values above 2^63-1 come back as negative longs,
     * which is the SDK's bit-preserving uint64 representation.
     */
    static long uint64(String value, String name) throws InvalidJSONRPC2RequestException {
        if (value == null || !UINT.matcher(value).matches()) {
            throw new InvalidJSONRPC2RequestException(name + " must be an unsigned decimal string");
        }
        try {
            return Long.parseUnsignedLong(value);
        } catch (NumberFormatException e) {
            throw new InvalidJSONRPC2RequestException(name + " must fit in 64 unsigned bits");
        }
    }

    private static Key parseKey(String value) {
        try {
            return KeyUtils.getKeyFromString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static PrivateKey ownershipPrivateKey(Optional<String> value) throws InvalidJSONRPC2RequestException {
        if (value.isPresent() && parseKey(value.get()) instanceof PrivateKey key) {
            return key;
        }
        throw new InvalidJSONRPC2RequestException("ownershipPrivateKey must be a private key string");
    }
}
