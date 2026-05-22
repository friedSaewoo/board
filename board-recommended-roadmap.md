# Board Project Recommended Roadmap

## 전제

- 목적: 장기 학습 프로젝트
- 구현 방식: 애플리케이션 코드는 직접 작성
- AI 사용 범위: 계획, 리뷰, 테스트 제안, 문서/체크리스트 보조
- 1차 비목표: 프론트 고도화, AI 코드작성, MSA/K8s, 완벽한 운영수준 보안
- 배포 방향: AWS Free Tier 중심, 비용 주의

## 0단계 — 프로젝트 기초 정리

현재 repo는 Spring Boot skeleton에 가깝다.

먼저 할 것:

1. `application.yaml` 하드코딩 DB 계정 제거
2. `local`, `dev`, `prod` profile 분리
3. MySQL 연결 확인
4. 기본 패키지 구조 생성
5. 테스트 실행 환경 만들기

체크포인트:

- `./gradlew test` 통과
- DB 비밀번호가 git에 직접 남지 않음
- README에 실행 방법 작성

## 1단계 — 게시판 CRUD

가장 먼저 직접 구현할 핵심.

순서:

1. 게시글 Entity
2. 게시글 생성
3. 게시글 단건 조회
4. 게시글 목록 조회
5. 게시글 수정
6. 게시글 삭제
7. validation
8. 예외 응답 형식 통일

이슈 예시:

- `feat: 게시글 생성 API`
- `feat: 게시글 목록/상세 조회 API`
- `feat: 게시글 수정/삭제 API`
- `test: 게시글 CRUD 통합 테스트`

## 2단계 — 로그인/인증

CRUD 다음에 붙인다.

추천 순서:

1. 회원가입
2. 로그인
3. 비밀번호 해싱
4. 세션 또는 JWT 중 하나 선택
5. 게시글 작성자 연결
6. 본인 글만 수정/삭제 가능

처음에는 세션 로그인을 추천한다. JWT보다 흐름이 단순하고 Spring Security 기본기를 배우기 좋다. 나중에 JWT로 리팩터링해도 된다.

## 3단계 — 게시판 기능 확장

추천 순서:

1. 댓글
2. 페이징
3. 검색
4. 정렬
5. 조회수
6. 좋아요
7. 첨부파일
8. soft delete
9. 관리자 기능

| 기능 | 학습 포인트 |
|---|---|
| 댓글 | 연관관계, cascade, 삭제 정책 |
| 페이징 | Pageable, 인덱스 |
| 검색 | query method, JPQL, QueryDSL 후보 |
| 조회수 | 동시성 기초 |
| 좋아요 | unique constraint |
| 파일 업로드 | storage, 보안 |
| soft delete | 데이터 보존 정책 |
| 관리자 | 권한 관리 |

## 4단계 — 테스트/품질

추천 순서:

1. Service 단위 테스트
2. Controller 통합 테스트
3. Repository 테스트
4. 인증 필요한 API 테스트
5. 실패 케이스 테스트

목표는 테스트 커버리지 숫자보다 “내가 만든 기능이 깨졌는지 알 수 있는가”다.

## 5단계 — 로그북 / 마스킹

추천:

- Zalando Logbook으로 HTTP request/response logging 실습
- `Authorization`
- `Cookie`
- `password`
- `accessToken`
- `refreshToken`
- `email`
- `phone`
- 주민번호 같은 민감정보는 애초에 저장하지 않기

추천 이슈:

- `chore: HTTP 요청/응답 로깅 도입`
- `feat: 민감정보 로그 마스킹 적용`
- `test: 로그인 요청에서 password가 로그에 남지 않는지 검증`

## 6단계 — GitHub 전략

기본 흐름:

```text
issue 생성
→ feature branch 생성
→ 직접 구현
→ test
→ PR 생성
→ AI PR review 요청
→ 직접 수정
→ rebase
→ squash merge
```

브랜치 예시:

```text
main
feature/1-board-create
feature/2-board-read
feature/3-login
fix/4-auth-error-response
chore/5-logbook
```

커밋 예시:

```text
feat: add post entity
feat: add post create api
test: add post create integration test
refactor: extract post response mapper
```

PR 규칙:

- 한 PR = 한 이슈
- 최대 300~500 lines changed 안쪽 권장
- 기능 + 테스트 같이 포함
- PR 설명에 직접 배운 점 기록

merge 전략:

- feature branch는 `main` 위로 rebase
- merge는 `Squash and merge`
- main에는 깨끗한 히스토리 유지

## 7단계 — AI PR 리뷰 정책

허용:

- PR 리뷰
- 테스트 케이스 제안
- 보안 위험 지적
- 네이밍/구조 피드백
- README/이슈 템플릿 작성 도움
- “왜 이 코드가 위험한지” 설명 요청

금지:

- AI가 앱 코드 직접 작성
- Copilot suggestion을 그대로 적용
- Copilot agent에게 구현 맡기기
- AI가 만든 커밋을 이해 없이 merge

추천 정책:

```md
AI Usage Policy

- Application code is written manually.
- AI may be used for PR review, test suggestions, documentation review, and security questions.
- AI suggestions must be manually understood and rewritten if accepted.
- No AI-generated code is merged without human understanding.
```

`.github/copilot-instructions.md` 추천 내용:

```md
This project is a manual-learning Spring Boot board project.

When reviewing PRs:
- Do not suggest large rewrites.
- Focus on correctness, security, tests, transaction boundaries, validation, and logging safety.
- Point out sensitive data leakage.
- Prefer small educational explanations.
- Do not generate full feature implementations.
```

## 8단계 — AWS Free Tier 배포/LB 실험

추천 순서:

1. 로컬 실행
2. Dockerfile 작성
3. Docker Compose로 app + MySQL
4. Nginx reverse proxy 로컬 실습
5. AWS EC2 1대 배포
6. RDS는 나중에, 처음엔 EC2 내부 MySQL 또는 Docker MySQL
7. CloudWatch logs 맛보기
8. ALB 붙이기
9. app instance 2개 띄우고 LB health check 실습
10. 장애 실험: 한 instance 내렸을 때 트래픽 동작 확인

처음부터 ALB 붙이지 말고, Nginx reverse proxy → EC2 → ALB 순서가 좋다.

## 최종 추천 진행 순서

```text
1. 설정/DB/README 정리
2. 게시글 CRUD
3. 테스트 기반 정리
4. 회원가입/로그인
5. 댓글/페이징/검색
6. GitHub issue → branch → PR → AI review 루틴 고정
7. Logbook + 마스킹
8. Docker / Docker Compose
9. AWS EC2 배포
10. Nginx reverse proxy
11. ALB / health check / 장애 실험
12. 모니터링/로그/릴리즈 노트 확장
```

## 핵심 원칙

기능을 많이 붙이는 것보다, 각 단계마다 “직접 구현 → 테스트 → PR → 리뷰 → 기록 → 배포/운영 실험” 사이클을 반복하는 게 더 큰 학습이다.
