// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ClprCloseChannelTransaction;
import com.hedera.hashgraph.sdk.ClprCompleteChannelTransaction;
import com.hedera.hashgraph.sdk.ClprRegisterChannelTransaction;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;

/**
 * How to register, complete and close a CLPR Channel (HIP-1535 commit-reveal).
 * <p>
 * Requires a network with {@code clpr.enabled=true}, a deployed verifier contract, and real peer-ledger
 * proof material that the verifier accepts (not placeholder bytes). Closing needs the treasury or system
 * admin account in GENESIS_OPERATOR_ID/GENESIS_OPERATOR_KEY.
 * <p>
 * Set CLPR_CHANNEL_ID to 64 hex characters without 0x, CLPR_OWNERSHIP_KEY to the ownership private key,
 * CLPR_VERIFIER_CONTRACT_ID to the verifier's contract ID, and CLPR_CONFIG_PROOF_FILE and
 * CLPR_MANIFEST_PROOF_FILE to binary proof files from the peer ledger. Keep the ownership key private.
 */
class ClprChannelLifecycleExample {

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
     * Operator's account ID, which pays for registration and completion.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * Treasury or system admin account ID, which is authorized to close Channels.
     */
    private static final AccountId GENESIS_OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("GENESIS_OPERATOR_ID")));

    /**
     * Treasury or system admin private key.
     */
    private static final PrivateKey GENESIS_OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("GENESIS_OPERATOR_KEY")));

    private static final String CLPR_CHANNEL_ID =
            Objects.requireNonNull(Dotenv.load().get("CLPR_CHANNEL_ID"));

    private static final PrivateKey CLPR_OWNERSHIP_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("CLPR_OWNERSHIP_KEY")));

    private static final ContractId CLPR_VERIFIER_CONTRACT_ID =
            ContractId.fromString(Objects.requireNonNull(Dotenv.load().get("CLPR_VERIFIER_CONTRACT_ID")));

    private static final Path CLPR_CONFIG_PROOF_FILE =
            Path.of(Objects.requireNonNull(Dotenv.load().get("CLPR_CONFIG_PROOF_FILE")));

    private static final Path CLPR_MANIFEST_PROOF_FILE =
            Path.of(Objects.requireNonNull(Dotenv.load().get("CLPR_MANIFEST_PROOF_FILE")));

    public static void main(String[] args) throws Exception {
        System.out.println("CLPR Channel Lifecycle Example Start!");

        if (!CLPR_CHANNEL_ID.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("CLPR_CHANNEL_ID must be 64 hex characters without 0x.");
        }
        byte[] channelId = Hex.decode(CLPR_CHANNEL_ID);

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1 (commit):
         * Register keccak256(channelId || ownership public key). This hides the Channel ID and key
         * until the reveal, which prevents front-running.
         */
        byte[] commitment =
                ClprRegisterChannelTransaction.computeOwnershipCommitment(channelId, CLPR_OWNERSHIP_KEY.getPublicKey());
        var registerReceipt = new ClprRegisterChannelTransaction()
                .setOwnershipCommitment(commitment)
                .execute(client)
                .getReceipt(client);
        System.out.println("Register Channel: " + registerReceipt.status);

        /*
         * Step 2 (reveal):
         * Complete the Channel. setOwnershipProof signs the Channel ID; the private key is not stored.
         */
        var completeReceipt = new ClprCompleteChannelTransaction()
                .setChannelId(channelId)
                .setVerifierContract(CLPR_VERIFIER_CONTRACT_ID)
                .setConfigProofBytes(Files.readAllBytes(CLPR_CONFIG_PROOF_FILE))
                .setEndpointManifestProofBytes(Files.readAllBytes(CLPR_MANIFEST_PROOF_FILE))
                .setOwnershipProof(CLPR_OWNERSHIP_KEY)
                .execute(client)
                .getReceipt(client);
        System.out.println("Complete Channel: " + completeReceipt.status);

        /*
         * Step 3:
         * Close the Channel with the admin account. Closing starts draining the queues,
         * so a successful receipt does not mean the Channel is CLOSED yet.
         */
        client.setOperator(GENESIS_OPERATOR_ID, GENESIS_OPERATOR_KEY);
        var closeReceipt = new ClprCloseChannelTransaction()
                .setChannelId(channelId)
                .execute(client)
                .getReceipt(client);
        System.out.println("Close Channel: " + closeReceipt.status);

        /*
         * Clean up:
         */
        client.close();
        System.out.println("CLPR Channel Lifecycle Example Complete!");
    }
}
