// SPDX-License-Identifier: Apache-2.0
plugins {
    id("org.hiero.gradle.module.library")
    id("org.hiero.gradle.feature.protobuf")
    id("org.hiero.gradle.feature.publish-dependency-constraints")
}

description = "Hiero SDK for Java"

// grpc-api / grpc-protobuf POMs still list jsr305; override the synthetic module descriptors so
// we do not need JSR-305 on the module path. Paired with the `modules { replacedBy(...) }` rule
// below (#2889).
extraJavaModuleInfo {
    module("io.grpc:grpc-api", "io.grpc") {
        requires("com.google.common")
        requiresTransitive("com.google.errorprone.annotations")
        requires("java.logging")
        exports("io.grpc")
        uses("io.grpc.LoadBalancerProvider")
        uses("io.grpc.ManagedChannelProvider")
        uses("io.grpc.NameResolverProvider")
        uses("io.grpc.ServerProvider")
    }
    module("io.grpc:grpc-protobuf", "io.grpc.protobuf") {
        requires("com.google.common")
        requires("io.grpc.protobuf.lite")
        requiresTransitive("io.grpc")
        requiresTransitive("com.google.protobuf")
        requiresTransitive("com.google.api.grpc.common")
        exportAllPackages()
    }
}

// Define dependency constraints for gRPC implementations so that clients automatically get the
// correct version
dependencies {
    modules {
        module("com.google.code.findbugs:jsr305") {
            replacedBy("org.jspecify:jspecify", "JSR-305 is superseded by JSpecify (#2889)")
        }
    }
    publishDependencyConstraint("io.grpc:grpc-netty")
    publishDependencyConstraint("io.grpc:grpc-netty-shaded")
    publishDependencyConstraint("io.grpc:grpc-okhttp")
}

javaModuleDependencies.moduleNameToGA.put(
    "com.google.protobuf",
    "com.google.protobuf:protobuf-java",
)

tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("-Xlint:-exports,-dep-ann") }

val sdkSrcMainProto = layout.projectDirectory.dir("../sdk/src/main/proto")
val sdkSrcMainJava =
    layout.projectDirectory.dir("../sdk/src/main/java").asFileTree.matching {
        exclude("module-info.java")
    }
val sdkSrcMainResources = layout.projectDirectory.dir("../sdk/src/main/resources")

tasks.generateProto {
    addIncludeDir(files(sdkSrcMainProto))
    addSourceDirs(files(sdkSrcMainProto))
}

tasks.compileJava { source(sdkSrcMainJava) }

tasks.processResources { from(sdkSrcMainResources) }

tasks.javadoc { source(sdkSrcMainJava) }

tasks.named<Jar>("sourcesJar") {
    from(sdkSrcMainJava)
    from(sdkSrcMainResources)
}

// 'sdk-full' is an alternative to 'sdk'. They cannot be used together.
// We express this via capability.
listOf(configurations.apiElements.get(), configurations.runtimeElements.get()).forEach {
    // The 'sdk-full' capability (default)
    it.outgoing.capability("${project.group}:${project.name}:${project.version}")
    // The 'sdk' capability
    it.outgoing.capability("${project.group}:sdk:${project.version}")
}
