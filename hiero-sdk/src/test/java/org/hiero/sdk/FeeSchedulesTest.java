// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FeeSchedulesTest {
    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    FeeSchedules spawnFeeSchedulesExample() {
        return new FeeSchedules()
                .setCurrent(new FeeSchedule()
                        .setExpirationTime(Instant.ofEpochSecond(1554158542))
                        .addTransactionFeeSchedule(new TransactionFeeSchedule()
                                .addFee(new FeeData()
                                        .setNodeData(new FeeComponents())
                                        .setNetworkData(
                                                new FeeComponents().setMin(2).setMax(5))
                                        .setServiceData(new FeeComponents()))))
                .setNext(new FeeSchedule()
                        .setExpirationTime(Instant.ofEpochSecond(1554158222))
                        .addTransactionFeeSchedule(new TransactionFeeSchedule()
                                .addFee(new FeeData()
                                        .setNodeData(
                                                new FeeComponents().setMin(1).setMax(2))
                                        .setNetworkData(new FeeComponents())
                                        .setServiceData(new FeeComponents()))));
    }

    @Test
    void shouldSerialize() throws Exception {
        var originalFeeSchedules = spawnFeeSchedulesExample();
        byte[] feeSchedulesBytes = originalFeeSchedules.toBytes();
        var copyFeeSchedules = FeeSchedules.fromBytes(feeSchedulesBytes);
        assertThat(copyFeeSchedules.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .isEqualTo(originalFeeSchedules.toString().replaceAll("@[A-Za-z0-9]+", ""));
        SnapshotMatcher.expect(originalFeeSchedules.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void shouldSerializeNull() throws Exception {
        var originalFeeSchedules = new FeeSchedules();
        byte[] feeSchedulesBytes = originalFeeSchedules.toBytes();
        var copyFeeSchedules = FeeSchedules.fromBytes(feeSchedulesBytes);
        assertThat(copyFeeSchedules.toString()).isEqualTo(originalFeeSchedules.toString());
    }
}
