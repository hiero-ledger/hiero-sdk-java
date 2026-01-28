// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SignaturePair;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.TransactionList;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;

/**
 *
 * Regression tests for signature poisoning.
 *
 * A malicious coordinator may tamper with a partially-signed transaction by inserting
 * garbage signature bytes for another signer's public key. The SDK must not treat that
 * as "already signed", must not bypass signer callbacks, and must allow the victim to
 * replace the poisoned signature with a valid one.
 */
public class SigPoisoningPocTest {

    private static final AccountId PAYER = new AccountId(0, 0, 1001);
    private static final AccountId RECIPIENT = new AccountId(0, 0, 1002);
    private static final AccountId NODE = new AccountId(0, 0, 3);
    private static final Instant VALID_START = Instant.parse("2024-01-01T00:00:00Z");

    private PrivateKey alicePrivateKey;
    private PublicKey alicePublicKey;
    private PrivateKey bobPrivateKey;
    private PublicKey bobPublicKey;

    @BeforeEach
    void setUp() {
        // Generate fresh ED25519 keys for each test
        alicePrivateKey = PrivateKey.generateED25519();
        alicePublicKey = alicePrivateKey.getPublicKey();
        bobPrivateKey = PrivateKey.generateED25519();
        bobPublicKey = bobPrivateKey.getPublicKey();
    }

    // Helper function which extracts bodyBytes (the actual signed message) from transaction

    private byte[] extractBodyBytes(Transaction<?> tx) throws InvalidProtocolBufferException {
        byte[] txBytes = tx.toBytes();
        var txList = TransactionList.parseFrom(txBytes);
        var protoTx = txList.getTransactionList(0);
        var signedTx = SignedTransaction.parseFrom(protoTx.getSignedTransactionBytes());
        return signedTx.getBodyBytes().toByteArray();
    }

    // Helper function which extracts signature bytes for a specific public key

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

    // Helper function which counts signatures in transaction

