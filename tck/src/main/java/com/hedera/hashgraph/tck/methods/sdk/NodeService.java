// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AddressBookQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Endpoint;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.NodeAddressBook;
import com.hedera.hashgraph.sdk.NodeCreateTransaction;
import com.hedera.hashgraph.sdk.NodeDeleteTransaction;
import com.hedera.hashgraph.sdk.NodeUpdateTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.node.AddressBookQueryParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.NodeCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.NodeDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.NodeUpdateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.ServiceEndpointParams;
import com.hedera.hashgraph.tck.methods.sdk.response.AddressBookResponse;
import com.hedera.hashgraph.tck.methods.sdk.response.NodeResponse;
import com.hedera.hashgraph.tck.util.KeyUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bouncycastle.util.encoders.Hex;

@JSONRPC2Service
public class NodeService extends AbstractJSONRPC2Service {
    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);
    private final SdkService sdkService;

    public NodeService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("getAddressBook")
    public AddressBookResponse addressBookQuery(final AddressBookQueryParams params) {
        AddressBookQuery query = new AddressBookQuery().setFileId(FileId.fromString(params.fileId()));
        Client client = sdkService.getClient(params.sessionId());

        NodeAddressBook addressBook = query.execute(client);
        AddressBookResponse response = new AddressBookResponse();

        addressBook.getNodeAddresses().forEach(address -> {
            List<AddressBookResponse.Endpoint> mappedEndpoints = address.getAddresses().stream()
                    .map(sdkEndpoint -> new AddressBookResponse.Endpoint(
                            sdkEndpoint.getAddress() != null ? Hex.toHexString(sdkEndpoint.getAddress()) : null,
                            sdkEndpoint.getPort(),
                            sdkEndpoint.getDomainName() != null ? sdkEndpoint.getDomainName() : ""))
                    .collect(Collectors.toList());

            response.addNodeAddress(new AddressBookResponse.NodeAddress(
                    address.getPublicKey() != null ? address.getPublicKey() : "",
                    address.getAccountId() != null ? address.getAccountId().toString() : "",
                    address.getNodeId(),
                    address.getCertHash() != null ? address.getCertHash().toString() : "",
                    mappedEndpoints,
                    address.getDescription(),
                    address.getStake()));
        });

        return response;
    }

    @JSONRPC2Method("createNode")
    public NodeResponse createNode(final NodeCreateParams params) throws Exception {
        NodeCreateTransaction tx = new NodeCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.sessionId());

        params.accountId().ifPresent(a -> tx.setAccountId(AccountId.fromString(a)));
        params.description().ifPresent(tx::setDescription);

        params.gossipEndpoints().ifPresent(endpoints -> setEndpoints(endpoints, tx::setGossipEndpoints));

        params.serviceEndpoints().ifPresent(endpoints -> setEndpoints(endpoints, tx::setServiceEndpoints));

        params.gossipCaCertificate().ifPresent(hex -> tx.setGossipCaCertificate(Hex.decode(hex)));

        params.grpcCertificateHash().ifPresent(hex -> tx.setGrpcCertificateHash(Hex.decode(hex)));

        params.grpcWebProxyEndpoint().ifPresent(ep -> tx.setGrpcWebProxyEndpoint(ep.toSdkEndpoint()));

        params.adminKey().ifPresent(keyStr -> {
            try {
                tx.setAdminKey(KeyUtils.getKeyFromString(keyStr));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException(e);
            }
        });

        params.declineReward().ifPresent(tx::setDeclineReward);

        params.commonTransactionParams().ifPresent(common -> common.fillOutTransaction(tx, client));

        TransactionReceipt receipt = tx.execute(client).getReceipt(client);

        String nodeId = receipt.nodeId > 0 ? Long.toString(receipt.nodeId) : "";
        return new NodeResponse(nodeId, receipt.status);
    }

    @JSONRPC2Method("updateNode")
    public NodeResponse updateNode(final NodeUpdateParams params) throws Exception {
        NodeUpdateTransaction tx = new NodeUpdateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.sessionId());

        try {
            params.nodeId().ifPresent(idStr -> tx.setNodeId(Long.parseLong(idStr)));
        } catch (NumberFormatException e) {
            // Set an invalid node ID to allow the network to return the proper error
            tx.setNodeId(Long.MAX_VALUE);
        }

        params.accountId().ifPresent(a -> tx.setAccountId(AccountId.fromString(a)));
        params.description().ifPresent(tx::setDescription);

        params.gossipEndpoints().ifPresent(endpoints -> setEndpoints(endpoints, tx::setGossipEndpoints));

        params.serviceEndpoints().ifPresent(endpoints -> setEndpoints(endpoints, tx::setServiceEndpoints));

        params.gossipCaCertificate().ifPresent(hex -> tx.setGossipCaCertificate(Hex.decode(hex)));

        params.grpcCertificateHash().ifPresent(hex -> tx.setGrpcCertificateHash(Hex.decode(hex)));

        params.grpcWebProxyEndpoint().ifPresent(ep -> tx.setGrpcWebProxyEndpoint(ep.toSdkEndpoint()));

        params.adminKey().ifPresent(keyStr -> {
            try {
                tx.setAdminKey(KeyUtils.getKeyFromString(keyStr));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException(e);
            }
        });

        params.declineReward().ifPresent(tx::setDeclineReward);

        params.commonTransactionParams().ifPresent(common -> common.fillOutTransaction(tx, client));

        TransactionReceipt receipt = tx.execute(client).getReceipt(client);

        String nodeId = receipt.nodeId > 0 ? Long.toString(receipt.nodeId) : "";
        return new NodeResponse(nodeId, receipt.status);
    }

    @JSONRPC2Method("deleteNode")
    public NodeResponse deleteNode(final NodeDeleteParams params) throws Exception {
        NodeDeleteTransaction tx = new NodeDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        Client client = sdkService.getClient(params.sessionId());

        try {
            params.nodeId().ifPresent(idStr -> tx.setNodeId(Long.parseLong(idStr)));
        } catch (NumberFormatException e) {
            // Set an invalid node ID to allow the network to return the proper error
            tx.setNodeId(Long.MAX_VALUE);
        }

        params.commonTransactionParams().ifPresent(common -> common.fillOutTransaction(tx, client));

        TransactionReceipt receipt = tx.execute(client).getReceipt(client);

        String nodeId = receipt.nodeId > 0 ? Long.toString(receipt.nodeId) : "";
        return new NodeResponse(nodeId, receipt.status);
    }

    private void setEndpoints(List<ServiceEndpointParams> input, java.util.function.Consumer<List<Endpoint>> setter) {
        List<Endpoint> eps = new ArrayList<>();
        for (ServiceEndpointParams p : input) {
            eps.add(p.toSdkEndpoint());
        }
        setter.accept(eps);
    }
}
