// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.node.NodeCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.ServiceEndpointParams;
import com.hedera.hashgraph.tck.methods.sdk.response.NodeResponse;
import com.hedera.hashgraph.tck.util.KeyUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;

@JSONRPC2Service
public class NodeService extends AbstractJSONRPC2Service {
    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(10L);
    private final SdkService sdkService;

    public NodeService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("createNode")
    public NodeResponse createNode(final NodeCreateParams params) throws Exception {
        NodeCreateTransaction tx = new NodeCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        params.getAccountId().ifPresent(a -> tx.setAccountId(AccountId.fromString(a)));
        params.getDescription().ifPresent(tx::setDescription);

        params.getGossipEndpoints().ifPresent(endpoints -> setEndpoints(endpoints, tx::setGossipEndpoints));

        params.getServiceEndpoints().ifPresent(endpoints -> setEndpoints(endpoints, tx::setServiceEndpoints));

        params.getGossipCaCertificate().ifPresent(hex -> tx.setGossipCaCertificate(Hex.decode(hex)));

        params.getGrpcCertificateHash().ifPresent(hex -> tx.setGrpcCertificateHash(Hex.decode(hex)));

        params.getGrpcWebProxyEndpoint().ifPresent(ep -> tx.setGrpcWebProxyEndpoint(ep.toSdkEndpoint()));

        params.getAdminKey().ifPresent(keyStr -> {
            try {
                tx.setAdminKey(KeyUtils.getKeyFromString(keyStr));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException(e);
            }
        });

        params.getDeclineReward().ifPresent(tx::setDeclineReward);

        params.getCommonTransactionParams().ifPresent(common -> common.fillOutTransaction(tx, sdkService.getClient()));

        TransactionReceipt receipt = tx.execute(sdkService.getClient()).getReceipt(sdkService.getClient());

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
