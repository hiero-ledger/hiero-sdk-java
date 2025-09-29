// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileAppendParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileCreateParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileDeleteParams;
import com.hedera.hashgraph.tck.methods.sdk.param.file.FileUpdateParams;
import com.hedera.hashgraph.tck.methods.sdk.response.FileResponse;
import com.hedera.hashgraph.tck.util.TransactionBuilders;
import java.time.Duration;

/**
 * FileService for file related methods
 */
@JSONRPC2Service
public class FileService extends AbstractJSONRPC2Service {

    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(3L);
    private final SdkService sdkService;

    public FileService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("createFile")
    public FileResponse createFile(final FileCreateParams params) throws Exception {
        FileCreateTransaction transaction = TransactionBuilders.FileBuilder.buildCreate(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        String fileId = "";
        if (receipt.status == Status.SUCCESS && receipt.fileId != null) {
            fileId = receipt.fileId.toString();
        }

        return new FileResponse(fileId, receipt.status);
    }

    @JSONRPC2Method("deleteFile")
    public FileResponse deleteFile(final FileDeleteParams params) throws Exception {
        FileDeleteTransaction transaction = TransactionBuilders.FileBuilder.buildDelete(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        return new FileResponse("", receipt.status);
    }

    @JSONRPC2Method("updateFile")
    public FileResponse updateFile(final FileUpdateParams params) throws Exception {
        FileUpdateTransaction transaction = TransactionBuilders.FileBuilder.buildUpdate(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        return new FileResponse("", receipt.status);
    }

    @JSONRPC2Method("appendFile")
    public FileResponse appendFile(final FileAppendParams params) throws Exception {
        FileAppendTransaction transaction = TransactionBuilders.FileBuilder.buildAppend(params);

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams ->
                        commonTransactionParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        return new FileResponse("", receipt.status);
    }
}
