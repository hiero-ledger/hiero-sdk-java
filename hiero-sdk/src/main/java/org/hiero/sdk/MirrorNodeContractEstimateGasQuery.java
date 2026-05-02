// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import java.util.concurrent.ExecutionException;

public class MirrorNodeContractEstimateGasQuery extends MirrorNodeContractQuery<MirrorNodeContractEstimateGasQuery> {

    /**
     * Returns gas estimation for the EVM execution.
     *
     * @param client The Client instance to perform the operation with
     * @return The estimated gas cost
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public long execute(Client client) throws ExecutionException, InterruptedException {
        return estimate(client);
    }

    @Override
    public String toString() {
        return "MirrorNodeContractEstimateGasQuery" + super.toString();
    }
}
