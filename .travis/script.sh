#!/usr/bin/env bash
set -x

# If this is a master build then publish
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
  ./gradlew \
  -Dorg.gradle.project.signing.keyId="$GPG_KEY_NAME" \
  -Dorg.gradle.project.signing.password="$GPG_PASSPHRASE" \
  -Dorg.gradle.project.signing.secretKeyRingFile="$GPG_SECRET_KEYRING_FILE" \
  -Dorg.gradle.project.ossrhUser="$OSSRH_USERNAME" \
  -Dorg.gradle.project.ossrhPassword="$OSSRH_PASSWORD" \
  -Dorg.gradle.project.gradle.publish.key="$key" \
  -Dorg.gradle.project.gradle.publish.secret="$secret" \
   build publishAll \
   --info
# Otherwise just build and test
else
  ./gradlew \
  build \
  --info
fi
