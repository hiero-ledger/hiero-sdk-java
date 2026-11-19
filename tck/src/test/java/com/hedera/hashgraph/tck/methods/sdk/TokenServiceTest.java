// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.proto.AccountID;
import com.hedera.hashgraph.sdk.proto.Duration;
import com.hedera.hashgraph.sdk.proto.Timestamp;
import com.hedera.hashgraph.sdk.proto.TokenGetInfoResponse;
import com.hedera.hashgraph.sdk.proto.TokenID;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    private static TokenInfo tokenInfoWithMetadata(ByteString metadata) throws Exception {
        var info = com.hedera.hashgraph.sdk.proto.TokenInfo.newBuilder()
                .setTokenId(TokenID.newBuilder().setTokenNum(1001))
                .setTreasury(AccountID.newBuilder().setAccountNum(2))
                .setAutoRenewPeriod(Duration.newBuilder().setSeconds(7776000))
                .setExpiry(Timestamp.newBuilder().setSeconds(1787170588))
                .setLedgerId(ByteString.copyFrom(new byte[] {1}))
                .setMetadata(metadata);
        return TokenInfo.fromBytes(
                TokenGetInfoResponse.newBuilder().setTokenInfo(info).build().toByteArray());
    }

    @Test
    void tokenInfoMetadataIsHexEncoded() throws Exception {
        // The TCK creates the token with the UTF-8 text "Hello World" as metadata.
        var tokenInfo = tokenInfoWithMetadata(ByteString.copyFromUtf8("Hello World"));

        var response = TokenService.toTokenInfoResponse(tokenInfo);

        assertEquals("48656c6c6f20576f726c64", response.metadata());
    }

    @Test
    void tokenInfoMetadataKeepsNonTextBytes() throws Exception {
        var tokenInfo = tokenInfoWithMetadata(ByteString.copyFrom(new byte[] {0x00, (byte) 0xff, 0x10}));

        var response = TokenService.toTokenInfoResponse(tokenInfo);

        assertEquals("00ff10", response.metadata());
    }

    @Test
    void tokenInfoWithEmptyMetadataReturnsEmptyString() throws Exception {
        var tokenInfo = tokenInfoWithMetadata(ByteString.EMPTY);

        assertEquals("", TokenService.toTokenInfoResponse(tokenInfo).metadata());
    }

    @Test
    void tokenInfoWithNullMetadataReturnsEmptyString() throws Exception {
        var tokenInfo = tokenInfoWithMetadata(ByteString.EMPTY);
        tokenInfo.metadata = null;

        assertEquals("", TokenService.toTokenInfoResponse(tokenInfo).metadata());
    }
}
