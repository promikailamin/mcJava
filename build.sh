#!/bin/bash
# CoreServerMod Build Script
# Builds the Fabric mod JAR

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== CoreServerMod Build Script ==="
echo "Working directory: $SCRIPT_DIR"

# Check for Gradle wrapper
if [ ! -f "./gradlew" ]; then
    echo "Generating Gradle wrapper..."
    gradle wrapper --gradle-version 8.10
fi

# Make gradlew executable
chmod +x ./gradlew

# Build the mod
echo "Building CoreServerMod..."
./gradlew clean build -x test

# Check if build was successful
JAR_PATH=$(find build/libs -name "CoreServerMod-*.jar" | head -1)
if [ -n "$JAR_PATH" ] && [ -f "$JAR_PATH" ]; then
    echo "Build successful! JAR: $JAR_PATH"
    
    # Copy to test server mods directory
    mkdir -p test-server/mods
    cp "$JAR_PATH" test-server/mods/
    echo "Copied JAR to test-server/mods/"
else
    echo "Build failed! JAR not found."
    exit 1
fi

echo "=== Build Complete ==="