// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MirrorNodeContractCallQuery extends MirrorNodeContractQuery<MirrorNodeContractCallQuery> {
    /**
     * Does transient simulation of read-write operations and returns the result in hexadecimal string format.
     *
     * @param client The Client instance to perform the operation with
     * @return The result of the contract call
     * @throws ExecutionException if the query fails
     * @throws InterruptedException if the thread is interrupted
     */
    public String execute(Client client) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        return call(client);
    }

    /**
     * Does transient simulation of read-write operations and returns the result in hexadecimal string format.
     *
     * @param client The Client instance to perform the operation with
     * @param timeout The budget for the whole call, retries and backoff included
     * @return The result of the contract call
     * @throws ExecutionException if the query fails
     * @throws InterruptedException if the thread is interrupted
     */
    public String execute(Client client, Duration timeout) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        return call(client, timeout);
    }

    @Override
    public String toString() {
        return "MirrorNodeContractCallQuery" + super.toString();
    }
}
