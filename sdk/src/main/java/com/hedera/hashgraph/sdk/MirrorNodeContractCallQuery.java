// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.concurrent.ExecutionException;

public class MirrorNodeContractCallQuery extends MirrorNodeContractQuery<MirrorNodeContractCallQuery> {
    /**
     * Does transient simulation of read-write operations and returns the result in hexadecimal string format.
     *
     * @param client The Client instance to perform the operation with
     * @return The result of the contract call
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public String execute(Client client) throws ExecutionException, InterruptedException {
        return call(client);
    }

    @Override
    public String toString() {
        return "MirrorNosdeContractCallQuery" + super.toString();
    }
}
