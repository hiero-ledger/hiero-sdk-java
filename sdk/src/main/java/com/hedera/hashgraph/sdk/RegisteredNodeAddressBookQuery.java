// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query the mirror node for the RegisteredAddressBook.
 */
public class RegisteredNodeAddressBookQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisteredNodeAddressBookQuery.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
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

    private static boolean shouldRetry(Throwable throwable) {
        return throwable instanceof HttpTimeoutException || throwable instanceof IOException;
    }

    private static boolean shouldRetry(int statusCode) {
        return statusCode == 408 || statusCode == 429 || (statusCode >= 500 && statusCode < 600);
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
                    String baseUrl = buildBaseUrl(client);
                    String apiEndpoint = buildInitialPath();

                    boolean isLastPage = false;

                    while (!isLastPage) {
                        JsonObject json = fetchPage(baseUrl + apiEndpoint, timeout);

                        if (json.has("registered_nodes")) {
                            JsonArray nodesArray = json.getAsJsonArray("registered_nodes");
                            for (JsonElement node : nodesArray) {
                                registeredNodes.add(RegisteredNode.fromJson(node.getAsJsonObject()));
                            }
                        }

                        String nextUrl = getNextPagePath(json);
                        if (nextUrl != null) {
                            apiEndpoint = nextUrl;
                        } else {
                            isLastPage = true;
                        }
                    }
                    return registeredNodes;
                },
                client.executor);
    }

    private JsonObject fetchPage(String url, Duration timeout) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                HttpResponse<String> response =
                        HTTP_CLIENT.send(buildHttpRequest(url, timeout), HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return JsonParser.parseString(response.body()).getAsJsonObject();
                }

                if (!shouldRetry(response.statusCode()) || attempt > maxAttempts) {
                    throw new IllegalStateException("Mirror Node error: HTTP " + response.statusCode());
                }

                lastException = new RuntimeException("HTTP " + response.statusCode());
                warnAndDelay(attempt, lastException);
            } catch (Exception e) {
                lastException = e;
                if (attempt > maxAttempts || !shouldRetry(e)) {
                    throw new RuntimeException("Failed to fetch page after " + attempt + " attempts", e);
                }
                warnAndDelay(attempt, lastException);
            }
        }

        throw new RuntimeException("Failed to fetch page after " + maxAttempts + " attempts", lastException);
    }

    private String getNextPagePath(JsonObject response) {
        if (response.has("links") && !response.get("links").isJsonNull()) {
            JsonObject links = response.getAsJsonObject("links");
            if (links.has("next") && !links.get("next").isJsonNull()) {
                return links.get("next").getAsString();
            }
        }
        return null;
    }

    private String buildBaseUrl(Client client) {
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

        return baseUrl.replace("/api/v1", "");
    }

    private String buildInitialPath() {
        StringBuilder path = new StringBuilder("/api/v1/network/registered-nodes");
        limit = limit > 0 ? limit : DEFAULT_LIMIT;
        path.append("?limit=").append(limit);

        if (registeredNodeId > -1) {
            path.append("&registerednode.id=").append(registeredNodeId);
        }

        return path.toString();
    }

    private HttpRequest buildHttpRequest(String url, Duration timeout) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .GET()
                .build();
    }

    private void warnAndDelay(int attempt, Throwable error) {
        var delay = Math.min(500 * (long) Math.pow(2, attempt), maxBackoff.toMillis());
        LOGGER.warn(
                "Error fetching registered nodes during attempt #{}. Waiting {} ms before next attempt: {}",
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
