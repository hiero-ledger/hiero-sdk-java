// SPDX-License-Identifier: Apache-2.0
dependencies {
    published(project(":sdk"))
    published(project(":sdk-full"))

    implementation(project(":tck"))
    implementation("io.grpc:grpc-protobuf")
}

// Separate publishing from the coverage aggregation as 'sdk' and 'sdk-full'
// cannot both exist on the compile/test path
configurations.implementation { setExtendsFrom(extendsFrom.filter { it.name != "published" }) }

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
