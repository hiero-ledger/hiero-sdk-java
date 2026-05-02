// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
     * The network fee component which covers the cost of gossip, consensus,
     * signature verifications, fee payment, and storage.
     */
    @Nullable
    private final NetworkFee network;

    /**
     * The node fee component which is to be paid to the node that submitted the
     * transaction to the network.  This fee exists to compensate the node for the
     * work it performed to pre-check the transaction before submitting it, and
     * incentivizes the node to accept new transactions from users.
     */
    @Nullable
    private final FeeEstimate node;

    /**
     * The service fee component which covers execution costs, state saved in the
     * Merkle tree, and additional costs to the blockchain storage.
     */
    @Nullable
    private final FeeEstimate service;

    /**
     * The high-volume throttle multiplier returned by the mirror node.
     * <p>
     * When non-zero high-volume throttle utilization is requested, this value
     * will be greater than or equal to 1.
     */
    private final long highVolumeMultiplier;

    /**
     * The sum of the network, node, and service subtotals in tinycents.
     */
    private final long total;

    /**
     * Constructor.
     *
     * @param network           the network fee component
     * @param node              the node fee estimate
     * @param highVolumeMultiplier the high-volume throttle multiplier
     * @param service           the service fee estimate
     * @param total                the total fee in tinycents
     */
    FeeEstimateResponse(
            @Nullable NetworkFee network,
            @Nullable FeeEstimate node,
            long highVolumeMultiplier,
            @Nullable FeeEstimate service,
            long total) {
        this.network = network;
        this.node = node;
        this.highVolumeMultiplier = highVolumeMultiplier;
        this.service = service;
        this.total = total;
    }

    /**
     * Create a FeeEstimateResponse from a REST JSON payload.
     *
     * @param json        the raw JSON response
     * @return the new FeeEstimateResponse
     */
    static FeeEstimateResponse fromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        return new FeeEstimateResponse(
                parseNetworkFeeFromJson(root),
                parseFeeEstimateFromJson(root, "node"),
                parseHighVolumeMultiplierFromJson(root),
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
            return NetworkFee.fromJson(root.getAsJsonObject("network"));
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
     * Parse high-volume multiplier from JSON.
     *
     * @param root the JSON object
     * @return the high-volume multiplier value, or 0 if not present
     */
    private static long parseHighVolumeMultiplierFromJson(JsonObject root) {
        return root.has("high_volume_multiplier")
                ? root.get("high_volume_multiplier").getAsLong()
                : 0L;
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
     * Extract the high-volume throttle multiplier.
     *
     * @return the high-volume multiplier
     */
    public long getHighVolumeMultiplier() {
        return highVolumeMultiplier;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("network", network)
                .add("node", node)
                .add("highVolumeMultiplier", highVolumeMultiplier)
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
                && highVolumeMultiplier == that.highVolumeMultiplier
                && Objects.equals(network, that.network)
                && Objects.equals(node, that.node)
                && Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(network, node, highVolumeMultiplier, service, total);
    }
}

