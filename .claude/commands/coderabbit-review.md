# CodeRabbit Review Processor

Process CodeRabbit PR review comments with individual analysis, decision-making, and replies.

## Target PR: $ARGUMENTS (or current branch if empty)

## Execution Steps

### 1. Get PR Information
First, identify the PR to process:
- If argument provided: Use that PR number
- Otherwise: Get PR from current branch with `gh pr view --json number -q '.number'`

### 2. Fetch CodeRabbit Comments
```bash
gh api repos/{owner}/{repo}/pulls/{PR_NUMBER}/comments --paginate
```
Filter comments where author login is `coderabbitai[bot]` or body contains CodeRabbit signature.

### 3. Process Each Comment Individually

For EACH comment, follow this cycle:

#### A. Analyze
- Read the file and surrounding context
- Understand what CodeRabbit is suggesting
- Check against CLAUDE.md rules and project conventions

#### B. Decide with Reasoning
**ACCEPT** when:
- Genuine bug fix or security improvement
- Aligns with project conventions
- Measurable quality improvement
- Not over-engineering

**REJECT** when:
- Contradicts CLAUDE.md (TDD rules, forbidden patterns, etc.)
- Over-engineering without practical benefit
- Stylistic preference only
- Already handled elsewhere

#### C. Fix (if ACCEPT) - PARALLEL EXECUTION MANDATORY

**All independent fixes MUST run in parallel:**

1. **Collect all ACCEPT decisions first** (don't fix one by one)
2. **Group by file** - Same file = sequential, Different files = parallel
3. **Launch ALL parallel agents in ONE message**

```text
Example: 4 ACCEPT comments on different files

WRONG (Sequential):
  Fix file A → Wait → Fix file B → Wait → Fix file C → Wait → Fix file D

CORRECT (Parallel):
  Launch 4 Task agents simultaneously in ONE message:
  - Agent 1: Fix file A
  - Agent 2: Fix file B
  - Agent 3: Fix file C
  - Agent 4: Fix file D
  All complete in parallel → Then verify tests
```

- Use `subagent_type: general-purpose` for each fix
- Each agent: Read → Fix → Test the specific file
- Run final `./gradlew test` after ALL agents complete

#### D. Reply Individually
Use GitHub API to reply to the specific comment:
```bash
gh api repos/{owner}/{repo}/pulls/{PR_NUMBER}/comments/{COMMENT_ID}/replies \
  -method POST \
  -f body="REPLY_CONTENT"
```

### 4. Reply Format (공손한 말투)

**For ACCEPT:**
```markdown
리뷰 감사합니다! 좋은 지적이라고 생각합니다.

**수용합니다**

말씀해주신 부분이 맞습니다. [기술적 이유]

**변경 사항:**
- `file.java` (line X): [변경 내용]

**검증:** 테스트 통과 확인

피드백 감사드립니다!
```

**For REJECT:**
```markdown
리뷰 감사합니다! 의견 주신 부분 신중하게 검토했습니다.

**현재 구현을 유지하려고 합니다**

**이유:**
- [기술적 근거]
- 프로젝트 컨벤션: [관련 규칙]

혹시 제가 놓친 부분이 있다면 말씀해 주세요!
```

### 5. Parallel Processing Strategy

When you find multiple independent fixes (different files, no dependencies):
1. Group them
2. Launch parallel Task agents - one per fix
3. Each agent: Read -> Fix -> Test
4. Wait for all agents
5. Reply to each comment after its fix is complete

### 6. Summary Report

After processing all comments:
```text
=== CodeRabbit Review Summary ===
PR: #XX - "PR Title"

Accepted: N comments
- Comment #ID: [brief description]

Rejected: M comments
- Comment #ID: [brief description]

All replies posted individually.
Tests: PASSED/FAILED
```

## Critical Rules

1. **Individual Replies**: Reply to each comment separately, NOT in batch
2. **Evidence-Based**: Always provide technical reasoning
3. **CLAUDE.md Priority**: Project conventions override generic suggestions
4. **TDD Compliance**: New behavior requires test first
5. **No Forbidden Patterns**: Never introduce .block(), empty catch, etc.
6. **Verify Before Reply**: Run tests before claiming fix is complete
