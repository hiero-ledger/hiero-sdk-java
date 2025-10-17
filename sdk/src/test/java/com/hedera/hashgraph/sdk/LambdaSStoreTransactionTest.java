// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.proto.LambdaSStoreTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class LambdaSStoreTransactionTest {

    private static final PrivateKey TEST_PRIVATE_KEY = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final HookId TEST_HOOK_ID = new HookId(new HookEntityId(AccountId.fromString("0.0.5006")), 42);

    private static final List<LambdaStorageUpdate> TEST_UPDATES = List.of(
            new LambdaStorageUpdate.LambdaStorageSlot(new byte[] {0x01}, new byte[] {0x02}),
            new LambdaStorageUpdate.LambdaStorageSlot(new byte[] {0x03}, new byte[] {0x04}));

    final Instant TEST_VALID_START = Instant.ofEpochSecond(1554158542);

    private LambdaSStoreTransaction spawnTestTransaction() {
        return new LambdaSStoreTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), TEST_VALID_START))
                .setHookId(TEST_HOOK_ID)
                .setStorageUpdates(TEST_UPDATES)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(TEST_PRIVATE_KEY);
    }

    @Test
    void bytesRoundTripNoSetters() throws Exception {
        var tx = new LambdaSStoreTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void bytesRoundTripWithSetters() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(tx2).isInstanceOf(LambdaSStoreTransaction.class);
    }

    // LambdaSStoreTransaction is not schedulable; no scheduled mapping exists.

    @Test
    void constructFromTransactionBodyProtobuf() {
        var lambdaBody = LambdaSStoreTransactionBody.newBuilder()
                .setHookId(TEST_HOOK_ID.toProtobuf())
                .addAllStorageUpdates(TEST_UPDATES.stream()
                        .map(LambdaStorageUpdate::toProtobuf)
                        .toList())
                .build();

        var txBody = TransactionBody.newBuilder().setLambdaSstore(lambdaBody).build();
        var tx = new LambdaSStoreTransaction(txBody);

        assertThat(tx.getHookId()).isEqualTo(TEST_HOOK_ID);
        assertThat(tx.getStorageUpdates()).hasSize(TEST_UPDATES.size());
    }

    @Test
    void settersAndFrozenBehavior() {
        var tx = new LambdaSStoreTransaction().setHookId(TEST_HOOK_ID).setStorageUpdates(TEST_UPDATES);

        assertThat(tx.getHookId()).isEqualTo(TEST_HOOK_ID);
        assertThat(tx.getStorageUpdates()).isEqualTo(TEST_UPDATES);

        var frozen = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> frozen.setHookId(TEST_HOOK_ID));
        assertThrows(IllegalStateException.class, () -> frozen.setStorageUpdates(TEST_UPDATES));
        assertThrows(IllegalStateException.class, () -> frozen.addStorageUpdate(TEST_UPDATES.get(0)));
    }

    @Test
    void validateChecksumsWithNullHookIdDoesNotThrow() throws Exception {
        var client = Client.forTestnet();
        var tx = new LambdaSStoreTransaction();

        // Should not throw when hookId is null
        tx.validateChecksums(client);
    }

    @Test
    void validateChecksumsWithAccountHookIdValidatesAccountId() throws Exception {
        var client = Client.forTestnet();
        var accountId = AccountId.fromString("0.0.1234");
        var hookId = new HookId(new HookEntityId(accountId), 1L);
        var tx = new LambdaSStoreTransaction().setHookId(hookId);

        // Should not throw with valid account ID
        tx.validateChecksums(client);
    }

    @Test
    void validateChecksumsWithContractHookIdValidatesContractId() throws Exception {
        var client = Client.forTestnet();
        var contractId = ContractId.fromString("0.0.5678");
        var hookId = new HookId(new HookEntityId(contractId), 2L);
        var tx = new LambdaSStoreTransaction().setHookId(hookId);

        // Should not throw with valid contract ID
        tx.validateChecksums(client);
    }

    @Test
    void validateChecksumsWithInvalidAccountIdThrows() throws Exception {
        var client = Client.forTestnet();
        // Create an account ID with invalid checksum (using a known bad checksum from AccountIdTest)
        var accountId = AccountId.fromString("0.0.123-ntjli");
        var hookId = new HookId(new HookEntityId(accountId), 3L);
        var tx = new LambdaSStoreTransaction().setHookId(hookId);

        // Should throw BadEntityIdException for invalid checksum
        assertThrows(BadEntityIdException.class, () -> tx.validateChecksums(client));
    }

    @Test
    void validateChecksumsWithInvalidContractIdThrows() throws Exception {
        var client = Client.forTestnet();
        // Create a contract ID with invalid checksum (using a known bad checksum)
        var contractId = ContractId.fromString("0.0.123-ntjli");
        var hookId = new HookId(new HookEntityId(contractId), 4L);
        var tx = new LambdaSStoreTransaction().setHookId(hookId);

        // Should throw BadEntityIdException for invalid checksum
        assertThrows(BadEntityIdException.class, () -> tx.validateChecksums(client));
    }
}
