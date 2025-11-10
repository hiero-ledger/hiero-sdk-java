// SPDX-License-Identifier: Apache-2.0
group = "org.hiero"

val bouncycastle = "1.80"
val grpc = "1.75.0"
val protobuf = "4.31.1"
val slf4j = "2.0.17"
val mockito = "5.19.0"

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:3.5.6"))
    api(platform("io.netty:netty-bom:4.2.6.Final"))
}

dependencies.constraints {
    api("com.esaulpaugh:headlong:13.3.1") { because("com.esaulpaugh.headlong") }
    api("com.google.code.findbugs:jsr305:3.0.2") { because("java.annotation") }
    api("com.google.code.gson:gson:2.13.2") { because("com.google.gson") }
    api("com.google.protobuf:protobuf-java:$protobuf") { because("com.google.protobuf") }
    api("com.google.protobuf:protobuf-javalite:$protobuf") { because("com.google.protobuf") }
    api("io.grpc:grpc-api:$grpc") { because("io.grpc") }
    api("io.grpc:grpc-inprocess:$grpc") { because("io.grpc.inprocess") }
    api("io.grpc:grpc-protobuf-lite:$grpc") { because("io.grpc.protobuf.lite") }
    api("io.grpc:grpc-protobuf:$grpc") { because("io.grpc.protobuf") }
    api("io.grpc:grpc-stub:$grpc") { because("io.grpc.stub") }
    api("io.grpc:grpc-netty:$grpc")
    api("io.grpc:grpc-netty-shaded:$grpc")
    api("io.grpc:grpc-okhttp:$grpc")
    api("org.bouncycastle:bcpkix-jdk18on:$bouncycastle") { because("org.bouncycastle.pkix") }
    api("org.bouncycastle:bcprov-jdk18on:$bouncycastle") { because("org.bouncycastle.provider") }
    api("org.slf4j:slf4j-api:$slf4j") { because("org.slf4j") }

    // Testing
    api("com.fasterxml.jackson.core:jackson-core:2.20.1") { because("com.fasterxml.jackson.core") }
    api("com.google.guava:guava:33.4.8-android") { because("com.google.common") }
    api("io.github.json-snapshot:json-snapshot:1.0.17") { because("json.snapshot") }
    api("org.apache.commons:commons-lang3:3.18.0") { because("org.apache.commons.lang3") }
    api("org.assertj:assertj-core:3.27.4") { because("org.assertj.core") }
    api("org.junit.jupiter:junit-jupiter-api:5.13.4") { because("org.junit.jupiter.api") }
    api("org.mockito:mockito-core:$mockito") { because("org.mockito") }
    api("org.mockito:mockito-junit-jupiter:$mockito") { because("org.mockito.junit.jupiter") }

    api("com.google.protobuf:protoc:$protobuf")
    api("io.grpc:protoc-gen-grpc-java:$grpc")

    api("com.thetransactioncompany:jsonrpc2-server:2.0")

    tasks.checkVersionConsistency {
        // Versions of additional tools that are not part of the product or test module paths
        excludes.add("com.google.protobuf:protoc")
        excludes.add("io.grpc:protoc-gen-grpc-java")
        // Versions that are not used but published as constraints for users
        excludes.add("io.grpc:grpc-netty")
        excludes.add("io.grpc:grpc-okhttp")
    }
}
