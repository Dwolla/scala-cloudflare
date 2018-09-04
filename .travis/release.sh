#!/usr/bin/env bash
set -o errexit -o nounset

if [ "$TRAVIS_BRANCH" != "master" ]; then
  echo "Only the master branch will be released. This branch is $TRAVIS_BRANCH."
  exit 0
fi

git config user.name "Dwolla Bot"
git config user.email "dev+dwolla-bot@dwolla.com"

git remote add release git@github.com:Dwolla/scala-cloudflare.git
git fetch release

git clean -dxf
git checkout master
git branch --set-upstream-to=release/master

MASTER=$(git rev-parse HEAD)
if [ "$TRAVIS_COMMIT" != "$MASTER" ]; then
  echo "Checking out master set HEAD to $MASTER, but Travis was building $TRAVIS_COMMIT, so refusing to continue."
  exit 0
fi

echo "Not releasing due to milestone version"
# sbt clean "release with-defaults"
