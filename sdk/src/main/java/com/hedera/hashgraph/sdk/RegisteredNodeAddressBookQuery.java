// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.Nullable;

/**
 * Query the mirror node for the RegisteredAddressBook.
 */
public class RegisteredNodeAddressBookQuery {
    private final int DEFAULT_LIMIT = 25;

    private long registeredNodeId = -1;
    private int limit;
    private int maxAttempts = 10;
    private Duration maxBackoff = Duration.ofSeconds(8L);

    /**
     * Returns the set registered node ID.
     *
     * @return The registered node ID.
     */
    public long getRegisteredNodeId() {
        return registeredNodeId;
    }

    /**
     * Sets the ID of the registered node to retrieve.
     *
     * @param registeredNodeId The unique identifier of the node.
     * @return {@code this}
     */
    public RegisteredNodeAddressBookQuery setRegisteredNodeId(long registeredNodeId) {
        this.registeredNodeId = registeredNodeId;
        return this;
    }

    /**
     * Gets the page size limit currently set for the REST requests.
     *
     * @return The maximum number of nodes requested per page.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Sets the maximum number of registered nodes to return per REST API request (page size).
     * Note: The query will still follow 'next' links to fetch all available nodes.
     *
     * @param limit The maximum number of nodes to return per request.
     * @return {@code this}
     */
    public RegisteredNodeAddressBookQuery setLimit(int limit) {
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
     * Set the maximum number of attempts for the query.
     *
     * @param maxAttempts the maximum number of attempts
     * @return {@code this}
     */
    public RegisteredNodeAddressBookQuery setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    /**
     * Extract the maximum backoff duration.
     *
     * @return the maximum backoff duration
     */
    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    /**
     * Set the maximum backoff duration for retry attempts.
     *
     * @param maxBackoff the maximum backoff duration
     * @return {@code this}
     */
    public RegisteredNodeAddressBookQuery setMaxBackoff(Duration maxBackoff) {
        Objects.requireNonNull(maxBackoff);
        if (maxBackoff.toMillis() < 500L) {
            throw new IllegalArgumentException("maxBackoff must be at least 500 ms");
        }
        this.maxBackoff = maxBackoff;
        return this;
    }

    /**
     * Executes the query with the user supplied client
     *
     * @param client The Client instance to perform the operation with.
     * @return The retrieved {@link RegisteredNodeAddressBook}.
     * @throws ExecutionException if the query fails.
     * @throws InterruptedException if the thread is interrupted.
     */
    public RegisteredNodeAddressBook execute(Client client) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        return executeAsync(client, client.getRequestTimeout()).get();
    }

