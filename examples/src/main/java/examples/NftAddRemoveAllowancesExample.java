// SPDX-License-Identifier: Apache-2.0
package examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * How to grant another account the right to transfer hbar, fungible and non-fungible tokens from your account (HIP-336).
 */
class NftAddRemoveAllowancesExample {

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
        System.out.println("Nft Add Remove Allowances (HIP-336) Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        PublicKey operatorPublicKey = OPERATOR_KEY.getPublicKey();

        /*
         * Step 1:
         * The beginning of the first example (approve/delete allowances for single serial numbers).
         * Create NFT using the Hedera Token Service.
         */
        System.out.println("The beginning of the first example (approve/delete allowances for single serial numbers).");

        String[] CIDs = {
            "QmNPCiNA3Dsu3K5FxDPMG5Q3fZRwVTg14EXA92uqEeSRXn",
            "QmZ4dgAgt8owvnULxnKxNe8YqpavtVCXmc1Lt2XajFpJs9",
            "QmPzY5GxevjyfMUF5vEAjtyRoigzWp47MiKAtLBduLMC1T",
        };

        System.out.println("Creating NFT using the Hedera Token Service...");
        TransactionReceipt nftCreateTxReceipt = new TokenCreateTransaction()
                .setTokenName("HIP-336 NFT1")
                .setTokenSymbol("HIP336NFT1")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setDecimals(0)
                .setInitialSupply(0)
                .setMaxSupply(CIDs.length)
                .setTreasuryAccountId(OPERATOR_ID)
                .setSupplyType(TokenSupplyType.FINITE)
                .setAdminKey(operatorPublicKey)
                .setSupplyKey(operatorPublicKey)
                .setWipeKey(operatorPublicKey)
                .freezeWith(client)
                .execute(client)
                .getReceipt(client);

        TokenId nftTokenId = nftCreateTxReceipt.tokenId;
        Objects.requireNonNull(nftTokenId);
        System.out.println("Created NFT with token ID: " + nftTokenId);

        /*
         * Step 2:
         * Mint NFTs.
         */
        System.out.println("Minting NFTs...");
        List<TransactionReceipt> nftMintTxReceipts = new ArrayList<>();
        for (int i = 0; i < CIDs.length; i++) {
            nftMintTxReceipts.add(new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(List.of(CIDs[i].getBytes(StandardCharsets.UTF_8)))
                    .freezeWith(client)
                    .execute(client)
                    .getReceipt(client));

            System.out.println("Minted NFT (token ID: " + nftTokenId + ") with serial: "
                    + nftMintTxReceipts.get(i).serials.get(0));
        }

        /*
         * Step 3:
         * Create spender and receiver accounts.
         */
        System.out.println("Creating spender and receiver accounts...");
        PrivateKey spenderPrivateKey = PrivateKey.generateECDSA();
        PublicKey spenderPublicKey = spenderPrivateKey.getPublicKey();
        AccountId spenderAccountId = new AccountCreateTransaction()
                .setKeyWithoutAlias(spenderPublicKey)
                .setInitialBalance(Hbar.from(2))
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(spenderAccountId);
        System.out.println("Created spender account with ID: " + spenderAccountId);

        PrivateKey receiverPrivateKey = PrivateKey.generateECDSA();
        PublicKey receiverPublicKey = receiverPrivateKey.getPublicKey();
        AccountId receiverAccountId = new AccountCreateTransaction()
                .setKeyWithoutAlias(receiverPublicKey)
                .setInitialBalance(Hbar.from(2))
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(receiverAccountId);
        System.out.println("Created receiver account with ID: " + receiverAccountId);

        /*
         * Step 4:
         * Associate spender and receiver accounts with the NFT.
         */
        System.out.println("Associating spender and receiver accounts with the NFT...");
        TransactionReceipt spenderAssociateReceipt = new TokenAssociateTransaction()
                .setAccountId(spenderAccountId)
                .setTokenIds(List.of(nftTokenId))
                .freezeWith(client)
                .sign(spenderPrivateKey)
                .execute(client)
                .getReceipt(client);
        System.out.println(
                "Spender association transaction was complete with status: " + spenderAssociateReceipt.status);

        TransactionReceipt receiverAssociateReceipt = new TokenAssociateTransaction()
                .setAccountId(receiverAccountId)
                .setTokenIds(List.of(nftTokenId))
                .freezeWith(client)
                .sign(receiverPrivateKey)
                .execute(client)
                .getReceipt(client);
        System.out.println(
                "Receiver association transaction was complete with status: " + receiverAssociateReceipt.status);

        /*
         * Step 5:
         * Approve NFT (serial '1' and '2') allowance for spender account.
         */
        NftId nft1 = new NftId(nftTokenId, 1);
        NftId nft2 = new NftId(nftTokenId, 2);

        System.out.println("Approving spender account allowance for NFT (serials #1 and #2)...");
        TransactionReceipt approveReceipt = new AccountAllowanceApproveTransaction()
                .approveTokenNftAllowance(nft1, OPERATOR_ID, spenderAccountId)
                .approveTokenNftAllowance(nft2, OPERATOR_ID, spenderAccountId)
                .execute(client)
                .getReceipt(client);
        System.out.println("Approve spender allowance transaction was complete with status: " + approveReceipt.status);

        /*
         * Step 6:
         * Send NFT with serial #1 from operator's to receiver account.
         * This transaction should be executed on behalf of the spender and should end up with SUCCESS.
         */
        // Generate TransactionId from spender's account id in order,
        // for the transaction to be executed on behalf of the spender.
        TransactionId onBehalfOfTransactionId = TransactionId.generate(spenderAccountId);

        System.out.println("Transferring NFT (serial #1) on behalf of the spender...");
        TransactionReceipt approvedSendReceipt = new TransferTransaction()
                .addApprovedNftTransfer(nft1, OPERATOR_ID, receiverAccountId)
                .setTransactionId(onBehalfOfTransactionId)
                .freezeWith(client)
                .sign(spenderPrivateKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("Transfer transaction was complete with status: " + approvedSendReceipt.status);

        /*
         * Step 7:
         * Remove all NFT token allowances (for serial #2).
         */
        System.out.println("Removing all NFT token allowances (for serial #2)...");
        TransactionReceipt deleteAllowanceReceipt = new AccountAllowanceDeleteTransaction()
                .deleteAllTokenNftAllowances(nft2, OPERATOR_ID)
                .execute(client)
                .getReceipt(client);
        System.out.println("Remove allowance transaction was complete with status: " + deleteAllowanceReceipt.status);

        /*
         * Step 8:
         * Send NFT with serial #2 from operator's to receiver account.
         * Spender does not have an allowance to send serial #2, should end up with SPENDER_DOES_NOT_HAVE_ALLOWANCE.
         */
        TransactionId onBehalfOfTransactionId2 = TransactionId.generate(spenderAccountId);

        try {
            System.out.println("Transferring NFT (serial #2) on behalf of the spender...");
            new TransferTransaction()
                    .addApprovedNftTransfer(nft2, OPERATOR_ID, receiverAccountId)
                    .setTransactionId(onBehalfOfTransactionId2)
                    .freezeWith(client)
                    .sign(spenderPrivateKey)
                    .execute(client)
                    .getReceipt(client);
        } catch (Exception e) {
            System.out.println("Transferring NFT (serial #2) was failed (as expected): " + e.getMessage());
        }

        System.out.println("---");

        /*
         * Step 9:
         * The beginning of the second example (approve/delete allowances for ALL serial numbers at once).
         * Create a fungible HTS token using the Hedera Token Service.
         */
        System.out.println(
                "The beginning of the second example (approve/delete allowances for ALL serial numbers at once).");

        String[] CIDs2 = {
            "QmNPCiNA3Dsu3K5FxDPMG5Q3fZRwVTg14EXA92uqEeSRXn",
            "QmZ4dgAgt8owvnULxnKxNe8YqpavtVCXmc1Lt2XajFpJs9",
            "QmPzY5GxevjyfMUF5vEAjtyRoigzWp47MiKAtLBduLMC1T",
        };

        System.out.println("Creating NFT using the Hedera Token Service...");
        TransactionReceipt nftCreateReceipt2 = new TokenCreateTransaction()
                .setTokenName("HIP336NFT2")
                .setTokenSymbol("HIP336NFT2")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setDecimals(0)
                .setInitialSupply(0)
                .setMaxSupply(CIDs2.length)
                .setTreasuryAccountId(OPERATOR_ID)
                .setSupplyType(TokenSupplyType.FINITE)
                .setAdminKey(operatorPublicKey)
                .setSupplyKey(operatorPublicKey)
                .setWipeKey(operatorPublicKey)
                .freezeWith(client)
                .execute(client)
                .getReceipt(client);

        TokenId nftTokenId2 = nftCreateReceipt2.tokenId;
        Objects.requireNonNull(nftCreateReceipt2);
        System.out.println("Created NFT with token ID: " + nftTokenId2);

        /*
         * Step 10:
         * Mint NFTs.
         */
        System.out.println("Minting NFTs...");
        List<TransactionReceipt> nftCollection2 = new ArrayList<>();
        for (int i = 0; i < CIDs2.length; i++) {
            nftCollection2.add(new TokenMintTransaction()
                    .setTokenId(nftTokenId2)
                    .setMetadata(List.of(CIDs2[i].getBytes(StandardCharsets.UTF_8)))
                    .freezeWith(client)
                    .execute(client)
                    .getReceipt(client));

            System.out.println("Minted NFT (token ID: " + nftTokenId2 + ") with serial: "
                    + nftCollection2.get(i).serials.get(0));
        }

        /*
         * Step 11:
         * Create spender and receiver accounts.
         */
        System.out.println("Creating spender and receiver accounts...");
        PrivateKey delegatingSpenderPrivateKey = PrivateKey.generateECDSA();
        PublicKey delegatingSpenderPublicKey2 = delegatingSpenderPrivateKey.getPublicKey();
        AccountId delegatingSpenderAccountId = new AccountCreateTransaction()
                .setKeyWithoutAlias(delegatingSpenderPublicKey2)
                .setInitialBalance(Hbar.from(2))
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(delegatingSpenderAccountId);
        System.out.println("Created spender account with ID: " + delegatingSpenderAccountId);

        PrivateKey receiverPrivateKey2 = PrivateKey.generateECDSA();
        PublicKey receiverPublicKey2 = receiverPrivateKey2.getPublicKey();
        AccountId receiverAccountId2 = new AccountCreateTransaction()
                .setKeyWithoutAlias(receiverPublicKey2)
                .setInitialBalance(Hbar.from(2))
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(receiverAccountId2);
        System.out.println("Created receiver account with ID: " + receiverAccountId2);

        /*
         * Step 12:
         * Associate spender and receiver accounts with the NFT.
         */
        System.out.println("Associating spender and receiver accounts with the NFT...");
        TransactionReceipt spenderAssociateReceipt2 = new TokenAssociateTransaction()
                .setAccountId(delegatingSpenderAccountId)
                .setTokenIds(List.of(nftTokenId2))
                .freezeWith(client)
                .sign(delegatingSpenderPrivateKey)
                .execute(client)
                .getReceipt(client);
        System.out.println(
                "Spender association transaction was complete with status: " + spenderAssociateReceipt2.status);

        TransactionReceipt receiverAssociateReceipt2 = new TokenAssociateTransaction()
                .setAccountId(receiverAccountId2)
                .setTokenIds(List.of(nftTokenId2))
                .freezeWith(client)
                .sign(receiverPrivateKey2)
                .execute(client)
                .getReceipt(client);
        System.out.println(
                "Receiver association transaction was complete with status: " + receiverAssociateReceipt2.status);

        /*
         * Step 13:
         * Approve NFT (all serials) allowance for spender account.
         */
        NftId example2Nft1 = new NftId(nftTokenId2, 1);
        NftId example2Nft2 = new NftId(nftTokenId2, 2);
        NftId example2Nft3 = new NftId(nftTokenId2, 3);

        System.out.println("Approving spender account allowance for NFT (all serials)...");
        TransactionReceipt approveReceipt2 = new AccountAllowanceApproveTransaction()
                .approveTokenNftAllowanceAllSerials(nftTokenId2, OPERATOR_ID, delegatingSpenderAccountId)
                .execute(client)
                .getReceipt(client);
        System.out.println("Approve spender allowance transaction was complete with status: " + approveReceipt2.status);

        /*
         * Step 14:
         * Create delegate spender account.
         */
        System.out.println("Creating delegate spender account...");
        PrivateKey spenderPrivateKey2 = PrivateKey.generateECDSA();
        PublicKey spenderPublicKey2 = spenderPrivateKey2.getPublicKey();
        AccountId spenderAccountId2 = new AccountCreateTransaction()
                .setKeyWithoutAlias(spenderPublicKey2)
                .setInitialBalance(Hbar.from(2))
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(spenderAccountId2);
        System.out.println("Created delegate spender account with ID: : " + spenderAccountId2);

        /*
         * Step 15:
         * Give delegatingSpender allowance for NFT with serial #3 on behalf of spender account which has approveForAll rights.
         */
        System.out.println(
                "Approving delegate spender account allowance for NFT (serial #3) on behalf of spender account which has `approveForAll` rights...");
        TransactionReceipt approveDelegateAllowanceReceipt = new AccountAllowanceApproveTransaction()
                .approveTokenNftAllowance(example2Nft3, OPERATOR_ID, spenderAccountId2, delegatingSpenderAccountId)
                .freezeWith(client)
                .sign(delegatingSpenderPrivateKey)
                .execute(client)
                .getReceipt(client);
        System.out.println(
                "Approve delegated spender allowance for serial 3 - status: " + approveDelegateAllowanceReceipt.status);

        /*
         * Step 16:
         * Send NFT with serial #3 from operator's to receiver account.
         * This transaction should be executed on behalf of the spenderAccountId2,
         * which has an allowance to send serial #3, and should end up with SUCCESS.
         */
        // Generate TransactionId from spender's account id in order,
        // for the transaction to be executed on behalf of the spender.
        TransactionId delegatedOnBehalfOfTxId = TransactionId.generate(spenderAccountId2);

        TransactionReceipt delegatedSendTx = new TransferTransaction()
                .addApprovedNftTransfer(example2Nft3, OPERATOR_ID, receiverAccountId2)
                .setTransactionId(delegatedOnBehalfOfTxId)
                .freezeWith(client)
                .sign(spenderPrivateKey2)
                .execute(client)
                .getReceipt(client);
        System.out.println("Transfer serial 3 on behalf of the delegated spender status:" + delegatedSendTx.status);

        /*
         * Step 17:
         * Send NFT with serial #1 from operator's to receiver account.
         * This transaction should be executed on behalf of the delegatingSpender,
         * which has an allowance to send serial #1, and should end up with SUCCESS.
         */
        // Generate TransactionId from spender's account id in order,
        // for the transaction to be executed on behalf of the spender.
        TransactionId onBehalfOfTransactionId3 = TransactionId.generate(delegatingSpenderAccountId);

        TransactionReceipt approvedSendReceipt3 = new TransferTransaction()
                .addApprovedNftTransfer(example2Nft1, OPERATOR_ID, receiverAccountId2)
                .setTransactionId(onBehalfOfTransactionId3)
                .freezeWith(client)
                .sign(delegatingSpenderPrivateKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("Transfer serial 1 on behalf of the spender status:" + approvedSendReceipt3.status);

        /*
         * Step 18:
         * Remove delegatingSpender allowance for all of NFT serials.
         */
        TransactionReceipt deleteAllowanceReceipt2 = new AccountAllowanceApproveTransaction()
                .deleteTokenNftAllowanceAllSerials(nftTokenId2, OPERATOR_ID, delegatingSpenderAccountId)
                .execute(client)
                .getReceipt(client);
        System.out.println("Remove spender's allowance for serial 2 - status: " + deleteAllowanceReceipt2.status);

        /*
         * Step 19:
         * Send NFT with serial #2 from operator's to receiver account.
         * Spender does not have an allowance to send serial #2, should end up with SPENDER_DOES_NOT_HAVE_ALLOWANCE.
         */
        // Generate TransactionId from spender's account id in order,
        // for the transaction to be executed on behalf of the spender.
        TransactionId onBehalfOfTransactionId4 = TransactionId.generate(delegatingSpenderAccountId);

        try {
            new TransferTransaction()
                    .addApprovedNftTransfer(example2Nft2, OPERATOR_ID, receiverAccountId2)
                    .setTransactionId(onBehalfOfTransactionId4)
                    .freezeWith(client)
                    .sign(delegatingSpenderPrivateKey)
                    .execute(client)
                    .getReceipt(client);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        /*
         * Clean up:
         * Delete created accounts and tokens.
         */
        new TokenWipeTransaction()
                .setTokenId(nftTokenId)
                .addSerial(1)
                .setAccountId(receiverAccountId)
                .freezeWith(client)
                .sign(OPERATOR_KEY)
                .execute(client)
                .getReceipt(client);

        new TokenWipeTransaction()
                .setTokenId(nftTokenId2)
                .addSerial(1)
                .addSerial(3)
                .setAccountId(receiverAccountId2)
                .freezeWith(client)
                .sign(OPERATOR_KEY)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(spenderAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(spenderPrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(receiverAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(receiverPrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(delegatingSpenderAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(delegatingSpenderPrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(receiverAccountId2)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(receiverPrivateKey2)
                .execute(client)
                .getReceipt(client);

        new TokenDeleteTransaction().setTokenId(nftTokenId).execute(client).getReceipt(client);

        new TokenDeleteTransaction().setTokenId(nftTokenId2).execute(client).getReceipt(client);

        client.close();

        System.out.println("Nft Add Remove Allowances (HIP-336) Example Complete!");
    }
}
