// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.hiero.sdk.proto.ContractLoginfo;

/**
 * The log information for an event returned by a smart contract function call.
 * One function call may return several such events.
 */
public final class ContractLogInfo {
    /**
     * Address of a contract that emitted the event.
     */
    public final ContractId contractId;

    /**
     * Bloom filter for a particular log.
     */
    public final ByteString bloom;

    /**
     * Topics of a particular event.
     */
    public final List<ByteString> topics;

    /**
     * The event data.
     */
    public final ByteString data;

    /**
     * Constructor.
     *
     * @param contractId                the contract id
     * @param bloom                     the bloom filter
     * @param topics                    list of topics
     * @param data                      the event data
     */
    private ContractLogInfo(ContractId contractId, ByteString bloom, List<ByteString> topics, ByteString data) {
        this.contractId = contractId;
        this.bloom = bloom;
        this.topics = topics;
        this.data = data;
    }

    /**
     * Convert to a protobuf.
     *
     * @param logInfo                   the log info object
     * @return                          the protobuf
     */
    static ContractLogInfo fromProtobuf(org.hiero.sdk.proto.ContractLoginfo logInfo) {
        return new ContractLogInfo(
                ContractId.fromProtobuf(logInfo.getContractID()),
                logInfo.getBloom(),
                logInfo.getTopicList(),
                logInfo.getData());
    }

    /**
     * Create the contract log info from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the contract log info object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static ContractLogInfo fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(ContractLoginfo.parseFrom(bytes));
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    org.hiero.sdk.proto.ContractLoginfo toProtobuf() {
        var contractLogInfo = org.hiero.sdk.proto.ContractLoginfo.newBuilder()
                .setContractID(contractId.toProtobuf())
                .setBloom(bloom);

        for (ByteString topic : topics) {
            contractLogInfo.addTopic(topic);
        }

        return contractLogInfo.build();
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
        var stringHelper = MoreObjects.toStringHelper(this)
                .add("contractId", contractId)
                .add("bloom", Hex.toHexString(bloom.toByteArray()));

        var topicList = new ArrayList<>();

        for (var topic : topics) {
            topicList.add(Hex.toHexString(topic.toByteArray()));
        }

        return stringHelper.add("topics", topicList).toString();
    }
}
