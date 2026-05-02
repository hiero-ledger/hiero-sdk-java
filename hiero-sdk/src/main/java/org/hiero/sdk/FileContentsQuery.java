// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import io.grpc.MethodDescriptor;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.FileGetContentsQuery;
import org.hiero.sdk.proto.FileServiceGrpc;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.ResponseHeader;

/**
 * Get the contents of a file. The content field is empty (no bytes) if the
 * file is empty.
 *
 * A query to get the contents of a file. Queries do not change the state of
 * the file or require network consensus. The information is returned from a
 * single node processing the query.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/file-storage/get-file-contents">Hedera Documentation</a>
 */
public final class FileContentsQuery extends Query<ByteString, FileContentsQuery> {

    @Nullable
    private FileId fileId = null;

    /**
     * Constructor.
     */
    public FileContentsQuery() {}

    /**
     * Extract the file id.
     *
     * @return                          the file id
     */
    @Nullable
    public FileId getFileId() {
        return fileId;
    }

    /**
     * Sets the file ID of the file whose contents are requested.
     *
     * @param fileId The FileId to be set
     * @return {@code this}
     */
    public FileContentsQuery setFileId(FileId fileId) {
        Objects.requireNonNull(fileId);
        this.fileId = fileId;
        return this;
    }

    @Override
    public CompletableFuture<Hbar> getCostAsync(Client client) {
        // deleted accounts return a COST_ANSWER of zero which triggers `INSUFFICIENT_TX_FEE`
        // if you set that as the query payment; 25 tinybar seems to be enough to get
        // `FILE_DELETED` back instead.
        return super.getCostAsync(client).thenApply((cost) -> Hbar.fromTinybars(Math.max(cost.toTinybars(), 25)));
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (fileId != null) {
            fileId.validateChecksum(client);
        }
    }

    @Override
    void onMakeRequest(org.hiero.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        var builder = FileGetContentsQuery.newBuilder();
        if (fileId != null) {
            builder.setFileID(fileId.toProtobuf());
        }

        queryBuilder.setFileGetContents(builder.setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getFileGetContents().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(org.hiero.sdk.proto.Query request) {
        return request.getFileGetContents().getHeader();
    }

    @Override
    ByteString mapResponse(Response response, AccountId nodeId, org.hiero.sdk.proto.Query request) {
        return response.getFileGetContents().getFileContents().getContents();
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Query, Response> getMethodDescriptor() {
        return FileServiceGrpc.getGetFileContentMethod();
    }
}
