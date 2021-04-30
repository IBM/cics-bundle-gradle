#!/usr/bin/env bash
set -x

# If this is a main build then publish
if [ "$TRAVIS_BRANCH" = 'main' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
  ./gradlew \
  -Dorg.gradle.internal.http.connectionTimeout=120000 \
  -Dorg.gradle.internal.http.socketTimeout=120000 \
  -Dorg.gradle.project.signing.keyId="$GPG_KEY_NAME" \
  -Dorg.gradle.project.signing.password="$GPG_PASSPHRASE" \
  -Dorg.gradle.project.signing.secretKeyRingFile="$GPG_SECRET_KEYRING_FILE" \
  -Dorg.gradle.project.ossrhUser="$OSSRH_USERNAME" \
  -Dorg.gradle.project.ossrhPassword="$OSSRH_PASSWORD" \
  -Dorg.gradle.project.gradle.publish.key="$key" \
  -Dorg.gradle.project.gradle.publish.secret="$secret" \
   build publishAll \
   --info \
   --stacktrace
# Otherwise just build and test
else
  ./gradlew \
  build \
   --info \
   --stacktrace
fi
