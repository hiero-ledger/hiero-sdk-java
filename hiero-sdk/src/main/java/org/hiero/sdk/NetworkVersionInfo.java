// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import org.hiero.sdk.proto.NetworkGetVersionInfoResponse;

/**
 * Internal utility class.
 */
public class NetworkVersionInfo {
    /**
     * Version of the protobuf schema in use by the network
     */
    public final SemanticVersion protobufVersion;

    /**
     * Version of the Hedera services in use by the network
     */
    public final SemanticVersion servicesVersion;

    /**
     * Constructor.
     *
     * @param hapi                      the protobuf version
     * @param hedera                    the hedera version
     */
    NetworkVersionInfo(SemanticVersion hapi, SemanticVersion hedera) {
        this.protobufVersion = hapi;
        this.servicesVersion = hedera;
    }

    /**
     * Create a network version info object from a protobuf.
     *
     * @param proto                     the protobuf
     * @return                          the new network version object
     */
    protected static NetworkVersionInfo fromProtobuf(NetworkGetVersionInfoResponse proto) {
        return new NetworkVersionInfo(
                SemanticVersion.fromProtobuf(proto.getHapiProtoVersion()),
                SemanticVersion.fromProtobuf(proto.getHederaServicesVersion()));
    }

    /**
     * Create a network version info object from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new network version object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static NetworkVersionInfo fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(NetworkGetVersionInfoResponse.parseFrom(bytes));
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    protected NetworkGetVersionInfoResponse toProtobuf() {
        return NetworkGetVersionInfoResponse.newBuilder()
                .setHapiProtoVersion(protobufVersion.toProtobuf())
                .setHederaServicesVersion(servicesVersion.toProtobuf())
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
}
