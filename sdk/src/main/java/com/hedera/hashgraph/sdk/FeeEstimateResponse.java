// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The response containing the estimated transaction fees.
 * <p>
 * This response provides a breakdown of the network, node, and service fees,
 * along with the total estimated cost in tinycents.
 */
public final class FeeEstimateResponse {
    /**
     * The mode that was used to calculate the fees.
     */
    private final FeeEstimateMode mode;

    /**
     * The network fee component which covers the cost of gossip, consensus,
     * signature verifications, fee payment, and storage.
     */
    @Nullable
    private final NetworkFee network;

    /**
     * The node fee component which is to be paid to the node that submitted the
     * transaction to the network. This fee exists to compensate the node for the
     * work it performed to pre-check the transaction before submitting it, and
     * incentivizes the node to accept new transactions from users.
     */
    @Nullable
    private final FeeEstimate node;

    /**
     * An array of strings for any caveats.
     * <p>
     * For example: ["Fallback to worst-case due to missing state"]
     */
    private final List<String> notes;

    /**
     * The service fee component which covers execution costs, state saved in the
     * Merkle tree, and additional costs to the blockchain storage.
     */
    @Nullable
    private final FeeEstimate service;

    /**
     * The sum of the network, node, and service subtotals in tinycents.
     */
    private final long total;

    /**
     * Constructor.
     *
     * @param mode    the fee estimate mode used
     * @param network the network fee component
     * @param node    the node fee estimate
     * @param notes   the list of notes/caveats
     * @param service the service fee estimate
     * @param total   the total fee in tinycents
     */
    FeeEstimateResponse(
            FeeEstimateMode mode,
            @Nullable NetworkFee network,
            @Nullable FeeEstimate node,
            List<String> notes,
            @Nullable FeeEstimate service,
            long total) {
        this.mode = mode;
        this.network = network;
        this.node = node;
        this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
        this.service = service;
        this.total = total;
    }

    /**
     * Create a FeeEstimateResponse from a protobuf.
     *
     * @param response the protobuf
     * @return the new FeeEstimateResponse
     */
    static FeeEstimateResponse fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse response) {
        var mode = FeeEstimateMode.valueOf(response.getModeValue());
        var network = response.hasNetwork() ? NetworkFee.fromProtobuf(response.getNetwork()) : null;
        var node = response.hasNode() ? FeeEstimate.fromProtobuf(response.getNode()) : null;
        var notes = new ArrayList<>(response.getNotesList());
        var service = response.hasService() ? FeeEstimate.fromProtobuf(response.getService()) : null;
        var total = response.getTotal();

        return new FeeEstimateResponse(mode, network, node, notes, service, total);
    }

    /**
     * Create a FeeEstimateResponse from a byte array.
     *
     * @param bytes the byte array
     * @return the new FeeEstimateResponse
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static FeeEstimateResponse fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse.parseFrom(bytes).toBuilder()
                .build());
    }

    /**
     * Extract the fee estimate mode used.
     *
     * @return the fee estimate mode
     */
    public FeeEstimateMode getMode() {
        return mode;
    }

    /**
     * Extract the network fee component.
     *
     * @return the network fee component, or null if not set
     */
    @Nullable
    public NetworkFee getNetwork() {
        return network;
    }

    /**
     * Extract the node fee estimate.
     *
     * @return the node fee estimate, or null if not set
     */
    @Nullable
    public FeeEstimate getNode() {
        return node;
    }

    /**
     * Extract the list of notes/caveats.
     *
     * @return an unmodifiable list of notes
     */
    public List<String> getNotes() {
        return notes;
    }

    /**
     * Extract the service fee estimate.
     *
     * @return the service fee estimate, or null if not set
     */
    @Nullable
    public FeeEstimate getService() {
        return service;
    }

    /**
     * Extract the total fee in tinycents.
     *
     * @return the total fee in tinycents
     */
    public long getTotal() {
        return total;
    }

    /**
     * Convert the fee estimate response to a protobuf.
     *
     * @return the protobuf
     */
    com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse toProtobuf() {
        var builder = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse.newBuilder()
                .setModeValue(mode.code)
                .setTotal(total)
                .addAllNotes(notes);

        if (network != null) {
            builder.setNetwork(network.toProtobuf());
        }
        if (node != null) {
            builder.setNode(node.toProtobuf());
        }
        if (service != null) {
            builder.setService(service.toProtobuf());
        }

        return builder.build();
    }

    /**
     * Convert the fee estimate response to a byte array.
     *
     * @return the byte array
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mode", mode)
                .add("network", network)
                .add("node", node)
                .add("notes", notes)
                .add("service", service)
                .add("total", total)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FeeEstimateResponse that)) {
            return false;
        }
        return total == that.total
                && mode == that.mode
                && Objects.equals(network, that.network)
                && Objects.equals(node, that.node)
                && Objects.equals(notes, that.notes)
                && Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, network, node, notes, service, total);
    }
}
