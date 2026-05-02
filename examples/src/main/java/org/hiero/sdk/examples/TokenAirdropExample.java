// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.AccountBalanceQuery;
import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.AccountId;
import org.hiero.sdk.Client;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.TokenAirdropTransaction;
import org.hiero.sdk.TokenCancelAirdropTransaction;
import org.hiero.sdk.TokenClaimAirdropTransaction;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenMintTransaction;
import org.hiero.sdk.TokenRejectTransaction;
import org.hiero.sdk.TokenSupplyType;
import org.hiero.sdk.TokenType;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TokenAirdropExample {

    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * Operator's account ID. Used to sign and pay for operations on Hedera.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * HEDERA_NETWORK defaults to testnet if not specified in dotenv file. Network can be: localhost, testnet,
     * previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file. Log levels can be: TRACE, DEBUG, INFO, WARN,
     * ERROR, SILENT.
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL, for example via VM
     * options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    public static void main(String[] args) throws Exception {
        System.out.println("Example Start!");

        /*
         * Step 0:
         * Create and configure SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Create 4 accounts
         */
        var privateKey1 = PrivateKey.generateECDSA();
        var alice = new AccountCreateTransaction()
                .setKeyWithoutAlias(privateKey1)
                .setInitialBalance(new Hbar(10))
                .setMaxAutomaticTokenAssociations(-1)
                .execute(client)
                .getReceipt(client)
                .accountId;

        var privateKey2 = PrivateKey.generateECDSA();
        var bob = new AccountCreateTransaction()
                .setKeyWithoutAlias(privateKey2)
                .setMaxAutomaticTokenAssociations(1)
                .execute(client)
                .getReceipt(client)
                .accountId;

        var privateKey3 = PrivateKey.generateECDSA();
        var carol = new AccountCreateTransaction()
                .setKeyWithoutAlias(privateKey3)
                .setMaxAutomaticTokenAssociations(0)
                .execute(client)
                .getReceipt(client)
                .accountId;

        var treasuryKey = PrivateKey.generateECDSA();
        var treasuryAccount = new AccountCreateTransaction()
                .setKeyWithoutAlias(treasuryKey)
                .setInitialBalance(new Hbar(10))
                .execute(client)
                .getReceipt(client)
                .accountId;

        /*
         * Step 2:
         * Create FT and NFT and mint
         */
        var tokenID = new TokenCreateTransaction()
                .setTokenName("Fungible Token")
                .setTokenSymbol("TFT")
                .setTokenMemo("Example memo")
                .setDecimals(3)
                .setInitialSupply(100)
                .setMaxSupply(100)
                .setTreasuryAccountId(treasuryAccount)
                .setSupplyType(TokenSupplyType.FINITE)
                .setAdminKey(client.getOperatorPublicKey())
                .setFreezeKey(client.getOperatorPublicKey())
                .setSupplyKey(client.getOperatorPublicKey())
                .setMetadataKey(client.getOperatorPublicKey())
                .setPauseKey(client.getOperatorPublicKey())
                .freezeWith(client)
                .sign(treasuryKey)
                .execute(client)
                .getReceipt(client)
                .tokenId;

        var nftID = new TokenCreateTransaction()
                .setTokenName("Test NFT")
                .setTokenSymbol("TNFT")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setTreasuryAccountId(treasuryAccount)
                .setSupplyType(TokenSupplyType.FINITE)
                .setMaxSupply(10)
                .setSupplyType(TokenSupplyType.FINITE)
                .setAdminKey(client.getOperatorPublicKey())
                .setFreezeKey(client.getOperatorPublicKey())
                .setSupplyKey(client.getOperatorPublicKey())
                .setMetadataKey(client.getOperatorPublicKey())
                .setPauseKey(client.getOperatorPublicKey())
                .freezeWith(client)
                .sign(treasuryKey)
                .execute(client)
                .getReceipt(client)
                .tokenId;

        new TokenMintTransaction()
                .setTokenId(nftID)
                .setMetadata(generateNftMetadata((byte) 3))
                .execute(client)
                .getReceipt(client);

        /*
         * Step 3:
         * Airdrop fungible tokens to all 3 accounts
         */
        System.out.println("Airdropping fts");
        var txnRecord = new TokenAirdropTransaction()
                .addTokenTransfer(tokenID, alice, 10)
                .addTokenTransfer(tokenID, treasuryAccount, -10)
                .addTokenTransfer(tokenID, bob, 10)
                .addTokenTransfer(tokenID, treasuryAccount, -10)
                .addTokenTransfer(tokenID, carol, 10)
                .addTokenTransfer(tokenID, treasuryAccount, -10)
                .freezeWith(client)
                .sign(treasuryKey)
                .execute(client)
                .getRecord(client);

        /*
         * Step 4:
         * Get the transaction record and see one pending airdrop (for carol)
         */
        System.out.println("Pending airdrops length: " + txnRecord.pendingAirdropRecords.size());
        System.out.println("Pending airdrops: " + txnRecord.pendingAirdropRecords.get(0));

        /*
         * Step 5:
         * Query to verify alice and bob received the airdrops and carol did not
         */
        var aliceBalance = new AccountBalanceQuery().setAccountId(alice).execute(client);
        var bobBalance = new AccountBalanceQuery().setAccountId(bob).execute(client);
        var carolBalance = new AccountBalanceQuery().setAccountId(carol).execute(client);

        System.out.println("Alice ft balance after airdrop: " + aliceBalance.tokens.get(tokenID));
        System.out.println("Bob ft balance after airdrop: " + bobBalance.tokens.get(tokenID));
        System.out.println("Carol ft balance after airdrop: " + carolBalance.tokens.get(tokenID));

        /*
         * Step 6:
         * Claim the airdrop for carol
         */
        System.out.println("Claiming ft with carol");
        new TokenClaimAirdropTransaction()
                .addPendingAirdrop(txnRecord.pendingAirdropRecords.get(0).getPendingAirdropId())
                .freezeWith(client)
                .sign(privateKey3)
                .execute(client)
                .getReceipt(client);

        carolBalance = new AccountBalanceQuery().setAccountId(carol).execute(client);
        System.out.println("Carol ft balance after claim: " + carolBalance.tokens.get(tokenID));

        /*
         * Step 7:
         * Airdrop the NFTs to all three accounts
         */
        System.out.println("Airdropping nfts");
        txnRecord = new TokenAirdropTransaction()
                .addNftTransfer(nftID.nft(1), treasuryAccount, alice)
                .addNftTransfer(nftID.nft(2), treasuryAccount, bob)
                .addNftTransfer(nftID.nft(3), treasuryAccount, carol)
                .freezeWith(client)
                .sign(treasuryKey)
                .execute(client)
                .getRecord(client);

        /*
         * Step 8:
         * Get the transaction record and verify two pending airdrops (for bob & carol)
         */
        System.out.println("Pending airdrops length: " + txnRecord.pendingAirdropRecords.size());
        System.out.println("Pending airdrops for Bob: " + txnRecord.pendingAirdropRecords.get(0));
        System.out.println("Pending airdrops for Carol: " + txnRecord.pendingAirdropRecords.get(1));

        /*
         * Step 9:
         * Query to verify alice received the airdrop and bob and carol did not
         */
        aliceBalance = new AccountBalanceQuery().setAccountId(alice).execute(client);
        bobBalance = new AccountBalanceQuery().setAccountId(bob).execute(client);
        carolBalance = new AccountBalanceQuery().setAccountId(carol).execute(client);

        System.out.println("Alice nft balance after airdrop: " + aliceBalance.tokens.get(nftID));
        System.out.println("Bob nft balance after airdrop: " + bobBalance.tokens.get(nftID));
        System.out.println("Carol nft balance after airdrop: " + carolBalance.tokens.get(nftID));

        /*
         * Step 10:
         * Claim the airdrop for bob
         */
        System.out.println("Claiming nft with Bob");
        new TokenClaimAirdropTransaction()
                .addPendingAirdrop(txnRecord.pendingAirdropRecords.get(0).getPendingAirdropId())
                .freezeWith(client)
                .sign(privateKey2)
                .execute(client)
                .getReceipt(client);

        bobBalance = new AccountBalanceQuery().setAccountId(bob).execute(client);
        System.out.println("Bob nft balance after claim: " + bobBalance.tokens.get(nftID));

        /*
         * Step 11:
         * Cancel the airdrop for carol
         */
        System.out.println("Canceling nft for Carol");
        new TokenCancelAirdropTransaction()
                .addPendingAirdrop(txnRecord.pendingAirdropRecords.get(1).getPendingAirdropId())
                .freezeWith(client)
                .sign(treasuryKey)
                .execute(client)
                .getReceipt(client);

        carolBalance = new AccountBalanceQuery().setAccountId(carol).execute(client);
        System.out.println("Carol nft balance after cancel: " + carolBalance.tokens.get(nftID));

        /*
         * Step 12:
         * Reject the NFT for bob
         */
        System.out.println("Rejecting nft with Bob");
        new TokenRejectTransaction()
                .setOwnerId(bob)
                .addNftId(nftID.nft(2))
                .freezeWith(client)
                .sign(privateKey2)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 13:
         * Query to verify bob no longer has the NFT
         */
        bobBalance = new AccountBalanceQuery().setAccountId(bob).execute(client);
        System.out.println("Bob nft balance after reject: " + bobBalance.tokens.get(nftID));

        /*
         * Step 13:
         * Query to verify the NFT was returned to the Treasury
         */
        var treasuryBalance =
                new AccountBalanceQuery().setAccountId(treasuryAccount).execute(client);
        System.out.println("Treasury nft balance after reject: " + treasuryBalance.tokens.get(nftID));

        /*
         * Step 14:
         * Reject the fungible tokens for Carol
         */
        System.out.println("Rejecting ft with Carol");
        new TokenRejectTransaction()
                .setOwnerId(carol)
                .addTokenId(tokenID)
                .freezeWith(client)
                .sign(privateKey3)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 14:
         * Query to verify carol no longer has the fungible tokens
         */
        carolBalance = new AccountBalanceQuery().setAccountId(carol).execute(client);
        System.out.println("Carol ft balance after reject: " + carolBalance.tokens.get(tokenID));

        /*
         * Step 15:
         * Query to verify Treasury received the rejected fungible tokens
         */
        treasuryBalance =
                new AccountBalanceQuery().setAccountId(treasuryAccount).execute(client);
        System.out.println("Treasury ft balance after reject: " + treasuryBalance.tokens.get(tokenID));

        /*
         * Clean up:
         */
        client.close();

        System.out.println("Example Complete!");
    }

    private static List<byte[]> generateNftMetadata(byte metadataCount) {
        List<byte[]> metadatas = new ArrayList<>();

        for (byte i = 0; i < metadataCount; i++) {
            byte[] md = {i};
            metadatas.add(md);
        }

        return metadatas;
    }
}

