// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * One request handed to an {@link HttpTransport}.
 *
 * <p>Note that this is the SDK's own transport-layer type, not {@code java.net.http.HttpRequest}. A
 * single-type import of the JDK type still wins inside a compilation unit that asks for it, so
 * existing code is unaffected.
 *
 * <p>The {@code url} is absolute and already resolved — a transport never joins anything. Header names
 * are lowercase, and the map is never null; an empty map is the absence of headers.
 */
public final class HttpRequest {
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[^\\s]+$");

    private final HttpMethod method;
    private final String url;

    @Nullable
    private final byte[] body;

    @Nullable
    private final String contentType;

    private final Map<String, String> headers;

    @Nullable
    private final Duration deadline;

    private HttpRequest(
            HttpMethod method,
            String url,
            @Nullable byte[] body,
            @Nullable String contentType,
            Map<String, String> headers,
            @Nullable Duration deadline) {
        this.method = method;
        this.url = url;
        this.body = body;
        this.contentType = contentType;
        this.headers = headers;
        this.deadline = deadline;
    }

    /**
     * Create a bodyless request.
     *
     * @param method the HTTP method
     * @param url an absolute {@code http} or {@code https} URL
     * @return the new request
     */
    public static HttpRequest create(HttpMethod method, String url) {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(url, "url must not be null");

        if (!URL_PATTERN.matcher(url).matches()) {
            throw new IllegalArgumentException("url must be an absolute http or https URL: " + url);
        }

        return new HttpRequest(method, url, null, null, Collections.emptyMap(), null);
    }

    /**
     * Derive a copy carrying a body.
     *
     * @param body the request body; defensively copied
     * @param contentType the body's media type
     * @return the derived request
     */
    public HttpRequest withBody(byte[] body, String contentType) {
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        return new HttpRequest(method, url, body.clone(), contentType, headers, deadline);
    }

    /**
     * Derive a copy carrying one more header. The name is lowercased.
     *
     * @param name the header name
     * @param value the header value
     * @return the derived request
     */
    public HttpRequest withHeader(String name, String value) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");

        var merged = new LinkedHashMap<>(headers);
        merged.put(name.toLowerCase(Locale.ROOT), value);
        return new HttpRequest(method, url, body, contentType, Collections.unmodifiableMap(merged), deadline);
    }

    /**
     * Derive a copy whose headers are replaced wholesale. Names are lowercased.
     *
     * @param headers the headers to carry
     * @return the derived request
     */
    public HttpRequest withHeaders(Map<String, String> headers) {
        Objects.requireNonNull(headers, "headers must not be null");
        return new HttpRequest(method, url, body, contentType, lowercaseKeys(headers), deadline);
    }

    /**
     * Derive a copy bounded by a deadline.
     *
     * <p>The deadline bounds <b>the whole exchange, body included</b> — not time to first byte. A
     * transport must abandon the exchange and fail with
     * {@link HttpTransportErrorKind#TIMEOUT_ERROR} when it elapses.
     *
     * @param deadline the bound, or null for no bound of the caller's own
     * @return the derived request
     */
    public HttpRequest withDeadline(@Nullable Duration deadline) {
        if (deadline != null && deadline.isNegative()) {
            throw new IllegalArgumentException("deadline must not be negative");
        }
        return new HttpRequest(method, url, body, contentType, headers, deadline);
    }

    /**
     * Extract the HTTP method.
     *
     * @return the method
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Extract the absolute URL.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Extract a copy of the request body.
     *
     * @return the body, or null on a bodyless method
     */
    @Nullable
    public byte[] getBody() {
        return body == null ? null : body.clone();
    }

    /**
     * Extract the body's media type.
     *
     * @return the content type, or null on a bodyless method
     */
    @Nullable
    public String getContentType() {
        return contentType;
    }

    /**
     * Extract the request headers, with lowercased names.
     *
     * @return an unmodifiable map, never null
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Extract the bound on the whole exchange.
     *
     * @return the deadline, or null when the caller imposes no bound of its own
     */
    @Nullable
    public Duration getDeadline() {
        return deadline;
    }

    private static Map<String, String> lowercaseKeys(Map<String, String> source) {
        var copy = new LinkedHashMap<String, String>(source.size());
        for (var entry : source.entrySet()) {
            copy.put(
                    Objects.requireNonNull(entry.getKey()).toLowerCase(Locale.ROOT),
                    Objects.requireNonNull(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public String toString() {
        return "HttpRequest{method=" + method + ", url=" + url + "}";
    }
}
