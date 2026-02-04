#!/bin/bash

set -e

# Configure Git to use custom hooks directory
if ! git config core.hooksPath .githooks; then
    echo "Failed to configure Git hooks path"
    exit 1
fi

# Ensure pre-commit hook is executable
chmod +x .githooks/pre-commit

echo "Git hooks configured successfully!"
echo "Pre-commit hook will now run tests before each commit."
