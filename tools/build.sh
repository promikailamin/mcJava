#!/bin/bash
# Build script for mcJava Android port
# This script runs the fix scripts and then builds the project

set -e

cd "$(dirname "$0")/.."

echo "=== mcJava Build Script ==="
echo "Working directory: $(pwd)"

# Run fix scripts in order
echo "Running fix_all_patterns.py..."
python3 tools/fix_all_patterns.py

echo "Running fix_comprehensive_instanceof.py..."
python3 tools/fix_comprehensive_instanceof.py

echo "Running fix_invalid_instanceof.py..."
python3 tools/fix_invalid_instanceof.py

echo "Running fix_missing_pattern_vars.py..."
python3 tools/fix_missing_pattern_vars.py

echo "Running fix_remaining_patterns.py..."
python3 tools/fix_remaining_patterns.py

echo "Running fix_remaining_errors.py..."
python3 tools/fix_remaining_errors.py

echo "Running rewrite_switches_v8.py..."
python3 tools/rewrite_switches_v8.py

# Build the project
echo "Building Android project..."
cd android
./gradlew :app:assembleDebug --no-daemon

echo "=== Build completed successfully! ==="