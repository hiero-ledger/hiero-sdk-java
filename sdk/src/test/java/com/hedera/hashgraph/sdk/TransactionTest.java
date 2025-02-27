// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.Transaction.fromBytes;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.TokenAssociateTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
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
}
