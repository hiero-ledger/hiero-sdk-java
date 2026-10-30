// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MirrorNodeContractEstimateGasQuery extends MirrorNodeContractQuery<MirrorNodeContractEstimateGasQuery> {

    /**
     * Returns gas estimation for the EVM execution.
     *
     * @param client The Client instance to perform the operation with
     * @return The estimated gas cost
     * @throws ExecutionException if the query fails
     * @throws InterruptedException if the thread is interrupted
     */
    public long execute(Client client) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        return estimate(client);
    }

    /**
     * Returns gas estimation for the EVM execution.
     *
     * @param client The Client instance to perform the operation with
     * @param timeout The budget for the whole call, retries and backoff included
     * @return The estimated gas cost
     * @throws ExecutionException if the query fails
     * @throws InterruptedException if the thread is interrupted
     */
    public long execute(Client client, Duration timeout) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        return estimate(client, timeout);
    }

    @Override
    public String toString() {
        return "MirrorNodeContractEstimateGasQuery" + super.toString();
    }
}
