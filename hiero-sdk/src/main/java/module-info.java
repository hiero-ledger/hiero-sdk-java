// SPDX-License-Identifier: Apache-2.0
module org.hiero.sdk {
    requires transitive com.google.protobuf;
    requires com.esaulpaugh.headlong;
    requires com.google.common;
    requires com.google.gson;
    requires io.grpc.inprocess;
    requires io.grpc.protobuf.lite;
    requires io.grpc.stub;
    requires io.grpc;
    requires java.net.http;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.slf4j;
    requires static transitive java.annotation;

    exports org.hiero.sdk;
    exports org.hiero.sdk.proto;
    exports org.hiero.sdk.logger;

    opens org.hiero.sdk;
    // for reflective access e.g. by com.fasterxml.jackson.databind
    opens org.hiero.sdk.proto;
}

