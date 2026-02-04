---
name: coderabbit-review
description: Process CodeRabbit PR review comments - analyze, decide, fix, and reply individually
disable-model-invocation: true
allowed-tools: Bash, Read, Edit, Write, Grep, Glob, Task
---

# CodeRabbit Review Processing Skill

## Overview
This skill processes CodeRabbit review comments on a PR. For each comment:
1. Analyze the suggestion with technical reasoning
2. Decide: ACCEPT or REJECT with justification
3. If ACCEPT: Fix the issue (use parallel agents for independent fixes)
4. Reply to each comment individually with the decision and rationale

## Workflow

### Step 1: Fetch PR Information
```bash
# Get PR number from current branch or argument
PR_NUMBER=$ARGUMENTS

# If no argument, get from current branch
if [ -z "$PR_NUMBER" ]; then
  PR_NUMBER=$(gh pr view --json number -q '.number' 2>/dev/null)
fi
```

### Step 2: Fetch All Review Comments
Use `gh api` to get all review comments:
```bash
gh api repos/{owner}/{repo}/pulls/{pr_number}/comments --paginate
```

Filter for CodeRabbit comments (author: `coderabbitai[bot]}` or contains CodeRabbit signature).

### Step 3: For Each Comment, Execute This Process

#### 3.1 Analysis Phase
For each comment, analyze:
- **File & Line**: Which file and code section is affected?
- **Category**: Bug fix, Performance, Security, Style, Best Practice, Refactoring?
- **Severity**: Critical, Major, Minor, Suggestion?
- **Current Code**: What does the current implementation do?
- **Suggested Change**: What is CodeRabbit suggesting?
- **Impact Assessment**: What are the implications of accepting/rejecting?

#### 3.2 Decision Phase
Make a decision with clear reasoning:

**ACCEPT if:**
- Fixes a genuine bug or security issue
- Improves code quality without over-engineering
- Aligns with project conventions (check CLAUDE.md)
- Performance improvement with measurable benefit
- Reduces complexity or improves readability

**REJECT if:**
- Over-engineering for no practical benefit
- Contradicts project conventions or CLAUDE.md rules
- Premature optimization without evidence
- Would break existing functionality
- Stylistic preference without technical merit
- Already handled elsewhere in the codebase

#### 3.3 Implementation Phase (if ACCEPT) - PARALLEL EXECUTION MANDATORY

**CRITICAL: All independent fixes MUST be executed in parallel using Task agents.**

**Step 1: Analyze all ACCEPT decisions**
```
ACCEPT comments:
- Comment #1: PaymentService.java:45 (null check)
- Comment #3: OrderController.java:89 (validation)
- Comment #5: CreditService.java:23 (error handling)
```

**Step 2: Group by independence**
- Same file? → Sequential
- Different files, no dependency? → PARALLEL

**Step 3: Launch ALL parallel agents in ONE message**
```
Launch simultaneously:
- Task Agent A: Fix PaymentService.java (Comment #1)
- Task Agent B: Fix OrderController.java (Comment #3)
- Task Agent C: Fix CreditService.java (Comment #5)
```

**Implementation:**
- Use `subagent_type: general-purpose` for each fix
- Each agent: Read file → Apply fix → Run relevant test
- ALL Task tool calls in SINGLE response (not sequential)
- Wait for ALL agents to complete before proceeding to replies

**Example: 5 ACCEPT comments on different files**
```
WRONG (Sequential - FORBIDDEN):
  Fix #1 → Wait → Fix #2 → Wait → Fix #3 → Wait → Fix #4 → Wait → Fix #5
  Total time: 5x

CORRECT (Parallel - REQUIRED):
  Fix #1, #2, #3, #4, #5 simultaneously (5 Task calls in ONE message)
  Total time: 1x
```

#### 3.4 Reply Phase
Reply to EACH comment individually using:
```bash
gh api repos/{owner}/{repo}/pulls/{pr_number}/comments/{comment_id}/replies \
  -method POST \
  -f body="REPLY_CONTENT"
```

### Reply Templates

#### ACCEPT Template
```markdown
**Decision: ACCEPT**

**Reasoning:**
[Technical explanation of why this suggestion is valid]

**Changes Made:**
- [List of specific changes]
- File: `path/to/file.java` line X-Y

**Verification:**
- Tests passed: `ClassName#testMethod`

---
Applied by Claude Code
```

#### REJECT Template
```markdown
**Decision: REJECT**

**Reasoning:**
[Technical explanation with evidence]

**Justification:**
- [Reference to CLAUDE.md rule if applicable]
- [Code evidence or architectural reason]
- [Why current implementation is preferred]

**Example/Evidence:**
```java
// Current approach is preferred because...
```

---
Reviewed by Claude Code
```

#### PARTIAL ACCEPT Template
```markdown
**Decision: PARTIAL ACCEPT**

**Accepted:**
- [What was accepted and why]

**Rejected:**
- [What was rejected and why]

**Changes Made:**
- [List of specific changes]

---
Applied by Claude Code
```

## Execution Rules

1. **Never batch replies** - Reply to each comment individually after processing
2. **Always provide evidence** - Reference specific code, tests, or documentation
3. **Check CLAUDE.md first** - Project conventions override generic suggestions
4. **Run tests after each fix** - Never commit broken code
5. **Use parallel agents** - For independent fixes across different files
6. **Track progress** - Report which comments are processed

## Command Usage

```bash
# Process PR by number
/coderabbit-review 42

# Process current branch's PR
/coderabbit-review
```

## Example Session

```
Processing PR #42: "feat: Add payment validation"

Found 5 CodeRabbit comments:

[1/5] Comment #123456 on PaymentService.java:45
  Suggestion: "Consider using Optional.ofNullable instead of null check"
  Decision: ACCEPT
  Reason: Improves null safety, aligns with modern Java patterns
  Action: Fixing...
  Reply: Posted

[2/5] Comment #123457 on PaymentController.java:89
  Suggestion: "Add @Validated annotation"
  Decision: REJECT
  Reason: Already validated at service layer per CLAUDE.md architecture
  Reply: Posted

[3/5] Comment #123458 on PaymentRepository.java:23
  Suggestion: "Use @Transactional(readOnly=true)"
  Decision: ACCEPT
  Reason: Performance optimization for read operations
  Action: Fixing...
  Reply: Posted

[4/5] Comment #123459 on PaymentTest.java:67
  Suggestion: "Use assertThat instead of assertEquals"
  Decision: ACCEPT
  Reason: AssertJ provides better error messages
  Action: Fixing...
  Reply: Posted

[5/5] Comment #123460 on Application.java:12
  Suggestion: "Consider adding startup logging"
  Decision: REJECT
  Reason: Over-engineering, Spring Boot already logs startup info
  Reply: Posted

Summary:
- Accepted: 3
- Rejected: 2
- All replies posted individually
- Tests: PASSED
```

## Important Notes

- **TDD Compliance**: If a fix requires new behavior, write test first (Red-Green-Refactor)
- **Forbidden Patterns**: Never introduce `.block()`, empty catch blocks, or swallowed exceptions
- **Reactive Code**: Maintain reactive patterns when modifying WebFlux code
- **Idempotency**: Each comment reply should be idempotent (check if already replied)

## Error Handling

If a fix fails:
1. Do NOT reply with success
2. Report the error in the reply
3. Request human review
4. Continue processing other comments

```markdown
**Decision: ACCEPT (FAILED TO APPLY)**

**Reasoning:**
[Why the suggestion was valid]

**Error:**
[What went wrong during implementation]

**Action Required:**
Manual review needed for this change.

---
Attempted by Claude Code
```
