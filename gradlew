#!/bin/sh
set -e

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION="8.7"
DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/azianda-gradle/${GRADLE_VERSION}"
DIST_DIR="${CACHE_DIR}/gradle-${GRADLE_VERSION}"

# IMPORTANT: do not use AndroidIDE/system Gradle (it may be Gradle 6.1.1).
# Always use the project-pinned Gradle distribution.
mkdir -p "$CACHE_DIR"
if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  ZIP="$CACHE_DIR/gradle-${GRADLE_VERSION}-bin.zip"
  echo "Gradle ${GRADLE_VERSION} non trovato. Download in corso..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 "$DIST_URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "$DIST_URL"
  else
    echo "Errore: servono curl o wget per scaricare Gradle ${GRADLE_VERSION}." >&2
    exit 1
  fi
  rm -rf "$DIST_DIR"
  unzip -q "$ZIP" -d "$CACHE_DIR"
fi

exec "$DIST_DIR/bin/gradle" --no-daemon "$@"
