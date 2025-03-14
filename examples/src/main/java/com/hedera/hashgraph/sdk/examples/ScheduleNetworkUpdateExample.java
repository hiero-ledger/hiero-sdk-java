// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.github.cdimascio.dotenv.Dotenv;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ScheduleNetworkUpdateExample {
    /**
     * Operator's account ID.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    public static void main(String[] args) throws Exception {
        System.out.println("Network Update Period Example Start!");

        /*
         * Step 1: Initialize the client.
         * Note: By default, the first network address book update will be executed now
         * and subsequent updates will occur every 24 hours.
         * This is controlled by network update period, which defaults to 24 hours.
         */
        Client client = ClientHelper.forName("testnet");
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        Duration networkUpdateDuration = client.getNetworkUpdatePeriod();
        System.out.println("The current default network update period is: " + networkUpdateDuration.toMinutes()
                + " minutes or " + networkUpdateDuration.toHours()
                + " hour.");

        /*
         * Step 2: Change network update period to 1 hour
         */
        System.out.println("Changing network update period to 1 hour...");
        client.setNetworkUpdatePeriod(Duration.ofHours(1));

        networkUpdateDuration = client.getNetworkUpdatePeriod();
        System.out.println("The current network update period is: " + networkUpdateDuration.toMinutes()
                + " minutes or " + networkUpdateDuration.toHours()
                + " hours.");

        /*
         * Step 3: Create client without scheduling network update
         */
        System.out.println("Creating client without scheduling network update...");

        // Define network nodes
        Map<String, AccountId> network = new HashMap<>();
        network.put("35.237.200.180:50211", AccountId.fromString("0.0.3"));
        network.put("35.186.191.247:50211", AccountId.fromString("0.0.4"));
        network.put("35.192.2.25:50211", AccountId.fromString("0.0.5"));
        network.put("35.199.161.108:50211", AccountId.fromString("0.0.6"));
        network.put("35.203.82.240:50211", AccountId.fromString("0.0.7"));
        network.put("35.236.5.219:50211", AccountId.fromString("0.0.8"));
        network.put("35.197.192.225:50211", AccountId.fromString("0.0.9"));
        network.put("35.242.233.154:50211", AccountId.fromString("0.0.10"));
        network.put("35.240.118.96:50211", AccountId.fromString("0.0.11"));
        network.put("35.204.86.32:50211", AccountId.fromString("0.0.12"));

        // network schedule update is not set for custom network
        Client clientWithoutScheduling = Client.forNetwork(network);
        clientWithoutScheduling.setOperator(OPERATOR_ID, OPERATOR_KEY);

        Duration newUpdateDuration = clientWithoutScheduling.getNetworkUpdatePeriod();
        if (newUpdateDuration == null) {
            System.out.println("Network updates are disabled for this client.");
        } else {
            System.out.println("The current network update period is: " + newUpdateDuration.toMinutes()
                    + " minutes or " + newUpdateDuration.toHours()
                    + " hours.");
        }

        // Clean up
        client.close();
        clientWithoutScheduling.close();

        System.out.println("Network Update Period Example Complete!");
    }
}
