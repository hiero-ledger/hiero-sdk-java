// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.time.Duration;
import java.time.Instant;
import org.hiero.sdk.proto.Timestamp;
import org.hiero.sdk.proto.TimestampSeconds;

/**
 * Instance in time utilities.
 */
final class InstantConverter {
    /**
     * Constructor.
     */
    private InstantConverter() {}

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
