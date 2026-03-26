// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.protobuf.ByteString;

import com.hedera.hashgraph.sdk.proto.AccountAmount;
import com.hedera.hashgraph.sdk.proto.AccountID;
import com.hedera.hashgraph.sdk.proto.CryptoTransferTransactionBody;
import com.hedera.hashgraph.sdk.proto.Duration;
import com.hedera.hashgraph.sdk.proto.FileAppendTransactionBody;
import com.hedera.hashgraph.sdk.proto.FileID;
import com.hedera.hashgraph.sdk.proto.SignatureMap;
import com.hedera.hashgraph.sdk.proto.SignaturePair;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.Timestamp;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionID;
import com.hedera.hashgraph.sdk.proto.TransactionList;
import com.hedera.hashgraph.sdk.proto.TransferList;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for Cross-Group Transaction Body Forgery (Immunefi #70093).
 *
 * Verifies that Transaction.fromBytes() rejects TransactionList payloads containing
 * multiple TransactionId groups with divergent bodies for non-chunked types, and
 * validates cross-group body consistency for chunked types.
 */
public class CrossGroupFixTest {

    private static final AccountID VICTIM_PROTO  = accountId(100);
    private static final AccountID ATTACKER_PROTO = accountId(200);

    private static final List<AccountID> NODES = List.of(accountId(3), accountId(4), accountId(5));

    private static final long BENIGN_AMOUNT    = 1L;
    private static final long MALICIOUS_AMOUNT = 100_000_000_000L;

    private static final TransactionID TX_ID_1 = TransactionID.newBuilder()
            .setAccountID(VICTIM_PROTO)
            .setTransactionValidStart(Timestamp.newBuilder().setSeconds(1234567890))
            .build();
    private static final TransactionID TX_ID_2 = TransactionID.newBuilder()
            .setAccountID(VICTIM_PROTO)
            .setTransactionValidStart(Timestamp.newBuilder().setSeconds(1234567891))
            .build();

    private static final Duration TX_DURATION = Duration.newBuilder().setSeconds(120).build();
    private static final long TX_FEE = 100_000_000L;

    private static final CryptoTransferTransactionBody BENIGN_TRANSFER =
            CryptoTransferTransactionBody.newBuilder()
                    .setTransfers(TransferList.newBuilder()
                            .addAccountAmounts(amount(VICTIM_PROTO, -BENIGN_AMOUNT))
                            .addAccountAmounts(amount(ATTACKER_PROTO, BENIGN_AMOUNT)))
                    .build();

    private static final CryptoTransferTransactionBody MALICIOUS_TRANSFER =
            CryptoTransferTransactionBody.newBuilder()
                    .setTransfers(TransferList.newBuilder()
                            .addAccountAmounts(amount(VICTIM_PROTO, -MALICIOUS_AMOUNT))
                            .addAccountAmounts(amount(ATTACKER_PROTO, MALICIOUS_AMOUNT)))
                    .build();

    // =========================================================================
    //  Layer 1: Multi-group non-chunked rejection
    // =========================================================================

    @Test
    @DisplayName("Multi-group CryptoTransfer TransactionList is rejected by fromBytes()")
    void crossGroupForgery_isRejected() {
        var attackerKey = PrivateKey.generateED25519();
        byte[] maliciousPayload = buildMaliciousCryptoTransferPayload(attackerKey);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Transaction.fromBytes(maliciousPayload))
                .withMessageContaining("Non-chunked transaction types must not have multiple transaction ID groups");
    }

    @Test
    @DisplayName("Single-group CryptoTransfer TransactionList works normally")
    void singleGroupTransactionStillWorks() throws Exception {
        var attackerKey = PrivateKey.generateED25519();
        byte[] singleGroupPayload = buildSingleGroupCryptoTransferPayload(attackerKey);

        var tx = Transaction.fromBytes(singleGroupPayload);
        assertThat(tx).isInstanceOf(TransferTransaction.class);

        var transferTx = (TransferTransaction) tx;
        var hbarTransfers = transferTx.getHbarTransfers();
        assertThat(hbarTransfers.get(new AccountId(0, 0, 100))).isEqualTo(Hbar.fromTinybars(-BENIGN_AMOUNT));
        assertThat(hbarTransfers.get(new AccountId(0, 0, 200))).isEqualTo(Hbar.fromTinybars(BENIGN_AMOUNT));
    }

    // =========================================================================
    //  Layer 2: Chunked cross-group validation
    // =========================================================================

    @Test
    @DisplayName("Legitimate multi-chunk FileAppendTransaction round-trips successfully")
    void multiGroupChunkedTransactionStillWorks() throws Exception {
        var privateKey = PrivateKey.fromString(
                "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

        // Create a large FileAppendTransaction that requires multiple chunks
        byte[] largeContents = new byte[4096]; // exceeds 2048 chunk size -> 2 chunks
        Arrays.fill(largeContents, (byte) 0x42);

        var tx = new FileAppendTransaction()
                .setNodeAccountIds(List.of(new AccountId(0, 0, 3)))
                .setTransactionId(TransactionId.withValidStart(new AccountId(0, 0, 100), Instant.ofEpochSecond(1234567890)))
                .setFileId(FileId.fromString("0.0.6006"))
                .setContents(largeContents)
                .setMaxTransactionFee(Hbar.fromTinybars(TX_FEE))
                .freeze()
                .sign(privateKey);

        byte[] bytes = tx.toBytes();

        // Verify it produces multiple groups
        var list = TransactionList.parseFrom(bytes);
        assertThat(list.getTransactionListCount()).isGreaterThan(1);

        // Must round-trip successfully
        var restored = Transaction.fromBytes(bytes);
        assertThat(restored).isInstanceOf(FileAppendTransaction.class);
    }

    @Test
    @DisplayName("Multi-chunk FileAppendTransaction with divergent fileID is rejected")
    void multiGroupChunkedWithDivergentBodiesThrows() {
        var attackerKey = PrivateKey.generateED25519();
        byte[] divergentPayload = buildDivergentFileAppendPayload(attackerKey);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Transaction.fromBytes(divergentPayload));
    }

    // =========================================================================
    //  Layer 0: DataCase consistency
    // =========================================================================

    @Test
    @DisplayName("Mixed DataCase TransactionList is rejected by fromBytes()")
    void mixedDataCaseIsRejected() {
        var attackerKey = PrivateKey.generateED25519();
        byte[] mixedPayload = buildMixedDataCasePayload(attackerKey);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Transaction.fromBytes(mixedPayload))
                .withMessageContaining("All transactions in a TransactionList must have the same data type");
    }

    // =========================================================================
    //  PAYLOAD CONSTRUCTION
    // =========================================================================

    private static byte[] buildMaliciousCryptoTransferPayload(PrivateKey attackerKey) {
        byte[] attackerPubRaw = attackerKey.getPublicKey().toBytesRaw();
        var list = TransactionList.newBuilder();

        // Group 1: benign transfer
        for (var node : NODES) {
            list.addTransactionList(buildCryptoTransferProto(TX_ID_1, node, BENIGN_TRANSFER, attackerKey, attackerPubRaw));
        }
        // Group 2: malicious transfer
        for (var node : NODES) {
            list.addTransactionList(buildCryptoTransferProto(TX_ID_2, node, MALICIOUS_TRANSFER, attackerKey, attackerPubRaw));
        }

        return list.build().toByteArray();
    }

    private static byte[] buildSingleGroupCryptoTransferPayload(PrivateKey attackerKey) {
        byte[] attackerPubRaw = attackerKey.getPublicKey().toBytesRaw();
        var list = TransactionList.newBuilder();

        // Single group: benign transfer only
        for (var node : NODES) {
            list.addTransactionList(buildCryptoTransferProto(TX_ID_1, node, BENIGN_TRANSFER, attackerKey, attackerPubRaw));
        }

        return list.build().toByteArray();
    }

    private static byte[] buildDivergentFileAppendPayload(PrivateKey attackerKey) {
        byte[] attackerPubRaw = attackerKey.getPublicKey().toBytesRaw();
        var list = TransactionList.newBuilder();

        var fileAppend1 = FileAppendTransactionBody.newBuilder()
                .setFileID(FileID.newBuilder().setFileNum(6006))
                .setContents(ByteString.copyFrom(new byte[]{1, 2, 3}))
                .build();

        // Group 2 has a DIFFERENT fileID — this is the attack
        var fileAppend2 = FileAppendTransactionBody.newBuilder()
                .setFileID(FileID.newBuilder().setFileNum(9999))
                .setContents(ByteString.copyFrom(new byte[]{4, 5, 6}))
                .build();

        // Group 1
        for (var node : NODES) {
            list.addTransactionList(buildFileAppendProto(TX_ID_1, node, fileAppend1, attackerKey, attackerPubRaw));
        }
        // Group 2 with divergent fileID
        for (var node : NODES) {
            list.addTransactionList(buildFileAppendProto(TX_ID_2, node, fileAppend2, attackerKey, attackerPubRaw));
        }

        return list.build().toByteArray();
    }

    private static byte[] buildMixedDataCasePayload(PrivateKey attackerKey) {
        byte[] attackerPubRaw = attackerKey.getPublicKey().toBytesRaw();
        var list = TransactionList.newBuilder();

        var fileAppend = FileAppendTransactionBody.newBuilder()
                .setFileID(FileID.newBuilder().setFileNum(6006))
                .setContents(ByteString.copyFrom(new byte[]{1, 2, 3}))
                .build();

        // Group 1: FileAppend
        for (var node : NODES) {
            list.addTransactionList(buildFileAppendProto(TX_ID_1, node, fileAppend, attackerKey, attackerPubRaw));
        }
        // Group 2: CryptoTransfer (different DataCase!)
        for (var node : NODES) {
            list.addTransactionList(buildCryptoTransferProto(TX_ID_2, node, BENIGN_TRANSFER, attackerKey, attackerPubRaw));
        }

        return list.build().toByteArray();
    }

    // =========================================================================
    //  PROTO BUILDERS
    // =========================================================================

    private static com.hedera.hashgraph.sdk.proto.Transaction buildCryptoTransferProto(
            TransactionID txId, AccountID node, CryptoTransferTransactionBody transfer,
            PrivateKey signerKey, byte[] signerPubRaw) {

        var txBody = TransactionBody.newBuilder()
                .setTransactionID(txId)
                .setNodeAccountID(node)
                .setTransactionFee(TX_FEE)
                .setTransactionValidDuration(TX_DURATION)
                .setCryptoTransfer(transfer)
                .build();

        return signAndWrap(txBody, signerKey, signerPubRaw);
    }

    private static com.hedera.hashgraph.sdk.proto.Transaction buildFileAppendProto(
            TransactionID txId, AccountID node, FileAppendTransactionBody fileAppend,
            PrivateKey signerKey, byte[] signerPubRaw) {

        var txBody = TransactionBody.newBuilder()
                .setTransactionID(txId)
                .setNodeAccountID(node)
                .setTransactionFee(TX_FEE)
                .setTransactionValidDuration(TX_DURATION)
                .setFileAppend(fileAppend)
                .build();

        return signAndWrap(txBody, signerKey, signerPubRaw);
    }

    private static com.hedera.hashgraph.sdk.proto.Transaction signAndWrap(
            TransactionBody txBody, PrivateKey signerKey, byte[] signerPubRaw) {

        byte[] bodyBytes = txBody.toByteString().toByteArray();
        byte[] signature = signerKey.sign(bodyBytes);

        var sigPair = SignaturePair.newBuilder()
                .setPubKeyPrefix(ByteString.copyFrom(signerPubRaw))
                .setEd25519(ByteString.copyFrom(signature))
                .build();

        var signedTx = SignedTransaction.newBuilder()
                .setBodyBytes(ByteString.copyFrom(bodyBytes))
                .setSigMap(SignatureMap.newBuilder().addSigPair(sigPair))
                .build();

        return com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                .setSignedTransactionBytes(signedTx.toByteString())
                .build();
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private static AccountID accountId(long num) {
        return AccountID.newBuilder().setShardNum(0).setRealmNum(0).setAccountNum(num).build();
    }

    private static AccountAmount amount(AccountID account, long amt) {
        return AccountAmount.newBuilder().setAccountID(account).setAmount(amt).build();
    }
}
