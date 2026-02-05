#!/bin/bash

# Claude Code Pre-commit Hook: Check commit size
# This hook warns about large commits before git commit is executed

MAX_LINES=400
MAX_FILES=10

# Get staged changes
STAGED_LINES=$(git diff --cached --stat 2>/dev/null | tail -1 | grep -oE '[0-9]+ insertion|[0-9]+ deletion' | grep -oE '[0-9]+' | paste -sd+ - | bc 2>/dev/null || echo "0")
STAGED_FILES=$(git diff --cached --name-only 2>/dev/null | wc -l | tr -d ' ')

# Skip if no staged files
if [ "$STAGED_FILES" -eq 0 ]; then
    exit 0
fi

# Check commit size
EXIT_CODE=0

if [ "$STAGED_LINES" -gt "$MAX_LINES" ]; then
    echo ""
    echo "WARNING: Large commit detected!"
    echo "  Lines: $STAGED_LINES (limit: $MAX_LINES)"
    echo ""
    echo "Consider breaking into smaller commits:"
    echo "  - One feature per commit"
    echo "  - Separate test and implementation"
    echo ""
    # Warning only, don't block
fi

if [ "$STAGED_FILES" -gt "$MAX_FILES" ]; then
    echo ""
    echo "WARNING: Many files in commit!"
    echo "  Files: $STAGED_FILES (limit: $MAX_FILES)"
    echo ""
fi

exit $EXIT_CODE
