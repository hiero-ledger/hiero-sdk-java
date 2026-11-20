// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ClprCompleteConnectorTransaction;
import com.hedera.hashgraph.sdk.ClprDeregisterConnectorTransaction;
import com.hedera.hashgraph.sdk.ClprRegisterConnectorTransaction;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.security.SecureRandom;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;

/**
 * How to register, complete and deregister a CLPR Connector (HIP-1535 commit-reveal).
 * <p>
 * Requires a network with {@code clpr.enabled=true}, an active Channel in CLPR_CHANNEL_ID (64 hex characters
 * without 0x), and a deployed Connector contract in CLPR_CONNECTOR_CONTRACT_ID. Set CLPR_OWNERSHIP_KEY and
 * CLPR_CONNECTOR_ADMIN_KEY to private keys, and CLPR_LOCKED_STAKE_TINYBARS to a stake that meets the
 * network's {@code clpr.minLockedStake}.
 * <p>
 * The operator is the explicit stake recipient and must sign the deregistration along with the Connector
 * admin. A fresh salt gives each run a new Connector ID, because completed commitments stay registered
 * after deregistration.
 */
class ClprConnectorLifecycleExample {

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
     * Operator's account ID, which pays for the transactions and receives the returned stake.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    private static final String CLPR_CHANNEL_ID =
            Objects.requireNonNull(Dotenv.load().get("CLPR_CHANNEL_ID"));

    private static final PrivateKey CLPR_OWNERSHIP_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("CLPR_OWNERSHIP_KEY")));

    private static final PrivateKey CLPR_CONNECTOR_ADMIN_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("CLPR_CONNECTOR_ADMIN_KEY")));

    private static final ContractId CLPR_CONNECTOR_CONTRACT_ID =
            ContractId.fromString(Objects.requireNonNull(Dotenv.load().get("CLPR_CONNECTOR_CONTRACT_ID")));

    private static final long CLPR_LOCKED_STAKE_TINYBARS =
            Long.parseLong(Objects.requireNonNull(Dotenv.load().get("CLPR_LOCKED_STAKE_TINYBARS")));

    public static void main(String[] args) throws Exception {
        System.out.println("CLPR Connector Lifecycle Example Start!");

        if (!CLPR_CHANNEL_ID.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("CLPR_CHANNEL_ID must be 64 hex characters without 0x.");
        }
        byte[] channelId = Hex.decode(CLPR_CHANNEL_ID);
        byte[] salt = new byte[32];
        new SecureRandom().nextBytes(salt);

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Derive the Connector ID and its ownership commitment.
         */
        byte[] connectorId =
                ClprRegisterConnectorTransaction.computeConnectorId(channelId, CLPR_OWNERSHIP_KEY.getPublicKey(), salt);
        byte[] commitment = ClprRegisterConnectorTransaction.computeOwnershipCommitment(
                connectorId, CLPR_OWNERSHIP_KEY.getPublicKey());
        System.out.println("Connector ID: " + Hex.toHexString(connectorId));
        System.out.println("Salt: " + Hex.toHexString(salt));

        /*
         * Step 2 (commit):
         * Register the commitment on the existing Channel.
         */
        var registerReceipt = new ClprRegisterConnectorTransaction()
                .setChannelId(channelId)
                .setCommitment(commitment)
                .execute(client)
                .getReceipt(client);
        System.out.println("Register Connector: " + registerReceipt.status);

        /*
         * Step 3 (reveal):
         * Complete the Connector. setOwnershipProof checks the Connector ID and signs the reveal;
         * the private key is not stored. The payer funds the locked stake.
         */
        var completeReceipt = new ClprCompleteConnectorTransaction()
                .setChannelId(channelId)
                .setConnectorId(connectorId)
                .setSalt(salt)
                .setConnectorContract(CLPR_CONNECTOR_CONTRACT_ID)
                .setAdminKey(CLPR_CONNECTOR_ADMIN_KEY.getPublicKey())
                .setLockedStake(Hbar.fromTinybars(CLPR_LOCKED_STAKE_TINYBARS))
                .setOwnershipProof(CLPR_OWNERSHIP_KEY)
                .execute(client)
                .getReceipt(client);
        System.out.println("Complete Connector: " + completeReceipt.status);

        /*
         * Step 4:
         * Deregister the Connector. Both the Connector admin and the stake recipient must sign.
         */
        var deregisterReceipt = new ClprDeregisterConnectorTransaction()
                .setChannelId(channelId)
                .setConnectorId(connectorId)
                .setStakeRecipient(OPERATOR_ID)
                .freezeWith(client)
                .sign(CLPR_CONNECTOR_ADMIN_KEY)
                .execute(client)
                .getReceipt(client);
        System.out.println("Deregister Connector: " + deregisterReceipt.status);

        /*
         * Clean up:
         */
        client.close();
        System.out.println("CLPR Connector Lifecycle Example Complete!");
    }
}
