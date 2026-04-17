// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.proto.RegisteredNodeCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RegisteredNodeCreateTransactionTest {
    private static final PrivateKey TEST_PRIVATE_KEY = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final AccountId TEST_ACCOUNT_ID = AccountId.fromString("0.6.9");

    private static final String TEST_DESCRIPTION = "Test description";

    private static final List<RegisteredServiceEndpoint> TEST_SERVICE_ENDPOINT =
            List.of(spawnTestEndpoint((byte) 0), spawnTestEndpoint((byte) 1), spawnTestEndpoint((byte) 2));

    private static final PublicKey TEST_ADMIN_KEY = PrivateKey.fromString(
                    "302e020100300506032b65700422042062c4b69e9f45a554e5424fb5a6fe5e6ac1f19ead31dc7718c2d980fd1f998d4b")
            .getPublicKey();

    final Instant TEST_VALID_START = Instant.ofEpochSecond(1554158542);

    static RegisteredServiceEndpoint spawnTestEndpoint(byte offset) {
        return new BlockNodeServiceEndpoint()
                .setDomainName("example.block.com")
                .setPort(443 + offset)
                .setRequiresTls(true)
                .addEndpointApi(BlockNodeApi.STATUS);
    }

    RegisteredNodeCreateTransaction spawnTestTransaction() {
        return new RegisteredNodeCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), TEST_VALID_START))
                .setAdminKey(TEST_ADMIN_KEY)
                .setDescription(TEST_DESCRIPTION)
                .setServiceEndpoints(TEST_SERVICE_ENDPOINT)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(TEST_PRIVATE_KEY);
    }

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytes() throws Exception {
        var tx1 = spawnTestTransaction();
        var tx2 = RegisteredNodeCreateTransaction.fromBytes(tx1.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx1.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx1 = new RegisteredNodeCreateTransaction();
        var tx2 = Transaction.fromBytes(tx1.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx1.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setRegisteredNodeCreate(
                        RegisteredNodeCreateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(RegisteredNodeCreateTransaction.class);
    }

    @Test
    void setAdminKey() {
        var tx = new RegisteredNodeCreateTransaction().setAdminKey(TEST_ADMIN_KEY);
        assertThat(tx.getAdminKey()).isEqualTo(TEST_ADMIN_KEY);
    }

    @Test
    void setAdminKeyFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setAdminKey(TEST_ADMIN_KEY));
    }

    @Test
    void setDescription() {
        var tx = new RegisteredNodeCreateTransaction().setDescription(TEST_DESCRIPTION);
        assertThat(tx.getDescription()).isEqualTo(TEST_DESCRIPTION);
    }

    @Test
    void setDescriptionFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setDescription(TEST_DESCRIPTION));
    }

    @Test
    void setDescriptionRejectsOver100Utf8Bytes() {
        var tx = new RegisteredNodeCreateTransaction();
        String tooLong = "a".repeat(101);
        assertThrows(IllegalArgumentException.class, () -> tx.setDescription(tooLong));
    }

    @Test
    void setDescriptionAcceptsExactly100Utf8Bytes() {
        var tx = new RegisteredNodeCreateTransaction();
        String exact = "a".repeat(100);
        tx.setDescription(exact);
        assertThat(tx.getDescription()).isEqualTo(exact);
    }

    @Test
    void setServiceEndpoint() {
        var tx = new RegisteredNodeCreateTransaction().setServiceEndpoints(TEST_SERVICE_ENDPOINT);
        assertThat(tx.getServiceEndpoints()).hasSize(TEST_SERVICE_ENDPOINT.size());
        assertThat(tx.getServiceEndpoints()).isEqualTo(TEST_SERVICE_ENDPOINT);
    }

    @Test
    void setServiceEndpointFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setServiceEndpoints(TEST_SERVICE_ENDPOINT));
    }

    @Test
    void setServiceEndpointRejectsMoreThan50() {
        var tx = new RegisteredNodeCreateTransaction();
        var serviceEndpoints = new ArrayList<RegisteredServiceEndpoint>();
        for (int i = 0; i < 51; i++) {
            serviceEndpoints.add(spawnTestEndpoint((byte) i));
        }

        assertThrows(IllegalArgumentException.class, () -> tx.setServiceEndpoints(serviceEndpoints));
    }

    @Test
    void addServiceEndpoint() {
        var tx = new RegisteredNodeCreateTransaction();
        var serviceEndpoint = spawnTestEndpoint((byte) 1);

        tx.addServiceEndpoint(serviceEndpoint);
        assertThat(tx.getServiceEndpoints()).hasSize(1);
        assertThat(tx.getServiceEndpoints().get(0)).isEqualTo(serviceEndpoint);
    }

    @Test
    void addServiceEndpointRejectsMoreThan50() {
        var tx = new RegisteredNodeCreateTransaction();
        for (int i = 0; i < 50; i++) {
            tx.addServiceEndpoint(spawnTestEndpoint((byte) i));
        }

        assertThrows(IllegalArgumentException.class, () -> tx.addServiceEndpoint(spawnTestEndpoint((byte) 50)));
    }

    @Test
    void constructRegisteredNodeCreateTransactionFromTransactionBodyProtobuf() {
        var transactionBodyBuilder = RegisteredNodeCreateTransactionBody.newBuilder();

        transactionBodyBuilder.setAdminKey(TEST_ADMIN_KEY.toProtobufKey());
        transactionBodyBuilder.setDescription(TEST_DESCRIPTION);

        for (RegisteredServiceEndpoint serviceEndpoint : TEST_SERVICE_ENDPOINT) {
            transactionBodyBuilder.addServiceEndpoint(serviceEndpoint.toProtobuf());
        }

        var transactionBody = TransactionBody.newBuilder()
                .setRegisteredNodeCreate(transactionBodyBuilder.build())
                .build();
        var tx = new RegisteredNodeCreateTransaction(transactionBody);

        assertThat(tx.getAdminKey()).isEqualTo(TEST_ADMIN_KEY);
        assertThat(tx.getDescription()).isEqualTo(TEST_DESCRIPTION);
        assertThat(tx.getServiceEndpoints()).hasSize(TEST_SERVICE_ENDPOINT.size());
    }

    @Test
    void shouldFreezeSuccessfullyWhenAdminKeySet() {
        final Instant VALID_START = Instant.ofEpochSecond(1596210382);
        final AccountId ACCOUNT_ID = AccountId.fromString("0.6.9");

        var tx = new RegisteredNodeCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(ACCOUNT_ID, VALID_START))
                .setAdminKey(TEST_ADMIN_KEY);

        assertThatCode(() -> tx.freezeWith(null)).doesNotThrowAnyException();
        assertThat(tx.getAdminKey()).isEqualTo(TEST_ADMIN_KEY);
    }

    @Test
    void shouldThrowErrorWhenFreezingWithoutAdminKey() {
        final Instant VALID_START = Instant.ofEpochSecond(1596210382);
        final AccountId ACCOUNT_ID = AccountId.fromString("0.6.9");

        var tx = new RegisteredNodeCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(ACCOUNT_ID, VALID_START));

        var exception = assertThrows(IllegalStateException.class, () -> tx.freezeWith(null));
        assertThat(exception.getMessage())
                .isEqualTo(
                        "RegisteredNodeCreateTransaction: 'adminKey' must be explicitly set before calling freeze().");
    }
}
