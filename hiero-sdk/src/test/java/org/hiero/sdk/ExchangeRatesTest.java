// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Instant;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

public class ExchangeRatesTest {
    private static final String exchangeRateSetHex =
            "0a1008b0ea0110b6b4231a0608f0bade9006121008b0ea01108cef231a060880d7de9006";

    @Test
    void fromProtobuf() throws InvalidProtocolBufferException {
        byte[] exchangeRatesBytes = Hex.decode(exchangeRateSetHex);

        ExchangeRates exchangeRates = ExchangeRates.fromBytes(exchangeRatesBytes);

        assertThat(exchangeRates.currentRate.cents).isEqualTo(580150);
        assertThat(exchangeRates.currentRate.hbars).isEqualTo(30000);
        Instant currentExpirationTime = Instant.ofEpochSecond(1645714800);
        assertThat(exchangeRates.currentRate.expirationTime).isEqualTo(currentExpirationTime);
        assertThat(exchangeRates.currentRate.exchangeRateInCents).isEqualTo(19.338333333333335);

        assertThat(exchangeRates.nextRate.cents).isEqualTo(587660);
        assertThat(exchangeRates.nextRate.hbars).isEqualTo(30000);
        Instant nextExpirationTime = Instant.ofEpochSecond(1645718400);
        assertThat(exchangeRates.nextRate.expirationTime).isEqualTo(nextExpirationTime);
        assertThat(exchangeRates.nextRate.exchangeRateInCents).isEqualTo(19.588666666666665);
    }
}
