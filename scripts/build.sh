#!/bin/bash
# Build script for mcJava Android project

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$PROJECT_ROOT/android"

echo "=== mcJava Build Script ==="
echo "Project root: $PROJECT_ROOT"
echo "Android dir: $ANDROID_DIR"
echo ""

cd "$ANDROID_DIR"

# Check for gradle wrapper
if [ ! -f "./gradlew" ]; then
    echo "Generating Gradle wrapper..."
    gradle wrapper --gradle-version 8.5
fi

# Make gradlew executable
chmod +x ./gradlew

# Build debug APK
echo "Building debug APK..."
./gradlew :app:assembleDebug

echo ""
echo "=== Build Complete ==="
echo "APK location: $ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
