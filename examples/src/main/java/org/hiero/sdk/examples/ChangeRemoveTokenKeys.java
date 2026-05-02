// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to change or remove existing keys from a token (HIP-540).
 * <p>
 * All entities across Hedera have opt-in administrative keys (or simply admin keys).
 * Currently, the Consensus Service and File service allow these keys to be removed
 * by an update that sets them to an empty KeyList, which is a sentinel value for immutability.
 * However the Hedera Token Service does not provide such a feature consistently.
 * We should enable existing admin keys for tokens created with the Hedera Token Service
 * to be able to sign an update transaction that changes or permanently removes any key
 * (Admin, Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata) from the token.
 */
class ChangeRemoveTokenKeys {

    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * Operator's account ID.
     * Used to sign and pay for operations on Hedera.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * HEDERA_NETWORK defaults to testnet if not specified in dotenv file.
     * Network can be: localhost, testnet, previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file.
     * Log levels can be: TRACE, DEBUG, INFO, WARN, ERROR, SILENT.
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL,
     * for example via VM options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    public static void main(String[] args) throws Exception {
        System.out.println("Change Or Remove Existing Keys From A Token (HIP-540) Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Generate ED25519 key pairs.
         */
        System.out.println("Generating ED25519 key pairs...");
        PrivateKey adminPrivateKey = PrivateKey.generateED25519();
        PublicKey adminPublicKey = adminPrivateKey.getPublicKey();

        PrivateKey supplyPrivateKey = PrivateKey.generateED25519();
        PublicKey supplyPublicKey = supplyPrivateKey.getPublicKey();

        PrivateKey newSupplyPrivateKey = PrivateKey.generateED25519();
        PublicKey newSupplyPublicKey = newSupplyPrivateKey.getPublicKey();

        PrivateKey wipePrivateKey = PrivateKey.generateED25519();
        PublicKey wipePublicKey = wipePrivateKey.getPublicKey();

        /*
         * Step 2:
         * Create NFT and check its keys.
         */
        System.out.println("Creating NFT using the Hedera Token Service...");
        var nftTokenId = new TokenCreateTransaction()
                .setTokenName("HIP-540 NFT")
                .setTokenSymbol("HIP540NFT")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setTreasuryAccountId(OPERATOR_ID)
                .setAdminKey(adminPublicKey)
                .setWipeKey(wipePublicKey)
                .setSupplyKey(supplyPublicKey)
                .freezeWith(client)
                .sign(adminPrivateKey)
                .execute(client)
                .getReceipt(client)
                .tokenId;
        Objects.requireNonNull(nftTokenId);

        var nftInfoBefore = new TokenInfoQuery().setTokenId(nftTokenId).execute(client);

        if (nftInfoBefore.adminKey != null && nftInfoBefore.supplyKey != null && nftInfoBefore.wipeKey != null) {
            System.out.println("Admin public key in the newly created token: " + nftInfoBefore.adminKey);
            System.out.println("Supply public key in the newly created token: " + nftInfoBefore.supplyKey);
            System.out.println("Wipe public key in the newly created token: " + nftInfoBefore.wipeKey);
        } else {
            throw new Exception("The required keys are not set correctly! (Fail)");
        }

        /*
         * Step 3:
         * Remove Wipe Key from a token (by updating it to an empty Key List) and check that its removed.
         */
        System.out.println("Removing the Wipe Key...(updating to an empty Key List).");

        // This HIP introduces ability to remove lower-privilege keys
        // (Wipe, KYC, Freeze, Pause, Supply, Fee Schedule, Metadata) from a Token
        // using an update with the empty KeyList.
        var emptyKeyList = new KeyList();

        new TokenUpdateTransaction()
                .setTokenId(nftTokenId)
                .setWipeKey(emptyKeyList)
                // It is set by default, but we set it here explicitly for illustration.
                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                .freezeWith(client)
                .sign(adminPrivateKey)
                .execute(client)
                .getReceipt(client);

        var nftInfoAfterWipeKeyRemoval =
                new TokenInfoQuery().setTokenId(nftTokenId).execute(client);

        if (nftInfoAfterWipeKeyRemoval.wipeKey == null) {
            System.out.println("Token Wipe Public Key (after removal): " + nftInfoAfterWipeKeyRemoval.wipeKey);
        } else {
            throw new Exception("Token Wipe Key was not removed after removal operation! (Fail)");
        }

        /*
         * Step 4:
         * Remove Admin Key from a token (by updating it to an empty Key List) and check that its removed.
         */
        System.out.println("Removing the Admin Key...(updating to an empty Key List).");

        new TokenUpdateTransaction()
                .setTokenId(nftTokenId)
                .setAdminKey(emptyKeyList)
                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                .freezeWith(client)
                .sign(adminPrivateKey)
                .execute(client)
                .getReceipt(client);

        var nftInfoAfterAdminKeyRemoval =
                new TokenInfoQuery().setTokenId(nftTokenId).execute(client);

        if (nftInfoAfterAdminKeyRemoval.adminKey == null) {
            System.out.println("Token Admin Public Key (after removal): " + nftInfoAfterAdminKeyRemoval.adminKey);
        } else {
            throw new Exception("Token Admin Key was not removed after removal operation! (Fail)");
        }

        /*
         * Step 5:
         * Update Supply Key and check that its updated.
         */
        System.out.println("Updating the Supply Key...(to the new key).");

        new TokenUpdateTransaction()
                .setTokenId(nftTokenId)
                .setSupplyKey(newSupplyPublicKey)
                .setKeyVerificationMode(TokenKeyValidation.FULL_VALIDATION)
                .freezeWith(client)
                .sign(supplyPrivateKey)
                .sign(newSupplyPrivateKey)
                .execute(client)
                .getReceipt(client);

        var nftInfoAfterSupplyKeyUpdate =
                new TokenInfoQuery().setTokenId(nftTokenId).execute(client);

        if (nftInfoAfterSupplyKeyUpdate.supplyKey.equals(newSupplyPublicKey)) {
            System.out.println("Token Supply Public Key (after update): " + nftInfoAfterSupplyKeyUpdate.supplyKey);
        } else {
            throw new Exception("Token Supply Key was not updated correctly! (Fail)");
        }

        /*
         * Step 6:
         * Remove Supply Key (update to the unusable key).
         */
        System.out.println("Removing the Supply Key...(updating to the unusable key).");

        new TokenUpdateTransaction()
                .setTokenId(nftTokenId)
                .setSupplyKey(PublicKey.unusableKey())
                .setKeyVerificationMode(TokenKeyValidation.NO_VALIDATION)
                .freezeWith(client)
                .sign(newSupplyPrivateKey)
                .execute(client)
                .getReceipt(client);

        var nftInfoAfterSupplyKeyRemoval =
                new TokenInfoQuery().setTokenId(nftTokenId).execute(client);

        var supplyKeyAfterRemoval = (PublicKey) nftInfoAfterSupplyKeyRemoval.supplyKey;

        if (supplyKeyAfterRemoval.equals(PublicKey.unusableKey())) {
            System.out.println("Token Supply Public Key (after removal): " + supplyKeyAfterRemoval.toStringRaw());
        } else {
            throw new Exception("Token Supply key was not removed after removal operation! (Fail)");
        }

        /*
         * Clean up:
         * Can't delete a token as it is immutable.
         */
        client.close();

        System.out.println("Change Or Remove Existing Keys From A Token (HIP-540) Example Complete!");
    }
}

