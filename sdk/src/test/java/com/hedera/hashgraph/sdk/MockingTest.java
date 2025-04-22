// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.AccountID;
import com.hedera.hashgraph.sdk.proto.CryptoGetAccountBalanceResponse;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MockingTest {
    @Test
    void testSucceedsWithCorrectHbars() throws PrecheckStatusException, TimeoutException, InterruptedException {
        List<Object> responses1 = List.of(
                Status.Code.UNAVAILABLE.toStatus().asRuntimeException(),
                (Function<Object, Object>)
                        o -> Status.Code.UNAVAILABLE.toStatus().asRuntimeException(),
                Response.newBuilder()
                        .setCryptogetAccountBalance(CryptoGetAccountBalanceResponse.newBuilder()
                                .setHeader(ResponseHeader.newBuilder()
                                        .setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)
                                        .build())
                                .setAccountID(
                                        AccountID.newBuilder().setAccountNum(10).build())
                                .setBalance(100)
                                .build())
                        .build());

        var responses = List.of(responses1);

        try (var mocker = Mocker.withResponses(responses)) {
            var balance = new AccountBalanceQuery()
                    .setAccountId(new AccountId(0, 0, 10))
                    .execute(mocker.client);

            Assertions.assertEquals(balance.hbars, Hbar.fromTinybars(100));
        }
    }

    @Test
    void testMetadataInterceptor() throws PrecheckStatusException, TimeoutException, InterruptedException {
        // Create a metadata capturing interceptor
        var metadataCaptor = new MetadataCapturingInterceptor();
        
        // Create responses for the mock server
        List<Object> responses1 = List.of(
            (Function<Object, Object>) request -> {
                // Verify metadata before returning response
                var metadata = metadataCaptor.getLastMetadata();
                Assertions.assertNotNull(metadata, "No metadata was captured");
                
                var userAgent = metadata.get(
                    Metadata.Key.of("x-user-agent", Metadata.ASCII_STRING_MARSHALLER));
                Assertions.assertNotNull(userAgent, "User agent header was not found");
                Assertions.assertTrue(userAgent.startsWith("hiero-sdk-java/"), 
                    "User agent header does not match expected format: " + userAgent);
                
                // Return successful response
                return Response.newBuilder()
                    .setCryptogetAccountBalance(CryptoGetAccountBalanceResponse.newBuilder()
                        .setHeader(ResponseHeader.newBuilder()
                            .setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)
                            .build())
                        .setAccountID(AccountID.newBuilder().setAccountNum(10).build())
                        .setBalance(100)
                        .build())
                    .build();
            }
        );

        var responses = List.of(responses1);

        try (var mocker = new Mocker(responses) {
            @Override
            protected void configureServerBuilder(InProcessServerBuilder builder) {
                builder.intercept(metadataCaptor);
            }
        }) {
            // Execute query to trigger metadata interceptor
            new AccountBalanceQuery()
                .setAccountId(new AccountId(0, 0, 10))
                .execute(mocker.client);
        }
    }

    private static class MetadataCapturingInterceptor implements ServerInterceptor {
        private Metadata lastMetadata;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata metadata,
                ServerCallHandler<ReqT, RespT> next) {
            this.lastMetadata = metadata;
            return next.startCall(call, metadata);
        }

        public Metadata getLastMetadata() {
            return lastMetadata;
        }
    }
}
