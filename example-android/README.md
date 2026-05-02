# Example Android application

## Description

Example Android Application is designed to showcase various functionalities
and capabilities when working with the Hiero Java SDK.
It operates on a `testnet` environment and offers the following features
through three distinct tabs:
- **Private Key tab** allows you to generate ED25519 key pairs by pressing
the "Generate" button.
- **Account Balance tab** allows you to retrieve the balance of an account by
pressing the "Get Account Balance" button and specifying the desired account,
which can be different from the default account specified in the
[`app/src/main/res/values/strings.xml`](app/src/main/res/values/strings.xml) file.
- **Crypto Transfer tab** allows you to transfer a specified amount of HBARs
from your account to a recipient account by pressing the "Send HBAR" button
and setting the recipient's account in the provided text field.

## Usage

### Configuration

Running the example requires `operator_id` and `operator_key`
resource strings to be set in
[`app/src/main/res/values/strings.xml`](app/src/main/res/values/strings.xml).

```xml
<string name="operator_id">...</string>
<string name="operator_key">...</string>
```

You can then build and install the application on a connected device or emulator:

```
../gradlew installDebug
```

## Running with local vs published SDK version

The example uses the local SDK version. If you want to use the example stand-alone with an SDK version published to
Maven Central you need to remove the line `includeBuild("..")` from [settings.gradle.kts](settings.gradle.kts)
and `implementation(platform("org.hiero:sdk-dependency-versions"))` from
[app/build.gradle.kts](app/build.gradle.kts). You can define the version of the SDK you would like to use by modifying
the `implementation("org.hiero:sdk:<version>")` dependency in [app/build.gradle.kts](app/build.gradle.kts).
