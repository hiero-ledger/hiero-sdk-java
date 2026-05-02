// SPDX-License-Identifier: Apache-2.0
module org.hiero.sdk.test.integration {
    requires org.hiero.sdk;
    requires com.esaulpaugh.headlong;
    requires org.assertj.core;
    requires org.bouncycastle.provider;
    requires org.junit.jupiter.api;
    requires static java.annotation;

    opens org.hiero.sdk.test.integration;
}
