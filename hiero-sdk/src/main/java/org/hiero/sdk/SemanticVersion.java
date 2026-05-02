// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Hedera follows semantic versioning () for both the HAPI protobufs and
 * the Services software. This type allows the getVersionInfo query in the
 * NetworkService to return the deployed versions of both protobufs and
 * software on the node answering the query.
 *
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/basic-types/semanticversion">Hedera Documentation</a>
 */
public class SemanticVersion {
    /**
     * Increases with incompatible API changes
     */
    public int major;
    /**
     * Increases with backwards-compatible new functionality
     */
    public int minor;
    /**
     * Increases with backwards-compatible bug fixes
     */
    public int patch;

    /**
     * Constructor.
     *
     * @param major                     the major part
     * @param minor                     the minor part
     * @param patch                     the patch part
     */
    SemanticVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Create a semantic version object from a protobuf.
     *
     * @param version                   the protobuf
     * @return                          the new semantic version
     */
    protected static SemanticVersion fromProtobuf(org.hiero.sdk.proto.SemanticVersion version) {
        return new SemanticVersion(version.getMajor(), version.getMinor(), version.getPatch());
    }

    /**
     * Create a semantic version from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new semantic version
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static SemanticVersion fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(org.hiero.sdk.proto.SemanticVersion.parseFrom(bytes));
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    protected org.hiero.sdk.proto.SemanticVersion toProtobuf() {
        return org.hiero.sdk.proto.SemanticVersion.newBuilder()
                .setMajor(major)
                .setMinor(minor)
                .setPatch(patch)
                .build();
    }

    /**
     * Create the byte array.
     *
     * @return                          the byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }
}
