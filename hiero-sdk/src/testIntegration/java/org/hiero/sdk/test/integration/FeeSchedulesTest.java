// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import org.hiero.sdk.FeeSchedules;
import org.hiero.sdk.FileContentsQuery;
import org.hiero.sdk.FileId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FeeSchedulesTest {
    @Test
    @DisplayName("FeeSchedules (CurrentAndNextFeeSchedule) is fetched and parsed from file 0.0.111")
    void canFetchFeeSchedules() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // note: is flaky in localnode env
            testEnv.assumeNotLocalNode();
            ByteString feeSchedulesBytes =
                    new FileContentsQuery().setFileId(new FileId(0, 0, 111)).execute(testEnv.client);

            FeeSchedules feeSchedules = FeeSchedules.fromBytes(feeSchedulesBytes.toByteArray());

            /*
             * Test whether the file 0.0.111 actually contains stuff
             */
            assertThat(feeSchedules.getCurrent()).isNotNull();
        }
    }
}
