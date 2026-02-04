#!/bin/bash

# Configure Git to use custom hooks directory
git config core.hooksPath .githooks

echo "Git hooks configured successfully!"
echo "Pre-commit hook will now run tests before each commit."
