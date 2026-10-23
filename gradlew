#!/usr/bin/env sh
# StayEasy bootstrap launcher: downloads the pinned Gradle distribution once,
# then delegates to it. The Android Gradle Plugin version is pinned in build.gradle.kts.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.9
GRADLE_USER_HOME=${GRADLE_USER_HOME:-"${HOME}/.gradle"}
GRADLE_HOME="$GRADLE_USER_HOME/stayeasy-bootstrap/gradle-$GRADLE_VERSION"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  command -v curl >/dev/null 2>&1 || { echo "StayEasy setup needs curl to download Gradle." >&2; exit 1; }
  command -v unzip >/dev/null 2>&1 || { echo "StayEasy setup needs unzip to extract Gradle." >&2; exit 1; }
  mkdir -p "$GRADLE_USER_HOME/stayeasy-bootstrap"
  TMP_DIR=$(mktemp -d "$GRADLE_USER_HOME/stayeasy-bootstrap/tmp.XXXXXX")
  cleanup() { rm -rf "$TMP_DIR"; }
  trap cleanup EXIT HUP INT TERM
  echo "Downloading Gradle $GRADLE_VERSION for the StayEasy project (first run only)..."
  curl --fail --location --retry 2 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" --output "$TMP_DIR/gradle.zip"
  unzip -q "$TMP_DIR/gradle.zip" -d "$TMP_DIR"
  if [ ! -x "$TMP_DIR/gradle-$GRADLE_VERSION/bin/gradle" ]; then
    echo "The Gradle archive did not contain the expected executable." >&2
    exit 1
  fi
  rm -rf "$GRADLE_HOME"
  mv "$TMP_DIR/gradle-$GRADLE_VERSION" "$GRADLE_HOME"
fi

exec "$GRADLE_HOME/bin/gradle" -p "$APP_HOME" "$@"
