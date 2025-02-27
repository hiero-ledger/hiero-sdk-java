// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.ContractDeleteTransaction;
import com.hedera.hashgraph.sdk.ContractInfoQuery;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenMintTransaction;
import com.hedera.hashgraph.sdk.TransferTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenManualAssociationIntegrationTest {

    @Test
    @DisplayName("Can Manually associate Account with a Fungible Token")
    void canManuallyAssociateAccountWithFungibleToken() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenDecimals = 3;
            var tokenId = EntityHelper.createFungibleToken(testEnv, 3);
            var accountKey = PrivateKey.generateED25519();
            var accountMaxAutomaticTokenAssociations = 0;
            var receiverAccountId =
                    EntityHelper.createAccount(testEnv, accountKey, accountMaxAutomaticTokenAssociations);

            new TokenAssociateTransaction()
                    .setAccountId(receiverAccountId)
                    .setTokenIds(Collections.singletonList(tokenId))
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var accountInfo =
                    new AccountInfoQuery().setAccountId(receiverAccountId).execute(testEnv.client);

            assertThat(accountInfo.tokenRelationships.get(tokenId).decimals).isEqualTo(tokenDecimals);

            new TransferTransaction()
                    .addTokenTransfer(tokenId, testEnv.operatorId, -10)
                    .addTokenTransfer(tokenId, receiverAccountId, 10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var accountBalance =
                    new AccountBalanceQuery().setAccountId(receiverAccountId).execute(testEnv.client);

            assertThat(accountBalance.tokens.get(tokenId)).isEqualTo(10);
        }
    }

    @Test
    @DisplayName("Can Manually associate Account with Nft")
    void canManuallyAssociateAccountWithNft() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenId = EntityHelper.createNft(testEnv);
            var accountKey = PrivateKey.generateED25519();
            var accountMaxAutomaticTokenAssociations = 0;
            var receiverAccountId =
                    EntityHelper.createAccount(testEnv, accountKey, accountMaxAutomaticTokenAssociations);

            var mintReceiptToken = new TokenMintTransaction()
                    .setTokenId(tokenId)
                    .setMetadata(NftMetadataGenerator.generate((byte) 10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenAssociateTransaction()
                    .setAccountId(receiverAccountId)
                    .setTokenIds(Collections.singletonList(tokenId))
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var serialsToTransfer = new ArrayList<>(mintReceiptToken.serials);
            var nftTransferTransaction = new TransferTransaction();
            for (var serial : serialsToTransfer) {
                nftTransferTransaction.addNftTransfer(tokenId.nft(serial), testEnv.operatorId, receiverAccountId);
            }
            nftTransferTransaction.execute(testEnv.client).getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can Manually associate Contract with a Fungible Token")
    void canManuallyAssociateContractWithFungibleToken() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenDecimals = 3;
            var tokenId = EntityHelper.createFungibleToken(testEnv, 3);
            var contractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            new TokenAssociateTransaction()
                    .setAccountId(new AccountId(0, 0, contractId.num))
                    .setTokenIds(Collections.singletonList(tokenId))
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var contractInfo = new ContractInfoQuery().setContractId(contractId).execute(testEnv.client);

            assertThat(contractInfo.contractId).isEqualTo(contractId);
            assertThat(contractInfo.accountId).isNotNull();
            assertThat(Objects.requireNonNull(contractInfo.accountId).toString())
                    .isEqualTo(Objects.requireNonNull(contractId).toString());
            assertThat(contractInfo.adminKey).isNotNull();
            assertThat(Objects.requireNonNull(contractInfo.adminKey).toString())
                    .isEqualTo(Objects.requireNonNull(testEnv.operatorKey).toString());
            assertThat(contractInfo.storage).isEqualTo(128);
            assertThat(contractInfo.contractMemo).isEqualTo("[e2e::ContractMemo]");
            assertThat(contractInfo.tokenRelationships.get(tokenId).decimals).isEqualTo(tokenDecimals);

            new ContractDeleteTransaction()
                    .setTransferAccountId(testEnv.operatorId)
                    .setContractId(contractId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can Manually associate contract with Nft")
    void canManuallyAssociateContractWithNft() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenId = EntityHelper.createNft(testEnv);
            var contractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            new TokenAssociateTransaction()
                    .setAccountId(new AccountId(0, 0, contractId.num))
                    .setTokenIds(Collections.singletonList(tokenId))
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var contractInfo = new ContractInfoQuery().setContractId(contractId).execute(testEnv.client);

            assertThat(contractInfo.contractId).isEqualTo(contractId);
            assertThat(contractInfo.accountId).isNotNull();
            assertThat(Objects.requireNonNull(contractInfo.accountId).toString())
                    .isEqualTo(Objects.requireNonNull(contractId).toString());
            assertThat(contractInfo.adminKey).isNotNull();
            assertThat(Objects.requireNonNull(contractInfo.adminKey).toString())
                    .isEqualTo(Objects.requireNonNull(testEnv.operatorKey).toString());
            assertThat(contractInfo.storage).isEqualTo(128);
            assertThat(contractInfo.contractMemo).isEqualTo("[e2e::ContractMemo]");

            new ContractDeleteTransaction()
                    .setTransferAccountId(testEnv.operatorId)
                    .setContractId(contractId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can execute token associate transaction even when token IDs are not set")
    void canExecuteTokenAssociateTransactionEvenWhenTokenIDsAreNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var accountKey = PrivateKey.generateED25519();
            var accountMaxAutomaticTokenAssociations = 0;
            var accountId = EntityHelper.createAccount(testEnv, accountKey, accountMaxAutomaticTokenAssociations);

            new TokenAssociateTransaction()
                    .setAccountId(accountId)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot Manually associate Account with a Token when Account ID is not set")
    void cannotAssociateAccountWithTokensWhenAccountIDIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var accountKey = PrivateKey.generateED25519();
            var accountMaxAutomaticTokenAssociations = 0;
            var accountId = EntityHelper.createAccount(testEnv, accountKey, accountMaxAutomaticTokenAssociations);

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenAssociateTransaction()
                                .freezeWith(testEnv.client)
                                .sign(accountKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_ACCOUNT_ID.toString());
        }
    }

    @Test
    @DisplayName("Cannot Manually Associate Account with a Token when Account Does Not sign transaction")
    void cannotAssociateAccountWhenAccountDoesNotSignTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var tokenDecimals = 3;
            var tokenId = EntityHelper.createFungibleToken(testEnv, tokenDecimals);
            var accountKey = PrivateKey.generateED25519();
            var accountMaxAutomaticTokenAssociations = 0;
            var accountId = EntityHelper.createAccount(testEnv, accountKey, accountMaxAutomaticTokenAssociations);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenAssociateTransaction()
                                .setAccountId(accountId)
                                .setTokenIds(Collections.singletonList(tokenId))
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }
}
