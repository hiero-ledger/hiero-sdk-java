// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Instant;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.FileGetInfoResponse;

/**
 * Current information for a file, including its size.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/file-storage/get-file-info">Hedera Documentation</a>
 */
public final class FileInfo {
    /**
     * The ID of the file for which information is requested.
     */
    public final FileId fileId;

    /**
     * Number of bytes in contents.
     */
    public final long size;

    /**
     * The current time at which this account is set to expire.
     */
    public final Instant expirationTime;

    /**
     * True if deleted but not yet expired.
     */
    public final boolean isDeleted;

    /**
     * One of these keys must sign in order to delete the file.
     * All of these keys must sign in order to update the file.
     */
    @Nullable
    public final KeyList keys;

    /**
     * The memo associated with the file
     */
    public final String fileMemo;

    /**
     * The ledger ID the response was returned from; please see <a href="https://github.com/hashgraph/hedera-improvement-proposal/blob/master/HIP/hip-198.md">HIP-198</a> for the network-specific IDs.
     */
    public final LedgerId ledgerId;

    private FileInfo(
            FileId fileId,
            long size,
            Instant expirationTime,
            boolean isDeleted,
            @Nullable KeyList keys,
            String fileMemo,
            LedgerId ledgerId) {
        this.fileId = fileId;
        this.size = size;
        this.expirationTime = expirationTime;
        this.isDeleted = isDeleted;
        this.keys = keys;
        this.fileMemo = fileMemo;
        this.ledgerId = ledgerId;
    }

    /**
     * Create a file info object from a ptotobuf.
     *
     * @param fileInfo                  the protobuf
     * @return                          the new file info object
     */
    static FileInfo fromProtobuf(FileGetInfoResponse.FileInfo fileInfo) {
        @Nullable KeyList keys = fileInfo.hasKeys() ? KeyList.fromProtobuf(fileInfo.getKeys(), null) : null;

        return new FileInfo(
                FileId.fromProtobuf(fileInfo.getFileID()),
                fileInfo.getSize(),
                InstantConverter.fromProtobuf(fileInfo.getExpirationTime()),
                fileInfo.getDeleted(),
                keys,
                fileInfo.getMemo(),
                LedgerId.fromByteString(fileInfo.getLedgerId()));
    }

    /**
     * Create a file info object from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new file info object
     * @throws InvalidProtocolBufferException   when there is an issue with the protobuf
     */
    public static FileInfo fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(
                FileGetInfoResponse.FileInfo.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    FileGetInfoResponse.FileInfo toProtobuf() {
        var fileInfoBuilder = FileGetInfoResponse.FileInfo.newBuilder()
                .setFileID(fileId.toProtobuf())
                .setSize(size)
                .setExpirationTime(InstantConverter.toProtobuf(expirationTime))
                .setDeleted(isDeleted)
                .setMemo(fileMemo)
                .setLedgerId(ledgerId.toByteString());

        if (keys != null) {
            var keyList = org.hiero.sdk.proto.KeyList.newBuilder();

            for (Key key : keys) {
                keyList.addKeys(key.toProtobufKey());
            }

            fileInfoBuilder.setKeys(keyList);
        }

        return fileInfoBuilder.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fileId", fileId)
                .add("size", size)
                .add("expirationTime", expirationTime)
                .add("isDeleted", isDeleted)
                .add("keys", keys)
                .add("fileMemo", fileMemo)
                .add("ledgerId", ledgerId)
                .toString();
    }

    /**
     * Create the byte array.
     *
     * @return                          the byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }
}
