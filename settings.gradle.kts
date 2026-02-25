// SPDX-License-Identifier: Apache-2.0
import org.gradlex.jvm.dependency.conflict.resolution.JvmDependencyConflictsExtension

plugins { id("org.hiero.gradle.build") version "0.7.4" }

rootProject.name = "hedera-sdk-java"

javaModules {
    module("sdk") { group = "com.hedera.hashgraph" }
    module("sdk-full") { group = "com.hedera.hashgraph" }
    module("tck") { group = "com.hedera.hashgraph.tck" }
}

includeBuild("examples")

includeBuild("example-android")

gradle.lifecycle.beforeProject {
    plugins.withId("org.gradlex.jvm-dependency-conflict-resolution") {
        the<JvmDependencyConflictsExtension>().patch {
            module("io.github.json-snapshot:json-snapshot") {
                // clean up unused dependencies from
                // https://github.com/json-snapshot/json-snapshot.github.io/blob/master/pom.xml
                removeDependency("org.junit.jupiter:junit-jupiter-engine")
                removeDependency("org.junit.platform:junit-platform-runner")
                removeDependency("org.junit.vintage:junit-vintage-engine")
                removeDependency("org.mockito:mockito-junit-jupiter")
                addRuntimeOnlyDependency("junit:junit")
            }
        }
    }
}
