// SPDX-License-Identifier: Apache-2.0
plugins {
    id("org.hiero.gradle.base.lifecycle")
    id("org.hiero.gradle.report.code-coverage")
    id("org.hiero.gradle.check.spotless")
    id("org.hiero.gradle.check.spotless-kotlin")
}

dependencies {
    implementation(project(":sdk"))
    implementation(project(":tck"))
    implementation("io.grpc:grpc-protobuf")
}

tasks.testCodeCoverageReport {
    // Integrate coverage data from integration tests into the report
    @Suppress("UnstableApiUsage")
    val testIntegrationExecutionData =
        configurations.aggregateCodeCoverageReportResults
            .get()
            .incoming
            .artifactView {
                withVariantReselection()
                componentFilter { id -> id is ProjectComponentIdentifier }
                attributes.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    objects.named(Category.VERIFICATION),
                )
                attributes.attribute(
                    VerificationType.VERIFICATION_TYPE_ATTRIBUTE,
                    objects.named(VerificationType.JACOCO_RESULTS),
                )
                attributes.attribute(
                    TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE,
                    objects.named("testIntegration"),
                )
            }
            .files

    executionData.from(testIntegrationExecutionData)
}