    /**
     * Executes the query with the user supplied client and timeout
     *
     * @param client The Client instance to perform the operation with.
     * @param timeout The maximum duration for each individual HTTP request.
     * @return The retrieved {@link RegisteredNodeAddressBook}.
     * @throws ExecutionException if the query fails.
     * @throws InterruptedException if the thread is interrupted.
     */
    public RegisteredNodeAddressBook execute(Client client, Duration timeout)
            throws ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        return executeAsync(client, timeout).get();
    }

    /**
     * Executes the query asynchronously with the user supplied client
     *
     * @param client The Client instance to perform the operation with.
     * @return A future representing the retrieved {@link RegisteredNodeAddressBook}.
     */
    public CompletableFuture<RegisteredNodeAddressBook> executeAsync(Client client) {
        Objects.requireNonNull(client, "client must not be null");
        return executeAsync(client, client.getRequestTimeout());
    }

    /**
     * Executes the query asynchronously with the user supplied client with timeout
     *
     * @param client The Client instance to perform the operation with.
     * @param timeout The maximum duration for each individual HTTP request.
     * @return A future representing the retrieved {@link RegisteredNodeAddressBook}.
     */
    public CompletableFuture<RegisteredNodeAddressBook> executeAsync(Client client, Duration timeout) {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");

        return fetchAllPagesAsync(client, timeout)
                .thenApply(registeredNodes -> new RegisteredNodeAddressBook(registeredNodes));
    }

    private CompletableFuture<List<RegisteredNode>> fetchAllPagesAsync(Client client, Duration timeout) {
        return CompletableFuture.supplyAsync(
                () -> {
                    List<RegisteredNode> registeredNodes = new ArrayList<>();
                    var baseUrl = resolveBaseUrl(client);
                    var apiPath = buildInitialPath();
                    var policy = client.getMirrorNodeHttpConfig()
                            .getRetryPolicy()
                            .withMaxAttempts(this.maxAttempts)
                            .withMaxBackoff(this.maxBackoff);
                    var httpClient = client.newMirrorNodeHttpClient(baseUrl, policy);

                    boolean isLastPage = false;

                    while (!isLastPage) {
                        var total =
                                MirrorNodeHttpClient.resolveTotalDeadline(policy, client.getRequestTimeout(), timeout);
                        JsonObject json = fetchPage(httpClient, apiPath, total);

                        if (json.has("registered_nodes")) {
                            JsonArray nodesArray = json.getAsJsonArray("registered_nodes");
                            for (JsonElement node : nodesArray) {
                                registeredNodes.add(RegisteredNode.fromJson(node.getAsJsonObject()));
                            }
                        }

                        String nextLink = getNextPageLink(json);
                        if (nextLink != null) {
                            apiPath = MirrorNodeRestPath.fromNextLink(nextLink);
                        } else {
                            isLastPage = true;
                        }
                    }
                    return registeredNodes;
                },
                client.executor);
    }

    private JsonObject fetchPage(MirrorNodeHttpClient httpClient, MirrorNodeRestPath path, Duration total) {
        HttpResponse response;

        try {
            response = httpClient.get(path, Cancellation.none(), MirrorNodeHttpClient.CallDeadline.startingNow(total));
        } catch (HttpTransportException e) {
            var lastResponse = e instanceof MirrorNodeHttpException mirrorNodeHttpException
                    ? mirrorNodeHttpException.getLastResponse()
                    : null;
            throw new CompletionException(new RuntimeException(
                    "Failed to fetch registered node address book: "
                            + e.getKind().getId()
                            + (lastResponse == null ? "" : "(" + MirrorNodeErrorMessage.describe(lastResponse) + ")"),
                    e));
        }

        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("Mirror Node error: HTTP " + response.getStatusCode() + ": "
                    + MirrorNodeErrorMessage.describe(response));
        }

        try {
            return JsonParser.parseString(new String(response.getBody(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Mirror Node returned a malformed JSON response", e);
        }
    }

    private @Nullable String getNextPageLink(JsonObject response) {
        if (response.has("links") && !response.get("links").isJsonNull()) {
            JsonObject links = response.getAsJsonObject("links");
            if (links.has("next") && !links.get("next").isJsonNull()) {
                return links.get("next").getAsString();
            }
        }
        return null;
    }

    private String resolveBaseUrl(Client client) {
        String baseUrl = client.getMirrorRestBaseUrl();

        // For localhost registered node calls, override to use port 8084 unless system property overrides
        if (baseUrl.contains("localhost:38081") || baseUrl.contains("127.0.0.1:38081")) {
            String registeredNodePort = System.getProperty("hedera.mirror.registerednode.port");
            if (registeredNodePort != null && !registeredNodePort.isEmpty()) {
                baseUrl = baseUrl.replace(":38081", ":" + registeredNodePort);
            } else {
                baseUrl = baseUrl.replace(":38081", ":8084");
            }
        }

        return baseUrl;
    }

    private MirrorNodeRestPath buildInitialPath() {
        StringBuilder path = new StringBuilder("/network/registered-nodes");
        limit = limit > 0 ? limit : DEFAULT_LIMIT;
        path.append("?limit=").append(limit);

        if (registeredNodeId > -1) {
            path.append("&registerednode.id=").append(registeredNodeId);
        }

        return MirrorNodeRestPath.of(path.toString());
    }
}
