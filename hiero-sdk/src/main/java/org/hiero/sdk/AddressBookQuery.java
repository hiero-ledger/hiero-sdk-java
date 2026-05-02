// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Deadline;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.mirror.NetworkServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query the mirror node for the address book.
 */
public class AddressBookQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressBookQuery.class);

    @Nullable
    private FileId fileId = null;

    @Nullable
    private Integer limit = null;

    private int maxAttempts = 10;
    private Duration maxBackoff = Duration.ofSeconds(8L);

    /**
     * Constructor.
     */
    public AddressBookQuery() {}

    private static boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException statusRuntimeException) {
            var code = statusRuntimeException.getStatus().getCode();
            var description = statusRuntimeException.getStatus().getDescription();

            return (code == io.grpc.Status.Code.UNAVAILABLE)
                    || (code == io.grpc.Status.Code.RESOURCE_EXHAUSTED)
                    || (code == Status.Code.INTERNAL
                            && description != null
                            && Executable.RST_STREAM.matcher(description).matches());
        }

        return false;
    }

    /**
     * Extract the file id.
     *
     * @return the file id that was assigned
     */
    @Nullable
    public FileId getFileId() {
        return fileId;
    }

    /**
     * Assign the file id of address book to retrieve.
     *
     * @param fileId the file id of the address book
     * @return {@code this}
     */
    public AddressBookQuery setFileId(FileId fileId) {
        this.fileId = fileId;
        return this;
    }

    /**
     * Extract the limit number.
     *
     * @return the limit number that was assigned
     */
    @Nullable
    public Integer getLimit() {
        return limit;
    }

    /**
     * Assign the number of node addresses to retrieve or all nodes set to 0.
     *
     * @param limit number of node addresses to get
     * @return {@code this}
     */
    public AddressBookQuery setLimit(@Nullable @Nonnegative Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Extract the maximum number of attempts.
     *
     * @return the maximum number of attempts
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Assign the maximum number of attempts.
     *
     * @param maxAttempts the maximum number of attempts
     * @return {@code this}
     */
    public AddressBookQuery setMaxAttempts(@Nonnegative int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    /**
     * Assign the maximum backoff duration.
     *
     * @param maxBackoff the maximum backoff duration
     * @return {@code this}
     */
    public AddressBookQuery setMaxBackoff(Duration maxBackoff) {
        Objects.requireNonNull(maxBackoff);
        if (maxBackoff.toMillis() < 500L) {
            throw new IllegalArgumentException("maxBackoff must be at least 500 ms");
        }
        this.maxBackoff = maxBackoff;
        return this;
    }

    /**
     * Execute the query with preset timeout.
     *
     * @param client the client object
     * @return the node address book
     */
    public NodeAddressBook execute(Client client) {
        return execute(client, client.getRequestTimeout());
    }

    /**
     * Execute the query with user supplied timeout.
     *
     * @param client  the client object
     * @param timeout the user supplied timeout
     * @return the node address book
     */
    public NodeAddressBook execute(Client client, Duration timeout) {
        var deadline = Deadline.after(timeout.toMillis(), TimeUnit.MILLISECONDS);
        for (int attempt = 1; true; attempt++) {
            try {
                var addressProtoIter =
                        ClientCalls.blockingServerStreamingCall(buildCall(client, deadline), buildQuery());
                List<NodeAddress> addresses = new ArrayList<>();
                while (addressProtoIter.hasNext()) {
                    addresses.add(NodeAddress.fromProtobuf(addressProtoIter.next()));
                }
                return new NodeAddressBook().setNodeAddresses(addresses);
            } catch (Throwable error) {
                if (!shouldRetry(error) || attempt >= maxAttempts) {
                    LOGGER.error("Error attempting to get address book at FileId {}", fileId, error);
                    throw error;
                }
                warnAndDelay(attempt, error);
            }
        }
    }

    /**
     * Execute the query with preset timeout asynchronously.
     *
     * @param client the client object
     * @return the node address book
     */
    public CompletableFuture<NodeAddressBook> executeAsync(Client client) {
        return executeAsync(client, client.getRequestTimeout());
    }

    /**
     * Execute the query with user supplied timeout.
     *
     * @param client  the client object
     * @param timeout the user supplied timeout
     * @return the node address book
     */
    public CompletableFuture<NodeAddressBook> executeAsync(Client client, Duration timeout) {
        var deadline = Deadline.after(timeout.toMillis(), TimeUnit.MILLISECONDS);
        CompletableFuture<NodeAddressBook> returnFuture = new CompletableFuture<>();
        executeAsync(client, deadline, returnFuture, 1);
        return returnFuture;
    }

    /**
     * Execute the query.
     *
     * @param client       the client object
     * @param deadline     the user supplied timeout
     * @param returnFuture returned promise callback
     * @param attempt      maximum number of attempts
     */
    void executeAsync(Client client, Deadline deadline, CompletableFuture<NodeAddressBook> returnFuture, int attempt) {
        List<NodeAddress> addresses = new ArrayList<>();
        ClientCalls.asyncServerStreamingCall(
                buildCall(client, deadline), buildQuery(), new StreamObserver<org.hiero.sdk.proto.NodeAddress>() {
                    @Override
                    public void onNext(org.hiero.sdk.proto.NodeAddress addressProto) {
                        addresses.add(NodeAddress.fromProtobuf(addressProto));
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (attempt >= maxAttempts || !shouldRetry(error)) {
                            LOGGER.error("Error attempting to get address book at FileId {}", fileId, error);
                            returnFuture.completeExceptionally(error);
                            return;
                        }
                        warnAndDelay(attempt, error);
                        addresses.clear();
                        executeAsync(client, deadline, returnFuture, attempt + 1);
                    }

                    @Override
                    public void onCompleted() {
                        returnFuture.complete(new NodeAddressBook().setNodeAddresses(addresses));
                    }
                });
    }

    /**
     * Build the address book query.
     *
     * @return {@link org.hiero.sdk.proto.mirror.AddressBookQuery buildQuery }
     */
    org.hiero.sdk.proto.mirror.AddressBookQuery buildQuery() {
        var builder = org.hiero.sdk.proto.mirror.AddressBookQuery.newBuilder();
        if (fileId != null) {
            builder.setFileId(fileId.toProtobuf());
        }
        if (limit != null) {
            builder.setLimit(limit);
        }
        return builder.build();
    }

    private ClientCall<org.hiero.sdk.proto.mirror.AddressBookQuery, org.hiero.sdk.proto.NodeAddress> buildCall(
            Client client, Deadline deadline) {
        try {
            return client.mirrorNetwork
                    .getNextMirrorNode()
                    .getChannel()
                    .newCall(NetworkServiceGrpc.getGetNodesMethod(), CallOptions.DEFAULT.withDeadline(deadline));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void warnAndDelay(int attempt, Throwable error) {
        var delay = Math.min(500 * (long) Math.pow(2, attempt), maxBackoff.toMillis());
        LOGGER.warn(
                "Error fetching address book at FileId {} during attempt #{}. Waiting {} ms before next attempt: {}",
                fileId,
                attempt,
                delay,
                error.getMessage());

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
