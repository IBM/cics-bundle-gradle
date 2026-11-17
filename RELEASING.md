# Releasing

This repository contains two Gradle plugins, `com.ibm.cics.bundle` and `com.ibm.cics.appdeploy`. They are one Gradle project with one version in `build.gradle.kts`, so every release includes both.

## Prerequisites

- Write access to this repository
- The `version` in `build.gradle.kts` on `main` must be a `-SNAPSHOT` version (e.g. `1.0.14-SNAPSHOT`). The Prepare Release workflow fails if it is not.
- If you are also releasing [cics-bundle-common](https://github.com/IBM/cics-bundle-common), release it first and wait until it is on Maven Central. The plugins depend on it.

## Steps

1. **Trigger the release workflow** — go to Actions → Prepare Release → Run workflow, or run:
   ```bash
   gh workflow run prepare-release.yml
   ```
   This runs `./gradlew setReleaseVersion` and opens a PR from a `release/<version>` branch into `main`. The PR strips `-SNAPSHOT` from `build.gradle.kts` and sets the plugin version (`id 'com.ibm.cics.bundle'` and `id 'com.ibm.cics.appdeploy'`) in `README.md` and in the `build.gradle` of each sample listed in the `setReleaseVersion` task.

2. **Make any manual changes on the release branch** — the workflow does not make these changes:
   - If the version of `cics-bundle-common` has changed, update the `com.ibm.cics:cics-bundle-common` dependency in `build.gradle.kts` by hand.

3. **Review and merge the PR** — merging triggers `gradle-build.yml` on `main`, which runs `./gradlew build publishAll`. Check the progress of the build under [Actions](https://github.com/IBM/cics-bundle-gradle/actions).

4. **Check Maven Central and the Gradle Plugin Portal** — for a release (non-SNAPSHOT) version, `publishAll` also runs `jreleaserFullRelease` and `publishPlugins`. `jreleaserFullRelease` uses JReleaser to publish directly to Maven Central from `build/staging-deploy`, with no Sonatype staging step, and `publishPlugins` publishes directly to the Gradle Plugin Portal during the build. After the build finishes, check that the new version is available:
   - Maven Central: [repo.maven.apache.org/maven2/com/ibm/cics/cics-bundle-gradle/](https://repo.maven.apache.org/maven2/com/ibm/cics/cics-bundle-gradle/). Search results are cached, so browse this URL instead.
   - Gradle Plugin Portal: [plugins.gradle.org/plugin/com.ibm.cics.bundle](https://plugins.gradle.org/plugin/com.ibm.cics.bundle)

   For a `-SNAPSHOT` version, `publishAll` only publishes to the local `build/staging-deploy` directory.

5. **Automation takes over** — `post-release.yml` runs when `build.gradle.kts` changes on `main` and the version is not a SNAPSHOT. It:
   - Computes the next patch SNAPSHOT (e.g. `1.0.14` → `1.0.15-SNAPSHOT`)
   - Runs `./gradlew setVersion` and opens a `develop/<version>` PR

6. **Merge the next-dev PR** — no rush, merge when ready.

## Adding a sample that names the plugin version

`setReleaseVersion` only updates the files listed in `build.gradle.kts`. If you add a sample whose `build.gradle` has `id 'com.ibm.cics.bundle' version '...'` or `id 'com.ibm.cics.appdeploy' version '...'`, add its path to that list, or it keeps the old version after a release.
