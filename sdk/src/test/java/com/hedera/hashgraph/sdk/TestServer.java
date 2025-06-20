// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

// TODO: we may want to refactor to separate TestClient from TestServer.
//       That way, we can have a client with a network of multiple test servers.
//       Maybe we can test load-balancing?

public class TestServer {
    public final Client client;
    private final Server[] grpcServers = new Server[2];

    public TestServer(String name, BindableService... services) throws IOException {
        for (int i = 0; i < 2; i++) {
            var serverBuilder = InProcessServerBuilder.forName(name + "[" + i + "]");
            for (var service : services) {
                serverBuilder.addService(service);
            }
            grpcServers[i] = serverBuilder.directExecutor().build().start();
        }

        var network = new HashMap<String, AccountId>();
        network.put("in-process:" + name + "[0]", AccountId.fromString("1.1.1"));
        network.put("in-process:" + name + "[1]", AccountId.fromString("1.1.2"));
        client = Client.forNetwork(network)
                .setNodeMinBackoff(Duration.ofMillis(500))
                .setNodeMaxBackoff(Duration.ofMillis(500))
                .setOperator(AccountId.fromString("2.2.2"), PrivateKey.generate());
    }

    public void close() throws TimeoutException, InterruptedException {
        client.close();
        for (var server : grpcServers) {
            server.shutdown();
            server.awaitTermination();
        }
    }
}
