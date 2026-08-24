// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.Timestamp;
import com.hedera.hashgraph.sdk.proto.TimestampSeconds;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Instance in time utilities.
 */
final class InstantConverter {
    /**
     * ISO-8601 formatter
     * Used to produce cross-SDK-identical JSON timestamps.
     */
    private static final DateTimeFormatter JSON_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    /**
     * Constructor.
     */
    private InstantConverter() {}

    /**
     * Format an instant as an ISO-8601 string with millisecond precision
     *
     * @param instant                   the instant
     * @return                          the ISO-8601 string
     */
    static String toJsonString(Instant instant) {
        return JSON_TIMESTAMP_FORMATTER.format(instant.truncatedTo(ChronoUnit.MILLIS));
    }

    /**
     * Create an instance from a timestamp protobuf.
     *
     * @param timestamp                 the protobuf
     * @return                          the instance
     */
    static Instant fromProtobuf(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    /**
     * Create an instance from a timestamp in seconds protobuf.
     *
     * @param timestampSeconds          the protobuf
     * @return                          the instance
     */
    static Instant fromProtobuf(TimestampSeconds timestampSeconds) {
        return Instant.ofEpochSecond(timestampSeconds.getSeconds());
    }

    /**
     * Convert an instance into a timestamp.
     *
     * @param instant                   the instance
     * @return                          the timestamp
     */
    static Timestamp toProtobuf(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    static Timestamp toProtobuf(Duration duration) {
        return Timestamp.newBuilder()
                .setSeconds(duration.getSeconds())
                .setNanos(duration.getNano())
                .build();
    }

    /**
     * Convert an instance into a timestamp in seconds.
     *
     * @param instant                   the instance
     * @return                          the timestamp in seconds
     */
    static TimestampSeconds toSecondsProtobuf(Instant instant) {
        return TimestampSeconds.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .build();
    }
}
