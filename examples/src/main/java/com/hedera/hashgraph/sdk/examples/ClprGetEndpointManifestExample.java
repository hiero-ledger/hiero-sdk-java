// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ClprGetEndpointManifestQuery;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;

/**
 * How to read the CLPR endpoint manifest (HIP-1535).
 * <p>
 * Requires a network with {@code clpr.enabled=true}. This is a paid query: the client looks up the
 * cost and the operator signs the payment, subject to the normal maximum query payment.
 * Keep the manifest and its state proof together when seeding a peer cache. An empty proof is a
 * successful read, so retry later if you need proof material.
 */
class ClprGetEndpointManifestExample {

    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * HEDERA_NETWORK defaults to testnet if not specified in dotenv file.
     * Network can be: localhost, testnet, previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file.
     * Log levels can be: TRACE, DEBUG, INFO, WARN, ERROR, SILENT.
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    /**
     * Operator's account ID, which pays for the query.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    public static void main(String[] args) throws Exception {
        System.out.println("CLPR Get Endpoint Manifest Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Query the endpoint manifest and its state proof.
         */
        var info = new ClprGetEndpointManifestQuery().execute(client);

        System.out.println("Manifest version: " + Long.toUnsignedString(info.getVersion()));
        System.out.println("Service address: " + Hex.toHexString(info.getServiceAddress()));
        for (var endpoint : info.getEndpoints()) {
            System.out.println("Endpoint " + endpoint.getServiceEndpoint().getIpAddress() + ":"
                    + endpoint.getServiceEndpoint().getPort() + ", account "
                    + Hex.toHexString(endpoint.getAccountId()) + ", TLS certificate "
                    + Hex.toHexString(endpoint.getTlsCertificate()));
        }
        System.out.println("Manifest state proof: " + Hex.toHexString(info.getManifestStateProof()));

        /*
         * Clean up:
         */
        client.close();
        System.out.println("CLPR Get Endpoint Manifest Example Complete!");
    }
}
