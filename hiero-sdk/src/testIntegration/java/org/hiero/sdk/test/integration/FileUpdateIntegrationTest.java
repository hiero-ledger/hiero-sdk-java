// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Objects;
import org.hiero.sdk.AccountId;
import org.hiero.sdk.FileCreateTransaction;
import org.hiero.sdk.FileDeleteTransaction;
import org.hiero.sdk.FileId;
import org.hiero.sdk.FileInfoQuery;
import org.hiero.sdk.FileUpdateTransaction;
import org.hiero.sdk.KeyList;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FileUpdateIntegrationTest {
    @Test
    @DisplayName("Can update file")
    void canUpdateFile() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            var info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(28);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNotNull();
            assertThat(info.keys.getThreshold()).isNull();
            assertThat(info.keys).isEqualTo(KeyList.of(testEnv.operatorKey));

            new FileUpdateTransaction()
                    .setFileId(fileId)
                    .setContents("[e2e::FileUpdateTransaction]")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(28);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNotNull();
            assertThat(info.keys.getThreshold()).isNull();
            assertThat(info.keys).isEqualTo(KeyList.of(testEnv.operatorKey));

            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot update immutable file")
    void cannotUpdateImmutableFile() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new FileCreateTransaction()
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            var info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(28);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNull();

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new FileUpdateTransaction()
                                .setFileId(fileId)
                                .setContents("[e2e::FileUpdateTransaction]")
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.UNAUTHORIZED.toString());
        }
    }

    @Test
    @DisplayName("Cannot update file when file ID is not set")
    void cannotUpdateFileWhenFileIDIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new FileUpdateTransaction()
                                .setContents("[e2e::FileUpdateTransaction]")
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_FILE_ID.toString());
        }
    }

    @Test
    @DisplayName("Can update fee schedule file")
    void canUpdateFeeScheduleFile() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            testEnv.client.setOperator(
                    new AccountId(0, 0, 2),
                    PrivateKey.fromString(
                            "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137"));

            var fileId = new FileId(0, 0, 111);
            var receipt = new FileUpdateTransaction()
                    .setFileId(fileId)
                    .setContents("[e2e::FileUpdateTransaction]")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThat(receipt.status).isEqualTo(Status.FEE_SCHEDULE_FILE_PART_UPLOADED);
        }
    }
}
