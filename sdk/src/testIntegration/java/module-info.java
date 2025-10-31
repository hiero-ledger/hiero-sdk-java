// SPDX-License-Identifier: Apache-2.0
module com.hedera.hashgraph.sdk.test.integration {
    requires com.hedera.hashgraph.sdk;
    requires com.esaulpaugh.headlong;
    requires io.grpc.inprocess;
    requires io.grpc;
    requires org.assertj.core;
    requires org.bouncycastle.provider;
    requires org.junit.jupiter.api;
    requires static java.annotation;

    opens com.hedera.hashgraph.sdk.test.integration;
}
