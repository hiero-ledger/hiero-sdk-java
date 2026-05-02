// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk;

import org.hiero.sdk.Client;
import org.hiero.sdk.EthereumTransaction;
import org.hiero.sdk.Status;
import org.hiero.sdk.TransactionReceipt;
import org.hiero.tck.annotation.JSONRPC2Method;
import org.hiero.tck.annotation.JSONRPC2Service;
import org.hiero.tck.methods.AbstractJSONRPC2Service;
import org.hiero.tck.methods.sdk.param.ethereum.EthereumTransactionParams;
import org.hiero.tck.methods.sdk.response.EthereumTransactionResponse;
import org.hiero.tck.util.TransactionBuilders;

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

