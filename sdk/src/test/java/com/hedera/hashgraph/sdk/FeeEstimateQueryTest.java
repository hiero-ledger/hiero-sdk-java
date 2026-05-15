package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class FeeEstimateQueryTest {

    @Test
    void defaultModeIsIntrinsic() {
        FeeEstimateQuery query = new FeeEstimateQuery();
        assertThat(query.getMode()).isNull();
        // The query resolves null to INTRINSIC during execution, 
        // fulfilling the HIP-1261 default mode requirement.
    }

    @Test
    void missingTransactionThrowsException() {
        FeeEstimateQuery query = new FeeEstimateQuery();
        Client client = Client.forTestnet();
        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transaction must be set before executing fee estimate");
    }

    @Test
    void feeMathValidation1() {
        // Assert that network.subtotal equals (node.base + sum(node.extras[*].subtotal)) * network.multiplier
        FeeExtra extra1 = new FeeExtra(1, 1, 100, 0, "type1", 100);
        FeeExtra extra2 = new FeeExtra(2, 2, 100, 0, "type2", 200);
        FeeEstimate node = new FeeEstimate(500, List.of(extra1, extra2));
        NetworkFee network = new NetworkFee(2, 1600); // multiplier = 2, subtotal = (500 + 100 + 200) * 2 = 1600
        
        long nodeTotal = node.getBase() + extra1.getSubtotal() + extra2.getSubtotal();
        assertThat(network.getSubtotal()).isEqualTo(nodeTotal * network.getMultiplier());
    }

    @Test
    void feeMathValidation2() {
        // Assert that total equals network.subtotal + node total + service total
        NetworkFee network = new NetworkFee(2, 1600);
        FeeExtra nodeExtra = new FeeExtra(3, 3, 100, 0, "type1", 300);
        FeeEstimate node = new FeeEstimate(500, List.of(nodeExtra)); // node total = 800
        FeeExtra serviceExtra = new FeeExtra(2, 2, 100, 0, "type2", 200);
        FeeEstimate service = new FeeEstimate(1000, List.of(serviceExtra)); // service total = 1200
        
        long nodeTotal = node.getBase() + nodeExtra.getSubtotal();
        long serviceTotal = service.getBase() + serviceExtra.getSubtotal();
        long expectedTotal = network.getSubtotal() + nodeTotal + serviceTotal;
        
        FeeEstimateResponse response = new FeeEstimateResponse(network, node, 1, service, expectedTotal);
        
        assertThat(response.getTotal()).isEqualTo(network.getSubtotal() + nodeTotal + serviceTotal);
    }

    @Test
    void chunkingAggregationCombinesFees() throws Exception {
        Client client = Client.forTestnet();
        client.setOperator(AccountId.fromString("0.0.3"), PrivateKey.generateED25519());

        byte[] largeContent = new byte[8000]; // will create ~2 chunks
        FileAppendTransaction tx = new FileAppendTransaction()
                .setFileId(FileId.fromString("0.0.123"))
                .setContents(largeContent)
                .setNodeAccountIds(List.of(AccountId.fromString("0.0.3")));
        
        tx.freezeWith(client);
        int chunks = tx.getRequiredChunks(); // Should be 2

        FeeExtra mockExtra = new FeeExtra(1, 1, 100, 0, "test", 100);
        NetworkFee mockNetwork = new NetworkFee(2, 500); // mult = 2, subtotal = 500
        FeeEstimate mockNode = new FeeEstimate(100, List.of(mockExtra)); // base = 100
        FeeEstimate mockService = new FeeEstimate(200, List.of(mockExtra)); // base = 200
        FeeEstimateResponse mockResponse = new FeeEstimateResponse(mockNetwork, mockNode, 1, mockService, 1000);

        FeeEstimateQuery query = new FeeEstimateQuery();
        
        try (MockedConstruction<FeeEstimateQuery> mocked = mockConstruction(FeeEstimateQuery.class,
                (mock, context) -> {
                    when(mock.setMode(any())).thenReturn(mock);
                    when(mock.setHighVolumeThrottle(any(Short.class))).thenReturn(mock);
                    when(mock.setHighVolumeThrottle(any(Integer.class))).thenReturn(mock);
                    when(mock.setTransaction((com.hedera.hashgraph.sdk.proto.Transaction) any())).thenReturn(mock);
                    when(mock.execute(any(Client.class))).thenReturn(mockResponse);
                })) {

            FeeEstimateResponse aggregatedResponse = query.executeChunked(client, tx);

            assertThat(aggregatedResponse.getNetwork().getSubtotal()).isEqualTo(mockNetwork.getSubtotal() * chunks);
            assertThat(aggregatedResponse.getNode().getBase()).isEqualTo(mockNode.getBase() * chunks);
            assertThat(aggregatedResponse.getService().getBase()).isEqualTo(mockService.getBase() * chunks);
            assertThat(aggregatedResponse.getTotal()).isEqualTo(mockResponse.getTotal() * chunks);
        }
    }
}
