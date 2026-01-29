// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.hashgraph.sdk.proto.SignaturePair;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.TransactionList;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for signature poisoning vulnerability.
 *
 * <p>These tests verify that:
 * <ol>
 *   <li>A malicious coordinator cannot inject garbage signatures that block legitimate signers</li>
 *   <li>Poisoned transactions are rejected by consensus nodes</li>
 *   <li>Victims can recover by replacing poisoned signatures with valid ones</li>
 *   <li>Hardware wallet callbacks are not bypassed when signatures are poisoned</li>
 * </ol>
 */
class SigPoisoningIntegrationTest {

    /**
     * Helper function which extracts bodyBytes (the actual signed message) from transaction
     */
    private byte[] extractBodyBytes(Transaction<?> tx) throws InvalidProtocolBufferException {
        byte[] txBytes = tx.toBytes();
        var txList = TransactionList.parseFrom(txBytes);
        var protoTx = txList.getTransactionList(0);
        var signedTx = SignedTransaction.parseFrom(protoTx.getSignedTransactionBytes());
        return signedTx.getBodyBytes().toByteArray();
    }

    /**
     * Helper function which extracts signature bytes for a specific public key
     */
    private byte[] extractSignatureForKey(Transaction<?> tx, PublicKey pubKey) throws InvalidProtocolBufferException {
        byte[] txBytes = tx.toBytes();
        var txList = TransactionList.parseFrom(txBytes);
        var protoTx = txList.getTransactionList(0);
        var signedTx = SignedTransaction.parseFrom(protoTx.getSignedTransactionBytes());

        byte[] pubKeyBytes = pubKey.toBytesRaw();

        for (SignaturePair sigPair : signedTx.getSigMap().getSigPairList()) {
            byte[] prefix = sigPair.getPubKeyPrefix().toByteArray();
            if (Arrays.equals(prefix, pubKeyBytes)) {
                if (sigPair.hasEd25519()) {
                    return sigPair.getEd25519().toByteArray();
                } else if (sigPair.hasECDSASecp256K1()) {
                    return sigPair.getECDSASecp256K1().toByteArray();
                }
            }
        }
        return null;
    }

    /**
     * Injects a garbage signature for the victim's public key into the transaction bytes.
     * This simulates what a malicious coordinator would do.
     */
    private Transaction<?> poisonSignatureInBytes(Transaction<?> tx, PublicKey victimKey, byte[] garbageSignature)
            throws InvalidProtocolBufferException {
        byte[] txBytes = tx.toBytes();
        var txList = TransactionList.parseFrom(txBytes).toBuilder();

        var protoTx = txList.getTransactionList(0).toBuilder();
        var signedTx = SignedTransaction.parseFrom(protoTx.getSignedTransactionBytes()).toBuilder();

        // Remove any existing victim signature pairs (if present) and inject garbage.
        var sigMap = signedTx.getSigMap().toBuilder();
        var retained = sigMap.getSigPairList().stream()
                .filter(p -> !Arrays.equals(p.getPubKeyPrefix().toByteArray(), victimKey.toBytesRaw()))
                .toList();
        sigMap.clearSigPair();
        sigMap.addAllSigPair(retained);
        sigMap.addSigPair(SignaturePair.newBuilder()
                .setPubKeyPrefix(ByteString.copyFrom(victimKey.toBytesRaw()))
                .setEd25519(ByteString.copyFrom(garbageSignature))
                .build());

        signedTx.setSigMap(sigMap);
        protoTx.setSignedTransactionBytes(signedTx.build().toByteString());

        txList.setTransactionList(0, protoTx.build());

        return Transaction.fromBytes(txList.build().toByteArray());
    }

    @Test
    @DisplayName("Poisoned transaction with garbage signature is rejected by consensus node")
    void testPoisonedTransactionRejected() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create keys for multi-sig account: Alice (coordinator) and Bob (victim)
            var aliceKey = PrivateKey.generateED25519();
            var bobKey = PrivateKey.generateED25519();

            // Create a multi-sig account that requires both Alice and Bob to sign
            var keyList = new KeyList();
            keyList.add(aliceKey.getPublicKey());
            keyList.add(bobKey.getPublicKey());

            var multiSigAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(keyList)
                    .setInitialBalance(new Hbar(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Create a recipient account
            var recipientKey = PrivateKey.generateED25519();
            var recipientId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(recipientKey)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Create a transfer FROM the multi-sig account (requires both Alice and Bob signatures)
            var tx = new TransferTransaction()
                    .setTransactionId(TransactionId.generate(multiSigAccountId))
                    .addHbarTransfer(multiSigAccountId, Hbar.fromTinybars(-100))
                    .addHbarTransfer(recipientId, Hbar.fromTinybars(100));

            // Freeze and sign with Alice
            tx.freezeWith(testEnv.client);
            tx.sign(aliceKey);

            // Coordinator (malicious) injects garbage signature for Bob
            byte[] garbageSignature = new byte[64];
            new SecureRandom().nextBytes(garbageSignature);
            var poisoned = poisonSignatureInBytes(tx, bobKey.getPublicKey(), garbageSignature);

            // Verify the poisoned signature is present and invalid
            byte[] bodyBytes = extractBodyBytes(poisoned);
            byte[] storedPoisoned = extractSignatureForKey(poisoned, bobKey.getPublicKey());
            assertThat(storedPoisoned).isNotNull();
            assertThat(bobKey.getPublicKey().verify(bodyBytes, storedPoisoned))
                    .as("Poisoned signature should be invalid")
                    .isFalse();
            assertThat(Arrays.equals(storedPoisoned, garbageSignature))
                    .as("Poisoned signature should match injected garbage")
                    .isTrue();

            // Attempt to execute: should be rejected by consensus node due to invalid signature
            try {
                TransactionResponse response = poisoned.execute(testEnv.client);
                // If it passes precheck, check receipt status - should still fail
                var receipt = response.getReceipt(testEnv.client);
                // Transaction should be rejected due to invalid signature
                assertThat(receipt.status)
                        .as("Transaction with poisoned signature should be rejected by consensus node")
                        .isIn(Status.INVALID_SIGNATURE, Status.INVALID_SIGNATURE_COUNT_MISMATCHING_KEY);
            } catch (PrecheckStatusException e) {
                // Network rejected at precheck - this is also expected
                assertThat(e.status)
                        .as("Transaction with poisoned signature should be rejected at precheck")
                        .isIn(Status.INVALID_SIGNATURE, Status.INVALID_SIGNATURE_COUNT_MISMATCHING_KEY);
            }
        }
    }

