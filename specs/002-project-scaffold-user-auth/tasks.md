# Tasks: 项目脚手架 + 用户认证体系

**Input**: Design documents from `/specs/002-project-scaffold-user-auth/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Included — 项目宪法和国际规范要求新 API 必须有集成测试。

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths in descriptions

## Phase 1: Setup (Project Initialization)

**Purpose**: Initialize Gradle/Spring Boot skeleton and Next.js frontend

- [ ] T001 Create Gradle wrapper and `build.gradle` with dependencies (Spring Boot 3.2.x, MyBatis-Plus 3.5+, Spring Security, jjwt, Flyway, SpringDoc, H2) at `build.gradle`
- [ ] T002 Create `settings.gradle` with project name `trading-diary` at `settings.gradle`
- [ ] T003 Create Spring Boot application entry point `Application.java` at `src/main/java/com/tradingdiary/Application.java`
- [ ] T004 [P] Create multi-environment config files: `application.yml`, `application-dev.yml`, `application-test.yml`, `application-prod.yml` at `src/main/resources/`
- [ ] T005 [P] Initialize Next.js 14 frontend with TypeScript, TailwindCSS, App Router at `frontend/`
- [ ] T006 [P] Install frontend dependencies: ky, zustand, @tanstack/react-query, shadcn/ui init at `frontend/package.json`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database schema, base entities, global error handling, and shared infrastructure that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database & Entities

- [ ] T007 Create Flyway migration V1: `sys_user`, `sys_role`, `sys_user_role`, `sys_refresh_token`, `sys_permission`, `sys_role_permission` tables at `src/main/resources/db/migration/V1__init_schema.sql`
- [ ] T008 Create Flyway migration V2: admin/role seed data (幂等 INSERT ... WHERE NOT EXISTS) at `src/main/resources/db/migration/V2__seed_admin.sql`
- [ ] T009 [P] Create `SysUser` entity with MyBatis-Plus annotations at `src/main/java/com/tradingdiary/entity/SysUser.java`
- [ ] T010 [P] Create `SysRole` entity at `src/main/java/com/tradingdiary/entity/SysRole.java`
- [ ] T011 [P] Create `SysUserRole` entity at `src/main/java/com/tradingdiary/entity/SysUserRole.java`
- [ ] T012 [P] Create `SysPermission` entity at `src/main/java/com/tradingdiary/entity/SysPermission.java`
- [ ] T013 [P] Create `SysRefreshToken` entity (token_hash, expires_at, revoked) at `src/main/java/com/tradingdiary/entity/SysRefreshToken.java`
- [ ] T014 [P] Create `SysUserMapper` interface extending `BaseMapper<SysUser>` at `src/main/java/com/tradingdiary/mapper/SysUserMapper.java`
- [ ] T015 [P] Create `SysRoleMapper` interface at `src/main/java/com/tradingdiary/mapper/SysRoleMapper.java`
- [ ] T016 [P] Create `SysUserRoleMapper` interface at `src/main/java/com/tradingdiary/mapper/SysUserRoleMapper.java`
- [ ] T017 [P] Create `SysPermissionMapper` interface at `src/main/java/com/tradingdiary/mapper/SysPermissionMapper.java`
- [ ] T018 [P] Create `SysRefreshTokenMapper` interface with `selectValidByUserId` and `revokeByUserId` at `src/main/java/com/tradingdiary/mapper/SysRefreshTokenMapper.java`

### Global Error Handling & Response Format

- [ ] T019 Create `ApiResponse<T>` unified response wrapper at `src/main/java/com/tradingdiary/model/ApiResponse.java`
- [ ] T020 Create `BaseException` abstract class at `src/main/java/com/tradingdiary/exception/BaseException.java`
- [ ] T021 [P] Create `BadRequestException` at `src/main/java/com/tradingdiary/exception/BadRequestException.java`
- [ ] T022 [P] Create `UnauthorizedException` at `src/main/java/com/tradingdiary/exception/UnauthorizedException.java`
- [ ] T023 [P] Create `NotFoundException` at `src/main/java/com/tradingdiary/exception/NotFoundException.java`
- [ ] T024 Create `GlobalExceptionHandler` with `@ControllerAdvice` at `src/main/java/com/tradingdiary/exception/GlobalExceptionHandler.java`

### Request/Response Models

- [ ] T025 [P] Create `LoginRequest` (username, password) at `src/main/java/com/tradingdiary/model/request/LoginRequest.java`
- [ ] T026 [P] Create `TokenVO` (accessToken, refreshToken, expiresIn) at `src/main/java/com/tradingdiary/model/vo/TokenVO.java`
- [ ] T027 [P] Create `UserInfoVO` (id, username, nickname, roles) at `src/main/java/com/tradingdiary/model/vo/UserInfoVO.java`

**Checkpoint**: Foundation ready — database schema, entities, mappers, error handling in place

---

## Phase 3: User Story 1 - 管理员登录 (Priority: P1) 🎯 MVP

**Goal**: 管理员通过用户名和密码登录，获取 JWT 令牌，使用令牌访问受保护资源

**Independent Test**: `POST /api/v1/auth/login` 返回令牌 → `GET /api/v1/auth/me` 携带令牌返回用户信息

### Implementation for US1

- [ ] T028 Create `JwtConfig` properties class (secret, access-expiration, refresh-expiration) at `src/main/java/com/tradingdiary/config/JwtConfig.java`
- [ ] T029 Create `JwtTokenProvider` (issue/validate/parse tokens) at `src/main/java/com/tradingdiary/security/JwtTokenProvider.java`
- [ ] T030 Create `UserDetailsServiceImpl` implementing `UserDetailsService` — loads user + roles from DB at `src/main/java/com/tradingdiary/security/UserDetailsServiceImpl.java`
- [ ] T031 Create `JwtAuthFilter` extending `OncePerRequestFilter` — extracts Bearer token, validates, sets SecurityContext at `src/main/java/com/tradingdiary/security/JwtAuthFilter.java`
- [ ] T032 Create `SecurityConfig` — SecurityFilterChain bean, endpoint permissions, CORS, password encoder at `src/main/java/com/tradingdiary/config/SecurityConfig.java`
- [ ] T033 Create `SecurityExceptionHandler` — maps AuthenticationException to ApiResponse at `src/main/java/com/tradingdiary/security/SecurityExceptionHandler.java`
- [ ] T034 Create `AuthService` interface (login) at `src/main/java/com/tradingdiary/service/AuthService.java`
- [ ] T035 Create `AuthServiceImpl` — validates credentials, issues tokens, writes refresh token hash to DB at `src/main/java/com/tradingdiary/service/impl/AuthServiceImpl.java`
- [ ] T036 Create `AuthController` with `POST /api/v1/auth/login` and `GET /api/v1/auth/me` at `src/main/java/com/tradingdiary/controller/AuthController.java`
- [ ] T037 Create `AdminInitializer` implementing `ApplicationRunner` — checks admin password placeholder, reads `ADMIN_INIT_PASSWORD` env var, BCrypt hashes, updates DB at `src/main/java/com/tradingdiary/config/AdminInitializer.java`

**Checkpoint**: Login flow fully functional. `POST /login` returns tokens, `GET /me` returns user info when authenticated.

---

## Phase 4: User Story 3 - 开发环境自动登录 (Priority: P1)

**Goal**: Dev 模式下开发者无需手动认证即可访问所有接口

**Independent Test**: `bootRun --spring.profiles.active=dev` → 不带 Authorization 头访问 `GET /api/v1/auth/me` 返回 admin 用户信息

### Implementation for US3

- [ ] T038 [US3] Create `AutoLoginFilter` — `@Profile("dev")`, before JwtAuthFilter, injects admin to SecurityContext at `src/main/java/com/tradingdiary/security/AutoLoginFilter.java`
- [ ] T039 [US3] Update `SecurityConfig` to include AutoLoginFilter in dev profile chain at `src/main/java/com/tradingdiary/config/SecurityConfig.java`
- [ ] T040 [US3] Configure `application-dev.yml` with dev-only default passwords and JWT secret at `src/main/resources/application-dev.yml`

**Checkpoint**: Dev mode auto-login works. Prod mode (no dev profile) still requires valid token.

---

## Phase 5: User Story 2 - 凭证刷新与安全登出 (Priority: P2)

**Goal**: 访问令牌过期后通过刷新令牌获取新令牌；登出后刷新令牌立即失效

**Independent Test**: `POST /login` → `POST /refresh` with refresh token → new token pair returned, old refresh token invalid (DB revoked) → `POST /logout` → `POST /refresh` with logged-out token fails

### Implementation for US2

- [ ] T041 [US2] Add token refresh support to `JwtTokenProvider` — validate refresh token, generate new pair at `src/main/java/com/tradingdiary/security/JwtTokenProvider.java`
- [ ] T042 [US2] Add `refreshToken()` and `logout()` to `AuthService` at `src/main/java/com/tradingdiary/service/AuthService.java`
- [ ] T043 [US2] Implement `refreshToken()` in `AuthServiceImpl` — token rotation: query DB for old hash, mark revoked=1, issue new pair, persist new hash at `src/main/java/com/tradingdiary/service/impl/AuthServiceImpl.java`
- [ ] T044 [US2] Implement `logout()` in `AuthServiceImpl` — mark all user's unexpired refresh tokens revoked=1 at `src/main/java/com/tradingdiary/service/impl/AuthServiceImpl.java`
- [ ] T045 [US2] Add `POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout` to `AuthController` at `src/main/java/com/tradingdiary/controller/AuthController.java`

**Checkpoint**: Token refresh and logout fully functional with DB-backed invalidation.

---

## Phase 6: Frontend (Authentication UI)

**Purpose**: Frontend pages and auth state management

- [ ] T046 [P] Create `api.ts` — ky instance with base URL, auto-inject Authorization header, 401 interceptor at `frontend/src/lib/api.ts`
- [ ] T047 [P] Create `auth.ts` — login/refresh/logout/me API call wrappers at `frontend/src/lib/auth.ts`
- [ ] T048 [P] Create `useAuth.ts` — zustand store: token state, login/logout/refresh actions at `frontend/src/hooks/useAuth.ts`
- [ ] T049 Create `AuthProvider.tsx` — next-auth context: dev mode auto-login, prod mode token lifecycle at `frontend/src/components/providers/AuthProvider.tsx`
- [ ] T050 Create `AuthGuard.tsx` — redirects unauthenticated users to /login (prod only) at `frontend/src/components/layout/AuthGuard.tsx`
- [ ] T051 Create root `layout.tsx` — wraps app with AuthProvider, QueryClientProvider at `frontend/src/app/layout.tsx`
- [ ] T052 [P] Create login page at `frontend/src/app/login/page.tsx`
- [ ] T053 [P] Create dashboard layout with sidebar + topbar + AuthGuard at `frontend/src/app/(dashboard)/layout.tsx`
- [ ] T054 [P] Create dashboard placeholder page at `frontend/src/app/(dashboard)/dashboard/page.tsx`
- [ ] T055 Create home page — dev mode redirects to dashboard, prod mode shows landing at `frontend/src/app/page.tsx`
- [ ] T056 [P] Install and configure shadcn/ui base components (button, card, input, dropdown-menu, avatar) at `frontend/src/components/ui/`

**Checkpoint**: Frontend auth flow complete. Dev mode: auto-login, directly enters dashboard. Prod mode: redirects to /login, login form works.

---

## Phase 7: Tests

**Purpose**: Verify all authentication flows work correctly

### Backend Tests

- [ ] T057 [P] [US1] Write `JwtTokenProviderTest` — token issuance, validation, expiration, parse at `src/test/java/com/tradingdiary/security/JwtTokenProviderTest.java`
- [ ] T058 [P] [US1] Write `AuthServiceTest` — login success, wrong password, disabled account at `src/test/java/com/tradingdiary/service/AuthServiceTest.java`
- [ ] T059 [P] [US1] Write `AuthControllerTest` using MockMvc — POST /login, GET /me at `src/test/java/com/tradingdiary/controller/AuthControllerTest.java`
- [ ] T060 [P] [US3] Write `AutoLoginFilterTest` — dev profile injects admin, prod profile absent at `src/test/java/com/tradingdiary/security/AutoLoginFilterTest.java`
- [ ] T061 [P] [US2] Write refresh token test — valid refresh succeeds, rotation invalidates old token (DB revoked) at `src/test/java/com/tradingdiary/security/JwtTokenProviderTest.java` (extend T057)
- [ ] T062 [P] [US2] Write logout test — logout invalidates refresh tokens (DB revoked=1) at `src/test/java/com/tradingdiary/controller/AuthControllerTest.java` (extend T059)

### Frontend Tests

- [ ] T063 [P] Write `useAuth` zustand store unit test (Vitest) at `frontend/src/hooks/useAuth.test.ts`
- [ ] T064 [P] Write `AuthGuard` component test — redirects when not authenticated at `frontend/src/components/layout/AuthGuard.test.tsx`

**Note**: SC-002（登录 < 2s）和 SC-003（token 验证 < 50ms）的性能验证在 Phase 2 部署时通过 JMeter 压测完成，本阶段不设单元性能测试。

**Checkpoint**: All tests pass. `./gradlew test && cd frontend && pnpm test` green.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [ ] T065 Verify all constitution Phase 0 checklist items pass
- [ ] T066 Run quickstart.md validation — follow quickstart steps end-to-end
- [ ] T067 [P] Remove stale `specs/001-project-scaffold-user-auth/` directory
- [ ] T068 Code cleanup — remove unused imports, ensure consistent formatting

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T001–T004 for DB migrations, T005–T006 for frontend) — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational — core login + token auth
- **US3 (Phase 4)**: Depends on US1 (needs SecurityConfig + JwtTokenProvider from Phase 3)
- **US2 (Phase 5)**: Depends on US1 (extends login token system, needs SysRefreshToken from Phase 2)
- **Frontend (Phase 6)**: Depends on US1 + US3 + US2 (all backend auth APIs ready)
- **Tests (Phase 7)**: Depends on all implementation phases
- **Polish (Phase 8)**: Depends on all phases complete

### User Story Dependencies

- **US1 (P1)**: Foundation → complete → 🎯 MVP ready
- **US3 (P1)**: US1 → complete → dev experience complete
- **US2 (P2)**: US1 → complete → full token lifecycle

### Within Each User Story

- Config / provider classes before filters
- Filters before SecurityConfig
- SecurityConfig before service
- Service before controller

### Parallel Opportunities

- T009–T013 (entities) can all run in parallel
- T014–T018 (mappers) can all run in parallel
- T021–T023 (exceptions) can run in parallel
- T025–T027 (VO/Request models) can run in parallel
- T057–T062 (backend tests) can all run in parallel
- T063–T064 (frontend tests) can run in parallel
- T046–T048, T052–T054, T056 (frontend files) large parallel batch

---

## Parallel Example: Phase 2 Entities

```bash
# Launch all entity tasks together:
Task: "Create SysUser entity at src/main/java/com/tradingdiary/entity/SysUser.java"
Task: "Create SysRole entity at src/main/java/com/tradingdiary/entity/SysRole.java"
Task: "Create SysUserRole entity at src/main/java/com/tradingdiary/entity/SysUserRole.java"
Task: "Create SysPermission entity at src/main/java/com/tradingdiary/entity/SysPermission.java"
Task: "Create SysRefreshToken entity at src/main/java/com/tradingdiary/entity/SysRefreshToken.java"

