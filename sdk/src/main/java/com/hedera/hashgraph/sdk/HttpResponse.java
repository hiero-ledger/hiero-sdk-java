// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One response from an {@link HttpTransport}.
 *
 * <p>Note that this is the SDK's own transport-layer type, not {@code java.net.http.HttpResponse}.
 *
 * <p>Header names are <b>ASCII-lowercased</b> and values are list-valued, and both are normative. The
 * same wire header otherwise produces a different key in every runtime — Go canonicalises to
 * {@code Retry-After}, browsers lowercase, and the JDK preserves whatever the server sent behind a
 * case-insensitive comparator that is lost the moment the map is copied. HTTP/2 lowercases every header
 * name on the wire while HTTP/1.1 does not, so a helper reading {@code Retry-After} would find it
 * against one mirror node and miss it against another. List-valued because a single-valued map
 * silently discards all but one value of a repeated header.
 */
public final class HttpResponse {
    private final int statusCode;
    private final byte[] body;
    private final Map<String, List<String>> headers;

    private HttpResponse(int statusCode, byte[] body, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    /**
     * Create a response.
     *
     * @param statusCode the HTTP status code
     * @param body the response body; defensively copied
     * @param headers the response headers; names are lowercased and values copied
     * @return the new response
     */
    public static HttpResponse create(int statusCode, byte[] body, Map<String, List<String>> headers) {
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(headers, "headers must not be null");

        if (statusCode < 0 || statusCode > 65535) {
            throw new IllegalArgumentException("statusCode out of range: " + statusCode);
        }

        var copy = new LinkedHashMap<String, List<String>>(headers.size());
        for (var entry : headers.entrySet()) {
            copy.merge(
                    Objects.requireNonNull(entry.getKey()).toLowerCase(Locale.ROOT),
                    List.copyOf(entry.getValue()),
                    (existing, added) -> {
                        var combined = new java.util.ArrayList<>(existing);
                        combined.addAll(added);
                        return List.copyOf(combined);
                    });
        }

        return new HttpResponse(statusCode, body.clone(), Collections.unmodifiableMap(copy));
    }

    /**
     * Extract the HTTP status code.
     *
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Extract a copy of the response body.
     *
     * @return the body
     */
    public byte[] getBody() {
        return body.clone();
    }

    /**
     * Extract the response headers, with lowercased names.
     *
     * @return an unmodifiable map, never null
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Extract the first value of a header.
     *
     * @param name the header name, matched after lowercasing
     * @return the first value, or empty when the header is absent
     */
    public Optional<String> getFirstHeaderValue(String name) {
        Objects.requireNonNull(name, "name must not be null");
        var values = headers.get(name.toLowerCase(Locale.ROOT));
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    @Override
    public String toString() {
        return "HttpResponse{statusCode=" + statusCode + ", bodyLength=" + body.length + "}";
    }
}
