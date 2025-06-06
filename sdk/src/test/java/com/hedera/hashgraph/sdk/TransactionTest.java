// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.Transaction.fromBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.TokenAssociateTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final PrivateKey mockPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final List<AccountId> testNodeAccountIds =
            Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006"));
    private static final AccountId testAccountId = AccountId.fromString("0.0.5006");
    private static final Instant validStart = Instant.ofEpochSecond(1554158542);
    private static final TransactionId testTransactionID = TransactionId.withValidStart(testAccountId, validStart);

    private Client client;
    // Additional test setup for new V2 methods
    private FileId fileID;
    private AccountId nodeAccountID1;
    private AccountId nodeAccountID2;
    private List<AccountId> nodeAccountIDs;
    private byte[] mockSignature;

    @BeforeEach
    void setUp() throws Exception {
        client = Client.forTestnet();
        client.setOperator(testAccountId, mockPrivateKey);

        fileID = new FileId(3);
        nodeAccountID1 = AccountId.fromString("0.0.3");
        nodeAccountID2 = AccountId.fromString("0.0.4");
        nodeAccountIDs = Arrays.asList(nodeAccountID1, nodeAccountID2);
        mockSignature = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    }

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
    @DisplayName("Should return single body chunk for transaction with no content")
    void transactionWithNoContentShouldReturnSingleBodyChunk() {
        var fileAppendTx = new FileAppendTransaction()
                .setFileId(new FileId(1))
                .setTransactionId(new TransactionId(testAccountId, validStart))
                .setNodeAccountIds(testNodeAccountIds)
                .freeze();

        var bodySizes = fileAppendTx.bodySizeAllChunks();

        assertThat(bodySizes).isNotNull();
        assertThat(bodySizes).hasSize(1); // Contains one empty chunk
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

    @Test
    @DisplayName("AddSignatureV2 - Single Node Single Chunk")
    void testAddSignatureV2SingleNodeSingleChunk() {
        var transaction = new FileAppendTransaction()
                .setFileId(fileID)
                .setContents("test content".getBytes())
                .setNodeAccountIds(Arrays.asList(nodeAccountID1))
                .setTransactionId(testTransactionID)
                .setChunkSize(2048)
                .freezeWith(client);

        transaction = transaction.addSignature(
                mockPrivateKey.getPublicKey(), mockSignature, testTransactionID, nodeAccountID1);

        Map<AccountId, Map<PublicKey, byte[]>> signatures = transaction.getSignatures();
        assertThat(signatures).hasSize(1);
        assertThat(signatures).containsKey(nodeAccountID1);

        Map<PublicKey, byte[]> nodeSignatures = signatures.get(nodeAccountID1);
        for (Map.Entry<PublicKey, byte[]> entry : nodeSignatures.entrySet()) {
            assertThat(entry.getValue()).isEqualTo(mockSignature);
        }
    }

    @Test
    @DisplayName("AddSignatureV2 - Multiple Nodes Single Chunk")
    void testAddSignatureV2MultipleNodesSingleChunk() {
        var transaction = new FileAppendTransaction()
                .setFileId(fileID)
                .setContents("test content".getBytes())
                .setNodeAccountIds(nodeAccountIDs)
                .setTransactionId(testTransactionID)
                .setChunkSize(2048)
                .freezeWith(client);

        transaction = transaction.addSignature(
                mockPrivateKey.getPublicKey(), mockSignature, testTransactionID, nodeAccountID1);
        transaction = transaction.addSignature(
                mockPrivateKey.getPublicKey(), mockSignature, testTransactionID, nodeAccountID2);

        Map<AccountId, Map<PublicKey, byte[]>> signatures = transaction.getSignatures();
        assertThat(signatures).hasSize(2);
        assertThat(signatures).containsKey(nodeAccountID1);
        assertThat(signatures).containsKey(nodeAccountID2);

        for (AccountId nodeID : nodeAccountIDs) {
            Map<PublicKey, byte[]> nodeSignatures = signatures.get(nodeID);
            for (Map.Entry<PublicKey, byte[]> entry : nodeSignatures.entrySet()) {
                assertThat(entry.getValue()).isEqualTo(mockSignature);
            }
        }
    }

    @Test
    @DisplayName("AddSignatureV2 - Multiple Nodes Multiple Chunks")
    void testAddSignatureV2MultipleNodesMultipleChunks() {
        byte[] content = new byte[2048];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i % 256);
        }

        var transaction = new FileAppendTransaction()
                .setFileId(fileID)
                .setContents(content)
                .setNodeAccountIds(nodeAccountIDs)
                .setTransactionId(testTransactionID)
                .setChunkSize(2048)
                .freezeWith(client);

        transaction = transaction.addSignature(
                mockPrivateKey.getPublicKey(), mockSignature, testTransactionID, nodeAccountID1);
        transaction = transaction.addSignature(
                mockPrivateKey.getPublicKey(), mockSignature, testTransactionID, nodeAccountID2);

        Map<AccountId, Map<PublicKey, byte[]>> signatures = transaction.getSignatures();
        assertThat(signatures).hasSize(2);
        assertThat(signatures).containsKey(nodeAccountID1);
        assertThat(signatures).containsKey(nodeAccountID2);

        for (AccountId nodeID : nodeAccountIDs) {
            Map<PublicKey, byte[]> nodeSigs = signatures.get(nodeID);
            assertThat(nodeSigs).hasSize(1);
            for (Map.Entry<PublicKey, byte[]> entry : nodeSigs.entrySet()) {
                assertThat(entry.getKey()).isNotNull();
                assertThat(entry.getKey().toString())
                        .isEqualTo(mockPrivateKey.getPublicKey().toString());
                assertThat(entry.getValue()).isEqualTo(mockSignature);
            }
        }
    }

    @Test
    @DisplayName("AddSignatureV2 - Wrong Node ID")
    void testAddSignatureV2WrongNodeID() {
        var transaction = new FileAppendTransaction()
                .setFileId(fileID)
                .setContents("test content".getBytes())
                .setNodeAccountIds(nodeAccountIDs)
                .setTransactionId(testTransactionID)
                .setChunkSize(2048)
                .freezeWith(client);

        AccountId invalidNodeID = AccountId.fromString("0.0.999");
        transaction = transaction.addSignature(
                mockPrivateKey.getPublicKey(), mockSignature, testTransactionID, invalidNodeID);

        Map<AccountId, Map<PublicKey, byte[]>> signatures = transaction.getSignatures();
        assertThat(signatures).doesNotContainKey(invalidNodeID);
    }

    @Test
    @DisplayName("AddSignatureV2 - Wrong Transaction ID")
    void testAddSignatureV2WrongTransactionID() {
        var transaction = new FileAppendTransaction()
                .setFileId(fileID)
                .setContents("test content".getBytes())
                .setNodeAccountIds(nodeAccountIDs)
                .setTransactionId(testTransactionID)
                .setChunkSize(2048)
                .freezeWith(client);

        TransactionId invalidTxID = TransactionId.withValidStart(AccountId.fromString("0.0.999"), Instant.now());

        transaction =
                transaction.addSignature(mockPrivateKey.getPublicKey(), mockSignature, invalidTxID, nodeAccountID1);

        Map<AccountId, Map<PublicKey, byte[]>> signatures = transaction.getSignatures();
        if (signatures.containsKey(nodeAccountID1)) {
            assertThat(signatures.get(nodeAccountID1)).doesNotContainKey(mockPrivateKey.getPublicKey());
        }
    }

    @Test
    @DisplayName("AddSignatureV2 - Adding Same Signature Twice")
    void testAddSignatureV2SameSignatureTwice() {
        var transaction = new FileAppendTransaction()
                .setFileId(fileID)
                .setContents("test content".getBytes())
                .setNodeAccountIds(Arrays.asList(nodeAccountID1))
                .setTransactionId(testTransactionID)
                .setChunkSize(2048)
                .freezeWith(client);

        transaction = transaction.addSignature(
                mockPrivateKey.getPublicKey(), mockSignature, testTransactionID, nodeAccountID1);

        transaction = transaction.addSignature(
                mockPrivateKey.getPublicKey(), mockSignature, testTransactionID, nodeAccountID1);

        Map<AccountId, Map<PublicKey, byte[]>> signatures = transaction.getSignatures();
        assertThat(signatures).hasSize(1);
        assertThat(signatures).containsKey(nodeAccountID1);

        Map<PublicKey, byte[]> nodeSigs = signatures.get(nodeAccountID1);
        assertThat(nodeSigs).hasSize(1);
        for (Map.Entry<PublicKey, byte[]> entry : nodeSigs.entrySet()) {
            assertThat(entry.getValue()).isEqualTo(mockSignature);
        }
    }

    @Test
    @DisplayName("AddSignatureV2 - Empty Inner Signed Transactions")
    void testAddSignatureV2WithEmptyInnerSignedTransactions() {
        var tx = new FileAppendTransaction();

        PrivateKey key = PrivateKey.generateED25519();

        byte[] mockSig = {0, 1, 2, 3, 4};

        AccountId nodeID = AccountId.fromString("0.0.3");
        TransactionId testTxID = TransactionId.withValidStart(AccountId.fromString("0.0.5"), Instant.now());

        var result = tx.addSignature(key.getPublicKey(), mockSig, testTxID, nodeID);

        assertThat(result).isSameAs(tx);
    }

    @Test
    @DisplayName("GetSignableNodeBodyBytesList - Unfrozen Transaction")
    void testGetSignableNodeBodyBytesListUnfrozen() {
        var tx = new TransferTransaction();

        assertThrows(RuntimeException.class, () -> {
            tx.getSignableNodeBodyBytesList();
        });
    }

    @Test
    @DisplayName("GetSignableNodeBodyBytesList - Basic")
    void testGetSignableNodeBodyBytesListBasic() {
        var tx = new TransferTransaction()
                .setNodeAccountIds(Arrays.asList(nodeAccountID1))
                .setTransactionId(testTransactionID)
                .addHbarTransfer(AccountId.fromString("0.0.2"), Hbar.from(-1))
                .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.from(1))
                .freezeWith(client);

        List<Transaction.SignableNodeTransactionBodyBytes> list = tx.getSignableNodeBodyBytesList();
        assertThat(list).isNotEmpty();
        assertThat(list).hasSize(1); // Should have one entry for our single node

        assertThat(list.get(0).getNodeID()).isEqualTo(nodeAccountID1);
        assertThat(list.get(0).getTransactionID()).isEqualTo(testTransactionID);
        assertThat(list.get(0).getBody()).isNotEmpty();
    }

    @Test
    @DisplayName("GetSignableNodeBodyBytesList - Contents Verification")
    void testGetSignableNodeBodyBytesListContents() throws InvalidProtocolBufferException {
        var tx = new TransferTransaction()
                .setNodeAccountIds(Arrays.asList(nodeAccountID1))
                .setTransactionId(testTransactionID)
                .addHbarTransfer(AccountId.fromString("0.0.2"), Hbar.from(-1))
                .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.from(1))
                .freezeWith(client);

        List<Transaction.SignableNodeTransactionBodyBytes> list = tx.getSignableNodeBodyBytesList();

        TransactionBody body = TransactionBody.parseFrom(list.get(0).getBody());
        assertThat(body.getCryptoTransfer()).isNotNull();
        assertThat(AccountId.fromProtobuf(body.getNodeAccountID()).toString()).isEqualTo(nodeAccountID1.toString());
        assertThat(TransactionId.fromProtobuf(body.getTransactionID()).toString())
                .isEqualTo(testTransactionID.toString());
    }

    @Test
    @DisplayName("GetSignableNodeBodyBytesList - Multiple Node IDs")
    void testGetSignableNodeBodyBytesListMultipleNodeIDs() throws InvalidProtocolBufferException {
        var tx = new TransferTransaction()
                .setNodeAccountIds(nodeAccountIDs)
                .setTransactionId(testTransactionID)
                .addHbarTransfer(AccountId.fromString("0.0.2"), Hbar.from(-1))
                .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.from(1))
                .freezeWith(client);

        List<Transaction.SignableNodeTransactionBodyBytes> list = tx.getSignableNodeBodyBytesList();
        assertThat(list).hasSize(2); // Should have two entries, one per node

        for (int i = 0; i < nodeAccountIDs.size(); i++) {
            AccountId nodeID = nodeAccountIDs.get(i);
            assertThat(list.get(i).getNodeID()).isEqualTo(nodeID);
            assertThat(list.get(i).getTransactionID()).isEqualTo(testTransactionID);
            assertThat(list.get(i).getBody()).isNotEmpty();

            // Verify body contents
            TransactionBody body = TransactionBody.parseFrom(list.get(i).getBody());
            assertThat(body.getCryptoTransfer()).isNotNull();
            assertThat(AccountId.fromProtobuf(body.getNodeAccountID()).toString())
                    .isEqualTo(nodeID.toString());
        }
    }

    @Test
    @DisplayName("GetSignableNodeBodyBytesList - FileAppend Multiple Chunks")
    void testGetSignableNodeBodyBytesListFileAppendMultipleChunks() throws InvalidProtocolBufferException {
        byte[] content = new byte[4096];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i % 256);
        }

        var tx = new FileAppendTransaction()
                .setNodeAccountIds(nodeAccountIDs)
                .setTransactionId(testTransactionID)
                .setFileId(new FileId(5))
                .setContents(content)
                .setChunkSize(2048) // Set small chunk size to force multiple chunks
                .freezeWith(client);

        List<Transaction.SignableNodeTransactionBodyBytes> list = tx.getSignableNodeBodyBytesList();
        assertThat(list).hasSize(4); // Should have 4 entries: 2 nodes * 2 chunks

        // Map to track transaction IDs per node
        Map<String, Map<String, Boolean>> txIDsByNode = new HashMap<>();
        for (AccountId nodeID : nodeAccountIDs) {
            txIDsByNode.put(nodeID.toString(), new HashMap<>());
        }

        for (int i = 0; i < list.size(); i++) {
            assertThat(nodeAccountIDs).contains(list.get(i).getNodeID());
            assertThat(list.get(i).getTransactionID()).isNotNull();
            assertThat(list.get(i).getBody()).isNotEmpty();

            String nodeIDStr = list.get(i).getNodeID().toString();
            String txIDStr = list.get(i).getTransactionID().toString();

            // Each transaction ID should appear exactly once per node
            assertThat(txIDsByNode.get(nodeIDStr).containsKey(txIDStr))
                    .as("Duplicate transaction ID found for the same node")
                    .isFalse();
            txIDsByNode.get(nodeIDStr).put(txIDStr, true);

            TransactionBody body = TransactionBody.parseFrom(list.get(i).getBody());
            assertThat(body.getFileAppend()).isNotNull();
            assertThat(AccountId.fromProtobuf(body.getNodeAccountID()).toString())
                    .isEqualTo(list.get(i).getNodeID().toString());
        }

        // Verify each node has the same number of unique transaction IDs
        for (AccountId nodeID : nodeAccountIDs) {
            assertThat(txIDsByNode.get(nodeID.toString()))
                    .as("Each node should have exactly 2 unique transaction IDs")
                    .hasSize(2);
        }

        // Verify that all nodes have the same set of transaction IDs
        Map<String, Boolean> firstNodeTxIDs = txIDsByNode.get(nodeAccountID1.toString());
        for (int i = 1; i < nodeAccountIDs.size(); i++) {
            Map<String, Boolean> nodeTxIDs =
                    txIDsByNode.get(nodeAccountIDs.get(i).toString());
            for (String txID : firstNodeTxIDs.keySet()) {
                assertThat(nodeTxIDs.containsKey(txID))
                        .as("All nodes should have the same set of transaction IDs")
                        .isTrue();
            }
        }
    }
}
