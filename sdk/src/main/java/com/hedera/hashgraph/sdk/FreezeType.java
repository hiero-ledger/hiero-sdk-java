// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * Enum for the freeze types.
 */
public enum FreezeType {
    /**
     * An invalid freeze type.
     * <p>
     * The first value in a protobuf enum is a default value. This default
     * is RECOMMENDED to be an invalid value to aid in detecting unset fields.
     */
    UNKNOWN_FREEZE_TYPE(com.hedera.hashgraph.sdk.proto.FreezeType.UNKNOWN_FREEZE_TYPE),

    /**
     * Freeze the network, and take no further action.
     * <p>
     * The `start_time` field is REQUIRED, MUST be strictly later than the
     * consensus time when this transaction is handled, and SHOULD be between
     * `300` and `3600` seconds after the transaction identifier
     * `transactionValidStart` field.<br/>
     * The fields `update_file` and `file_hash` SHALL be ignored.<br/>
     * A `FREEZE_ONLY` transaction SHALL NOT perform any network
     * changes or upgrades.<br/>
     * After this freeze is processed manual intervention is REQUIRED
     * to restart the network.
     */
    FREEZE_ONLY(com.hedera.hashgraph.sdk.proto.FreezeType.FREEZE_ONLY),

    /**
     * This freeze type does not freeze the network, but begins
     * "preparation" to upgrade the network.
     * <p>
     * The fields `update_file` and `file_hash` are REQUIRED
     * and MUST be valid.<br/>
     * The `start_time` field SHALL be ignored.<br/>
     * A `PREPARE_UPGRADE` transaction SHALL NOT freeze the network or
     * interfere with general transaction processing.<br/>
     * If this freeze type is initiated after a `TELEMETRY_UPGRADE`, the
     * prepared telemetry upgrade SHALL be reset and all telemetry upgrade
     * artifacts in the filesystem SHALL be deleted.<br/>
     * At some point after this freeze type completes (dependent on the size
     * of the upgrade file), the network SHALL be prepared to complete
     * a software upgrade of all nodes.
     */
    PREPARE_UPGRADE(com.hedera.hashgraph.sdk.proto.FreezeType.PREPARE_UPGRADE),

    /**
     * Freeze the network to perform a software upgrade.
     * <p>
     * The `start_time` field is REQUIRED, MUST be strictly later than the
     * consensus time when this transaction is handled, and SHOULD be between
     * `300` and `3600` seconds after the transaction identifier
     * `transactionValidStart` field.<br/>
     * A software upgrade file MUST be prepared prior to this transaction.<br/>
     * After this transaction completes, the network SHALL initiate an
     * upgrade and restart of all nodes at the start time specified.
     */
    FREEZE_UPGRADE(com.hedera.hashgraph.sdk.proto.FreezeType.FREEZE_UPGRADE),

    /**
     * Abort a pending network freeze operation.
     * <p>
     * All fields SHALL be ignored for this freeze type.<br/>
     * This freeze type MAY be submitted after a `FREEZE_ONLY`,
     * `FREEZE_UPGRADE`, or `TELEMETRY_UPGRADE` is initiated.<br/>
     * This freeze type MUST be submitted and reach consensus
     * before the `start_time` designated for the current pending
     * freeze to be effective.<br/>
     * After this freeze type is processed, the upgrade file hash
     * and pending freeze start time stored in the network SHALL
     * be reset to default (empty) values.
     */
    FREEZE_ABORT(com.hedera.hashgraph.sdk.proto.FreezeType.FREEZE_ABORT),

    /**
     * Prepare an upgrade of auxiliary services and containers
     * providing telemetry/metrics.
     * <p>
     * The `start_time` field is REQUIRED, MUST be strictly later than the
     * consensus time when this transaction is handled, and SHOULD be between
     * `300` and `3600` seconds after the transaction identifier
     * `transactionValidStart` field.<br/>
     * The `update_file` field is REQUIRED and MUST be valid.<br/>
     * A `TELEMETRY_UPGRADE` transaction SHALL NOT freeze the network or
     * interfere with general transaction processing.<br/>
     * This freeze type MUST NOT be initiated between a `PREPARE_UPGRADE`
     * and `FREEZE_UPGRADE`. If this freeze type is initiated after a
     * `PREPARE_UPGRADE`, the prepared upgrade SHALL be reset and all software
     * upgrade artifacts in the filesystem SHALL be deleted.<br/>
     * At some point after this freeze type completes (dependent on the
     * size of the upgrade file), the network SHALL automatically upgrade
     * the telemetry/metrics services and containers as directed in
     * the specified telemetry upgrade file.
     * <blockquote> The condition that `start_time` is REQUIRED is an
     * historical anomaly and SHOULD change in a future release.</blockquote>
     */
    TELEMETRY_UPGRADE(com.hedera.hashgraph.sdk.proto.FreezeType.TELEMETRY_UPGRADE);

    final com.hedera.hashgraph.sdk.proto.FreezeType code;

    FreezeType(com.hedera.hashgraph.sdk.proto.FreezeType code) {
        this.code = code;
    }

    static FreezeType valueOf(com.hedera.hashgraph.sdk.proto.FreezeType code) {
        return switch (code) {
            case UNKNOWN_FREEZE_TYPE -> UNKNOWN_FREEZE_TYPE;
            case FREEZE_ONLY -> FREEZE_ONLY;
            case PREPARE_UPGRADE -> PREPARE_UPGRADE;
            case FREEZE_UPGRADE -> FREEZE_UPGRADE;
            case FREEZE_ABORT -> FREEZE_ABORT;
            case TELEMETRY_UPGRADE -> TELEMETRY_UPGRADE;
            case UNRECOGNIZED ->
                // NOTE: Protobuf deserialization will not give us the code on the wire
                throw new IllegalArgumentException(
                        "network returned unrecognized response code; your SDK may be out of date");
        };
    }

    @Override
    public String toString() {
        return code.name();
    }
}
