// SPDX-License-Identifier: Apache-2.0
plugins { id("org.hiero.gradle.build") version "0.4.1" }

rootProject.name = "hedera-sdk-java"

javaModules {
    module("sdk") { group = "com.hedera.hashgraph" }
    module("sdk-full") { group = "com.hedera.hashgraph" }
    module("tck") { group = "com.hedera.hashgraph.tck" }
}

includeBuild("examples")

includeBuild("example-android")
