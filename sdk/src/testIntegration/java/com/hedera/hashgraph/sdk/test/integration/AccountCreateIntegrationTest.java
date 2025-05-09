// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.hashgraph.sdk.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountCreateIntegrationTest {
    @Test
    @DisplayName("Can create account with only initial balance and key")
    void canCreateAccountWithOnlyInitialBalanceAndKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateED25519();

            var response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client);

            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isEqualTo(accountId);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.key.toString()).isEqualTo(key.getPublicKey().toString());
            assertThat(info.balance).isEqualTo(new Hbar(1));
            assertThat(info.autoRenewPeriod).isEqualTo(Duration.ofDays(90));
            assertThat(info.proxyAccountId).isNull();
            assertThat(info.proxyReceived).isEqualTo(Hbar.ZERO);
        }
    }

    @Test
    @DisplayName("Can create account with no initial balance")
    void canCreateAccountWithNoInitialBalance() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateED25519();

            var response =
                    new AccountCreateTransaction().setKeyWithoutAlias(key).execute(testEnv.client);

            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isEqualTo(accountId);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.key.toString()).isEqualTo(key.getPublicKey().toString());
            assertThat(info.balance).isEqualTo(new Hbar(0));
            assertThat(info.autoRenewPeriod).isEqualTo(Duration.ofDays(90));
            assertThat(info.proxyAccountId).isNull();
            assertThat(info.proxyReceived).isEqualTo(Hbar.ZERO);
        }
    }

    @Test
    @DisplayName("Cannot create account with no key")
    void canNotCreateAccountWithNoKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> new AccountCreateTransaction()
                            .setInitialBalance(new Hbar(1))
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining(Status.KEY_REQUIRED.toString());
        }
    }

    @Test
    @DisplayName("Can create account using aliasKey")
    void canCreateWithAliasKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateED25519();

            var aliasId = key.toAccountId(0, 0);

            new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, new Hbar(10).negated())
                    .addHbarTransfer(aliasId, new Hbar(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var info = new AccountInfoQuery().setAccountId(aliasId).execute(testEnv.client);

            assertThat(key.getPublicKey()).isEqualTo(info.aliasKey);
        }
    }

    @Test
    @DisplayName("Regenerates TransactionIds in response to expiration")
    void managesExpiration() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateED25519();

            var response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setTransactionId(
                            new TransactionId(testEnv.operatorId, Instant.now().minusSeconds(40)))
                    .setTransactionValidDuration(Duration.ofSeconds(30))
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client);

            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isEqualTo(accountId);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.key.toString()).isEqualTo(key.getPublicKey().toString());
            assertThat(info.balance).isEqualTo(new Hbar(0));
            assertThat(info.autoRenewPeriod).isEqualTo(Duration.ofDays(90));
            assertThat(info.proxyAccountId).isNull();
            assertThat(info.proxyReceived).isEqualTo(Hbar.ZERO);
        }
    }

    @Test
    @DisplayName("Can create account with alias from admin key")
    void createAccountWithAliasFromAdminKey() throws Exception {
        // Tests the third row of this table
        // https://github.com/hashgraph/hedera-improvement-proposal/blob/d39f740021d7da592524cffeaf1d749803798e9a/HIP/hip-583.md#signatures
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();
            var evmAddress = adminKey.getPublicKey().toEvmAddress();

            // Create the admin account
            new AccountCreateTransaction()
                    .setKeyWithoutAlias(adminKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client);

            var accountId = new AccountCreateTransaction()
                    .setKeyWithAlias(adminKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            assertThat(accountId).isNotNull();

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isNotNull();
            assertThat(info.contractAccountId).hasToString(evmAddress.toString());
            assertThat(info.key).isEqualTo(adminKey.getPublicKey());
        }
    }

    @Test
    @DisplayName("Can create account with alias from admin key with receiver sig required")
    void createAccountWithAliasFromAdminKeyWithReceiverSigRequired() throws Exception {
        // Tests the fourth row of this table
        // https://github.com/hashgraph/hedera-improvement-proposal/blob/d39f740021d7da592524cffeaf1d749803798e9a/HIP/hip-583.md#signatures
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();
            var evmAddress = adminKey.getPublicKey().toEvmAddress();

            // Create the admin account
            new AccountCreateTransaction()
                    .setKeyWithoutAlias(adminKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client);

            var accountId = new AccountCreateTransaction()
                    .setReceiverSignatureRequired(true)
                    .setKeyWithAlias(adminKey)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            assertThat(accountId).isNotNull();

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isNotNull();
            assertThat(info.contractAccountId).hasToString(evmAddress.toString());
            assertThat(info.key).isEqualTo(adminKey.getPublicKey());
        }
    }

    @Test
    @DisplayName("Cannot create account with alias from admin key with receiver sig required without signature")
    void cannotCreateAccountWithAliasFromAdminKeyWithReceiverSigRequiredAndNoSignature() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();
            var evmAddress = adminKey.getPublicKey().toEvmAddress();

            // Create the admin account
            new AccountCreateTransaction()
                    .setKeyWithoutAlias(adminKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new AccountCreateTransaction()
                            .setReceiverSignatureRequired(true)
                            .setKeyWithAlias(adminKey)
                            .setAlias(evmAddress)
                            .freezeWith(testEnv.client)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    @Test
    @DisplayName("Can create account with alias different from admin key")
    void createAccountWithAlias() throws Exception {
        // Tests the fifth row of this table
        // https://github.com/hashgraph/hedera-improvement-proposal/blob/d39f740021d7da592524cffeaf1d749803798e9a/HIP/hip-583.md#signatures
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateED25519();

            // Create the admin account
            new AccountCreateTransaction()
                    .setKeyWithoutAlias(adminKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client);

            var key = PrivateKey.generateECDSA();
            var evmAddress = key.getPublicKey().toEvmAddress();

            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(adminKey)
                    .setAlias(evmAddress)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            assertThat(accountId).isNotNull();

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isNotNull();
            assertThat(info.contractAccountId).hasToString(evmAddress.toString());
            assertThat(info.key).isEqualTo(adminKey.getPublicKey());
        }
    }

    @Test
    @DisplayName("Cannot create account with alias different from admin key without signature")
    void cannotCreateAccountWithAliasWithoutSignature() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateED25519();

            // Create the admin account
            new AccountCreateTransaction()
                    .setKeyWithoutAlias(adminKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client);

            var key = PrivateKey.generateECDSA();

            assertThatExceptionOfType(BadKeyException.class)
                    .isThrownBy(() -> new AccountCreateTransaction()
                            .setReceiverSignatureRequired(true)
                            .setKeyWithAlias(key, adminKey)
                            .freezeWith(testEnv.client)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining("Private key is not ECDSA");
        }
    }

    @Test
    @DisplayName("Can create account with alias different from admin key with receiver sig required")
    void createAccountWithAliasWithReceiverSigRequired() throws Exception {
        // Tests the sixth row of this table
        // https://github.com/hashgraph/hedera-improvement-proposal/blob/d39f740021d7da592524cffeaf1d749803798e9a/HIP/hip-583.md#signatures
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateED25519();

            // Create the admin account
            new AccountCreateTransaction()
                    .setKeyWithoutAlias(adminKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client);

            var key = PrivateKey.generateECDSA();
            var evmAddress = key.getPublicKey().toEvmAddress();

            var accountId = new AccountCreateTransaction()
                    .setReceiverSignatureRequired(true)
                    .setKeyWithoutAlias(adminKey)
                    .setAlias(evmAddress)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            assertThat(accountId).isNotNull();

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isNotNull();
            assertThat(info.contractAccountId).hasToString(evmAddress.toString());
            assertThat(info.key).isEqualTo(adminKey.getPublicKey());
        }
    }

    @Test
    @DisplayName(
            "Cannot create account with alias different from admin key and receiver sig required without signature")
    void cannotCreateAccountWithAliasWithReceiverSigRequiredWithoutSignature() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();

            // Create the admin account
            new AccountCreateTransaction()
                    .setKeyWithoutAlias(adminKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client);

            var key = PrivateKey.generateECDSA();

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new AccountCreateTransaction()
                            .setReceiverSignatureRequired(true)
                            .setKeyWithAlias(adminKey)
                            .freezeWith(testEnv.client)
                            .sign(key)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    @Test
    @DisplayName("Cannot create account with alias different from admin key without both key signature")
    void cannotCreateAccountWithAliasWithoutBothKeySignatures() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateED25519();

            // Create the admin account
            new AccountCreateTransaction()
                    .setKeyWithoutAlias(adminKey)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client);

            var key = PrivateKey.generateECDSA();

            assertThatExceptionOfType(BadKeyException.class)
                    .isThrownBy(() -> new AccountCreateTransaction()
                            .setReceiverSignatureRequired(true)
                            .setKeyWithAlias(key, adminKey)
                            .freezeWith(testEnv.client)
                            .sign(adminKey)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining("Private key is not ECDSA");
        }
    }

    @Test
    @DisplayName(
            "Can create account with ECDSA key using setKeyWithAlias, account should have same ECDSA as key and same key's alias")
    void createAccountUsingSetKeyWithAliasAccountShouldHaveSameKeyAndSameKeysAlias() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var ecdsaKey = PrivateKey.generateECDSA();
            var evmAddress = ecdsaKey.getPublicKey().toEvmAddress();

            var accountId = new AccountCreateTransaction()
                    .setReceiverSignatureRequired(true)
                    .setKeyWithAlias(ecdsaKey)
                    .freezeWith(testEnv.client)
                    .sign(ecdsaKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            assertThat(accountId).isNotNull();

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isNotNull();
            assertThat(info.key).isEqualTo(ecdsaKey.getPublicKey());
            assertThat(info.contractAccountId).hasToString(evmAddress.toString());
        }
    }

    @Test
    @DisplayName("Can create account with using setKeyWithAlias, account should have key as key and ECDSA key as alias")
    void createAccountUsingSetKeyWithAliasAccountShouldHaveKeyAsKeyAndECDSAKEyAsAlias() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var ecdsaKey = PrivateKey.generateECDSA();
            var key = PrivateKey.generateED25519();

            var evmAddress = ecdsaKey.getPublicKey().toEvmAddress();

            var accountId = new AccountCreateTransaction()
                    .setReceiverSignatureRequired(true)
                    .setKeyWithAlias(key, ecdsaKey)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .sign(ecdsaKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            assertThat(accountId).isNotNull();

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isNotNull();
            assertThat(info.key).isEqualTo(key.getPublicKey());
            assertThat(info.contractAccountId).hasToString(evmAddress.toString());
        }
    }

    @Test
    @DisplayName("Can create account using setKeyWithoutAlias, account should have key as key and no alias")
    void createAccountUsingSetKeyWithoutAliasAccountShouldHaveKeyAsKeyAndNoAlias() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateECDSA();

            var accountId = new AccountCreateTransaction()
                    .setReceiverSignatureRequired(true)
                    .setKeyWithoutAlias(key)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            assertThat(accountId).isNotNull();

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isNotNull();
            assertThat(info.key).isEqualTo(key.getPublicKey());
            assertTrue(isLongZeroAddress(Hex.decode(info.contractAccountId)));
        }
    }

    @Test
    @DisplayName("Can not create account using setKeyWithAlias with only ed25519 key, exception should be thrown")
    void createAccountUsingSetKeyWithAliasWithED25519KeyShouldThrowAnException() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateED25519();

            var accountId = new AccountCreateTransaction()
                    .setReceiverSignatureRequired(true)
                    .setKeyWithoutAlias(key)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            assertThat(accountId).isNotNull();

            assertThatExceptionOfType(BadKeyException.class)
                    .isThrownBy(() -> new AccountCreateTransaction()
                            .setReceiverSignatureRequired(true)
                            .setKeyWithAlias(key)
                            .freezeWith(testEnv.client)
                            .sign(key)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining("Private key is not ECDSA");
        }
    }

    @Test
    @DisplayName(
            "Can create account with ECDSA public key using setKeyWithAlias, account should have public key as key and derived alias")
    void createAccountUsingSetKeyWithAliasWithPublicKeyShouldHavePublicKeyAndDerivedAlias() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var privateKey = PrivateKey.generateECDSA();
            var publicKey = privateKey.getPublicKey();
            var evmAddress = publicKey.toEvmAddress();

            var accountId = new AccountCreateTransaction()
                    .setKeyWithAlias(publicKey)
                    .freezeWith(testEnv.client)
                    .sign(privateKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            assertThat(accountId).isNotNull();

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isNotNull();
            assertThat(info.key).isEqualTo(publicKey);
            assertThat(info.contractAccountId).hasToString(evmAddress.toString());
        }
    }

    @Test
    @DisplayName(
            "Can create account with ED25519 key and ECDSA public key for alias, account should have ED25519 key and derived alias")
    void createAccountUsingSetKeyWithAliasWithED25519KeyAndPublicECDSAKeyShouldHaveED25519KeyAndDerivedAlias()
            throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var accountKey = PrivateKey.generateED25519();
            var aliasPrivateKey = PrivateKey.generateECDSA();
            var aliasPublicKey = aliasPrivateKey.getPublicKey();
            var evmAddress = aliasPublicKey.toEvmAddress();

            var accountId = new AccountCreateTransaction()
                    .setKeyWithAlias(accountKey, aliasPublicKey)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .sign(aliasPrivateKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            assertThat(accountId).isNotNull();

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isNotNull();
            assertThat(info.key).isEqualTo(accountKey.getPublicKey());
            assertThat(info.contractAccountId).hasToString(evmAddress.toString());
        }
    }

    private boolean isLongZeroAddress(byte[] address) {
        for (int i = 0; i < 12; i++) {
            if (address[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
