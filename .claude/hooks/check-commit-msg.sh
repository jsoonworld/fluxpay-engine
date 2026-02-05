#!/bin/bash

# Claude Code Hook: Validate commit message format
# Checks that commit message follows conventional commit format

# Get the commit message from stdin or argument
if [ -n "$1" ]; then
    COMMIT_MSG="$1"
else
    COMMIT_MSG=$(cat)
fi

# Skip if empty
if [ -z "$COMMIT_MSG" ]; then
    exit 0
fi

# Skip merge commits
if echo "$COMMIT_MSG" | grep -qE "^Merge"; then
    exit 0
fi

VALID_TYPES="feat|fix|refactor|test|docs|chore"
SUBJECT=$(echo "$COMMIT_MSG" | head -1)

# Check format
if ! echo "$SUBJECT" | grep -qE "^($VALID_TYPES): .+"; then
    echo ""
    echo "ERROR: Invalid commit message format!"
    echo ""
    echo "Expected: <type>: <subject>"
    echo "Types: feat, fix, refactor, test, docs, chore"
    echo ""
    echo "Your message: $SUBJECT"
    echo ""
    exit 1
fi

exit 0
