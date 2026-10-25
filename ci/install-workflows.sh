#!/usr/bin/env bash
# Installs the CoreServerMod CI workflows into .github/workflows/.
#
# The workflow files live in ci/workflows/ because the GitHub Actions
# GITHUB_TOKEN cannot create or update files under .github/workflows/ unless
# the token has the `workflows` permission. Run this script with a credential
# that has the `workflows` permission (for example a classic PAT with the
# `workflow` scope) to activate the workflows.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEST_DIR="$REPO_ROOT/.github/workflows"

mkdir -p "$DEST_DIR"
cp "$SCRIPT_DIR/workflows/build.yml" "$DEST_DIR/build.yml"
cp "$SCRIPT_DIR/workflows/test.yml" "$DEST_DIR/test.yml"

echo "Installed build.yml and test.yml into $DEST_DIR"
