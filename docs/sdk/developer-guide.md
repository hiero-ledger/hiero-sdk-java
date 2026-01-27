# Java SDK Setup and Maintenance

This guide provides instructions for setting up, building, testing, and maintaining the Java SDK project. It covers JVM requirements, build processes, testing, dependency management, and file updates to ensure a smooth development experience.

## JVM Requirements

JDK 17 is required. The Temurin builds from Eclipse Adoptium are strongly recommended.

## Setup Instructions

Note that all `./gradlew` commands should be run from the root of the project.

This project uses the Hiero Gradle Conventions for its Gradle setup. For more details on working with the project, refer to the documentation.

### Building the Project

To build the project, run:

```sh
./gradlew assemble
```

### Running Unit Tests

To execute unit tests for the SDK, run:

```sh
./gradlew :sdk:test
```

### Running Integration Tests

Integration tests are only executed if the required configuration is provided. Pass the configuration file or properties at the start of the command.

#### Using Gradle Properties

Provide `OPERATOR_ID`, `OPERATOR_KEY`, and `HEDERA_NETWORK` as Gradle properties using `-P` parameters. `HEDERA_NETWORK` can be set to `localhost`, `testnet`, or `previewnet`.

```sh
./gradlew :sdk:testIntegration -POPERATOR_ID="" -POPERATOR_KEY="" -PHEDERA_NETWORK=""
```

#### Using a Configuration File

```sh
./gradlew :sdk:testIntegration -PCONFIG_FILE=""
```

An example configuration file is available in the repository at: sdk/src/test/resources/client-config-with-operator.json.

**Running Against a Local Network**

Use a configuration file in this format:

```json
{
    "network": {
        "0.0.3": "127.0.0.1:50211"
    },
    "mirrorNetwork": [
        "127.0.0.1:5600"
    ],
    "operator": {
        "accountId": "0.0.1022",
        "privateKey": "0xa608e2130a0a3cb34f86e757303c862bee353d9ab77ba4387ec084f881d420d4"
    }
}
```

**Running Against Remote Networks**

Use a configuration file in this format:

```json
{
    "network": "testnet",
    "operator": {
        "accountId": "0.0.7",
        "privateKey": "d5d37..."
    }
}
```

`HEDERA_NETWORK` can be set to `testnet`, `previewnet`, or `mainnet`.

#### Running Individual Test Classes or Functions

To run a specific test class:

```sh
./gradlew :sdk:testIntegration -POPERATOR_ID="" -POPERATOR_KEY="" -PHEDERA_NETWORK="testnet" --tests ""
```

To run a specific test function:

```sh
./gradlew :sdk:testIntegration -POPERATOR_ID="" -POPERATOR_KEY="" -PHEDERA_NETWORK="testnet" --tests ""
```

#### Running with IntelliJ IDEA

1. Create a new Gradle run configuration (the easiest way is to run a test class or individual test function directly from the IDE).
2. Update the "Run" configuration to pass the required Gradle properties (`OPERATOR_ID`, `OPERATOR_KEY`, and `HEDERA_NETWORK`).

## Managing Dependencies

This project uses a combination of Java Modules (JPMS) and Gradle to define and manage dependencies to third-party libraries. Dependencies for the SDK are defined in sdk/src/main/java/module-info.java (mirrored in sdk-full/src/main/java/module-info.java). Running `./gradlew qualityGate` includes a dependency scope check to ensure both files are in sync. Versions of third-party dependencies are defined in hiero-dependency-versions/build.gradle.kts. For more details on adding or modifying dependencies, see the Hiero Gradle Conventions documentation on defining modules and dependencies.

## Maintaining Generated Files

Note that all `./gradlew` commands should be run from the root of the project.

### Updating Unit Test Snapshots

```sh
./gradlew updateSnapshots
```

### Updating Proto Files

```sh
./gradlew updateSnapshots
```

### Updating Address Books

To update all address books:

```sh
./gradlew examples:updateAddressbooks
```

To update address books only for mainnet:

```sh
./gradlew examples:updateAddressbooksMainnet
```

To update address books only for testnet:

```sh
./gradlew examples:updateAddressbooksTestnet
```

To update address books only for previewnet:

```sh
./gradlew examples:updateAddressbooksPreviewnet
```

[1] https://adoptium.net