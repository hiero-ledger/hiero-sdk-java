// SPDX-License-Identifier: Apache-2.0
plugins {
    id("org.hiero.gradle.module.library")
    id("org.hiero.gradle.feature.test-integration")
    id("org.hiero.gradle.feature.protobuf")
    id("org.hiero.gradle.feature.publish-dependency-constraints")
}

description = "Hedera™ Hashgraph SDK for Java"

javaModuleDependencies.moduleNameToGA.put(
    "com.google.protobuf",
    "com.google.protobuf:protobuf-javalite",
)

// Define dependency constraints for gRPC implementations so that clients automatically get the
// correct version
dependencies {
    publishDependencyConstraint("io.grpc:grpc-netty")
    publishDependencyConstraint("io.grpc:grpc-netty-shaded")
    publishDependencyConstraint("io.grpc:grpc-okhttp")
}

testModuleInfo {
    requires("com.fasterxml.jackson.annotation")
    requires("com.fasterxml.jackson.core")
    requires("com.fasterxml.jackson.databind")
    requires("json.snapshot")
    requires("org.assertj.core")
    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
    requires("org.mockito")

    runtimeOnly("io.grpc.netty.shaded")
    runtimeOnly("org.slf4j.simple")
}

testIntegrationModuleInfo {
    runtimeOnly("io.grpc.netty.shaded")
    runtimeOnly("org.slf4j.simple")
}

protobuf {
    generateProtoTasks {
        all().configureEach {
            builtins.named("java") { option("lite") }
            plugins.named("grpc") { option("lite") }
        }
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("CONFIG_FILE", providers.gradleProperty("CONFIG_FILE").getOrElse(""))
    systemProperty("HEDERA_NETWORK", providers.gradleProperty("HEDERA_NETWORK").getOrElse(""))
    systemProperty("OPERATOR_ID", providers.gradleProperty("OPERATOR_ID").getOrElse(""))
    systemProperty("OPERATOR_KEY", providers.gradleProperty("OPERATOR_KEY").getOrElse(""))
}

tasks.testIntegration {
    maxParallelForks = (Runtime.getRuntime().availableProcessors()).coerceAtLeast(1)
    failFast = true
}

tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("-Xlint:-exports,-dep-ann") }

dependencyAnalysis.abi {
    exclusions {
        // Exposes: org.slf4j.Logger
        excludeClasses("logger")
        // Exposes: com.google.common.base.MoreObjects.ToStringHelper
        excludeClasses(".*\\.CustomFee")
        // Exposes: com.esaulpaugh.headlong.abi.Tuple
        excludeClasses(".*\\.ContractFunctionResult")
        // Exposes: org.bouncycastle.crypto.params.KeyParameter
        excludeClasses(".*\\.PrivateKey.*")
        // Exposes: io.grpc.stub.AbstractFutureStub (and others)
        excludeClasses(".*Grpc")
    }
}

tasks.register<Delete>("updateSnapshots") {
    delete(sourceSets.test.get().allSource.matching { include("**/*.snap") })
    finalizedBy(tasks.test)
}

tasks.register<Exec>("updateProto") {
    executable = File(rootDir, "scripts/update_protobufs.py").absolutePath
    args("main") // argument is the branch/tag
}
