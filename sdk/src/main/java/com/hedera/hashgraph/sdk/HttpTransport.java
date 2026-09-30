// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * The HTTP service provider interface: one request in, one response out, no policy.
 *
 * <p>This is the type an application, a platform or a test implements to replace the SDK's HTTP stack —
 * for a corporate proxy, mTLS, tracing, or to run on Android, where {@code java.net.http} does not
 * exist at any API level.
 *
 * <p>An implementer must satisfy the following; everything not listed is free.
 *
 * <ul>
 *   <li><b>A non-2xx status is a successful exchange.</b> {@code 404} and {@code 503} come back as an
 *       {@link HttpResponse} carrying their status code. The transport must not fail on a status code;
 *       the layer above reads it and decides.
 *   <li><b>Only a failure to obtain a response fails the call</b>, and it fails with an
 *       {@link HttpTransportException} carrying one of the {@link HttpTransportErrorKind} identifiers.
 *       A failure the layer above cannot classify is treated as non-retryable.
 *   <li><b>No policy at this layer.</b> No retry, no backoff, no rate limiting, no circuit breaking, no
 *       status interpretation.
 *   <li><b>{@link #roundTrip} is safe to call concurrently</b>, including concurrently with
 *       {@link #close}.
 *   <li><b>The body is fully buffered</b> before the returned stage completes, bounded by
 *       {@link HttpTransportConfiguration#getMaxResponseBytes()}. That is what lets the layer above
 *       replay a request without body-lifecycle discipline at every call site.
 *   <li><b>{@link #close} aborts, draining first where it can.</b> It stops accepting new work, waits up
 *       to its timeout for exchanges in flight, then cancels whatever remains. It is idempotent and
 *       never reports failure — a bounded shutdown is the guarantee, not an outcome.
 *   <li><b>{@link #close} releases only what the transport itself owns.</b> A transport wrapping a
 *       client the application built must not shut that client down; a {@code close} that does nothing
 *       is a conforming {@code close}.
 *   <li><b>After {@link #close}, {@link #roundTrip} fails fast</b> with
 *       {@link HttpTransportErrorKind#CLIENT_CLOSED_ERROR} and must not open a new connection or
 *       construct a replacement client.
 * </ul>
 *
 * <p><b>Ownership follows construction.</b> A {@link Client} closes only a transport it built itself; an
 * injected transport is never closed by the SDK, because it is almost always a wrapper over a
 * process-wide client the application also uses.
 */
public interface HttpTransport {
    /**
     * Perform one exchange.
     *
     * <p>Never throws: a failure completes the returned stage exceptionally with an
     * {@link HttpTransportException}.
     *
     * @param request the request to perform
     * @param cancellation the caller's cancellation; {@link Cancellation#none()} when there is none.
     *                     An implementation must release its registration when the exchange ends.
     * @return a stage completing with the response, or exceptionally with an
     *         {@link HttpTransportException}
     */
    CompletionStage<HttpResponse> roundTrip(HttpRequest request, Cancellation cancellation);

    /**
     * Release whatever this transport owns, within a bounded grace period.
     *
     * <p>Not asynchronous, because every SDK's {@code Client.close()} is synchronous and making this one
     * asynchronous would make that one asynchronous too.
     *
     * @param closeTimeout how long to wait for exchanges already in flight before cancelling them
     */
    void close(Duration closeTimeout);
}
