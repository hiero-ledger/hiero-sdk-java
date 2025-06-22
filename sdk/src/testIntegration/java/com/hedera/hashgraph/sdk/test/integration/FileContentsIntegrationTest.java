// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.FileContentsQuery;
import com.hedera.hashgraph.sdk.FileCreateTransaction;
import com.hedera.hashgraph.sdk.FileDeleteTransaction;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.MaxQueryPaymentExceededException;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.Status;
import java.util.Objects;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FileContentsIntegrationTest {

    @Test
    @DisplayName("Can query file contents")
    void canQueryFileContents() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            var contents = new FileContentsQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(contents.toStringUtf8()).isEqualTo("[e2e::FileCreateTransaction]");

            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can query empty file contents")
    void canQueryEmptyFileContents() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response =
                    new FileCreateTransaction().setKeys(testEnv.operatorKey).execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            var contents = new FileContentsQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(contents.size()).isEqualTo(0);

            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot query file contents when file ID is not set")
    void cannotQueryFileContentsWhenFileIDIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new FileContentsQuery().execute(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_FILE_ID.toString());
        }
    }

    @Test
    @DisplayName("Can get cost, even with a big max")
    void getCostBigMaxQueryFileContents() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            var contentsQuery = new FileContentsQuery().setFileId(fileId).setMaxQueryPayment(new Hbar(1000));

            var contents = contentsQuery.execute(testEnv.client);

            assertThat(contents.toStringUtf8()).isEqualTo("[e2e::FileCreateTransaction]");

            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Error, max is smaller than set payment.")
    void getCostSmallMaxQueryFileContents() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            var contentsQuery = new FileContentsQuery().setFileId(fileId).setMaxQueryPayment(Hbar.fromTinybars(1));

            assertThatExceptionOfType(MaxQueryPaymentExceededException.class).isThrownBy(() -> {
                contentsQuery.execute(testEnv.client);
            });

            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @Disabled
    @DisplayName("Insufficient tx fee error.")
    void getCostInsufficientTxFeeQueryFileContents() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            var contentsQuery = new FileContentsQuery().setFileId(fileId).setMaxQueryPayment(new Hbar(100));

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        contentsQuery.setQueryPayment(Hbar.fromTinybars(1)).execute(testEnv.client);
                    })
                    .satisfies(error -> assertThat(error.status.toString()).isEqualTo("INSUFFICIENT_TX_FEE"));

            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }
}
