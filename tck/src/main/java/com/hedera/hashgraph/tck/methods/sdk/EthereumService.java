// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.EthereumTransaction;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.ethereum.EthereumTransactionParams;
import com.hedera.hashgraph.tck.methods.sdk.response.EthereumTransactionResponse;
import com.hedera.hashgraph.tck.util.TransactionBuilders;

@JSONRPC2Service
public class EthereumService extends AbstractJSONRPC2Service {
    private final SdkService sdkService;

    public EthereumService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("createEthereumTransaction")
    public EthereumTransactionResponse createEthereumTransaction(final EthereumTransactionParams params)
            throws Exception {
        EthereumTransaction transaction = TransactionBuilders.EthereumBuilder.buildCreate(params);
        Client client = sdkService.getClient(params.getSessionId());

        if (params.getCommonTransactionParams() != null) {
            params.getCommonTransactionParams().fillOutTransaction(transaction, client);
        }

        TransactionReceipt receipt = transaction.execute(client).getReceipt(client);
        String contractId = "";

        if (receipt.status == Status.SUCCESS) {
            contractId = receipt.contractId.toString();
        }

        return new EthereumTransactionResponse(receipt.status.toString(), contractId);
    }
}
