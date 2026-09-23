// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * The retry budget for one mirror node REST call.
 *
 * <p>It is a <b>value</b>, not client state: it is resolved per call as package default, then the
 * {@link Client}'s policy, then any per-query setter, and carried into the call by value. That lets
 * concurrent calls hold different budgets while sharing one connection pool.
 *
 * <p>Instances are derived from {@link #defaults()} with the {@code withX} methods. There is no public
 * constructor, so a policy with a zero attempt count or a null status list — five attempts that never
 * retry, silently — is unreachable.
 *
 * <p>The retryable set is data rather than a rule in prose: "5xx" is read differently by every
 * implementer, and {@code 501}, {@code 505}, {@code 506}, {@code 507}, {@code 508}, {@code 510} and
 * {@code 511} all describe a server that will answer identically next time.
 */
public final class MirrorNodeHttpRetryPolicy {
    private static final List<Integer> DEFAULT_RETRYABLE_STATUS_CODES = List.of(408, 429, 500, 502, 503, 504);

    private static final MirrorNodeHttpRetryPolicy DEFAULTS = new MirrorNodeHttpRetryPolicy(
            5,
            Duration.ofSeconds(30),
            Duration.ZERO,
            Duration.ofMillis(250),
            Duration.ofSeconds(8),
            DEFAULT_RETRYABLE_STATUS_CODES);

    private final int maxAttempts;
    private final Duration perAttemptTimeout;
    private final Duration totalDeadline;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final List<Integer> retryableStatusCodes;

    private MirrorNodeHttpRetryPolicy(
            int maxAttempts,
            Duration perAttemptTimeout,
            Duration totalDeadline,
            Duration initialBackoff,
            Duration maxBackoff,
            List<Integer> retryableStatusCodes) {
        this.maxAttempts = maxAttempts;
        this.perAttemptTimeout = perAttemptTimeout;
        this.totalDeadline = totalDeadline;
        this.initialBackoff = initialBackoff;
        this.maxBackoff = maxBackoff;
        this.retryableStatusCodes = retryableStatusCodes;
    }

    /**
     * A policy with every field at its declared default: five attempts, a 30 s per-attempt bound, a
     * total bound inherited from {@link Client#getRequestTimeout()}, 250 ms of initial backoff capped at
     * 8 s, and {@code 408, 429, 500, 502, 503, 504} as the retryable statuses.
     *
     * @return the default policy
     */
    public static MirrorNodeHttpRetryPolicy defaults() {
        return DEFAULTS;
    }

    /**
     * Derive a copy with a different attempt budget.
     *
     * <p>Counted <b>per request</b>: one initial request plus up to {@code maxAttempts - 1} retries of
     * that request. A paginated walk gets the budget afresh for each page; what spans the whole walk is
     * {@link #getTotalDeadline()}.
     *
     * @param maxAttempts the number of attempts per request, at least 1
     * @return the derived policy
     */
    public MirrorNodeHttpRetryPolicy withMaxAttempts(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
        return new MirrorNodeHttpRetryPolicy(
                maxAttempts, perAttemptTimeout, totalDeadline, initialBackoff, maxBackoff, retryableStatusCodes);
    }

    /**
     * Derive a copy with a different per-attempt bound.
     *
     * @param perAttemptTimeout the bound on one attempt, end to end; {@link Duration#ZERO} means no
     *                          per-attempt cap, leaving the remaining total as the only bound
     * @return the derived policy
     */
    public MirrorNodeHttpRetryPolicy withPerAttemptTimeout(Duration perAttemptTimeout) {
        Objects.requireNonNull(perAttemptTimeout, "perAttemptTimeout must not be null");
        if (perAttemptTimeout.isNegative()) {
            throw new IllegalArgumentException("perAttemptTimeout must not be negative");
        }
        return new MirrorNodeHttpRetryPolicy(
                maxAttempts, perAttemptTimeout, totalDeadline, initialBackoff, maxBackoff, retryableStatusCodes);
    }

    /**
     * Derive a copy with a different total bound.
     *
     * @param totalDeadline the bound on the whole call, every page and every backoff included;
     *                      {@link Duration#ZERO} means inherit {@link Client#getRequestTimeout()} rather
     *                      than "no bound"
     * @return the derived policy
     */
    public MirrorNodeHttpRetryPolicy withTotalDeadline(Duration totalDeadline) {
        Objects.requireNonNull(totalDeadline, "totalDeadline must not be null");
        if (totalDeadline.isNegative()) {
            throw new IllegalArgumentException("totalDeadline must not be negative");
        }
        return new MirrorNodeHttpRetryPolicy(
                maxAttempts, perAttemptTimeout, totalDeadline, initialBackoff, maxBackoff, retryableStatusCodes);
    }

    /**
     * Derive a copy with a different backoff base.
     *
     * <p>Named {@code initialBackoff} rather than {@code minBackoff} because with full jitter the draw is
     * from {@code [0, min(maxBackoff, initialBackoff * 2^n))}, so there is no floor — the value is the
     * base of the exponential, not a minimum wait.
     *
     * @param initialBackoff the base of the exponential
     * @return the derived policy
     */
    public MirrorNodeHttpRetryPolicy withInitialBackoff(Duration initialBackoff) {
        Objects.requireNonNull(initialBackoff, "initialBackoff must not be null");
        if (initialBackoff.isNegative()) {
            throw new IllegalArgumentException("initialBackoff must not be negative");
        }
        return new MirrorNodeHttpRetryPolicy(
                maxAttempts, perAttemptTimeout, totalDeadline, initialBackoff, maxBackoff, retryableStatusCodes);
    }

    /**
     * Derive a copy with a different backoff cap.
     *
     * @param maxBackoff the cap on the computed backoff
     * @return the derived policy
     */
    public MirrorNodeHttpRetryPolicy withMaxBackoff(Duration maxBackoff) {
        Objects.requireNonNull(maxBackoff, "maxBackoff must not be null");
        if (maxBackoff.isNegative()) {
            throw new IllegalArgumentException("maxBackoff must not be negative");
        }
        return new MirrorNodeHttpRetryPolicy(
                maxAttempts, perAttemptTimeout, totalDeadline, initialBackoff, maxBackoff, retryableStatusCodes);
    }

    /**
     * Derive a copy with a different retryable status set.
     *
     * @param retryableStatusCodes the statuses worth repeating the exchange for
     * @return the derived policy
     */
    public MirrorNodeHttpRetryPolicy withRetryableStatusCodes(List<Integer> retryableStatusCodes) {
        Objects.requireNonNull(retryableStatusCodes, "retryableStatusCodes must not be null");
        return new MirrorNodeHttpRetryPolicy(
                maxAttempts,
                perAttemptTimeout,
                totalDeadline,
                initialBackoff,
                maxBackoff,
                List.copyOf(retryableStatusCodes));
    }

    /**
     * Extract the attempt budget.
     *
     * @return the number of attempts per request
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Extract the per-attempt bound.
     *
     * @return the bound on one attempt; {@link Duration#ZERO} means no per-attempt cap
     */
    public Duration getPerAttemptTimeout() {
        return perAttemptTimeout;
    }

    /**
     * Extract the total bound.
     *
     * @return the bound on the whole call; {@link Duration#ZERO} means inherit the client's request
     *         timeout
     */
    public Duration getTotalDeadline() {
        return totalDeadline;
    }

    /**
     * Extract the backoff base.
     *
     * @return the base of the exponential
     */
    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    /**
     * Extract the backoff cap.
     *
     * @return the cap on the computed backoff
     */
    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    /**
     * Extract the retryable status set.
     *
     * @return an unmodifiable list of status codes
     */
    public List<Integer> getRetryableStatusCodes() {
        return retryableStatusCodes;
    }

    /**
     * Whether a status is worth repeating the exchange for.
     *
     * @param statusCode the status the mirror node answered with
     * @return true if it should be retried
     */
    public boolean isRetryable(int statusCode) {
        return retryableStatusCodes.contains(statusCode);
    }

    @Override
    public String toString() {
        return "MirrorNodeHttpRetryPolicy{maxAttempts=" + maxAttempts + ", perAttemptTimeout=" + perAttemptTimeout
                + ", totalDeadline=" + totalDeadline + ", initialBackoff=" + initialBackoff + ", maxBackoff="
                + maxBackoff + ", retryableStatusCodes=" + retryableStatusCodes + "}";
    }
}
