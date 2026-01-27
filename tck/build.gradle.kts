// SPDX-License-Identifier: Apache-2.0
plugins {
    id("org.springframework.boot") version "3.5.6"
    id("org.hiero.gradle.module.application")
    id("org.hiero.gradle.feature.legacy-classpath")
}

description = "Hiero SDK TCK Server"

version = "0.0.1"

mainModuleInfo {
    requires("com.thetransactioncompany.jsonrpc2.base")
    requires("com.thetransactioncompany.jsonrpc2.server")
    requires("net.minidev.json.smart")
    requires("org.apache.tomcat.embed.core")
    requires("org.bouncycastle.provider")
    requires("com.hedera.hashgraph.sdk")
    requires("org.slf4j")
    requires("spring.boot")
    requires("spring.boot.autoconfigure")
    requires("spring.context")
    requires("spring.web")
    requires("spring.webmvc")
    requiresStatic("lombok")
    annotationProcessor("lombok")
    runtimeOnly("io.grpc.netty.shaded")
    runtimeOnly("spring.boot.starter.web")
}

testModuleInfo {
    requires("org.junit.jupiter.api")
    requires("org.mockito")
    requires("org.mockito.junit.jupiter")
}

// 'protobuf' implementation provided through 'sdk' as it differs between 'sdk' and 'sdk-full'
dependencyAnalysis {
    issues { onUsedTransitiveDependencies { exclude("com.google.protobuf:protobuf-javalite") } }
}

// Configure spring boot specific classpath to access module versions
configurations.productionRuntimeClasspath {
    @Suppress("UnstableApiUsage")
    shouldResolveConsistentlyWith(configurations.mainRuntimeClasspath.get())
}
