#!/bin/bash

set -e

echo "Setting up Git hooks for FluxPay Engine..."
echo ""

# Configure Git to use custom hooks directory
if ! git config core.hooksPath .githooks; then
    echo "Failed to configure Git hooks path"
    exit 1
fi

# Ensure all hooks are executable
chmod +x .githooks/pre-commit
chmod +x .githooks/commit-msg
chmod +x .claude/hooks/*.sh 2>/dev/null || true

echo "Git hooks configured:"
echo "  - pre-commit: Tests + commit size check + forbidden patterns"
echo "  - commit-msg: Commit message format validation"
echo ""
echo "Claude Code hooks configured:"
echo "  - PreToolUse: Commit size warning before git commit"
echo ""
echo "Setup complete!"
