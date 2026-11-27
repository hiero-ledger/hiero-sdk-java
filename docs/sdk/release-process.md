# Release Process

This document describes how versions are managed in this repository and the steps maintainers follow to release a
new version of the SDK.

## Versioning rule

> **The version in [`version.txt`](../../version.txt) on `main` is always a `-SNAPSHOT` version.
> A non-SNAPSHOT version exists only on the release commit that is tagged and published.**

For example, after `2.79.0` has been released, `main` carries `2.80.0-SNAPSHOT` until the next release.

Why this matters:

- **Local builds must not shadow a published release.** `./gradlew publishToMavenLocal` on `main` produces
  `X.Y.Z-SNAPSHOT` artifacts. If `main` carried a released version, the local build would overwrite the official
  release in `~/.m2/repository` for every project on that machine.
- **Release versions are immutable, SNAPSHOT versions are not.** Maven and Gradle cache release versions forever,
  while SNAPSHOT versions are re-resolved. Only SNAPSHOT versions allow iterating on the SDK from a consuming
  application.
- **Unambiguous version numbers.** `2.80.0-SNAPSHOT` clearly identifies unreleased code from `main`, while `2.79.0`
  always means the published release.

A `-SNAPSHOT` version on `main` can never be published to Maven Central by accident: the
[Release Artifacts](../../.github/workflows/release-artifacts.yml) workflow only publishes on `v*.*.*` tag pushes and
verifies that the tag matches the version in `version.txt`.

## Releasing a new version

The following steps release version `X.Y.Z`, starting from `main` with `version.txt` containing `X.Y.Z-SNAPSHOT`.

### 1. Release pull request

Create a release pull request (title `chore: release X.Y.Z`) that:

1. Sets `version.txt` to the release version:

   ```sh
   ./gradlew versionAsSpecified -PnewVersion=X.Y.Z
   ```
2. Adds a `## X.Y.Z` section to the [CHANGELOG](../../CHANGELOG.md).
3. Updates the SDK version used by the examples and quickstart guides to `X.Y.Z`:
   - [`examples/build.gradle.kts`](../../examples/build.gradle.kts)
   - [`example-android/app/build.gradle.kts`](../../example-android/app/build.gradle.kts)
   - [`docs/java-app/java-app-quickstart.md`](../java-app/java-app-quickstart.md)
   - [`docs/android-app/android-app-quickstart.md`](../android-app/android-app-quickstart.md)
4. Updates the address books if needed (`./gradlew examples:updateAddressbooks`).

Merge the pull request once it is approved.

### 2. Tag and publish

Create the tag `vX.Y.Z` **on the release commit** (the merge commit of the release pull request) and push it:

```sh
git checkout <release-commit>
git tag -s vX.Y.Z -m "vX.Y.Z"
git push origin vX.Y.Z
```

Pushing the tag triggers the [Release Artifacts](../../.github/workflows/release-artifacts.yml) workflow. It validates
that the tag matches `version.txt` and publishes the artifacts to Maven Central.

### 3. Post-release: restore the SNAPSHOT version on `main`

Immediately after the tag has been pushed, open a pull request (title `chore: prepare next development version`) that
sets `version.txt` to the next development version. By default, this is the next minor version:

```sh
./gradlew versionAsSpecified -PnewVersion=X.(Y+1).0-SNAPSHOT
```

Merge this pull request before any other pull request, so that `main` never stays on a published version.

> **Note:** The examples and quickstart guides keep referencing the latest **published** version (`X.Y.Z`); only
> `version.txt` is moved to the SNAPSHOT version.

## Pre-releases

Pre-releases (e.g. `X.Y.Z-beta.1`) follow the same steps. Set `version.txt` to the pre-release version in step 1 and
restore `X.Y.Z-SNAPSHOT` in step 3, since the final `X.Y.Z` release has not been published yet.
