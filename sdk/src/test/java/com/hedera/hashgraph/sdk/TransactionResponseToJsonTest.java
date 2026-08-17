// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

public class TransactionResponseToJsonTest {
    static final Instant time = Instant.ofEpochSecond(1554158542);

    @Test
    void shouldSerializeToJson() {
        var response = new TransactionResponse(
                AccountId.fromString("0.0.3"),
                TransactionId.withValidStart(AccountId.fromString("0.0.1234"), time),
                Hex.decode("deadbeef"),
                null,
                null);

        assertThat(response.toJson())
                .isEqualTo(
                        "{\"nodeId\":\"0.0.3\",\"transactionHash\":\"deadbeef\",\"transactionId\":\"0.0.1234@1554158542.000000000\"}");
    }
}
