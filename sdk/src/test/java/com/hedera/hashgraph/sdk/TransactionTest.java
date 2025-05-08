// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.Transaction.fromBytes;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.TokenAssociateTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final List<AccountId> testNodeAccountIds =
            Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006"));
    private static final AccountId testAccountId = AccountId.fromString("0.0.5006");
    private static final Instant validStart = Instant.ofEpochSecond(1554158542);

    @Test
    void transactionFromBytesWorksWithProtobufTransactionBytes() throws InvalidProtocolBufferException {
        var bytes = Hex.decode(
                "1acc010a640a2046fe5013b6f6fc796c3e65ec10d2a10d03c07188fc3de13d46caad6b8ec4dfb81a4045f1186be5746c9783f68cb71d6a71becd3ffb024906b855ac1fa3a2601273d41b58446e5d6a0aaf421c229885f9e70417353fab2ce6e9d8e7b162e9944e19020a640a20f102e75ff7dc3d72c9b7075bb246fcc54e714c59714814011e8f4b922d2a6f0a1a40f2e5f061349ab03fa21075020c75cf876d80498ae4bac767f35941b8e3c393b0e0a886ede328e44c1df7028ea1474722f2dcd493812d04db339480909076a10122500a180a0c08a1cc98830610c092d09e0312080800100018e4881d120608001000180418b293072202087872240a220a0f0a080800100018e4881d10ff83af5f0a0f0a080800100018eb881d108084af5f");

        var transaction = (TransferTransaction) fromBytes(bytes);

        assertThat(transaction.getHbarTransfers()).containsEntry(new AccountId(0, 0, 476260), new Hbar(1).negated());
        assertThat(transaction.getHbarTransfers()).containsEntry(new AccountId(0, 0, 476267), new Hbar(1));
    }

    @Test
    void tokenAssociateTransactionFromTransactionBodyBytes() throws InvalidProtocolBufferException {
        var tokenAssociateTransactionBodyProto =
                TokenAssociateTransactionBody.newBuilder().build();
        var transactionBodyProto = TransactionBody.newBuilder()
                .setTokenAssociate(tokenAssociateTransactionBodyProto)
                .build();

        TokenAssociateTransaction tokenAssociateTransaction = spawnTestTransaction(transactionBodyProto);

        var tokenAssociateTransactionFromBytes = Transaction.fromBytes(tokenAssociateTransaction.toBytes());

        assertThat(tokenAssociateTransactionFromBytes).isInstanceOf(TokenAssociateTransaction.class);
    }

    @Test
    void tokenAssociateTransactionFromSignedTransactionBytes() throws InvalidProtocolBufferException {
        var tokenAssociateTransactionBodyProto =
                TokenAssociateTransactionBody.newBuilder().build();
        var transactionBodyProto = TransactionBody.newBuilder()
                .setTokenAssociate(tokenAssociateTransactionBodyProto)
                .build();

        var signedTransactionProto = SignedTransaction.newBuilder()
                .setBodyBytes(transactionBodyProto.toByteString())
                .build();
        var signedTransactionBodyProto = TransactionBody.parseFrom(signedTransactionProto.getBodyBytes());

        TokenAssociateTransaction tokenAssociateTransaction = spawnTestTransaction(signedTransactionBodyProto);

        var tokenAssociateTransactionFromBytes = Transaction.fromBytes(tokenAssociateTransaction.toBytes());

        assertThat(tokenAssociateTransactionFromBytes).isInstanceOf(TokenAssociateTransaction.class);
    }

    @Test
    void tokenAssociateTransactionFromTransactionBytes() throws InvalidProtocolBufferException {
        var tokenAssociateTransactionBodyProto =
                TokenAssociateTransactionBody.newBuilder().build();
        var transactionBodyProto = TransactionBody.newBuilder()
                .setTokenAssociate(tokenAssociateTransactionBodyProto)
                .build();

        var signedTransactionProto = SignedTransaction.newBuilder()
                .setBodyBytes(transactionBodyProto.toByteString())
                .build();
        var signedTransactionBodyProto = TransactionBody.parseFrom(signedTransactionProto.getBodyBytes());

        var transactionSignedProto = com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                .setSignedTransactionBytes(signedTransactionBodyProto.toByteString())
                .build();
        var transactionSignedBodyProto = TransactionBody.parseFrom(transactionSignedProto.getSignedTransactionBytes());

        TokenAssociateTransaction tokenAssociateTransaction = spawnTestTransaction(transactionSignedBodyProto);

        var tokenAssociateTransactionFromBytes = Transaction.fromBytes(tokenAssociateTransaction.toBytes());

        assertThat(tokenAssociateTransactionFromBytes).isInstanceOf(TokenAssociateTransaction.class);
    }

    private TokenAssociateTransaction spawnTestTransaction(TransactionBody txBody) {
        return new TokenAssociateTransaction(txBody)
                .setNodeAccountIds(testNodeAccountIds)
                .setTransactionId(TransactionId.withValidStart(testAccountId, validStart))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    @DisplayName("two identical transactions should have the same size")
    void sameSizeForIdenticalTransactions() {

        var accountCreateTransaction = new AccountCreateTransaction()
                .setInitialBalance(new Hbar(2))
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        var accountCreateTransaction2 = new AccountCreateTransaction()
                .setInitialBalance(new Hbar(2))
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        assertThat(accountCreateTransaction.getTransactionSize())
                .isEqualTo(accountCreateTransaction2.getTransactionSize());
    }

    @Test
    @DisplayName("signed Transaction should have larger size")
    void signedTransactionShouldHaveLargerSize() {

        var accountCreateTransaction = new AccountCreateTransaction()
                .setInitialBalance(new Hbar(2))
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze()
                .sign(PrivateKey.generateECDSA());

        var accountCreateTransaction2 = new AccountCreateTransaction()
                .setInitialBalance(new Hbar(2))
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        assertThat(accountCreateTransaction.getTransactionSize())
                .isGreaterThan(accountCreateTransaction2.getTransactionSize());
    }

    @Test
    @DisplayName("Transaction with larger content should have larger transactionBody")
    void transactionWithLargerContentShouldHaveLargerTransactionBody() {
        var fileCreateTransactionSmallContent = new FileCreateTransaction()
                .setContents("smallBody")
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();
        var fileCreateTransactionLargeContent = new FileCreateTransaction()
                .setContents("largeLargeBody")
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        assertThat(fileCreateTransactionSmallContent.getTransactionBodySize())
                .isLessThan(fileCreateTransactionLargeContent.getTransactionBodySize());
    }

    @Test
    @DisplayName("Transaction with without optional fields should have smaller transactionBody")
    void transactionWithoutOptionalFieldsShouldHaveSmallerTransactionBody() {
        var noOptionalFieldsTransaction = new AccountCreateTransaction()
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        var fullOptionalFieldsTransaction = new AccountCreateTransaction()
                .setInitialBalance(new Hbar(2))
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .setMaxTransactionFee(new Hbar(1))
                .setTransactionValidDuration(Duration.ofHours(1))
                .freeze();

        assertThat(noOptionalFieldsTransaction.getTransactionBodySize())
                .isLessThan(fullOptionalFieldsTransaction.getTransactionBodySize());
    }

    @Test
    @DisplayName("Should return array of body sizes for multi-chunk transaction")
    void multiChunkTransactionShouldReturnArrayOfBodySizes() {

        var chunkSize = 1024;
        byte[] content = new byte[chunkSize * 3];
        Arrays.fill(content, (byte) 'a');

        var fileAppentTx = new FileAppendTransaction()
                .setFileId(new FileId(1))
                .setChunkSize(chunkSize)
                .setContents(content)
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        var objects = fileAppentTx.bodySizeAllChunks();
        assertThat(objects).isNotNull();
        assertThat(objects).hasSize(3);
    }

    @Test
    @DisplayName("Should return array of one size for single-chunk transaction")
    void singleChunkTransactionShouldReturnArrayOfOneSize() {
        // Small enough for one chunk
        byte[] smallContent = new byte[500];
        Arrays.fill(smallContent, (byte) 'a');

        var fileAppendTx = new FileAppendTransaction()
                .setFileId(new FileId(1))
                .setContents(smallContent)
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        var bodySizes = fileAppendTx.bodySizeAllChunks();

        assertThat(bodySizes).isNotNull();
        assertThat(bodySizes).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty array for transaction with no content")
    void transactionWithNoContentShouldReturnEmptyArray() {
        var fileAppendTx = new FileAppendTransaction()
                .setFileId(new FileId(1))
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        var bodySizes = fileAppendTx.bodySizeAllChunks();

        assertThat(bodySizes).isNotNull();
        assertThat(bodySizes).isEmpty();
    }

    @Test
    @DisplayName("Should return proper sizes for FileAppend transactions when chunking occurs")
    void chunkedFileAppendTransactionShouldReturnProperSizes() {
        byte[] largeContent = new byte[2048];
        Arrays.fill(largeContent, (byte) 'a');

        var largeFileAppendTx = new FileAppendTransaction()
                .setFileId(new FileId(1))
                .setContents(largeContent)
                .setChunkSize(1024)
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        long largeSize = largeFileAppendTx.getTransactionSize();

        byte[] smallContent = new byte[512];
        Arrays.fill(smallContent, (byte) 'a');

        var smallFileAppendTx = new FileAppendTransaction()
                .setFileId(new FileId(1))
                .setContents(smallContent)
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        long smallSize = smallFileAppendTx.getTransactionSize();

        // Since large content is 2KB and chunk size is 1KB, this should create 2 chunks
        // Size should be greater than single chunk size
        assertThat(largeSize).isGreaterThan(1024);

        // The larger chunked transaction should be bigger than the small single-chunk transaction
        assertThat(largeSize).isGreaterThan(smallSize);
    }
}
