// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import java.util.HashMap;
import java.util.List;

public class ClientHelper {

    public static final String LOCAL_NETWORK_NAME = "solo";

    private static final String LOCAL_CONSENSUS_NODE_ENDPOINT = "127.0.0.1:50211";

    private static final String LOCAL_MIRROR_NODE_GRPC_ENDPOINT = "127.0.0.1:5600";

    private static final AccountId LOCAL_CONSENSUS_NODE_ACCOUNT_ID = new AccountId(0, 0, 3);

    public static Client forName(String network) throws InterruptedException {
        Client client;

        if (network.equals(LOCAL_NETWORK_NAME)) {
            client = forLocalNetwork();
        } else {
            client = Client.forName(network);
        }
        return client;
    }

    public static Client forLocalNetwork() throws InterruptedException {
        var network = new HashMap<String, AccountId>();
        network.put(LOCAL_CONSENSUS_NODE_ENDPOINT, LOCAL_CONSENSUS_NODE_ACCOUNT_ID);

        return Client.forNetwork(network).setMirrorNetwork(List.of(LOCAL_MIRROR_NODE_GRPC_ENDPOINT));
    }
}