    private int countSignatures(Transaction<?> tx) throws InvalidProtocolBufferException {
        byte[] txBytes = tx.toBytes();
        var txList = TransactionList.parseFrom(txBytes);
        var protoTx = txList.getTransactionList(0);
        var signedTx = SignedTransaction.parseFrom(protoTx.getSignedTransactionBytes());
        return signedTx.getSigMap().getSigPairCount();
    }

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
                .setPubKeyPrefix(com.google.protobuf.ByteString.copyFrom(victimKey.toBytesRaw()))
                .setEd25519(com.google.protobuf.ByteString.copyFrom(garbageSignature))
                .build());

        signedTx.setSigMap(sigMap);
        protoTx.setSignedTransactionBytes(signedTx.build().toByteString());

        txList.setTransactionList(0, protoTx.build());

        return com.hedera.hashgraph.sdk.Transaction.fromBytes(txList.build().toByteArray());
    }

    // Helper function which creates a test transaction

    private TransferTransaction createTestTransaction() {
        return new TransferTransaction()
                .setTransactionId(new TransactionId(PAYER, VALID_START))
                .setNodeAccountIds(List.of(NODE))
                .addHbarTransfer(PAYER, Hbar.fromTinybars(-100))
                .addHbarTransfer(RECIPIENT, Hbar.fromTinybars(100));
    }

    // First impact: transaction tampering must not block victim signing

    @Test
    @DisplayName("Poisoned signature does not block victim signing (signature is replaced)")
    void testTransactionTampering() throws Exception {
        var tx = createTestTransaction().freeze();
        byte[] bodyBytes = extractBodyBytes(tx);

        tx.sign(alicePrivateKey);

        byte[] garbageSignature = new byte[64];
        new SecureRandom().nextBytes(garbageSignature);
        var poisoned = poisonSignatureInBytes(tx, bobPublicKey, garbageSignature);

        // Prove the poisoned signature is invalid
        byte[] storedPoisoned = extractSignatureForKey(poisoned, bobPublicKey);
        assertThat(bobPublicKey.verify(bodyBytes, storedPoisoned)).isFalse();

        // Victim signs: must replace poisoned signature
        poisoned.sign(bobPrivateKey);
        byte[] storedAfter = extractSignatureForKey(poisoned, bobPublicKey);
        assertThat(bobPublicKey.verify(bodyBytes, storedAfter)).isTrue();
        assertThat(Arrays.equals(storedAfter, garbageSignature)).isFalse();
        assertThat(countSignatures(poisoned)).isEqualTo(2);
    }

    // Second impact: signer callback must not be bypassed

    @Test
    @DisplayName("Poisoned signature does not bypass signer callback (hardware wallet invoked)")
    void testHardwareWalletBypass() throws Exception {
        var tx = createTestTransaction().freeze();
        byte[] bodyBytes = extractBodyBytes(tx);

        tx.sign(alicePrivateKey);

        byte[] garbage = new byte[64];
        new SecureRandom().nextBytes(garbage);
        var poisoned = poisonSignatureInBytes(tx, bobPublicKey, garbage);

        AtomicInteger hwWalletInvocations = new AtomicInteger(0);

        var hardwareWalletSigner = (java.util.function.UnaryOperator<byte[]>) (message) -> {
            hwWalletInvocations.incrementAndGet();
            return bobPrivateKey.sign(message);
        };

        assertThat(hwWalletInvocations.get())
                .as("Sanity: not invoked before signWith()")
                .isEqualTo(0);

        // Application calls signWith(): must invoke signer and replace poisoned signature
        poisoned.signWith(bobPublicKey, hardwareWalletSigner);
        // Signers are applied lazily during serialization/build.
        // Force build/serialization and ensure the signer was invoked.
        poisoned.toBytes();
        assertThat(hwWalletInvocations.get()).isEqualTo(1);

        byte[] storedSig = extractSignatureForKey(poisoned, bobPublicKey);
        assertThat(bobPublicKey.verify(bodyBytes, storedSig)).isTrue();
    }

    // demonstrate recovery is possible

    @Test
    @DisplayName("Victim can recover from a poisoned signature")
    void testRecoveryPossible() throws Exception {
        var tx = createTestTransaction().freeze();
        byte[] bodyBytes = extractBodyBytes(tx);

        byte[] garbage = new byte[64];
        new SecureRandom().nextBytes(garbage);
        var poisoned = poisonSignatureInBytes(tx, bobPublicKey, garbage);
        assertThat(bobPublicKey.verify(bodyBytes, extractSignatureForKey(poisoned, bobPublicKey)))
                .as("Poisoned signature should start invalid")
                .isFalse();

        // Recovery attempt 1: sign()
        poisoned.sign(bobPrivateKey);
        byte[] sig1 = extractSignatureForKey(poisoned, bobPublicKey);
        assertThat(bobPublicKey.verify(bodyBytes, sig1)).isTrue();

        // Re-poison and recover via signWith()
        poisoned = poisonSignatureInBytes(poisoned, bobPublicKey, garbage);
        poisoned.signWith(bobPublicKey, bobPrivateKey::sign);
        byte[] sig2 = extractSignatureForKey(poisoned, bobPublicKey);
        assertThat(bobPublicKey.verify(bodyBytes, sig2)).isTrue();

        // Re-poison and recover via addSignature() with correct signature
        poisoned = poisonSignatureInBytes(poisoned, bobPublicKey, garbage);
        byte[] correctSig = bobPrivateKey.sign(bodyBytes);
        poisoned.addSignature(bobPublicKey, correctSig);
        byte[] sig3 = extractSignatureForKey(poisoned, bobPublicKey);
        assertThat(bobPublicKey.verify(bodyBytes, sig3)).isTrue();
    }

    // test normal signing works correctly

    @Test
    @DisplayName("show normal multi-sig signing works when not poisoned")
    void testNormalSigningWorks() throws Exception {
        var tx = createTestTransaction().freeze();
        byte[] bodyBytes = extractBodyBytes(tx);

        // Both sign normally (no poisoning)
        tx.sign(alicePrivateKey);
        tx.sign(bobPrivateKey);

        // Verify both signatures are valid
        byte[] aliceSig = extractSignatureForKey(tx, alicePublicKey);
        byte[] bobSig = extractSignatureForKey(tx, bobPublicKey);

        boolean aliceValid = alicePublicKey.verify(bodyBytes, aliceSig);
        boolean bobValid = bobPublicKey.verify(bodyBytes, bobSig);

        assertThat(aliceValid).isTrue();
        assertThat(bobValid).isTrue();
    }
}