    @Test
    @DisplayName("Victim can recover from poisoned signature and transaction succeeds")
    void testRecoveryFromPoisonedSignature() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create two accounts for multi-sig: Alice (coordinator) and Bob (victim)
            var aliceKey = PrivateKey.generateED25519();
            var aliceId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(aliceKey)
                    .setInitialBalance(new Hbar(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var bobKey = PrivateKey.generateED25519();
            var bobId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(bobKey)
                    .setInitialBalance(new Hbar(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Create a multi-sig transfer transaction
            var tx = new TransferTransaction()
                    .addHbarTransfer(aliceId, Hbar.fromTinybars(-100))
                    .addHbarTransfer(bobId, Hbar.fromTinybars(100));

            // Alice signs legitimately
            tx.freezeWith(testEnv.client);
            tx.sign(aliceKey);

            // Coordinator injects garbage signature for Bob
            byte[] garbageSignature = new byte[64];
            new SecureRandom().nextBytes(garbageSignature);
            var poisoned = poisonSignatureInBytes(tx, bobKey.getPublicKey(), garbageSignature);

            // Victim recovers by signing (should replace poisoned signature)
            poisoned.sign(bobKey);

            // Transaction should now succeed
            var response = poisoned.execute(testEnv.client);
            var receipt = response.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName("Hardware wallet callback is invoked even when signature was poisoned")
    void testHardwareWalletCallbackNotBypassed() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create two accounts for multi-sig: Alice (coordinator) and Bob (victim with hardware wallet)
            var aliceKey = PrivateKey.generateED25519();
            var aliceId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(aliceKey)
                    .setInitialBalance(new Hbar(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var bobKey = PrivateKey.generateED25519();
            var bobId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(bobKey)
                    .setInitialBalance(new Hbar(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Create a multi-sig transfer transaction
            var tx = new TransferTransaction()
                    .addHbarTransfer(aliceId, Hbar.fromTinybars(-100))
                    .addHbarTransfer(bobId, Hbar.fromTinybars(100));

            // Alice signs legitimately
            tx.freezeWith(testEnv.client);
            tx.sign(aliceKey);

            // Coordinator injects garbage signature for Bob
            byte[] garbageSignature = new byte[64];
            new SecureRandom().nextBytes(garbageSignature);
            var poisoned = poisonSignatureInBytes(tx, bobKey.getPublicKey(), garbageSignature);

            // Simulate hardware wallet with invocation counter
            AtomicInteger hwWalletInvocations = new AtomicInteger(0);

            var hardwareWalletSigner = (java.util.function.UnaryOperator<byte[]>) (message) -> {
                hwWalletInvocations.incrementAndGet();
                return bobKey.sign(message);
            };

            assertThat(hwWalletInvocations.get())
                    .as("Sanity: not invoked before signWith()")
                    .isEqualTo(0);

            // Application calls signWith(): must invoke signer and replace poisoned signature
            poisoned.signWith(bobKey.getPublicKey(), hardwareWalletSigner);
            // Signers are applied lazily during serialization/build.
            // Force build/serialization and ensure the signer was invoked.
            poisoned.toBytes();
            assertThat(hwWalletInvocations.get()).isEqualTo(1);

            // Transaction should now succeed with hardware wallet signature
            var response = poisoned.execute(testEnv.client);
            var receipt = response.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName("Normal multi-sig transaction without poisoning succeeds")
    void testNormalMultiSigTransactionSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create two accounts for multi-sig: Alice and Bob
            var aliceKey = PrivateKey.generateED25519();
            var aliceId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(aliceKey)
                    .setInitialBalance(new Hbar(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var bobKey = PrivateKey.generateED25519();
            var bobId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(bobKey)
                    .setInitialBalance(new Hbar(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Create a multi-sig transfer transaction
            var tx = new TransferTransaction()
                    .addHbarTransfer(aliceId, Hbar.fromTinybars(-100))
                    .addHbarTransfer(bobId, Hbar.fromTinybars(100));

            // Both sign normally (no poisoning)
            tx.freezeWith(testEnv.client);
            tx.sign(aliceKey);
            tx.sign(bobKey);

            // Transaction should succeed
            var response = tx.execute(testEnv.client);
            var receipt = response.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }
}
