// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.AccountID;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Regression tests for the proposed PR #288 profile; maintainer agreement is still required. */
class TransactionBodyHashTest {
    private static final AccountId PAYER = new AccountId(1001);
    private static final List<AccountId> NODES = List.of(new AccountId(3), new AccountId(4));
    private static final Instant VALID_START = Instant.ofEpochSecond(1_700_000_000L, 123);

    private static JsonNode fixtures() throws IOException {
        return fixtures("transaction-body-hash-vectors.json");
    }

    private static JsonNode fixtures(String filename) throws IOException {
        try (var input = TransactionBodyHashTest.class.getResourceAsStream("/" + filename)) {
            assertThat(input).as("exact shared candidate fixtures").isNotNull();
            return new ObjectMapper().readTree(input);
        }
    }

    private static JsonNode vector(String name) throws IOException {
        for (var vector : fixtures().get("vectors")) {
            if (name.equals(vector.get("name").asText())) {
                return vector;
            }
        }
        throw new IllegalArgumentException("Unknown fixture: " + name);
    }

    private static Stream<Arguments> sharedSuccesses() throws IOException {
        var result = new ArrayList<Arguments>();
        for (var vector : fixtures().get("vectors")) {
            if (vector.has("expectedHashHex")) {
                result.add(Arguments.of(vector.get("name").asText(), vector));
            }
        }
        return result.stream();
    }

    private static Stream<Arguments> sharedRejections() throws IOException {
        var result = new ArrayList<Arguments>();
        for (var vector : fixtures().get("vectors")) {
            if (vector.has("expectedError")) {
                result.add(Arguments.of(vector.get("name").asText(), vector));
            }
        }
        return result.stream();
    }

