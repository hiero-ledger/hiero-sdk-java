// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.base.Stopwatch;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.AccountID;
import com.hedera.hashgraph.sdk.proto.ConsensusMessageChunkInfo;
import com.hedera.hashgraph.sdk.proto.Timestamp;
import com.hedera.hashgraph.sdk.proto.TopicID;
import com.hedera.hashgraph.sdk.proto.TransactionID;
import com.hedera.hashgraph.sdk.proto.mirror.ConsensusServiceGrpc;
import com.hedera.hashgraph.sdk.proto.mirror.ConsensusTopicQuery;
import com.hedera.hashgraph.sdk.proto.mirror.ConsensusTopicResponse;
import io.github.jsonSnapshot.SnapshotMatcher;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TopicMessageQueryTest {

    private static final Instant START_TIME = Instant.now();

    private Client client;
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final List<Throwable> errors = new ArrayList<>();
    private final List<TopicMessage> received = new ArrayList<>();
    private final ConsensusServiceStub consensusServiceStub = new ConsensusServiceStub();
    private Server server;
    private TopicMessageQuery topicMessageQuery;

    @BeforeAll
    static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @BeforeEach
    void setup() throws Exception {
        client = Client.forNetwork(Collections.emptyMap());
        client.setMirrorNetwork(List.of("in-process:test"));
        server = InProcessServerBuilder.forName("test")
                .addService(consensusServiceStub)
                .directExecutor()
                .build()
                .start();
        topicMessageQuery = new TopicMessageQuery();
        topicMessageQuery.setCompletionHandler(() -> complete.set(true));
        topicMessageQuery.setEndTime(START_TIME.plusSeconds(100L));
        topicMessageQuery.setErrorHandler((t, r) -> errors.add(t));
        topicMessageQuery.setMaxBackoff(Duration.ofMillis(500L));
        topicMessageQuery.setStartTime(START_TIME);
        topicMessageQuery.setTopicId(TopicId.fromString("0.0.1000"));
    }

    @AfterEach
    void teardown() throws Exception {
        consensusServiceStub.verify();
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.shutdown();
            server.awaitTermination();
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void setCompletionHandlerNull() {
        assertThatThrownBy(() -> topicMessageQuery.setCompletionHandler(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("completionHandler must not be null");
    }

    @Test
    @SuppressWarnings("NullAway")
    void setEndTimeNull() {
        assertThatThrownBy(() -> topicMessageQuery.setEndTime(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("endTime must not be null");
    }

    @Test
    @SuppressWarnings("NullAway")
    void setErrorHandlerNull() {
        assertThatThrownBy(() -> topicMessageQuery.setErrorHandler(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("errorHandler must not be null");
    }

    @Test
    @SuppressWarnings("NullAway")
    void setMaxAttemptsNegative() {
        assertThatThrownBy(() -> topicMessageQuery.setMaxAttempts(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxAttempts must be positive");
    }

    @Test
    @SuppressWarnings("NullAway")
    void setMaxBackoffNull() {
        assertThatThrownBy(() -> topicMessageQuery.setMaxBackoff(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxBackoff must be at least 500 ms");
    }

    @Test
    void setMaxBackoffLow() {
        assertThatThrownBy(() -> topicMessageQuery.setMaxBackoff(Duration.ofMillis(499L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxBackoff must be at least 500 ms");
    }

    @Test
    @SuppressWarnings("NullAway")
    void setRetryHandlerNull() {
        assertThatThrownBy(() -> topicMessageQuery.setRetryHandler(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("retryHandler must not be null");
    }

    @Test
    @SuppressWarnings("NullAway")
    void setStartTimeNull() {
        assertThatThrownBy(() -> topicMessageQuery.setStartTime(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("startTime must not be null");
    }

    @Test
    @SuppressWarnings("NullAway")
    void setTopicIdNull() {
        assertThatThrownBy(() -> topicMessageQuery.setTopicId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("topicId must not be null");
    }

    @Test
    @Timeout(3)
    void subscribe() {
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.responses.add(response(1L));
        consensusServiceStub.responses.add(response(2L));

        subscribeToMirror(received::add);

        assertThat(errors).isEmpty();
        Assertions.assertThat(received)
                .hasSize(2)
                .extracting(t -> t.sequenceNumber)
                .containsExactly(1L, 2L);
    }

    @Test
    @Timeout(3)
    void subscribeChunked() {
        ConsensusTopicResponse response1 = response(1L, 2);
        ConsensusTopicResponse response2 = response(2L, 2);
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.responses.add(response1);
        consensusServiceStub.responses.add(response2);

        subscribeToMirror(received::add);

        var message = combine(
                response1.getMessage().toByteArray(), response2.getMessage().toByteArray());
        assertThat(errors).isEmpty();
        Assertions.assertThat(received)
                .hasSize(1)
                .first()
                .returns(toInstant(response2.getConsensusTimestamp()), t -> t.consensusTimestamp)
                .returns(
                        response2.getChunkInfo().getInitialTransactionID(),
                        t -> Objects.requireNonNull(t.transactionId).toProtobuf())
                .returns(message, t -> t.contents)
                .returns(response2.getRunningHash().toByteArray(), t -> t.runningHash)
                .returns(response2.getSequenceNumber(), t -> t.sequenceNumber)
                .extracting(t -> t.chunks)
                .asInstanceOf(InstanceOfAssertFactories.ARRAY)
                .hasSize(2)
                .extracting(c -> ((TopicMessageChunk) c).sequenceNumber)
                .contains(1L, 2L);
    }

    @Test
    @Timeout(3)
    void subscribeNoResponse() {
        consensusServiceStub.requests.add(request().build());

        subscribeToMirror(received::add);

        assertThat(errors).isEmpty();
        Assertions.assertThat(received).isEmpty();
    }

    @Test
    @Timeout(3)
    void errorDuringOnNext() {
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.responses.add(response(1L));

        subscribeToMirror(t -> {
            throw new RuntimeException();
        });

        assertThat(errors).hasSize(1).first().isInstanceOf(RuntimeException.class);
        Assertions.assertThat(received).isEmpty();
    }

    @ParameterizedTest(name = "Retry recovers w/ status {0} and description {1}")
    @CsvSource({
        "INTERNAL, internal RST_STREAM error",
        "INTERNAL, rst stream",
        "NOT_FOUND, ",
        "RESOURCE_EXHAUSTED, ",
        "UNAVAILABLE, "
    })
    @Timeout(3)
    void retryRecovers(Status.Code code, String description) {
        ConsensusTopicResponse response = response(1L);
        Instant nextTimestamp = toInstant(response.getConsensusTimestamp()).plusNanos(1L);
        ConsensusTopicQuery.Builder request = request();

        consensusServiceStub.requests.add(request.build());
        consensusServiceStub.requests.add(
                request.setConsensusStartTime(toTimestamp(nextTimestamp)).build());
        consensusServiceStub.responses.add(response);
        consensusServiceStub.responses.add(
                code.toStatus().withDescription(description).asRuntimeException());
        consensusServiceStub.responses.add(response(2L));

        subscribeToMirror(received::add);

        Assertions.assertThat(received)
                .hasSize(2)
                .extracting(t -> t.sequenceNumber)
                .containsExactly(1L, 2L);
        assertThat(errors).isEmpty();
    }

    @ParameterizedTest(name = "No retry w/ status {0} and description {1}")
    @CsvSource({"INTERNAL, internal first_stream error", "INTERNAL, internal error", "INTERNAL, ", "INVALID_ARGUMENT, "
    })
    @Timeout(3)
    void noRetry(Status.Code code, String description) {
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.responses.add(
                code.toStatus().withDescription(description).asRuntimeException());

        subscribeToMirror(received::add);

        Assertions.assertThat(received).isEmpty();
        assertThat(errors)
                .hasSize(1)
                .first()
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(t -> ((StatusRuntimeException) t).getStatus().getCode())
                .isEqualTo(code);
    }

    @Test
    @Timeout(3)
    void customRetry() {
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.responses.add(Status.INVALID_ARGUMENT.asRuntimeException());
        consensusServiceStub.responses.add(response(1L));
        topicMessageQuery.setRetryHandler(t -> true);

        subscribeToMirror(received::add);

        Assertions.assertThat(received)
                .hasSize(1)
                .extracting(t -> t.sequenceNumber)
                .containsExactly(1L);
        assertThat(errors).isEmpty();
    }

    @Test
    @Timeout(3)
    void retryWithLimit() {
        ConsensusTopicResponse response = response(1L);
        Instant nextTimestamp = toInstant(response.getConsensusTimestamp()).plusNanos(1L);
        ConsensusTopicQuery.Builder request = request();
        topicMessageQuery.setLimit(2);

        consensusServiceStub.requests.add(request.setLimit(2L).build());
        consensusServiceStub.requests.add(request.setConsensusStartTime(toTimestamp(nextTimestamp))
                .setLimit(1L)
                .build());
        consensusServiceStub.responses.add(response);
        consensusServiceStub.responses.add(Status.RESOURCE_EXHAUSTED.asRuntimeException());
        consensusServiceStub.responses.add(response(2L));

        subscribeToMirror(received::add);

        Assertions.assertThat(received)
                .hasSize(2)
                .extracting(t -> t.sequenceNumber)
                .containsExactly(1L, 2L);
        assertThat(errors).isEmpty();
    }

    @Test
    @Timeout(3)
    void retriesExhausted() {
        topicMessageQuery.setMaxAttempts(1);
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.responses.add(Status.RESOURCE_EXHAUSTED.asRuntimeException());
        consensusServiceStub.responses.add(Status.RESOURCE_EXHAUSTED.asRuntimeException());

        subscribeToMirror(received::add);

        Assertions.assertThat(received).isEmpty();
        assertThat(errors)
                .hasSize(1)
                .first()
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(t -> ((StatusRuntimeException) t).getStatus())
                .isEqualTo(Status.RESOURCE_EXHAUSTED);
    }

    @Test
    @Timeout(5)
    void errorWhenCallIsCancelled() {
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.responses.add(Status.CANCELLED.asRuntimeException());

        subscribeToMirror(received::add);

        assertThat(errors)
                .hasSize(1)
                .first()
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(t -> ((StatusRuntimeException) t).getStatus())
                .isEqualTo(Status.CANCELLED);

        Assertions.assertThat(received).isEmpty();
    }

    @Test
    @Timeout(3)
    void unsubscribeDoesNotInvokeErrorOrRetry() {
        consensusServiceStub.requests.add(request().build());
        SubscriptionHandle handle = topicMessageQuery.subscribe(client, received::add);
        handle.unsubscribe();

        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

        assertThat(errors).isEmpty();
        Assertions.assertThat(received).isEmpty();
    }

    @Test
    @Timeout(3)
    void serverCancelledRetriesWhenCustomRetryAllows() {
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.requests.add(request().build());
        consensusServiceStub.responses.add(Status.CANCELLED.asRuntimeException());
        consensusServiceStub.responses.add(response(1L));
        topicMessageQuery.setRetryHandler(t -> {
            if (t instanceof StatusRuntimeException sre) {
                return sre.getStatus().getCode() == Status.Code.CANCELLED;
            }
            return false;
        });
        subscribeToMirror(received::add);
        Assertions.assertThat(received)
            .hasSize(1)
            .extracting(t -> t.sequenceNumber)
            .containsExactly(1L);
        assertThat(errors).isEmpty();
    }

    private void subscribeToMirror(Consumer<TopicMessage> onNext) {
        SubscriptionHandle subscriptionHandle = topicMessageQuery.subscribe(client, onNext);
        Stopwatch stopwatch = Stopwatch.createStarted();

        while (!complete.get() && errors.isEmpty() && stopwatch.elapsed(TimeUnit.SECONDS) < 3) {
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        subscriptionHandle.unsubscribe();
    }

    private static ConsensusTopicQuery.Builder request() {
        return ConsensusTopicQuery.newBuilder()
                .setConsensusEndTime(toTimestamp(START_TIME.plusSeconds(100L)))
                .setConsensusStartTime(toTimestamp(START_TIME))
                .setTopicID(TopicID.newBuilder().setTopicNum(1000).build());
    }

    private static ConsensusTopicResponse response(long sequenceNumber) {
        return response(sequenceNumber, 0);
    }

    private static ConsensusTopicResponse response(long sequenceNumber, int total) {
        ConsensusTopicResponse.Builder consensusTopicResponseBuilder = ConsensusTopicResponse.newBuilder();

        if (total > 0) {
            var chunkInfo = ConsensusMessageChunkInfo.newBuilder()
                    .setInitialTransactionID(TransactionID.newBuilder()
                            .setAccountID(
                                    AccountID.newBuilder().setAccountNum(3).build())
                            .setTransactionValidStart(toTimestamp(START_TIME))
                            .build())
                    .setNumber((int) sequenceNumber)
                    .setTotal(total)
                    .build();
            consensusTopicResponseBuilder.setChunkInfo(chunkInfo);
        }

        var message = ByteString.copyFrom(Longs.toByteArray(sequenceNumber));
        return consensusTopicResponseBuilder
                .setConsensusTimestamp(toTimestamp(START_TIME.plusSeconds(sequenceNumber)))
                .setSequenceNumber(sequenceNumber)
                .setMessage(message)
                .setRunningHash(message)
                .setRunningHashVersion(2L)
                .build();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private static class ConsensusServiceStub extends ConsensusServiceGrpc.ConsensusServiceImplBase {

        private final Queue<ConsensusTopicQuery> requests = new ArrayDeque<>();
        private final Queue<Object> responses = new ArrayDeque<>();

        @Override
        public void subscribeTopic(
                ConsensusTopicQuery consensusTopicQuery, StreamObserver<ConsensusTopicResponse> streamObserver) {
            var request = requests.poll();
            assertThat(request).isNotNull();
            assertThat(consensusTopicQuery).isEqualTo(request);

            while (!responses.isEmpty()) {
                var response = responses.poll();
                assertThat(response).isNotNull();

                if (response instanceof Throwable) {
                    streamObserver.onError((Throwable) response);
                    return;
                }

                streamObserver.onNext((ConsensusTopicResponse) response);
            }

            streamObserver.onCompleted();
        }

        public void verify() {
            assertThat(requests).isEmpty();
            assertThat(responses).isEmpty();
        }
    }

    private byte[] combine(byte[] array1, byte[] array2) {
        byte[] joinedArray = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }
}
