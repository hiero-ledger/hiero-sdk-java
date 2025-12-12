// SPDX-License-Identifier:  Apache-2.0
package com.hedera.hashgraph. sdk;

import com.google.common.base. MoreObjects;
import com.google. gson.JsonObject;
import com.google.gson. JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java. util.Collections;
import java. util.List;
import java.util. Objects;
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
    private final NetworkFee networkFee;

    /**
     * The node fee component which is to be paid to the node that submitted the
     * transaction to the network.  This fee exists to compensate the node for the
     * work it performed to pre-check the transaction before submitting it, and
     * incentivizes the node to accept new transactions from users.
     */
    @Nullable
    private final FeeEstimate nodeFee;

    /**
     * The service fee component which covers execution costs, state saved in the
     * Merkle tree, and additional costs to the blockchain storage.
     */
    @Nullable
    private final FeeEstimate serviceFee;

    /**
     * An array of strings for any caveats.
     * <p>
     * For example:  ["Fallback to worst-case due to missing state"]
     */
    private final List<String> notes;

    /**
     * The sum of the network, node, and service subtotals in tinycents.
     */
    private final long total;

    /**
     * Constructor.
     *
     * @param mode       the fee estimate mode used
     * @param networkFee the network fee component
     * @param nodeFee    the node fee estimate
     * @param notes      the list of notes/caveats
     * @param serviceFee the service fee estimate
     * @param total      the total fee in tinycents
     */
    FeeEstimateResponse(
        FeeEstimateMode mode,
        @Nullable NetworkFee networkFee,
        @Nullable FeeEstimate nodeFee,
        List<String> notes,
        @Nullable FeeEstimate serviceFee,
        long total) {
        this.mode = mode;
        this. networkFee = networkFee;
        this.nodeFee = nodeFee;
        this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
        this.serviceFee = serviceFee;
        this.total = total;
    }

    /**
     * Create a FeeEstimateResponse from a protobuf.
     *
     * @param response the protobuf
     * @return the new FeeEstimateResponse
     */
    static FeeEstimateResponse fromProtobuf(com.hedera.hashgraph. sdk.proto.mirror.FeeEstimateResponse response) {
        var mode = FeeEstimateMode.valueOf(response.getModeValue());
        var network = response.hasNetwork() ? NetworkFee.fromProtobuf(response.getNetwork()) : null;
        var node = response.hasNode() ? FeeEstimate.fromProtobuf(response.getNode()) : null;
        var notes = new ArrayList<>(response.getNotesList());
        var service = response.hasService() ? FeeEstimate.fromProtobuf(response.getService()) : null;
        var total = response.getTotal();

        return new FeeEstimateResponse(mode, network, node, notes, service, total);
    }

    /**
     * Create a FeeEstimateResponse from a REST JSON payload.
     *
     * @param json        the raw JSON response
     * @param defaultMode the mode to fall back to when the response omits mode
     * @return the new FeeEstimateResponse
     */
    static FeeEstimateResponse fromJson(String json, FeeEstimateMode defaultMode) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        return new FeeEstimateResponse(
            parseModeFromJson(root, defaultMode),
            parseNetworkFeeFromJson(root),
            parseFeeEstimateFromJson(root, "node"),
            parseNotesFromJson(root),
            parseFeeEstimateFromJson(root, "service"),
            parseTotalFromJson(root));
    }

    /**
     * Parse the fee estimate mode from JSON.
     *
     * @param root        the JSON object
     * @param defaultMode the default mode if not present
     * @return the parsed mode
     */
    private static FeeEstimateMode parseModeFromJson(JsonObject root, FeeEstimateMode defaultMode) {
        if (root.has("mode")) {
            return FeeEstimateMode.fromString(root.get("mode").getAsString());
        }
        if (root.has("mode_used")) {
            return FeeEstimateMode.fromString(root.get("mode_used").getAsString());
        }
        return defaultMode;
    }

    /**
     * Parse the network fee from JSON.
     *
     * @param root the JSON object
     * @return the parsed NetworkFee or null
     */
    @Nullable
    private static NetworkFee parseNetworkFeeFromJson(JsonObject root) {
        if (root.has("network") && root.get("network").isJsonObject()) {
            return NetworkFee. fromJson(root.getAsJsonObject("network"));
        }
        return null;
    }

    /**
     * Parse a fee estimate from JSON by field name.
     *
     * @param root      the JSON object
     * @param fieldName the field name to parse ("node" or "service")
     * @return the parsed FeeEstimate or null
     */
    @Nullable
    private static FeeEstimate parseFeeEstimateFromJson(JsonObject root, String fieldName) {
        if (root.has(fieldName) && root.get(fieldName).isJsonObject()) {
            return FeeEstimate.fromJson(root.getAsJsonObject(fieldName));
        }
        return null;
    }

    /**
     * Parse notes from JSON.
     *
     * @param root the JSON object
     * @return the list of notes
     */
    private static List<String> parseNotesFromJson(JsonObject root) {
        List<String> notes = new ArrayList<>();
        if (root.has("notes") && root.get("notes").isJsonArray()) {
            root.getAsJsonArray("notes").forEach(element -> notes.add(element. getAsString()));
        }
        return notes;
    }

    /**
     * Parse total from JSON.
     *
     * @param root the JSON object
     * @return the total value
     */
    private static long parseTotalFromJson(JsonObject root) {
        return root.has("total") ? root.get("total").getAsLong() : 0L;
    }

    /**
     * Create a FeeEstimateResponse from a byte array.
     *
     * @param bytes the byte array
     * @return the new FeeEstimateResponse
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static FeeEstimateResponse fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(com.hedera.hashgraph.sdk. proto.mirror.FeeEstimateResponse.parseFrom(bytes).toBuilder()
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
    public NetworkFee getNetworkFee() {
        return networkFee;
    }

    /**
     * Extract the node fee estimate.
     *
     * @return the node fee estimate, or null if not set
     */
    @Nullable
    public FeeEstimate getNodeFee() {
        return nodeFee;
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
    public FeeEstimate getServiceFee() {
        return serviceFee;
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
    com.hedera.hashgraph.sdk. proto.mirror.FeeEstimateResponse toProtobuf() {
        var builder = com.hedera.hashgraph. sdk.proto.mirror.FeeEstimateResponse.newBuilder()
            .setModeValue(mode.code)
            .setTotal(total)
            .addAllNotes(notes);

        if (networkFee != null) {
            builder.setNetwork(networkFee.toProtobuf());
        }
        if (nodeFee != null) {
            builder.setNode(nodeFee.toProtobuf());
        }
        if (serviceFee != null) {
            builder.setService(serviceFee.toProtobuf());
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
            .add("network", networkFee)
            .add("node", nodeFee)
            .add("notes", notes)
            .add("service", serviceFee)
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
        return total == that. total
            && mode == that.mode
            && Objects.equals(networkFee, that.networkFee)
            && Objects.equals(nodeFee, that.nodeFee)
            && Objects.equals(notes, that.notes)
            && Objects.equals(serviceFee, that.serviceFee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, networkFee, nodeFee, notes, serviceFee, total);
    }
}