    @Test
    void consumesAll35SharedVectors() throws IOException {
        assertThat(fixtures().get("profile").asText()).isEqualTo("transaction-body-hash-proposed-v1");
        assertThat(fixtures().get("vectors").size()).isEqualTo(35);
        assertThat(sharedSuccesses().count()).isEqualTo(20);
        assertThat(sharedRejections().count()).isEqualTo(15);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sharedSuccesses")
    void sharedDigestSurvivesImportFreezeRepeatedHashAndRoundtrip(String name, JsonNode vector) throws Exception {
        assertSharedDigestRoundtrip(name, vector);
    }

    private static void assertSharedDigestRoundtrip(String name, JsonNode vector) throws Exception {
        var input = Hex.decode(vector.get("transactionListHex").asText());
        var expectedEnvelopes = envelopes(input);
        var transaction = Transaction.fromBytes(input).freeze();

        // Import may regroup entries; the original bodies and signature maps must survive freeze.
        assertThat(preparedBodies(transaction))
                .as(name)
                .containsExactlyInAnyOrderElementsOf(expectedEnvelopes.stream()
                        .map(SignedTransaction::getBodyBytes)
                        .collect(Collectors.toList()));
        var before = transaction.toBytes();
        assertThat(envelopes(before).stream().map(SignedTransaction::getSigMap).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(expectedEnvelopes.stream()
                        .map(SignedTransaction::getSigMap)
                        .collect(Collectors.toList()));

        byte[] hash = transaction.getTransactionBodyHash();

        assertThat(hash)
                .hasSize(48)
                .isEqualTo(Hex.decode(vector.get("expectedHashHex").asText()));
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
        assertThat(transaction.toBytes()).isEqualTo(before);
        var roundtrip = Transaction.fromBytes(transaction.toBytes()).freeze();
        assertThat(preparedBodies(roundtrip)).containsExactlyElementsOf(preparedBodies(transaction));
        assertThat(roundtrip.getTransactionBodyHash()).isEqualTo(hash);
        assertThat(roundtrip.toBytes()).isEqualTo(before);
        if (vector.has("sameHashAs")) {
            assertThat(hash)
                    .isEqualTo(importFixture(vector.get("sameHashAs").asText()).getTransactionBodyHash());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sharedRejections")
    void sharedInvalidInputIsRejectedThroughPublicApi(String name, JsonNode vector) {
        assertRejected(Hex.decode(vector.get("transactionListHex").asText()));
    }

    private static Stream<Arguments> identifierVectors() throws IOException {
        var result = new ArrayList<Arguments>();
        for (var vector :
                fixtures("transaction-body-hash-identifier-vectors.json").get("vectors")) {
            result.add(Arguments.of(vector.get("name").asText(), vector));
        }
        return result.stream();
    }

    @Test
    void consumesSeparateFiveIdentifierBoundaryVectors() throws IOException {
        var vectors = fixtures("transaction-body-hash-identifier-vectors.json").get("vectors");
        var successes = 0;
        var rejections = 0;
        for (var vector : vectors) {
            successes += vector.has("expectedHashHex") ? 1 : 0;
            rejections += vector.has("expectedError") ? 1 : 0;
        }
        assertThat(vectors.size()).isEqualTo(5);
        assertThat(successes).isEqualTo(1);
        assertThat(rejections).isEqualTo(4);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("identifierVectors")
    void identifierBoundaryVectorsUsePublicImportFreezeHashAndRoundtrip(String name, JsonNode vector) throws Exception {
        if (vector.has("expectedError")) {
            assertRejected(Hex.decode(vector.get("transactionListHex").asText()));
        } else {
            assertSharedDigestRoundtrip(name, vector);
        }
    }

    private static Stream<Arguments> nestedIdentifierRejections() throws IOException {
        var result = new ArrayList<Arguments>();
        for (var identifier : List.of("transaction-id", "account-id", "timestamp")) {
            for (var record : List.of("a0868080800001", "a20681808080800078", "a306a406", "a006ffffffffffffffffff02")) {
                result.add(
                        Arguments.of(identifier, record, listOfBodies(identifierBody(identifier, Hex.decode(record)))));
            }
        }
        return result.stream();
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("nestedIdentifierRejections")
    void malformedNestedTransactionAccountAndTimestampRecordsAreRejected(
            String identifier, String record, byte[] input) {
        assertRejected(input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"node-id", "transaction-id", "account-id", "timestamp"})
    void acceptedIdentifierRecordsPreserveNonminimalValuesAndOpaqueUnknownPayloads(String identifier) throws Exception {
        for (var record : List.of(
                Hex.decode("a00681808080808080808000"), field(100, Hex.decode("00a306a406ffffffffffffffffff02")))) {
            var raw = identifierBody(identifier, record);
            var transaction = Transaction.fromBytes(listOfBodies(raw)).freeze();
            var bytes = transaction.toBytes();
            var hash = transaction.getTransactionBodyHash();

            assertThat(preparedBodies(transaction)).containsExactly(ByteString.copyFrom(raw));
            assertThat(hash).isEqualTo(referenceOrdinaryHash(List.of(ByteString.copyFrom(raw))));
            assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
            assertThat(transaction.toBytes()).isEqualTo(bytes);
            var roundtrip = Transaction.fromBytes(bytes).freeze();
            assertThat(preparedBodies(roundtrip)).containsExactly(ByteString.copyFrom(raw));
            assertThat(roundtrip.getTransactionBodyHash()).isEqualTo(hash);
        }
    }

    @Test
    void operationSubmessagesRetainTheExistingSdkDecoderBehavior() throws Exception {
        var original = ordinaryBody();
        var operation = TransactionBody.parseFrom(original).getCryptoTransfer().toByteArray();
        var raw = replaceMessageField(original, 14, concat(operation, Hex.decode("a306a406")));
        var transaction = Transaction.fromBytes(listOfBodies(raw)).freeze();

        assertThat(preparedBodies(transaction)).containsExactly(ByteString.copyFrom(raw));
        assertThat(transaction.getTransactionBodyHash())
                .isEqualTo(referenceOrdinaryHash(List.of(ByteString.copyFrom(raw))));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void freshOrdinaryHashIncludesEveryNodeAndAlwaysUsesTheOuterHash(int nodeCount) throws Exception {
        var transaction = ordinary(NODES.subList(0, nodeCount)).freeze();
        var bodies = preparedBodies(transaction);

        byte[] hash = transaction.getTransactionBodyHash();

        assertThat(bodies).hasSize(nodeCount);
        assertThat(hash).hasSize(48).isEqualTo(referenceOrdinaryHash(bodies));
        if (nodeCount == 1) {
            assertThat(hash).isNotEqualTo(sha384(bodies.get(0).toByteArray()));
        }
    }

    @Test
    void freshNodeOrderAndMembershipAffectTheHash() {
        var original = ordinary(NODES).freeze();
        var reversed = ordinary(List.of(NODES.get(1), NODES.get(0))).freeze();
        var extraNode =
                ordinary(List.of(NODES.get(0), NODES.get(1), new AccountId(5))).freeze();

        assertThat(preparedBodies(extraNode).subList(0, 2)).containsExactlyElementsOf(preparedBodies(original));
        assertThat(reversed.getTransactionBodyHash()).isNotEqualTo(original.getTransactionBodyHash());
        assertThat(extraNode.getTransactionBodyHash()).isNotEqualTo(original.getTransactionBodyHash());
    }

    @Test
    void freshIdsFeesAndMemoAffectTheHash() {
        var baseline = ordinary(NODES).freeze().getTransactionBodyHash();

        assertThat(new TransferTransaction()
                        .setNodeAccountIds(NODES)
                        .setMaxTransactionFee(new Hbar(1))
                        .setTransactionMemo("review me")
                        .setTransactionId(TransactionId.withValidStart(PAYER, VALID_START.plusSeconds(1)))
                        .freeze()
                        .getTransactionBodyHash())
                .isNotEqualTo(baseline);
        assertThat(ordinary(NODES).setMaxTransactionFee(new Hbar(2)).freeze().getTransactionBodyHash())
                .isNotEqualTo(baseline);
        assertThat(ordinary(NODES).setTransactionMemo("changed").freeze().getTransactionBodyHash())
                .isNotEqualTo(baseline);
    }

    @ParameterizedTest
    @CsvSource({"1, 4", "2, 4", "2, 8"})
    void freshFileAppendIncludesEveryChunkAndNode(int nodeCount, int contentLength) throws Exception {
        var transaction = append("abcdefgh".substring(0, contentLength), NODES.subList(0, nodeCount));
        var bodies = preparedBodies(transaction);

        assertThat(bodies).hasSize(nodeCount * contentLength / 4);
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(referenceOrdinaryHash(bodies));
    }

    @Test
    void changingOnlyTheFinalFileChunkChangesTheHash() {
        var original = append("abcdefgh", NODES);
        var changed = append("abcdEFGH", NODES);

        assertThat(preparedBodies(original).subList(0, 2))
                .containsExactlyElementsOf(preparedBodies(changed).subList(0, 2));
        assertThat(changed.getTransactionBodyHash()).isNotEqualTo(original.getTransactionBodyHash());
    }

    @Test
    void freshTopicMessageIncludesTheFinalChunk() throws Exception {
        var original = topicMessage("abcdefghijkl");
        var changed = topicMessage("abcdefghIJKL");

        assertThat(preparedBodies(original)).hasSize(6);
        assertThat(preparedBodies(original).subList(0, 4))
                .containsExactlyElementsOf(preparedBodies(changed).subList(0, 4));
        assertThat(original.getTransactionBodyHash()).isEqualTo(referenceOrdinaryHash(preparedBodies(original)));
        assertThat(changed.getTransactionBodyHash()).isNotEqualTo(original.getTransactionBodyHash());
    }

    @Test
    void firstChunkAndFirstNodeOrderMatterButLaterNodeOrderAndNodeMajorLayoutDoNot() throws Exception {
        var canonical = importFixture("chunk-major").getTransactionBodyHash();

        assertThat(importFixture("node-major").getTransactionBodyHash()).isEqualTo(canonical);
        assertThat(importFixture("later-node-order-normalized").getTransactionBodyHash())
                .isEqualTo(canonical);
        assertThat(importFixture("chunk-groups-reversed").getTransactionBodyHash())
                .isNotEqualTo(canonical);
        assertThat(importFixture("ordinary-node-order-reversed").getTransactionBodyHash())
                .isNotEqualTo(importFixture("ordinary-multi-node").getTransactionBodyHash());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "chunk-major",
                "later-node-order-normalized",
                "node-major",
                "chunk-groups-reversed",
                "permuted-node-major"
            })
    void executionSelectsEveryDecodedChunkAndNodeWithoutChangingStoredOrderOrBytes(String layout) throws Exception {
        var source = Hex.decode(vector(layout.equals("permuted-node-major") ? "later-node-order-normalized" : layout)
                .get("transactionListHex")
                .asText());
        if (layout.equals("permuted-node-major")) {
            var input = TransactionList.parseFrom(source);
            var reordered = TransactionList.newBuilder();
            for (var index : List.of(0, 2, 1, 3)) {
                reordered.addTransactionList(input.getTransactionList(index));
            }
            source = reordered.build().toByteArray();
        }
        var transaction = Transaction.fromBytes(source).freeze();
        var bodies = transaction.getSignableNodeBodyBytesList();
        var bytes = transaction.toBytes();
        var hash = transaction.getTransactionBodyHash();
        var networkHashes = networkHashes(transaction);

        for (int chunk = 0; chunk < transaction.transactionIds.size(); chunk++) {
            for (int node = 0; node < transaction.nodeAccountIds.size(); node++) {
                transaction.transactionIds.setIndex(chunk);
                transaction.nodeAccountIds.setIndex(node);
                var id = transaction.transactionIds.getCurrent();
                var nodeId = transaction.nodeAccountIds.getCurrent();
                var expected = bodies.stream()
                        .filter(body -> body.getTransactionID().equals(id)
                                && body.getNodeID().equals(nodeId))
                        .findFirst()
                        .orElseThrow();
                var request = transaction.makeRequest();
                var signed = SignedTransaction.parseFrom(request.getSignedTransactionBytes());
                var actual = TransactionBody.parseFrom(signed.getBodyBytes());

                assertThat(TransactionId.fromProtobuf(actual.getTransactionID()))
                        .isEqualTo(id);
                assertThat(AccountId.fromProtobuf(actual.getNodeAccountID())).isEqualTo(nodeId);
                assertThat(signed.getBodyBytes()).isEqualTo(ByteString.copyFrom(expected.getBody()));
                assertThat(transaction.transactionIds.getIndex()).isEqualTo(chunk);
                assertThat(transaction.nodeAccountIds.getIndex()).isEqualTo(node);
            }
        }
        assertThat(transaction.toBytes()).isEqualTo(bytes);
        assertThat(networkHashes(transaction)).isEqualTo(networkHashes);
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
    }

    @Test
    void signaturesCanBeAddedRemovedAndReorderedAfterHashing() {
        var transaction = ordinary(NODES).freeze();
        var firstKey = PrivateKey.generateED25519();
        var secondKey = PrivateKey.generateED25519();
        var originalBodies = preparedBodies(transaction);
        var hash = transaction.getTransactionBodyHash();
        var unsignedBytes = transaction.toBytes();
        var unsignedNetworkHash = transaction.getTransactionHash();

        transaction.sign(firstKey).sign(secondKey);
        var signedBytes = transaction.toBytes();

        assertThat(signedBytes).isNotEqualTo(unsignedBytes);
        assertThat(transaction.getTransactionHash()).isNotEqualTo(unsignedNetworkHash);
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
        assertThat(transaction.getSignatures().values()).allSatisfy(signatures -> assertThat(signatures)
                .containsKeys(firstKey.getPublicKey(), secondKey.getPublicKey()));
        transaction.removeAllSignatures();
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
        transaction.sign(secondKey).sign(firstKey);
        assertThat(transaction.toBytes()).isNotEqualTo(signedBytes);
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
        assertThat(preparedBodies(transaction)).containsExactlyElementsOf(originalBodies);
    }

    @Test
    void batchHashIgnoresOuterAndChildSignaturesWithoutChangingNetworkBytes() throws Exception {
        var unsigned = importFixture("batch-unsigned");
        var signed = importFixture("batch-child-and-outer-signatures");
        var signedBytes = signed.toBytes();
        var networkHash = signed.getTransactionHash();

        assertThat(signed.getTransactionBodyHash()).isEqualTo(unsigned.getTransactionBodyHash());
        assertThat(signed.toBytes()).isEqualTo(signedBytes);
        assertThat(signed.getTransactionHash()).isEqualTo(networkHash);
        signed.sign(PrivateKey.generateED25519());
        assertThat(signed.toBytes()).isNotEqualTo(signedBytes);
        assertThat(signed.getTransactionBodyHash()).isEqualTo(unsigned.getTransactionBodyHash());
    }

    @Test
    void concurrentReviewWaitsForEditedImportedBatchPreparationAndHashesOnlyFinalBodies() throws Exception {
        var transaction = editableBatch();
        var oldHash = Hex.decode(vector("batch-unsigned").get("expectedHashHex").asText());
        transaction.setTransactionMemo("edited before preparation");
        var signerEntered = new CountDownLatch(1);
        var finishSigner = new CountDownLatch(1);
        var hashStarted = new CountDownLatch(1);
        var hashFinished = new CountDownLatch(1);
        var signerHeldMonitor = new AtomicBoolean();
        var key = PrivateKey.generateED25519();
        var child = (Transaction<?>) transaction.getInnerTransactions().get(0);
        child.freeze().signWith(key.getPublicKey(), body -> {
            signerHeldMonitor.set(Thread.holdsLock(transaction));
            signerEntered.countDown();
            awaitPreparation(finishSigner);
            return key.sign(body);
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            var freeze = executor.submit(transaction::freeze);
            awaitPreparation(signerEntered);
            var hash = executor.submit(() -> {
                hashStarted.countDown();
                try {
                    return transaction.getTransactionBodyHash();
                } finally {
                    hashFinished.countDown();
                }
            });
            awaitPreparation(hashStarted);
            assertThat(hashFinished.await(100, TimeUnit.MILLISECONDS))
                    .as("review must wait while the real batch child signer is preparing the outer body")
                    .isFalse();
            assertThat(signerHeldMonitor).isTrue();
            finishSigner.countDown();

            assertThat(freeze.get(5, TimeUnit.SECONDS)).isSameAs(transaction);
            var reviewed = hash.get(5, TimeUnit.SECONDS);
            assertThat(reviewed).isNotEqualTo(oldHash);
            assertThat(transaction.getTransactionBodyHash()).isEqualTo(reviewed);
            assertThat(Transaction.fromBytes(transaction.toBytes()).freeze().getTransactionBodyHash())
                    .isEqualTo(reviewed);
            for (var body : preparedBodies(transaction)) {
                assertThat(TransactionBody.parseFrom(body).getMemo()).isEqualTo("edited before preparation");
            }
        } finally {
            finishSigner.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void reentrantChildSignerCannotReviewIncompleteOuterBatchPreparation() throws Exception {
        var transaction = editableBatch().setTransactionMemo("edited before reentrant review");
        var idsLocked = transaction.transactionIds.isLocked();
        var nodesLocked = transaction.nodeAccountIds.isLocked();
        var callbacks = new AtomicInteger();
        var key = PrivateKey.generateED25519();
        var child = (Transaction<?>) transaction.getInnerTransactions().get(0);
        child.freeze().signWith(key.getPublicKey(), body -> {
            callbacks.incrementAndGet();
            assertThatThrownBy(transaction::getTransactionBodyHash)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("prepar");
            assertThat(transaction.transactionIds.isLocked()).isEqualTo(idsLocked);
            assertThat(transaction.nodeAccountIds.isLocked()).isEqualTo(nodesLocked);
            assertThat(transaction.isBodyHashLocked()).isFalse();
            return key.sign(body);
        });

        transaction.freeze();

        assertThat(callbacks).hasValue(1);
        var reviewed = transaction.getTransactionBodyHash();
        assertThat(reviewed)
                .isNotEqualTo(Hex.decode(
                        vector("batch-unsigned").get("expectedHashHex").asText()));
        var bytes = transaction.toBytes();
        assertThat(transaction.freezeWith(null)).isSameAs(transaction);
        assertThat(transaction.toBytes()).isEqualTo(bytes);
        assertThat(callbacks).hasValue(1);
        transaction.sign(PrivateKey.generateED25519()).toBytes();
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(reviewed);
        assertThat(Transaction.fromBytes(transaction.toBytes()).freeze().getTransactionBodyHash())
                .isEqualTo(reviewed);
    }

    @Test
    void throwingPreparationLeavesImportedBatchUnfrozenWithOriginalSourceAndCanRetry() throws Exception {
        var transaction = editableBatch().setTransactionMemo("edited before throwing preparation");
        var source = transaction.sourceTransactionBody;
        var bodies = transaction.innerSignedTransactions.stream()
                .map(SignedTransaction.Builder::getBodyBytes)
                .collect(Collectors.toList());
        var idsLocked = transaction.transactionIds.isLocked();
        var nodesLocked = transaction.nodeAccountIds.isLocked();
        var fail = new AtomicBoolean(true);
        var callbacks = new AtomicInteger();
        var failure = new IllegalStateException("child signer failed during preparation");
        var key = PrivateKey.generateED25519();
        var child = (Transaction<?>) transaction.getInnerTransactions().get(0);
        child.freeze().signWith(key.getPublicKey(), body -> {
            callbacks.incrementAndGet();
            if (fail.get()) {
                throw failure;
            }
            return key.sign(body);
        });

        assertThatThrownBy(transaction::freeze).isSameAs(failure);

        assertThat(transaction.isFrozen()).isFalse();
        assertThatThrownBy(transaction::getTransactionBodyHash).isInstanceOf(IllegalStateException.class);
        assertThat(transaction.isBodyHashLocked()).isFalse();
        assertThat(transaction.transactionIds.isLocked()).isEqualTo(idsLocked);
        assertThat(transaction.nodeAccountIds.isLocked()).isEqualTo(nodesLocked);
        assertThat(transaction.sourceTransactionBody).isSameAs(source);
        assertThat(transaction.innerSignedTransactions.stream()
                        .map(SignedTransaction.Builder::getBodyBytes)
                        .collect(Collectors.toList()))
                .containsExactlyElementsOf(bodies);
        transaction.setTransactionMemo("retry after failed preparation");
        fail.set(false);
        transaction.freeze();

        assertThat(callbacks).hasValue(2);
        var reviewed = transaction.getTransactionBodyHash();
        assertThat(reviewed)
                .isNotEqualTo(Hex.decode(
                        vector("batch-unsigned").get("expectedHashHex").asText()));
        for (var body : preparedBodies(transaction)) {
            assertThat(TransactionBody.parseFrom(body).getMemo()).isEqualTo("retry after failed preparation");
        }
        transaction.sign(PrivateKey.generateED25519()).toBytes();
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(reviewed);
        assertThat(Transaction.fromBytes(transaction.toBytes()).freeze().getTransactionBodyHash())
                .isEqualTo(reviewed);
    }

    private static BatchTransaction editableBatch() throws Exception {
        var transaction = (BatchTransaction) Transaction.fromBytes(
                Hex.decode(vector("batch-unsigned").get("transactionListHex").asText()));
        assertThat(transaction.isFrozen()).isFalse();
        return transaction;
    }

    private static void awaitPreparation(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS))
                    .as("batch preparation barrier reached")
                    .isTrue();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

    @Test
    void queuedSerializationPreservesReviewedImportedBodiesAndExternalSignaturesWithoutReplayingSigners()
            throws Exception {
        var transaction = Transaction.fromBytes(
                Hex.decode(vector("ordinary-single").get("transactionListHex").asText()));
        assertThat(transaction.isFrozen()).isFalse();
        transaction.setTransactionMemo("edited before queued serialization");
        var serializerStarted = new CountDownLatch(1);
        var serialized = new FutureTask<byte[]>(() -> {
            serializerStarted.countDown();
            return transaction.toBytes();
        });
        var serializer = new Thread(serialized, "queued-transaction-serializer");
        var externalKey = PrivateKey.generateED25519();
        var deferredKey = PrivateKey.generateED25519();
        var callbacks = new AtomicInteger();
        byte[] reviewed;
        byte[] bytes;
        List<ByteString> bodies;
        List<TransactionId> ids;
        List<AccountId> nodes;
        try {
            synchronized (transaction) {
                serializer.start();
                awaitPreparation(serializerStarted);
                var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (serializer.getState() != Thread.State.BLOCKED
                        || Stream.of(serializer.getStackTrace())
                                .noneMatch(frame -> frame.getClassName().equals(Transaction.class.getName())
                                        && frame.getMethodName().equals("prepareTransactionBodies"))) {
                    if (System.nanoTime() >= deadline) {
                        throw new AssertionError("serializer did not queue at the transaction preparation monitor");
                    }
                    Thread.onSpinWait();
                }

                transaction.freeze();
                reviewed = transaction.getTransactionBodyHash();
                bodies = preparedBodies(transaction);
                ids = new ArrayList<>(transaction.transactionIds.getList());
                nodes = new ArrayList<>(transaction.getNodeAccountIds());
                var externalSignature = externalKey.sign(bodies.get(0).toByteArray());
                transaction.addSignature(externalKey.getPublicKey(), externalSignature);
                assertThat(transaction.sigPairLists.get(0).getSigPairList())
                        .containsExactly(externalKey.getPublicKey().toSignaturePairProtobuf(externalSignature));
                transaction.signWith(deferredKey.getPublicKey(), body -> {
                    callbacks.incrementAndGet();
                    return deferredKey.sign(body);
                });
                bytes = transaction.toBytes();
                assertThat(envelopes(bytes).get(0).getSigMap().getSigPairCount())
                        .isEqualTo(2);
                assertThat(callbacks).hasValue(1);
            }

            var queuedBytes = serialized.get(5, TimeUnit.SECONDS);
            assertThat(envelopes(queuedBytes).get(0).getSigMap())
                    .as("queued serialization must retain the distinct external signature and the deferred signature")
                    .isEqualTo(envelopes(bytes).get(0).getSigMap());
            assertThat(queuedBytes).isEqualTo(bytes);
            for (int attempt = 0; attempt < 3; attempt++) {
                assertThat(transaction.toBytes()).isEqualTo(bytes);
                assertThat(transaction.getTransactionBodyHash()).isEqualTo(reviewed);
                assertThat(preparedBodies(transaction)).containsExactlyElementsOf(bodies);
                assertThat(transaction.transactionIds.getList()).containsExactlyElementsOf(ids);
                assertThat(transaction.getNodeAccountIds()).containsExactlyElementsOf(nodes);
                assertThat(transaction.transactionIds.isLocked()).isTrue();
                assertThat(transaction.nodeAccountIds.isLocked()).isTrue();
                assertThat(transaction.isBodyHashLocked()).isTrue();
                assertThat(callbacks).hasValue(1);
            }
        } finally {
            serializer.interrupt();
            serializer.join(TimeUnit.SECONDS.toMillis(5));
            assertThat(serializer.isAlive()).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"sig-map", "algorithm-flag", "unknown-metadata"})
    void batchNodeConsistencyIgnoresChildEnvelopeMetadataAndPreservesWireBytes(String metadata) throws Exception {
        var original = Hex.decode(
                vector("batch-multi-node-unsigned").get("transactionListHex").asText());
        var bodies = envelopes(original).stream()
                .map(SignedTransaction::getBodyBytes)
                .collect(Collectors.toList());
        var children = new ArrayList<>(
                TransactionBody.parseFrom(bodies.get(1)).getAtomicBatch().getTransactionsList());
        var child = SignedTransaction.parseFrom(children.get(0));
        switch (metadata) {
            case "sig-map" -> {
                var signedOuter = TransactionBody.parseFrom(batchBodyFromFixture("batch-child-and-outer-signatures"));
                var signatures = SignedTransaction.parseFrom(
                                signedOuter.getAtomicBatch().getTransactions(0))
                        .getSigMap();
                children.set(0, child.toBuilder().setSigMap(signatures).build().toByteString());
            }
            case "algorithm-flag" ->
                children.set(
                        0,
                        child.toBuilder()
                                .setUseSerializedTxMessageHashAlgorithm(true)
                                .build()
                                .toByteString());
            case "unknown-metadata" ->
                children.set(0, children.get(0).concat(ByteString.copyFrom(Hex.decode("a00601"))));
            default -> throw new AssertionError(metadata);
        }
        assertThat(SignedTransaction.parseFrom(children.get(0)).getBodyBytes()).isEqualTo(child.getBodyBytes());
        var wrapper = concat(children.stream()
                .map(envelope -> field(1, envelope.toByteArray()))
                .toArray(byte[][]::new));
        var changed = withBatchWrapper(bodies.get(1).toByteArray(), wrapper);
        var input = listOfBodies(bodies.get(0).toByteArray(), changed);
        var transaction = Transaction.fromBytes(input).freeze();
        var bytes = transaction.toBytes();
        var networkHashes = networkHashes(transaction);

        assertThat(transaction.getTransactionBodyHash())
                .isEqualTo(
                        Hex.decode(
                                "841a324c41d819fbbe63282a2ffef773a50914e592df000acca1bd6b8de820d2a92558f59657b7a0787a8a1680ea56cd"));
        assertThat(preparedBodies(transaction)).containsExactly(bodies.get(0), ByteString.copyFrom(changed));
        assertThat(transaction.toBytes()).isEqualTo(input).isEqualTo(bytes);
        assertThat(networkHashes(transaction)).isEqualTo(networkHashes);
        var roundtrip = Transaction.fromBytes(bytes).freeze();
        assertThat(roundtrip.toBytes()).isEqualTo(bytes);
        assertThat(roundtrip.getTransactionBodyHash()).isEqualTo(transaction.getTransactionBodyHash());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"child-memo", "child-unknown", "child-reencoded", "child-order", "child-count", "outer-memo"})
    void batchNodeConsistencyStillRejectsChildBodyOrderCountAndOuterContentDifferences(String difference)
            throws Exception {
        var original = Hex.decode(
                vector("batch-multi-node-unsigned").get("transactionListHex").asText());
        var bodies = envelopes(original).stream()
                .map(SignedTransaction::getBodyBytes)
                .collect(Collectors.toList());
        var second = TransactionBody.parseFrom(bodies.get(1));
        var children = new ArrayList<>(second.getAtomicBatch().getTransactionsList());
        var child = SignedTransaction.parseFrom(children.get(0));
        switch (difference) {
            case "child-memo" ->
                children.set(
                        0,
                        child.toBuilder()
                                .setBodyBytes(TransactionBody.parseFrom(child.getBodyBytes()).toBuilder()
                                        .setMemo("changed child")
                                        .build()
                                        .toByteString())
                                .build()
                                .toByteString());
            case "child-unknown" ->
                children.set(
                        0,
                        child.toBuilder()
                                .setBodyBytes(child.getBodyBytes().concat(ByteString.copyFrom(Hex.decode("a00601"))))
                                .build()
                                .toByteString());
            case "child-reencoded" -> {
                var reencoded = TransactionBody.parseFrom(child.getBodyBytes()).toByteString();
                assertThat(reencoded).isNotEqualTo(child.getBodyBytes());
                assertThat(TransactionBody.parseFrom(reencoded))
                        .isEqualTo(TransactionBody.parseFrom(child.getBodyBytes()));
                children.set(
                        0, child.toBuilder().setBodyBytes(reencoded).build().toByteString());
            }
            case "child-order" -> java.util.Collections.reverse(children);
            case "child-count" -> children.remove(1);
            case "outer-memo" -> {}
            default -> throw new AssertionError(difference);
        }
        var wrapper = concat(children.stream()
                .map(envelope -> field(1, envelope.toByteArray()))
                .toArray(byte[][]::new));
        var changed = difference.equals("outer-memo")
                ? second.toBuilder().setMemo("changed outer").build().toByteArray()
                : withBatchWrapper(bodies.get(1).toByteArray(), wrapper);

        assertThatThrownBy(
                        () -> Transaction.fromBytes(listOfBodies(bodies.get(0).toByteArray(), changed)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void freshMultiNodeBatchHashSurvivesChildSignatureCollectionAndDetectsBodyChanges() {
        var batchKey = PrivateKey.generateED25519().getPublicKey();
        var first = child("child A", batchKey, 0);
        var second = child("child B", batchKey, 1);
        var unsigned = batch(List.of(first, second), "outer");
        var hash = unsigned.getTransactionBodyHash();
        var singleNode = new BatchTransaction()
                .setTransactionId(TransactionId.withValidStart(PAYER, VALID_START))
                .setNodeAccountIds(List.of(NODES.get(0)))
                .setTransactionMemo("outer")
                .addInnerTransaction(first)
                .addInnerTransaction(second)
                .freeze();

        assertThat(preparedBodies(singleNode))
                .containsExactly(preparedBodies(unsigned).get(0));
        assertThat(singleNode.getTransactionBodyHash()).isNotEqualTo(hash);

        first.sign(PrivateKey.generateED25519());
        second.sign(PrivateKey.generateED25519());
        var signed = batch(List.of(first, second), "outer");
        signed.sign(PrivateKey.generateED25519());

        assertThat(preparedBodies(unsigned)).hasSize(2);
        assertThat(signed.toBytes()).isNotEqualTo(unsigned.toBytes());
        assertThat(signed.getTransactionBodyHash()).isEqualTo(hash);
        assertThat(batch(List.of(second, first), "outer").getTransactionBodyHash())
                .isNotEqualTo(hash);
        assertThat(batch(List.of(first), "outer").getTransactionBodyHash()).isNotEqualTo(hash);
        assertThat(batch(List.of(first, child("changed", batchKey, 1)), "outer").getTransactionBodyHash())
                .isNotEqualTo(hash);
        assertThat(batch(List.of(first, second), "changed").getTransactionBodyHash())
                .isNotEqualTo(hash);
    }

    @Test
    void deferredSignersAreNotCalledAndChunkAndNodeCursorsArePreserved() {
        var transaction = append("abcdefgh", NODES);
        var key = PrivateKey.generateED25519();
        var signedBodies = new ArrayList<ByteString>();
        transaction.signWith(key.getPublicKey(), body -> {
            signedBodies.add(ByteString.copyFrom(body));
            return key.sign(body);
        });
        var bodies = preparedBodies(transaction);
        transaction.transactionIds.setIndex(1);
        transaction.nodeAccountIds.setIndex(1);

        var hash = transaction.getTransactionBodyHash();

        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
        assertThat(signedBodies).isEmpty();
        assertThat(transaction.transactionIds.getIndex()).isEqualTo(1);
        assertThat(transaction.nodeAccountIds.getIndex()).isEqualTo(1);
        assertThat(transaction.sigPairLists).allSatisfy(signatures -> assertThat(signatures.getSigPairCount())
                .isZero());
        assertThat(preparedBodies(transaction)).containsExactlyElementsOf(bodies);

        transaction.toBytes();

        assertThat(signedBodies).containsExactlyElementsOf(bodies);
        assertThat(transaction.getAllSignatures()).allSatisfy(chunk -> assertThat(chunk.values())
                .allSatisfy(signatures -> assertThat(signatures).containsKey(key.getPublicKey())));
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
        assertThat(transaction.transactionIds.getIndex()).isEqualTo(1);
        assertThat(transaction.nodeAccountIds.getIndex()).isEqualTo(1);
    }

    @Test
    void batchGetterUsesPreparedChildrenWithoutCallingOuterOrChildSigners() {
        var child = child("child", PrivateKey.generateED25519().getPublicKey(), 0);
        var transaction = batch(List.of(child), "outer");
        var calls = new AtomicInteger();
        var outerKey = PrivateKey.generateED25519();
        var childKey = PrivateKey.generateED25519();
        child.signWith(childKey.getPublicKey(), body -> {
            calls.incrementAndGet();
            return childKey.sign(body);
        });
        transaction.signWith(outerKey.getPublicKey(), body -> {
            calls.incrementAndGet();
            return outerKey.sign(body);
        });
        var bodies = preparedBodies(transaction);

        var hash = transaction.getTransactionBodyHash();

        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
        assertThat(calls.get()).isZero();
        assertThat(preparedBodies(transaction)).containsExactlyElementsOf(bodies);
        transaction.toBytes();
        assertThat(calls.get()).isEqualTo(NODES.size());
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
    }

    @Test
    void successfulHashLocksReviewedIdsAndNodesAgainstExpirationRegeneration() {
        var transaction = append("abcdefgh", NODES).setRegenerateTransactionId(true);
        // Simulate the unlocked lists used when execution selects IDs and nodes.
        transaction.transactionIds.setLocked(false);
        transaction.nodeAccountIds.setLocked(false);
        var ids = new ArrayList<>(transaction.transactionIds.getList());
        var nodes = transaction.getNodeAccountIds();
        var bodies = preparedBodies(transaction);

        var hash = transaction.getTransactionBodyHash();

        assertThat(transaction.getExecutionState(Status.TRANSACTION_EXPIRED, null))
                .isEqualTo(ExecutionState.REQUEST_ERROR);
        assertThatThrownBy(() -> transaction.transactionIds.set(0, TransactionId.generate(PAYER)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> transaction.nodeAccountIds.remove(NODES.get(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(transaction.transactionIds.getList()).containsExactlyElementsOf(ids);
        assertThat(transaction.getNodeAccountIds()).containsExactlyElementsOf(nodes);
        assertThat(preparedBodies(transaction)).containsExactlyElementsOf(bodies);
        transaction.sign(PrivateKey.generateED25519()).toBytes();
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
    }

    @Test
    void reviewedBodyCannotBeRegeneratedByReceiptRetry() throws Exception {
        var transaction = ordinary(NODES).freeze().setRegenerateTransactionId(true);
        var hash = transaction.getTransactionBodyHash();
        var ids = new ArrayList<>(transaction.transactionIds.getList());
        var bodies = preparedBodies(transaction);
        try (var client = Client.forTestnet().setOperator(PAYER, PrivateKey.generateED25519())) {
            assertThatThrownBy(() -> transaction.regenerateTransactionId(client))
                    .isInstanceOf(IllegalStateException.class);
        }

        assertThat(transaction.isFrozen()).isTrue();
        assertThat(transaction.transactionIds.getList()).containsExactlyElementsOf(ids);
        assertThat(preparedBodies(transaction)).containsExactlyElementsOf(bodies);
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
    }

    @Test
    void unfrozenGetterRejectsWithoutFreezingOrPreventingSubsequentEdits() {
        var transaction = ordinary(NODES);
        var idsLocked = transaction.transactionIds.isLocked();
        var nodesLocked = transaction.nodeAccountIds.isLocked();

        assertThatThrownBy(() -> transaction.getTransactionBodyHash()).isInstanceOf(IllegalStateException.class);

        assertThat(transaction.isFrozen()).isFalse();
        assertThat(transaction.transactionIds.isLocked()).isEqualTo(idsLocked);
        assertThat(transaction.nodeAccountIds.isLocked()).isEqualTo(nodesLocked);
        transaction.setTransactionMemo("still editable").freeze();
        assertThat(transaction.getTransactionMemo()).isEqualTo("still editable");
        assertThat(transaction.getTransactionBodyHash()).hasSize(48);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-set", "empty-body", "truncated-body", "identifier-wire"})
    void failedHashLeavesPreparedStateSignaturesCursorsAndLocksUnchanged(String corruption) {
        var transaction = append("abcdefgh", NODES);
        var key = PrivateKey.generateED25519();
        var calls = new AtomicInteger();
        transaction.sign(key).toBytes();
        transaction.signWith(PrivateKey.generateED25519().getPublicKey(), body -> {
            calls.incrementAndGet();
            throw new AssertionError("hashing must not sign");
        });
        if (corruption.equals("missing-set")) {
            transaction.innerSignedTransactions.clear();
        } else if (corruption.equals("identifier-wire")) {
            var bytes = transaction.innerSignedTransactions.get(3).getBodyBytes();
            try {
                var node = TransactionBody.parseFrom(bytes).getNodeAccountID().toByteArray();
                transaction
                        .innerSignedTransactions
                        .get(3)
                        .setBodyBytes(ByteString.copyFrom(replaceMessageField(
                                bytes.toByteArray(), 2, concat(node, Hex.decode("a006ffffffffffffffffff02")))));
            } catch (IOException error) {
                throw new AssertionError(error);
            }
        } else {
            transaction
                    .innerSignedTransactions
                    .get(3)
                    .setBodyBytes(
                            corruption.equals("empty-body")
                                    ? ByteString.EMPTY
                                    : ByteString.copyFrom(Hex.decode("0a80")));
        }
        transaction.transactionIds.setLocked(false).setIndex(1);
        transaction.nodeAccountIds.setLocked(false).setIndex(1);
        var signed = transaction.innerSignedTransactions.stream()
                .map(builder -> builder.build())
                .collect(Collectors.toList());
        var outer = new ArrayList<>(transaction.outerTransactions);
        var signatures = transaction.sigPairLists.stream()
                .map(builder -> builder.build())
                .collect(Collectors.toList());
        var ids = new ArrayList<>(transaction.transactionIds.getList());
        var nodes = transaction.getNodeAccountIds();
        var publicKeys = new ArrayList<>(transaction.publicKeys);
        var signers = new ArrayList<>(transaction.signers);

        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> transaction.getTransactionBodyHash())
                    .isInstanceOfAny(IllegalArgumentException.class, IllegalStateException.class);
            assertThat(transaction.innerSignedTransactions.stream()
                            .map(builder -> builder.build())
                            .collect(Collectors.toList()))
                    .containsExactlyElementsOf(signed);
            assertThat(transaction.outerTransactions).containsExactlyElementsOf(outer);
            assertThat(transaction.sigPairLists.stream()
                            .map(builder -> builder.build())
                            .collect(Collectors.toList()))
                    .containsExactlyElementsOf(signatures);
            assertThat(transaction.transactionIds.getList()).containsExactlyElementsOf(ids);
            assertThat(transaction.getNodeAccountIds()).containsExactlyElementsOf(nodes);
            assertThat(transaction.publicKeys).containsExactlyElementsOf(publicKeys);
            assertThat(transaction.signers).containsExactlyElementsOf(signers);
            assertThat(transaction.transactionIds.isLocked()).isFalse();
            assertThat(transaction.nodeAccountIds.isLocked()).isFalse();
            assertThat(transaction.isBodyHashLocked()).isFalse();
            assertThat(transaction.transactionIds.getIndex()).isEqualTo(1);
            assertThat(transaction.nodeAccountIds.getIndex()).isEqualTo(1);
            assertThat(calls.get()).isZero();
        }
    }

    @Test
    void noncanonicalUnknownBodyBytesSurviveUnsignedImportFreezeHashAndRoundtrip() throws Exception {
        // Field 100 precedes known fields and encodes 1 with an overlong varint; the shared body has
        // explicit defaults.
        var raw = concat(Hex.decode("a0068100"), ordinaryBody());
        assertThat(TransactionBody.parseFrom(raw).toByteArray()).isNotEqualTo(raw);
        var transaction = Transaction.fromBytes(listOfBodies(raw));
        assertThat(transaction.isFrozen()).isFalse();
        transaction.freeze();

        assertThat(preparedBodies(transaction)).containsExactly(ByteString.copyFrom(raw));
        assertThat(Hex.toHexString(transaction.getTransactionBodyHash()))
                .isEqualTo(
                        "03e36c904a051776fda31e5f86c5ea9b018f88c87ebacaf7e66d6402ff1fd392691fbbb6ca961b7a1ac0250ebb7f1bea");
        var roundtrip = Transaction.fromBytes(transaction.toBytes()).freeze();
        assertThat(preparedBodies(roundtrip)).containsExactly(ByteString.copyFrom(raw));
        assertThat(roundtrip.getTransactionBodyHash()).isEqualTo(transaction.getTransactionBodyHash());
        assertThat(transaction.getTransactionBodyHash())
                .isNotEqualTo(importFixture("ordinary-single").getTransactionBodyHash());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a08680800001", "a206818080800078", "a00681808080808080808000"})
    void nonminimalTagsLengthsAndValuesWithinProposedWidthsRetainOriginalBytes(String prefix) throws Exception {
        var raw = concat(Hex.decode(prefix), ordinaryBody());
        var transaction = Transaction.fromBytes(listOfBodies(raw)).freeze();

        assertThat(preparedBodies(transaction)).containsExactly(ByteString.copyFrom(raw));
        assertThat(transaction.getTransactionBodyHash())
                .isEqualTo(referenceOrdinaryHash(List.of(ByteString.copyFrom(raw))));
        var roundtrip = Transaction.fromBytes(transaction.toBytes()).freeze();
        assertThat(preparedBodies(roundtrip)).containsExactly(ByteString.copyFrom(raw));
        assertThat(roundtrip.getTransactionBodyHash()).isEqualTo(transaction.getTransactionBodyHash());
    }

    @Test
    void equivalentDecodedBodiesWithDifferentWireEncodingHaveDifferentHashes() throws Exception {
        var raw = ordinaryBody();
        var normalized = TransactionBody.parseFrom(raw).toByteArray();
        assertThat(TransactionBody.parseFrom(normalized)).isEqualTo(TransactionBody.parseFrom(raw));
        var original = Transaction.fromBytes(listOfBodies(raw)).freeze();
        var reencoded = Transaction.fromBytes(listOfBodies(normalized)).freeze();

        assertThat(original.getTransactionBodyHash()).isNotEqualTo(reencoded.getTransactionBodyHash());
    }

    @Test
    void validRepeatedOrdinaryOperationRecordsRetainTheirOriginalBytes() throws Exception {
        var original = ordinaryBody();
        var raw = concat(original, field(14, new byte[0]));
        assertThat(TransactionBody.parseFrom(raw)).isEqualTo(TransactionBody.parseFrom(original));
        var transaction = Transaction.fromBytes(listOfBodies(raw)).freeze();

        assertThat(preparedBodies(transaction)).containsExactly(ByteString.copyFrom(raw));
        assertThat(transaction.getTransactionBodyHash())
                .isEqualTo(referenceOrdinaryHash(List.of(ByteString.copyFrom(raw))))
                .isNotEqualTo(importFixture("ordinary-single").getTransactionBodyHash());
    }

    @Test
    void signatureShapedUnknownBodyFieldsAreHashedAsContent() throws Exception {
        var signatures = envelopes(Hex.decode(
                        vector("ordinary-signed").get("transactionListHex").asText()))
                .get(0)
                .getSigMap()
                .toByteArray();
        var raw = concat(ordinaryBody(), field(100, signatures));
        var transaction = Transaction.fromBytes(listOfBodies(raw)).freeze();

        assertThat(preparedBodies(transaction)).containsExactly(ByteString.copyFrom(raw));
        assertThat(transaction.getTransactionBodyHash())
                .isEqualTo(referenceOrdinaryHash(List.of(ByteString.copyFrom(raw))))
                .isNotEqualTo(importFixture("ordinary-single").getTransactionBodyHash());
    }

    @Test
    void unsignedImportRemainsMutableAndEditedFieldsAreRebuiltAtFreeze() throws Exception {
        var raw = concat(Hex.decode("a0068100"), ordinaryBody());
        var unchangedHash = Transaction.fromBytes(listOfBodies(raw)).freeze().getTransactionBodyHash();
        var transaction = (TransferTransaction) Transaction.fromBytes(listOfBodies(raw));

        transaction.setTransactionMemo("edited before freeze");
        transaction.addHbarTransfer(PAYER, Hbar.fromTinybars(-1));
        transaction.addHbarTransfer(new AccountId(1002), Hbar.fromTinybars(1));
        transaction.freeze();

        var body = TransactionBody.parseFrom(preparedBodies(transaction).get(0));
        assertThat(body.getMemo()).isEqualTo("edited before freeze");
        assertThat(body.getCryptoTransfer().getTransfers().getAccountAmountsCount())
                .isEqualTo(2);
        assertThat(transaction.getTransactionBodyHash()).isNotEqualTo(unchangedHash);
        var roundtrip = (TransferTransaction) Transaction.fromBytes(transaction.toBytes());
        assertThat(roundtrip.getTransactionMemo()).isEqualTo("edited before freeze");
        assertThat(roundtrip.getHbarTransfers()).containsEntry(PAYER, Hbar.fromTinybars(-1));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"memo", "fee", "duration", "contents", "file-id", "chunk-size", "transaction-id", "node-set"})
    void editingAnUnsignedChunkedImportRebuildsTheChangedField(String edit) throws Exception {
        var input = Hex.decode(vector("chunk-major").get("transactionListHex").asText());
        var hash = Transaction.fromBytes(input).freeze().getTransactionBodyHash();
        var transaction = (FileAppendTransaction) Transaction.fromBytes(input);
        switch (edit) {
            case "memo" -> transaction.setTransactionMemo("edited");
            case "fee" -> transaction.setMaxTransactionFee(new Hbar(2));
            case "duration" -> transaction.setTransactionValidDuration(Duration.ofSeconds(180));
            case "contents" -> transaction.setContents("abcdEFGH");
            case "file-id" -> transaction.setFileId(new FileId(1003));
            case "chunk-size" -> transaction.setChunkSize(8);
            case "transaction-id" ->
                transaction.setTransactionId(TransactionId.withValidStart(PAYER, VALID_START.plusSeconds(1)));
            case "node-set" -> transaction.setNodeAccountIds(List.of(new AccountId(3), new AccountId(5)));
            default -> throw new AssertionError(edit);
        }
        transaction.freeze();
        var roundtrip = (FileAppendTransaction)
                Transaction.fromBytes(transaction.toBytes()).freeze();

        assertThat(transaction.getTransactionBodyHash()).isNotEqualTo(hash);
        assertThat(roundtrip.getTransactionBodyHash()).isEqualTo(transaction.getTransactionBodyHash());
        switch (edit) {
            case "memo" -> assertThat(roundtrip.getTransactionMemo()).isEqualTo("edited");
            case "fee" -> assertThat(roundtrip.getMaxTransactionFee()).isEqualTo(new Hbar(2));
            case "duration" ->
                assertThat(roundtrip.getTransactionValidDuration()).isEqualTo(Duration.ofSeconds(180));
            case "contents" -> assertThat(roundtrip.getContents()).isEqualTo(ByteString.copyFromUtf8("abcdEFGH"));
            case "file-id" -> assertThat(roundtrip.getFileId()).isEqualTo(new FileId(1003));
            case "chunk-size" -> assertThat(preparedBodies(roundtrip)).hasSize(2);
            case "transaction-id" ->
                assertThat(roundtrip.getTransactionId())
                        .isEqualTo(TransactionId.withValidStart(PAYER, VALID_START.plusSeconds(1)));
            case "node-set" ->
                assertThat(roundtrip.getNodeAccountIds()).containsExactly(new AccountId(3), new AccountId(5));
            default -> throw new AssertionError(edit);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "modern-list, false",
        "modern-single, false",
        "legacy-single, false",
        "modern-list, true",
        "modern-single, true",
        "legacy-single, true"
    })
    void modernAndSupportedLegacyImportsHashOriginalBodies(String encoding, boolean signed) throws Exception {
        var source = envelopes(Hex.decode(vector(signed ? "ordinary-signed" : "ordinary-single")
                        .get("transactionListHex")
                        .asText()))
                .get(0);
        var envelope = encoding.equals("legacy-single")
                ? com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                        .setBodyBytes(source.getBodyBytes())
                        .setSigMap(source.getSigMap())
                        .build()
                : com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                        .setSignedTransactionBytes(source.toByteString())
                        .build();
        var input = encoding.equals("modern-list")
                ? TransactionList.newBuilder()
                        .addTransactionList(envelope)
                        .build()
                        .toByteArray()
                : envelope.toByteArray();
        var transaction = Transaction.fromBytes(input).freeze();

        assertThat(preparedBodies(transaction)).containsExactly(source.getBodyBytes());
        assertThat(transaction.getTransactionBodyHash())
                .isEqualTo(Hex.decode(
                        vector("ordinary-single").get("expectedHashHex").asText()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ordinary", "chunked", "batch"})
    void hashingPreservesSerializationAndExistingNetworkHashBehavior(String kind) throws Exception {
        Transaction<?> transaction;
        if (kind.equals("chunked")) {
            transaction = append("abcdefgh", NODES);
        } else if (kind.equals("batch")) {
            transaction = importFixture("batch-child-and-outer-signatures");
        } else {
            transaction = ordinary(NODES).freeze().sign(PrivateKey.generateED25519());
        }
        var bytes = transaction.toBytes();
        var networkHashes = networkHashes(transaction);

        var hash = transaction.getTransactionBodyHash();

        assertThat(transaction.toBytes()).isEqualTo(bytes);
        assertThat(networkHashes(transaction)).isEqualTo(networkHashes);
        if (kind.equals("chunked")) {
            assertThatThrownBy(() -> transaction.getTransactionHash()).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> transaction.getTransactionHashPerNode()).isInstanceOf(IllegalStateException.class);
        } else {
            var firstEnvelope =
                    TransactionList.parseFrom(bytes).getTransactionList(0).getSignedTransactionBytes();
            assertThat(transaction.getTransactionHash()).isEqualTo(sha384(firstEnvelope.toByteArray()));
        }
        assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
    }

    @Test
    void unknownOuterBatchBytesAreIncludedAndPreserved() throws Exception {
        var raw = concat(Hex.decode("a0068100"), batchBody());
        var transaction = Transaction.fromBytes(listOfBodies(raw)).freeze();

        assertThat(preparedBodies(transaction)).containsExactly(ByteString.copyFrom(raw));
        assertThat(Hex.toHexString(transaction.getTransactionBodyHash()))
                .isEqualTo(
                        "81bfb451813bc99ddaf4120a3b2dc4a1b027736bcf73bb8239100b01e55f233603d06b37e7f968a104ce0627b4f242f1");
        assertThat(transaction.getTransactionBodyHash())
                .isNotEqualTo(importFixture("batch-unsigned").getTransactionBodyHash());
        var roundtrip = Transaction.fromBytes(transaction.toBytes()).freeze();
        assertThat(preparedBodies(roundtrip)).containsExactly(ByteString.copyFrom(raw));
        assertThat(roundtrip.getTransactionBodyHash()).isEqualTo(transaction.getTransactionBodyHash());
    }

    @Test
    void childEnvelopeMetadataIsIgnoredButChildUnknownBodyBytesRemainContent() throws Exception {
        var body = TransactionBody.parseFrom(batchBody());
        var children = new ArrayList<>(body.getAtomicBatch().getTransactionsList());
        children.set(0, children.get(0).concat(ByteString.copyFrom(Hex.decode("a00601"))));
        var wrapper = concat(
                children.stream().map(child -> field(1, child.toByteArray())).toArray(byte[][]::new));
        var withMetadata =
                Transaction.fromBytes(listOfBodies(withBatchWrapper(wrapper))).freeze();

        assertThat(withMetadata.getTransactionBodyHash())
                .isEqualTo(importFixture("batch-unsigned").getTransactionBodyHash());
        assertThat(importFixture("batch-child-unknown-content").getTransactionBodyHash())
                .isNotEqualTo(withMetadata.getTransactionBodyHash());
    }

    @Test
    void duplicateIdsAreComparedByDecodedValuesRatherThanWireEncoding() throws Exception {
        var raw = ordinaryBody();
        var normalized = TransactionBody.parseFrom(raw).toByteArray();
        assertThat(normalized).isNotEqualTo(raw);
        assertThat(TransactionBody.parseFrom(normalized)).isEqualTo(TransactionBody.parseFrom(raw));

        assertRejected(listOfBodies(raw, normalized));
    }

    private static Stream<Arguments> invalidLayouts() throws Exception {
        var ordinary = TransactionBody.parseFrom(ordinaryBody());
        var chunks = envelopes(Hex.decode(
                        vector("chunk-major").get("transactionListHex").asText()))
                .stream()
                .map(SignedTransaction::getBodyBytes)
                .map(ByteString::toByteArray)
                .collect(Collectors.toList());
        var changedNode = TransactionBody.parseFrom(chunks.get(3)).toBuilder()
                .setNodeAccountID(AccountID.newBuilder().setAccountNum(5))
                .build()
                .toByteArray();
        return Stream.of(
                Arguments.of(
                        "missing transaction ID",
                        listOfBodies(ordinary.toBuilder()
                                .clearTransactionID()
                                .build()
                                .toByteArray())),
                Arguments.of(
                        "missing node ID",
                        listOfBodies(ordinary.toBuilder()
                                .clearNodeAccountID()
                                .build()
                                .toByteArray())),
                Arguments.of(
                        "different later node set",
                        listOfBodies(chunks.get(0), chunks.get(1), chunks.get(2), changedNode)),
                Arguments.of(
                        "extra later node",
                        listOfBodies(chunks.get(0), chunks.get(1), chunks.get(2), chunks.get(3), changedNode)),
                Arguments.of(
                        "empty transaction list",
                        TransactionList.getDefaultInstance().toByteArray()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidLayouts")
    void invalidPreparedLayoutsAreRejected(String name, byte[] input) {
        assertRejected(input);
    }

    private static Stream<Arguments> malformedBodies() throws Exception {
        var ordinary = ordinaryBody();
        var outer = batchBody();
        var child = TransactionBody.parseFrom(outer)
                .getAtomicBatch()
                .getTransactions(0)
                .toByteArray();
        var childBody = SignedTransaction.parseFrom(child).getBodyBytes().toByteArray();
        return Stream.of(
                Arguments.of("truncated tag", listOfBodies(concat(ordinary, Hex.decode("80")))),
                Arguments.of("invalid field zero", listOfBodies(concat(ordinary, Hex.decode("00")))),
                Arguments.of("truncated length", listOfBodies(concat(ordinary, Hex.decode("a20605ff")))),
                Arguments.of("truncated fixed64", listOfBodies(concat(ordinary, Hex.decode("a10601")))),
                Arguments.of("truncated fixed32", listOfBodies(concat(ordinary, Hex.decode("a50601")))),
                Arguments.of(
                        "overflowing varint", listOfBodies(concat(ordinary, Hex.decode("a006ffffffffffffffffff02")))),
                Arguments.of("conflicting ordinary operations", listOfBodies(concat(ordinary, field(16, new byte[0])))),
                Arguments.of("operation before batch", listOfBodies(concat(field(14, new byte[0]), outer))),
                Arguments.of("operation after batch", listOfBodies(concat(outer, field(14, new byte[0])))),
                Arguments.of("mistyped batch wrapper child", listOfBodies(withBatchWrapper(Hex.decode("0801")))),
                Arguments.of("truncated wrapper", listOfBodies(withBatchWrapper(Hex.decode("0aff")))),
                Arguments.of("empty child envelope", listOfBodies(withBatchWrapper(field(1, new byte[0])))),
                Arguments.of(
                        "invalid child body wire",
                        listOfBodies(withBatchWrapper(field(1, field(1, Hex.decode("0a80")))))),
                Arguments.of(
                        "child without operation",
                        listOfBodies(withBatchWrapper(field(
                                1,
                                field(
                                        1,
                                        TransactionBody.parseFrom(childBody).toBuilder()
                                                .clearCryptoTransfer()
                                                .build()
                                                .toByteArray()))))),
                Arguments.of(
                        "child without transaction ID",
                        listOfBodies(withBatchWrapper(field(
                                1,
                                field(
                                        1,
                                        TransactionBody.parseFrom(childBody).toBuilder()
                                                .clearTransactionID()
                                                .build()
                                                .toByteArray()))))),
                Arguments.of(
                        "child without node ID",
                        listOfBodies(withBatchWrapper(field(
                                1,
                                field(
                                        1,
                                        TransactionBody.parseFrom(childBody).toBuilder()
                                                .clearNodeAccountID()
                                                .build()
                                                .toByteArray()))))),
                Arguments.of(
                        "conflicting child operations",
                        listOfBodies(withBatchWrapper(field(1, field(1, concat(childBody, field(16, new byte[0]))))))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedBodies")
    void malformedOrUnsupportedWireContentIsRejected(String name, byte[] input) {
        assertRejected(input);
    }

    private static void assertRejected(byte[] input) {
        assertThatThrownBy(() -> Transaction.fromBytes(input).freeze().getTransactionBodyHash())
                .isInstanceOfAny(
                        InvalidProtocolBufferException.class,
                        IllegalArgumentException.class,
                        IllegalStateException.class);
    }

    private static TransferTransaction ordinary(List<AccountId> nodes) {
        return new TransferTransaction()
                .setTransactionId(TransactionId.withValidStart(PAYER, VALID_START))
                .setNodeAccountIds(nodes)
                .setMaxTransactionFee(new Hbar(1))
                .setTransactionMemo("review me");
    }

    private static FileAppendTransaction append(String contents, List<AccountId> nodes) {
        return new FileAppendTransaction()
                .setFileId(new FileId(1002))
                .setChunkSize(4)
                .setContents(contents)
                .setTransactionId(TransactionId.withValidStart(PAYER, VALID_START))
                .setNodeAccountIds(nodes)
                .freeze();
    }

    private static TopicMessageSubmitTransaction topicMessage(String message) {
        return new TopicMessageSubmitTransaction()
                .setTopicId(new TopicId(1002))
                .setChunkSize(4)
                .setMessage(message)
                .setTransactionId(TransactionId.withValidStart(PAYER, VALID_START))
                .setNodeAccountIds(NODES)
                .freeze();
    }

    private static TransferTransaction child(String memo, Key batchKey, int offset) {
        return new TransferTransaction()
                .setNodeAccountIds(List.of(new AccountId(0)))
                .setMaxTransactionFee(new Hbar(1))
                .setTransactionId(TransactionId.withValidStart(PAYER, VALID_START.plusNanos(offset)))
                .setTransactionMemo(memo)
                .setBatchKey(batchKey)
                .freeze();
    }

    private static BatchTransaction batch(List<TransferTransaction> children, String memo) {
        var batch = new BatchTransaction()
                .setTransactionId(TransactionId.withValidStart(PAYER, VALID_START))
                .setNodeAccountIds(NODES)
                .setTransactionMemo(memo);
        children.forEach(batch::addInnerTransaction);
        return batch.freeze();
    }

    private static Transaction<?> importFixture(String name) throws Exception {
        return Transaction.fromBytes(
                        Hex.decode(vector(name).get("transactionListHex").asText()))
                .freeze();
    }

    private static List<ByteString> preparedBodies(Transaction<?> transaction) {
        return transaction.getSignableNodeBodyBytesList().stream()
                .map(body -> ByteString.copyFrom(body.getBody()))
                .collect(Collectors.toList());
    }

    private static List<SignedTransaction> envelopes(byte[] serialized) throws InvalidProtocolBufferException {
        var result = new ArrayList<SignedTransaction>();
        for (var transaction : TransactionList.parseFrom(serialized).getTransactionListList()) {
            result.add(SignedTransaction.parseFrom(transaction.getSignedTransactionBytes()));
        }
        return result;
    }

    private static byte[] ordinaryBody() throws IOException {
        return envelopes(Hex.decode(
                        vector("ordinary-single").get("transactionListHex").asText()))
                .get(0)
                .getBodyBytes()
                .toByteArray();
    }

    private static byte[] batchBody() throws IOException {
        return batchBodyFromFixture("batch-unsigned");
    }

    private static byte[] batchBodyFromFixture(String name) throws IOException {
        return envelopes(Hex.decode(vector(name).get("transactionListHex").asText()))
                .get(0)
                .getBodyBytes()
                .toByteArray();
    }

    private static byte[] listOfBodies(byte[]... bodies) {
        var list = TransactionList.newBuilder();
        for (var body : bodies) {
            list.addTransactionList(com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                    .setSignedTransactionBytes(SignedTransaction.newBuilder()
                            .setBodyBytes(ByteString.copyFrom(body))
                            .build()
                            .toByteString()));
        }
        return list.build().toByteArray();
    }

    private static byte[] withBatchWrapper(byte[] wrapper) throws IOException {
        return withBatchWrapper(batchBody(), wrapper);
    }

    private static byte[] withBatchWrapper(byte[] raw, byte[] wrapper) throws IOException {
        var input = CodedInputStream.newInstance(raw);
        var outer = new ByteArrayOutputStream();
        while (!input.isAtEnd()) {
            var start = input.getTotalBytesRead();
            var tag = input.readTag();
            input.skipField(tag);
            if (tag >>> 3 != 74) {
                outer.write(raw, start, input.getTotalBytesRead() - start);
            }
        }
        return concat(outer.toByteArray(), field(74, wrapper));
    }

    private static byte[] identifierBody(String identifier, byte[] record) throws IOException {
        var raw = ordinaryBody();
        var body = TransactionBody.parseFrom(raw);
        if (identifier.equals("node-id")) {
            return replaceMessageField(raw, 2, concat(body.getNodeAccountID().toByteArray(), record));
        }
        var id = body.getTransactionID();
        var changed = id.toByteArray();
        switch (identifier) {
            case "transaction-id" -> changed = concat(changed, record);
            case "account-id" ->
                changed =
                        replaceMessageField(changed, 2, concat(id.getAccountID().toByteArray(), record));
            case "timestamp" ->
                changed = replaceMessageField(
                        changed, 1, concat(id.getTransactionValidStart().toByteArray(), record));
            default -> throw new AssertionError(identifier);
        }
        return replaceMessageField(raw, 1, changed);
    }

    private static byte[] replaceMessageField(byte[] raw, int number, byte[] replacement) throws IOException {
        var input = CodedInputStream.newInstance(raw);
        var output = new ByteArrayOutputStream();
        var replaced = false;
        while (!input.isAtEnd()) {
            var start = input.getTotalBytesRead();
            var tag = input.readTag();
            input.skipField(tag);
            if (tag >>> 3 == number) {
                output.writeBytes(field(number, replacement));
                replaced = true;
            } else {
                output.write(raw, start, input.getTotalBytesRead() - start);
            }
        }
        assertThat(replaced).isTrue();
        return output.toByteArray();
    }

    private static byte[] field(int number, byte[] payload) {
        var bytes = new byte[CodedOutputStream.computeByteArraySize(number, payload)];
        var output = CodedOutputStream.newInstance(bytes);
        try {
            output.writeByteArray(number, payload);
            output.checkNoSpaceLeft();
        } catch (IOException error) {
            throw new AssertionError(error);
        }
        return bytes;
    }

    private static byte[] concat(byte[]... values) {
        var output = new ByteArrayOutputStream();
        for (var value : values) {
            output.write(value, 0, value.length);
        }
        return output.toByteArray();
    }

    private static byte[] sha384(byte[] input) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-384").digest(input);
    }

    private static byte[] referenceOrdinaryHash(List<ByteString> bodies) throws NoSuchAlgorithmException {
        var entries = new ArrayList<byte[]>();
        for (var body : bodies) {
            entries.add(sha384(body.toByteArray()));
        }
        return sha384(concat(entries.toArray(new byte[0][])));
    }

    private static List<Map<AccountId, String>> networkHashes(Transaction<?> transaction) {
        var hashes = transaction instanceof ChunkedTransaction
                ? ((ChunkedTransaction<?>) transaction).getAllTransactionHashesPerNode()
                : List.of(transaction.getTransactionHashPerNode());
        return hashes.stream()
                .map(chunk -> chunk.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> Hex.toHexString(entry.getValue()))))
                .collect(Collectors.toList());
    }
}
