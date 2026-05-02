// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk;

import com.google.protobuf.ByteString;
import java.time.Duration;
import java.util.List;
import org.hiero.sdk.Client;
import org.hiero.sdk.FileAppendTransaction;
import org.hiero.sdk.FileContentsQuery;
import org.hiero.sdk.FileCreateTransaction;
import org.hiero.sdk.FileDeleteTransaction;
import org.hiero.sdk.FileInfo;
import org.hiero.sdk.FileInfoQuery;
import org.hiero.sdk.FileUpdateTransaction;
import org.hiero.sdk.Status;
import org.hiero.sdk.TransactionReceipt;
import org.hiero.sdk.TransactionResponse;
import org.hiero.tck.annotation.JSONRPC2Method;
import org.hiero.tck.annotation.JSONRPC2Service;
import org.hiero.tck.methods.AbstractJSONRPC2Service;
import org.hiero.tck.methods.sdk.param.file.FileAppendParams;
import org.hiero.tck.methods.sdk.param.file.FileContentsParams;
import org.hiero.tck.methods.sdk.param.file.FileCreateParams;
import org.hiero.tck.methods.sdk.param.file.FileDeleteParams;
import org.hiero.tck.methods.sdk.param.file.FileInfoQueryParams;
import org.hiero.tck.methods.sdk.param.file.FileUpdateParams;
import org.hiero.tck.methods.sdk.response.FileContentsResponse;
import org.hiero.tck.methods.sdk.response.FileInfoResponse;
import org.hiero.tck.methods.sdk.response.FileResponse;
import org.hiero.tck.util.QueryBuilders;
import org.hiero.tck.util.TransactionBuilders;

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
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams -> commonTransactionParams.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.getReceipt(client);

        String fileId = "";
        if (receipt.status == Status.SUCCESS && receipt.fileId != null) {
            fileId = receipt.fileId.toString();
        }

        return new FileResponse(fileId, receipt.status);
    }

    @JSONRPC2Method("deleteFile")
    public FileResponse deleteFile(final FileDeleteParams params) throws Exception {
        FileDeleteTransaction transaction = TransactionBuilders.FileBuilder.buildDelete(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams -> commonTransactionParams.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.getReceipt(client);

        return new FileResponse("", receipt.status);
    }

    @JSONRPC2Method("updateFile")
    public FileResponse updateFile(final FileUpdateParams params) throws Exception {
        FileUpdateTransaction transaction = TransactionBuilders.FileBuilder.buildUpdate(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams -> commonTransactionParams.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.getReceipt(client);

        return new FileResponse("", receipt.status);
    }

    @JSONRPC2Method("appendFile")
    public FileResponse appendFile(final FileAppendParams params) throws Exception {
        FileAppendTransaction transaction = TransactionBuilders.FileBuilder.buildAppend(params);
        Client client = sdkService.getClient(params.getSessionId());

        params.getCommonTransactionParams()
                .ifPresent(commonTransactionParams -> commonTransactionParams.fillOutTransaction(transaction, client));

        TransactionResponse txResponse = transaction.execute(client);
        TransactionReceipt receipt = txResponse.getReceipt(client);

        return new FileResponse("", receipt.status);
    }

    @JSONRPC2Method("getFileInfo")
    public FileInfoResponse getFileInfo(final FileInfoQueryParams params) throws Exception {
        FileInfoQuery query = QueryBuilders.FileBuilder.buildFileInfoQuery(params);
        Client client = sdkService.getClient(params.getSessionId());

        FileInfo result = query.execute(client);
        return mapFileInfoResponse(result);
    }

    /**
     *  Map FileInfo from SDK to FileInfoResponse for JSON-RPC
     */
    private FileInfoResponse mapFileInfoResponse(FileInfo fileInfo) {
        List<String> keys = fileInfo.keys == null
                ? null
                : fileInfo.keys.stream().map(key -> key.toString()).toList();

        return new FileInfoResponse(
                fileInfo.fileId.toString(),
                String.valueOf(fileInfo.size),
                fileInfo.expirationTime.toString(),
                fileInfo.isDeleted,
                fileInfo.fileMemo,
                fileInfo.ledgerId.toString(),
                keys);
    }

    @JSONRPC2Method("getFileContents")
    public FileContentsResponse getFileContents(final FileContentsParams params) throws Exception {
        FileContentsQuery query = QueryBuilders.FileBuilder.buildFileContents(params);
        Client client = sdkService.getClient(params.getSessionId());

        ByteString response = query.execute(client);

        // Convert ByteString to string
        String contents = response.toStringUtf8();

        return new FileContentsResponse(contents);
    }
}
