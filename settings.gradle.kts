// SPDX-License-Identifier: Apache-2.0
plugins { id("org.hiero.gradle.build") version "0.7.6" }

rootProject.name = "hiero-sdk-java"

javaModules {
    module("hiero-sdk") { group = "org.hiero" }
    module("hiero-sdk-full") { group = "org.hiero" }
    module("tck") { group = "org.hiero" }
}

includeBuild("examples")

includeBuild("example-android")
