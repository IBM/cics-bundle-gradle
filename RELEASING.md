# Releasing

This repository contains two Gradle plugins, `com.ibm.cics.bundle` and `com.ibm.cics.appdeploy`. They are one Gradle project with one version in `build.gradle.kts`, and they are released together. There is no way to release one on its own.

## Prerequisites

- Write access to this repository
- The `version` in `build.gradle.kts` on `main` must be a `-SNAPSHOT` version (e.g. `1.0.14-SNAPSHOT`)

## Steps

1. **Trigger the release workflow** — go to Actions → Prepare Release → Run workflow, or run:
   ```bash
   gh workflow run prepare-release.yml
   ```
   This runs `./gradlew setReleaseVersion` and opens a PR on a `release/<version>` branch. The PR strips `-SNAPSHOT` from `build.gradle.kts` and sets the plugin version (`id 'com.ibm.cics.bundle'` and `id 'com.ibm.cics.appdeploy'`) in `README.md` and in the `build.gradle` of each sample listed in the `setReleaseVersion` task.

2. **Review and merge the PR** — merging to `main` triggers `gradle-build.yml`, which runs `./gradlew build publishAll`. For a release (non-SNAPSHOT) version, `publishAll` publishes to Maven Central with JReleaser and to the Gradle Plugin Portal with `publishPlugins`. For a `-SNAPSHOT` version it only publishes to the local staging directory.

3. **Automation takes over** — `post-release.yml` runs when `build.gradle.kts` changes on `main` and the version is not a SNAPSHOT. It:
   - Computes the next patch SNAPSHOT (e.g. `1.0.14` → `1.0.15-SNAPSHOT`)
   - Runs `./gradlew setVersion` and opens a `develop/<version>` PR

4. **Merge the next-dev PR** — no rush, merge when ready.

## Adding a sample that names the plugin version

`setReleaseVersion` only updates the files listed in `build.gradle.kts`. If you add a sample whose `build.gradle` has `id 'com.ibm.cics.bundle' version '...'` or `id 'com.ibm.cics.appdeploy' version '...'`, add its path to that list, or it keeps the old version after a release.
