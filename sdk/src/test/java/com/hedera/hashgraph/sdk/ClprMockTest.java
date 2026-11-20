// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.ClprTestSupport.bytes32;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hedera.hashgraph.sdk.proto.ClprGetEndpointManifestResponse;
import com.hedera.hashgraph.sdk.proto.ClprGetLedgerConfigurationResponse;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClprMockTest {
    @Test
    void ledgerConfigurationQueryExecutesWithPayment() throws Exception {
        var requests = new ArrayList<com.hedera.hashgraph.sdk.proto.Query>();
        var response = Response.newBuilder()
                .setClprGetLedgerConfiguration(ClprInfoTest.CONFIGURATION_RESPONSE.toBuilder()
                        .setHeader(ResponseHeader.newBuilder().setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)))
                .build();
        List<Object> node = List.of((Function<Object, Object>) request -> {
            requests.add((com.hedera.hashgraph.sdk.proto.Query) request);
            return response;
        });

        try (var mocker = Mocker.withResponses(List.of(node))) {
            var info = new ClprGetLedgerConfigurationQuery()
                    .setQueryPayment(Hbar.fromTinybars(10))
                    .execute(mocker.client);
            assertThat(info.getChainId()).isEqualTo("hedera-local");
            assertThat(info.getConfigurationStateProof()).containsExactly(0xde, 0xad, 0xbe, 0xef);
        }

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).hasClprGetLedgerConfiguration()).isTrue();
        assertThat(requests.get(0).getClprGetLedgerConfiguration().getHeader().hasPayment())
                .isTrue();
    }

    @Test
    void endpointManifestQueryExecutes() throws Exception {
        var response = Response.newBuilder()
                .setClprGetEndpointManifest(ClprInfoTest.MANIFEST_RESPONSE.toBuilder()
                        .setHeader(ResponseHeader.newBuilder().setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)))
                .build();

        try (var mocker = Mocker.withResponses(List.of(List.of(response)))) {
            var info = new ClprGetEndpointManifestQuery()
                    .setQueryPayment(Hbar.fromTinybars(10))
                    .execute(mocker.client);
            assertThat(info.getEndpoints()).hasSize(1);
        }
    }

    @Test
    void emptyProofIsSuccessfulRead() throws Exception {
        var response = Response.newBuilder()
                .setClprGetEndpointManifest(ClprGetEndpointManifestResponse.newBuilder()
                        .setHeader(ResponseHeader.newBuilder().setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)))
                .build();

        try (var mocker = Mocker.withResponses(List.of(List.of(response)))) {
            var info = new ClprGetEndpointManifestQuery()
                    .setQueryPayment(Hbar.fromTinybars(10))
                    .execute(mocker.client);
            assertThat(info.getManifestStateProof()).isEmpty();
        }
    }

    @Test
    void notEnabledSurfacesAsPrecheckStatus() throws Exception {
        var response = Response.newBuilder()
                .setClprGetLedgerConfiguration(ClprGetLedgerConfigurationResponse.newBuilder()
                        .setHeader(ResponseHeader.newBuilder()
                                .setNodeTransactionPrecheckCode(ResponseCodeEnum.CLPR_NOT_ENABLED)))
                .build();

        try (var mocker = Mocker.withResponses(List.of(List.of(response)))) {
            assertThatThrownBy(() -> new ClprGetLedgerConfigurationQuery()
                            .setQueryPayment(Hbar.fromTinybars(10))
                            .execute(mocker.client))
                    .isInstanceOf(PrecheckStatusException.class)
                    .hasMessageContaining("CLPR_NOT_ENABLED");
        }
    }

    @Test
    void transactionExecutesThroughClprService() throws Exception {
        var bodies = new ArrayList<TransactionBody>();
        List<Object> node = List.of((Function<Object, Object>) request -> {
            try {
                var tx = (com.hedera.hashgraph.sdk.proto.Transaction) request;
                var signed = com.hedera.hashgraph.sdk.proto.SignedTransaction.parseFrom(tx.getSignedTransactionBytes());
                bodies.add(TransactionBody.parseFrom(signed.getBodyBytes()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return TransactionResponse.newBuilder()
                    .setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)
                    .build();
        });

        try (var mocker = Mocker.withResponses(List.of(node))) {
            new ClprRegisterConnectorTransaction()
                    .setChannelId(bytes32(1))
                    .setCommitment(bytes32(2))
                    .execute(mocker.client);
        }

        assertThat(bodies).hasSize(1);
        assertThat(bodies.get(0).getClprRegisterConnector().getChannelId().toByteArray())
                .isEqualTo(bytes32(1));
    }

    @Test
    void decodedFrozenBytesWithoutRequiredFieldsAreRejectedBeforeExecuting() throws Exception {
        // Build a frozen body without the required commitment, bypassing the builder.
        var unchecked = new ClprRegisterChannelTransaction(TransactionBody.newBuilder()
                        .setClprRegisterChannel(
                                com.hedera.hashgraph.sdk.proto.ClprRegisterChannelTransactionBody.getDefaultInstance())
                        .build())
                .setNodeAccountIds(List.of(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.generate(AccountId.fromString("0.0.1800")));
        var bytes = ((Transaction<?>) unchecked).toBytes();
        var decoded = Transaction.fromBytes(bytes);

        try (var mocker = Mocker.withResponses(List.of(List.of()))) {
            assertThatThrownBy(() -> decoded.execute(mocker.client))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(
                            "ClprRegisterChannelTransaction: 'ownershipCommitment' must be explicitly set before freezing or executing.");
        }
    }
}
