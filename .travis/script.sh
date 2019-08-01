#!/usr/bin/env bash
set -x

# If this is a master build then publish
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
  ORG_GRADLE_PROJECT_signing.keyId="$GPG_KEY_NAME" \
  ORG_GRADLE_PROJECT_signing.password="$GPG_PASSPHRASE" \
  ORG_GRADLE_PROJECT_signing.secretKeyRingFile="$GPG_SECRET_KEYRING_FILE" \
  ORG_GRADLE_PROJECT_ossrhUser="$OSSRH_USERNAME" \
  ORG_GRADLE_PROJECT_ossrhPassword="$OSSRH_PASSWORD" \
   ./gradlew build publishAll
# Otherwise just build and test
else
  ./gradlew build
fi
