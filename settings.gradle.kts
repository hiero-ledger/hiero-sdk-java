// SPDX-License-Identifier: Apache-2.0
plugins { id("org.hiero.gradle.build") version "0.3.2" }

rootProject.name = "hiero-sdk-java"

javaModules {
    module("sdk-java") { group = "org.hiero.sdk" }
    module("sdk-java-full") { group = "org.hiero.sdk" }
    module("tck") { group = "org.hiero.sdk.java.tck" }
}

includeBuild("examples")

includeBuild("example-android")
