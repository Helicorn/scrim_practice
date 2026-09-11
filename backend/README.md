# civilwar-backend

내전 서포트 페이지 Spring Boot 백엔드 (Oracle + JPA 기본).

## 요구 사항

- Java 21+
- Oracle XE (로컬) + `c##lol_scrim` 스키마
- (Gradle Wrapper 포함 — 별도 Gradle 설치 불필요)

## 실행

프로젝트 **루트**에서:

| 명령 | 설명 |
|------|------|
| `npm run server` | Oracle + JPA + REST API |
| `npm run server:build` | Gradle 빌드 |

또는 `backend-dev.bat` 더블클릭.

`backend` 폴더에서 직접: `npm run dev`

기본 포트: `http://localhost:8080`

기본 Spring 프로필: `oracle,oracle-local` (`application.yml`)

## 사전 준비 (최초 1회)

| 항목 | 값 |
|------|-----|
| Host | `localhost` |
| Port | `1521` |
| SID | `xe` |
| 사용자 | `c##lol_scrim` |

1. **SYS** → [`database/grant-tablespace-quota.sql`](../database/grant-tablespace-quota.sql)
2. **`c##lol_scrim`** → [`database/schema.sql`](../database/schema.sql)
3. 로컬 비밀번호 파일:

```powershell
copy backend\src\main\resources\application-oracle-local.yml.example backend\src\main\resources\application-oracle-local.yml
# application-oracle-local.yml 에 비밀번호 입력 (Git 제외)
```

비밀번호만 환경 변수로 줄 때: `ORACLE_PASSWORD` 설정 가능.

## 엔드포인트

| 경로 | 설명 |
|------|------|
| `GET /api/health` | 서버 상태 |
| `/riot-api/**` | Riot API 프록시 (asia, `X-Riot-Token`) |
| `GET /api/sessions/{sessionCode}` | 진행 중 세션 스냅샷 (이어서 하기) |
| `POST /api/sessions/{sessionCode}/cancel` | 진행 중 세션 취소 (`CANCELLED`) |
| `POST /api/sessions/{sessionCode}/players` | 10인 소환사·세션 저장 |
| `PUT /api/sessions/{sessionCode}/teams` | 팀·포지션 배치 저장 (`TEAM_SETUP`) |
| `POST /api/sessions/{sessionCode}/rank-stats` | League-V4 랭크 갱신 (`X-Riot-Token`) |
| `POST /api/sessions/{sessionCode}/pick-ban` | 밴픽 마감 → `RESULT_INPUT` |
| `POST /api/sessions/{sessionCode}/match-result` | 경기 결과 → `DRAFT` 또는 `FINISHED` |

### 세션 STATUS 전이

| 이벤트 | STATUS |
|--------|--------|
| 로스터 등록 | `PLAYERS` |
| 팀 배치 저장 | `TEAM_SETUP` |
| 밴픽 마감 | `RESULT_INPUT` |
| 결과 저장 · 시리즈 계속 | `DRAFT` (`CURRENT_MATCH_NO` = 다음 판) |
| 결과 저장 · 시리즈 종료 | `FINISHED` |
| 사용자 삭제 | `CANCELLED` |

### 이어서 하기 — 수동 검증

| # | 시나리오 | 확인 |
|---|----------|------|
| 1 | 로스터만 `POST .../players` → `GET` 스냅샷 | `status=PLAYERS`, `suggestedRoute=teams` |
| 2 | `PUT .../teams` 후 스냅샷 | `TEAM_SETUP`, `players[].teamColor/positionName` 채움 |
| 3 | `POST .../pick-ban` 후 스냅샷 | `RESULT_INPUT`, `currentMatchPicks` 10픽 |
| 4 | `POST .../match-result` (시리즈 계속) | `DRAFT`, `currentMatchNo` +1, 스코어 반영 |
| 5 | `POST .../cancel` 후 `GET` | 404 · 전적 테이블은 유지 |

상세 UI 시나리오: [`../기획.md`](../기획.md) §8 세션 이어서 하기.

## JPA

| 패키지 | 내용 |
|--------|------|
| `com.civilwar.domain.entity` | Oracle 테이블 Entity |
| `com.civilwar.domain.repository` | Spring Data JPA |
| `com.civilwar.domain.service` | 세션·랭크·전적 표시 분기 |

DDL: [`../database/schema.sql`](../database/schema.sql) · `ddl-auto: validate`
