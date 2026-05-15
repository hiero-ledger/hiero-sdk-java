package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.FeeEstimateMode;
import com.hedera.hashgraph.sdk.FeeEstimateQuery;
import com.hedera.hashgraph.sdk.FeeEstimateResponse;
import com.hedera.hashgraph.sdk.FileAppendTransaction;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TransferTransaction;
import org.junit.jupiter.api.Test;

class FeeEstimateQueryIntegrationTest {

    @Test
    void executeWithStateMode() throws Exception {
        try (Client client = Client.forTestnet()) {
            client.setOperator(AccountId.fromString("0.0.3"), PrivateKey.generateED25519());

            TransferTransaction tx = new TransferTransaction()
                    .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.fromTinybars(-10))
                    .addHbarTransfer(AccountId.fromString("0.0.4"), Hbar.fromTinybars(10))
                    .freezeWith(client);

            FeeEstimateQuery query = new FeeEstimateQuery()
                    .setTransaction(tx)
                    .setMode(FeeEstimateMode.STATE);

            FeeEstimateResponse response = query.execute(client);

            assertThat(response).isNotNull();
            assertThat(response.getTotal()).isGreaterThan(0);
            assertThat(response.getNetwork()).isNotNull();
            assertThat(response.getNode()).isNotNull();
            assertThat(response.getService()).isNotNull();
        }
    }

    @Test
    void executeWithIntrinsicMode() throws Exception {
        try (Client client = Client.forTestnet()) {
            client.setOperator(AccountId.fromString("0.0.3"), PrivateKey.generateED25519());

            TransferTransaction tx = new TransferTransaction()
                    .addHbarTransfer(AccountId.fromString("0.0.3"), Hbar.fromTinybars(-10))
                    .addHbarTransfer(AccountId.fromString("0.0.4"), Hbar.fromTinybars(10))
                    .freezeWith(client);

            FeeEstimateQuery query = new FeeEstimateQuery()
                    .setTransaction(tx)
                    .setMode(FeeEstimateMode.INTRINSIC);

            FeeEstimateResponse response = query.execute(client);

            assertThat(response).isNotNull();
            assertThat(response.getTotal()).isGreaterThan(0);
            assertThat(response.getNetwork()).isNotNull();
            assertThat(response.getNode()).isNotNull();
            assertThat(response.getService()).isNotNull();
        }
    }

    @Test
    void executeChunkedIntegrationTest() throws Exception {
        try (Client client = Client.forTestnet()) {
            client.setOperator(AccountId.fromString("0.0.3"), PrivateKey.generateED25519());

            byte[] largeContent = new byte[6000]; // forces multiple chunks
            FileAppendTransaction tx = new FileAppendTransaction()
                    .setFileId(FileId.fromString("0.0.123"))
                    .setContents(largeContent)
                    .freezeWith(client);

            FeeEstimateQuery query = new FeeEstimateQuery()
                    .setMode(FeeEstimateMode.INTRINSIC);

            FeeEstimateResponse response = query.executeChunked(client, tx);

            assertThat(response).isNotNull();
            assertThat(response.getTotal()).isGreaterThan(0);
            assertThat(response.getNetwork()).isNotNull();
            assertThat(response.getNode()).isNotNull();
            assertThat(response.getService()).isNotNull();
        }
    }
}
