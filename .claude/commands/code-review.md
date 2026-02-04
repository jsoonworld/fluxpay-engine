# Code Review Processor

PR 리뷰 코멘트를 처리합니다. (리뷰어: 사람, CodeRabbit, 기타 모두 적용)

## Target PR: $ARGUMENTS (없으면 현재 브랜치 PR)

## Tone: 공손하고 친절하게

- 항상 감사 표현으로 시작
- 거절해도 존중하는 태도
- "틀렸다"가 아닌 "다른 관점" 표현
- 추가 의견 환영하는 마무리

---

## Execution Steps

### 1. PR 정보 가져오기
```bash
gh pr view --json number,title,url
gh api repos/{owner}/{repo}/pulls/{PR_NUMBER}/comments --paginate
```

### 2. 각 코멘트 분석

**수용 기준:**
- 실제 버그/보안 이슈 수정
- 프로젝트 컨벤션 준수 (CLAUDE.md)
- 측정 가능한 품질 향상

**거절 기준:**
- CLAUDE.md 규칙 충돌
- 과도한 엔지니어링
- 기술적 근거 없는 스타일 선호

### 3. 수정 (ACCEPT 시) - 병렬 필수

**독립적 수정은 반드시 병렬 실행:**

```
4개 파일 수정 필요:

WRONG (순차):
  File A → Wait → File B → Wait → File C → Wait → File D

CORRECT (병렬):
  4개 Task 에이전트 동시 실행 (하나의 메시지)
```

### 4. 개별 Reply

각 코멘트에 **개별적으로** 답변 (배치 X)

---

## Reply Templates

### ACCEPT
```markdown
리뷰 감사합니다! 좋은 지적이라고 생각합니다.

**수용합니다**

말씀해주신 부분이 맞습니다. [이유]

**변경 사항:**
- `file.java` (line X): [변경 내용]

**검증:** 테스트 통과 확인

피드백 감사드립니다!
```

### REJECT
```markdown
리뷰 감사합니다! 의견 주신 부분 신중하게 검토했습니다.

**현재 구현을 유지하려고 합니다**

**이유:**
- [기술적 근거]
- 프로젝트 컨벤션: [관련 규칙]

혹시 제가 놓친 부분이 있다면 말씀해 주세요!
```

### PARTIAL ACCEPT
```markdown
리뷰 감사합니다!

**일부 수용합니다**

**수용:** [내용] - 좋은 지적이라 반영했습니다.
**유지:** [내용] - [이유]

추가 의견 있으시면 편하게 말씀해 주세요!
```

---

## Critical Rules

1. **개별 Reply** - 코멘트마다 따로 답변
2. **병렬 실행** - 독립적 수정은 동시에
3. **공손한 말투** - 감사, 존중, 겸손
4. **근거 필수** - 기술적 이유 항상 포함
5. **테스트 필수** - 수정 후 검증
