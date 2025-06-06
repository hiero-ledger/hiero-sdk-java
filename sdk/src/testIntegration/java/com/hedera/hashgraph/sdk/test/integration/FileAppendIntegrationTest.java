// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FileAppendIntegrationTest {
    @Test
    @DisplayName("Can append to file")
    void canAppendToFile() throws Exception {
        // There are potential bugs in FileAppendTransaction which require more than one node to trigger.
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

            new FileAppendTransaction()
                    .setFileId(fileId)
                    .setContents("[e2e::FileAppendTransaction]")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(56);
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
    @DisplayName("Can append large contents to file")
    void canAppendLargeContentsToFile() throws Exception {
        // There are potential bugs in FileAppendTransaction which require more than one node to trigger.
        try (var testEnv = new IntegrationTestEnv(2)) {

            // Skip if using local node.
            // Note: this check should be removed once the local node is supporting multiple nodes.
            testEnv.assumeNotLocalNode();

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            Thread.sleep(5000);

            var info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(28);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNotNull();
            assertThat(info.keys.getThreshold()).isNull();
            assertThat(info.keys).isEqualTo(KeyList.of(testEnv.operatorKey));

            new FileAppendTransaction()
                    .setFileId(fileId)
                    .setContents(Contents.BIG_CONTENTS)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var contents = new FileContentsQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(contents.toStringUtf8()).isEqualTo("[e2e::FileCreateTransaction]" + Contents.BIG_CONTENTS);

            info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(13522);
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
    @DisplayName("Can append large contents to file despite TRANSACTION_EXPIRATION response codes")
    void canAppendLargeContentsToFileDespiteExpiration() throws Exception {
        // There are potential bugs in FileAppendTransaction which require more than one node to trigger.
        try (var testEnv = new IntegrationTestEnv(2)) {

            // Skip if using local node.
            // Note: this check should be removed once the local node is supporting multiple nodes.
            testEnv.assumeNotLocalNode();

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            Thread.sleep(5000);

            var info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(28);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNotNull();
            assertThat(info.keys.getThreshold()).isNull();
            assertThat(info.keys).isEqualTo(KeyList.of(testEnv.operatorKey));

            var appendTx = new FileAppendTransaction()
                    .setFileId(fileId)
                    .setContents(Contents.BIG_CONTENTS)
                    .setTransactionValidDuration(Duration.ofSeconds(25))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var contents = new FileContentsQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(contents.toStringUtf8()).isEqualTo("[e2e::FileCreateTransaction]" + Contents.BIG_CONTENTS);

            info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(13522);
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

    /**
     * Integration test for FileAppendTransaction with AddSignature for multiple nodes.
     * This test mirrors the Go TestIntegrationFileAppendTransactionSignForMultipleNodes test.
     * Tests the HSM signing workflow with chunked file append across multiple nodes.
     */
    @Test
    @DisplayName("FileAppend with addSignature - can sign for multiple nodes with large content")
    void canFileAppendSignForMultipleNodes() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var newKey = PrivateKey.generateED25519();

            var createTransaction = new FileCreateTransaction()
                    .setKeys(newKey.getPublicKey())
                    .setNodeAccountIds(
                            testEnv.client.getNetwork().values().stream().toList())
                    .setContents("Hello")
                    .setTransactionMemo("java sdk e2e tests")
                    .freezeWith(testEnv.client)
                    .sign(newKey);

            var createResponse = createTransaction.execute(testEnv.client);
            var createReceipt = createResponse.getReceipt(testEnv.client);
            var fileId = Objects.requireNonNull(createReceipt.fileId);

            assertThat(fileId).isNotNull();

            // Create file append transaction with large content that will be chunked
            var appendTransaction = new FileAppendTransaction()
                    .setFileId(fileId)
                    .setContents(Contents.BIG_CONTENTS)
                    .freezeWith(testEnv.client);

            var signableBodyList = appendTransaction.getSignableNodeBodyBytesList();
            assertThat(signableBodyList).isNotEmpty();

            for (var signableBody : signableBodyList) {
                // External signing simulation (like HSM)
                byte[] signature = newKey.sign(signableBody.getBody());

                // Add signature back to transaction using addSignature
                appendTransaction = appendTransaction.addSignature(
                        newKey.getPublicKey(), signature, signableBody.getTransactionID(), signableBody.getNodeID());
            }

            // Step 6: Execute the file append transaction
            var appendResponse = appendTransaction.execute(testEnv.client);
            var appendReceipt = appendResponse.getReceipt(testEnv.client);

            assertThat(appendReceipt.status).isEqualTo(Status.SUCCESS);

            var contents = new FileContentsQuery()
                    .setFileId(fileId)
                    .setNodeAccountIds(Arrays.asList(appendResponse.nodeId))
                    .execute(testEnv.client);

            var expectedContent = "Hello" + Contents.BIG_CONTENTS;
            assertThat(contents.toStringUtf8()).isEqualTo(expectedContent);
            assertThat(contents.size()).isEqualTo(expectedContent.length());

            var info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);
            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(expectedContent.length());
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNotNull();
            assertThat(info.keys).isEqualTo(KeyList.of(newKey.getPublicKey()));

            // Cleanup - delete the file
            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .freezeWith(testEnv.client)
                    .sign(newKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }
}
