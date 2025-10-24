// SPDX-License-Identifier: Apache-2.0
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://central.sonatype.com/repository/maven-snapshots")
    }
}

buildscript {
    configurations.classpath { resolutionStrategy.cacheDynamicVersionsFor(0, "seconds") }
}

plugins { id("org.hiero.gradle.build") version "0.6.0-SNAPSHOT" }

@Suppress("UnstableApiUsage") dependencyResolutionManagement { repositories.mavenCentral() }

// --- Remove to use a published SDK version ---
includeBuild("..")
