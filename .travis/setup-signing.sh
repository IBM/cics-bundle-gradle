#!/usr/bin/env bash
set -x

# If this is a master build then lay out signing information
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
  openssl aes-256-cbc -K $encrypted_cfe97df84ed0_key -iv $encrypted_cfe97df84ed0_iv -in .travis/signingkey.asc.enc -out .travis/signingkey.asc -d
  gpg --version
  gpg --fast-import .travis/signingkey.asc
  rm .travis/signingkey.asc*
fi