# Launch all mapper tasks together:
Task: "Create SysUserMapper at src/main/java/com/tradingdiary/mapper/SysUserMapper.java"
Task: "Create SysRoleMapper at src/main/java/com/tradingdiary/mapper/SysRoleMapper.java"
Task: "Create SysUserRoleMapper at src/main/java/com/tradingdiary/mapper/SysUserRoleMapper.java"
Task: "Create SysPermissionMapper at src/main/java/com/tradingdiary/mapper/SysPermissionMapper.java"
Task: "Create SysRefreshTokenMapper at src/main/java/com/tradingdiary/mapper/SysRefreshTokenMapper.java"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 (管理员登录)
4. **STOP and VALIDATE**: `POST /login` → token → `GET /me` works
5. Demo as MVP

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. Add US1 (P1) → login works → 🎯 **MVP!**
3. Add US3 (P1) → dev auto-login → dev experience complete
4. Add US2 (P2) → full token lifecycle (DB-backed invalidation)
5. Add Frontend → UI complete
6. Add Tests → quality verified
7. Polish → ready to merge

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story (US1/US2/US3)
- Each user story checkpoint validates independence
- Commit after each task or logical group
- Backend paths use `src/main/java/com/tradingdiary/`
- Frontend paths use `frontend/src/`
- Dev default password: `admin123` (from `application-dev.yml`)
- Refresh token 失效通过 `sys_refresh_token` 表管理：签发时写入 SHA-256 哈希，刷新/登出时标记 revoked=1
