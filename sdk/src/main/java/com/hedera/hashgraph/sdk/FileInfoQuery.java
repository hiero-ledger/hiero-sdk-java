// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.FileGetInfoQuery;
import com.hedera.hashgraph.sdk.proto.FileServiceGrpc;
import com.hedera.hashgraph.sdk.proto.QueryHeader;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import io.grpc.MethodDescriptor;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

/**
 * Get all of the information about a file, except for its contents.
 * <p>
 * When a file expires, it no longer exists, and there will be no info about it, and the fileInfo field
 * will be blank.
 * <p>
 * If a transaction or smart contract deletes the file, but it has not yet expired, then the
 * fileInfo field will be non-empty, the deleted field will be true, its size will be 0,
 * and its contents will be empty. Note that each file has a FileID, but does not have a filename.
 */
public final class FileInfoQuery extends Query<FileInfo, FileInfoQuery> {

    private @Nullable FileId fileId = null;

    /**
     * Constructor.
     */
    public FileInfoQuery() {}

    /**
     * Extract the file id.
     *
     * @return                          the file id
     */
    public @Nullable FileId getFileId() {
        return fileId;
    }

    /**
     * Sets the file ID for which information is requested.
     *
     * @param fileId The FileId to be set
     * @return {@code this}
     */
    public FileInfoQuery setFileId(FileId fileId) {
        Objects.requireNonNull(fileId);
        this.fileId = fileId;
        return this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (fileId != null) {
            fileId.validateChecksum(client);
        }
    }

    @Override
    void onMakeRequest(com.hedera.hashgraph.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        var builder = FileGetInfoQuery.newBuilder();
        if (fileId != null) {
            builder.setFileID(fileId.toProtobuf());
        }

        queryBuilder.setFileGetInfo(builder.setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getFileGetInfo().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(com.hedera.hashgraph.sdk.proto.Query request) {
        return request.getFileGetInfo().getHeader();
    }

    @Override
    FileInfo mapResponse(Response response, AccountId nodeId, com.hedera.hashgraph.sdk.proto.Query request) {
        return FileInfo.fromProtobuf(response.getFileGetInfo().getFileInfo());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Query, Response> getMethodDescriptor() {
        return FileServiceGrpc.getGetFileInfoMethod();
    }

    @Override
    public CompletableFuture<Hbar> getCostAsync(Client client) {
        // deleted accounts return a COST_ANSWER of zero which triggers `INSUFFICIENT_TX_FEE`
        // if you set that as the query payment; 25 tinybar seems to be enough to get
        // `FILE_DELETED` back instead.
        return super.getCostAsync(client).thenApply((cost) -> Hbar.fromTinybars(Math.max(cost.toTinybars(), 25)));
    }
}
