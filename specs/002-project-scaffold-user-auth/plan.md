# Implementation Plan: 项目脚手架 + 用户认证体系

**Branch**: `002-project-scaffold-user-auth` | **Date**: 2026-05-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-project-scaffold-user-auth/spec.md`

## Summary

搭建 trading-diary 项目骨架（Spring Boot 3.2 + Next.js 14）并实现完整的用户认证体系。管理员通过 Flyway 种子数据初始化（BCrypt 加密），系统支持 JWT 无状态认证（access/refresh token 轮换）。开发环境通过 AutoLoginFilter 实现自动登录。预置 ADMIN/USER 角色，建立 RBAC 框架。技术方案详见 [design doc](../../docs/superpowers/specs/2026-05-11-project-scaffold-user-system-design.md)。

## Technical Context

**Language/Version**: Java 17+ / TypeScript
**Primary Dependencies**: Spring Boot 3.2.x, MyBatis-Plus 3.5+, Spring Security, jjwt, Flyway, Next.js 14, shadcn/ui, ky, zustand, TanStack Query
**Storage**: MySQL 8.0+ (dev/test: H2)
**Testing**: JUnit 5 + Mockito + H2 + MockMvc / Vitest + Playwright
**Target Platform**: Web (backend API + frontend SPA)
**Project Type**: web-service (monorepo: Java backend + Next.js frontend)
**Performance Goals**: API P95 < 200ms, token validation overhead < 50ms
**Constraints**: 密码 BCrypt cost ≥ 12, access token 15min, refresh token 7d
**Scale/Scope**: 初始 — 单管理员账号，为后续多用户扩展预留

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | 检查项 | 状态 |
|---|--------|------|
| 1 | 包路径在 `com.tradingdiary` 下，未私自添加顶层包 | ✅ |
| 2 | Controller 未直接注入 Mapper（分层不可逆） | ✅ |
| 3 | Controller 方法未包含业务逻辑（if/else 业务判断、金额计算） | ✅ |
| 4 | API 响应使用 `ApiResponse<T>` 包装，未直接返回实体对象 | ✅ |
| 5 | 金额/价格/盈亏字段使用 `BigDecimal`，非 `float`/`double` | N/A — 本功能不涉及金额 |
| 6 | `BigDecimal` 除法使用 `divide(value, scale, RoundingMode.HALF_UP)` | N/A |
| 7 | 数据库金额字段定义为 `decimal(20,8)` | N/A |
| 8 | 数据库表包含 `id`, `user_id`, `created_at`, `updated_at`, `is_deleted` | ✅ — 业务表满足，sys_* 表无 user_id（系统表） |
| 9 | 时间相关字段明确按 UTC 存储 | ✅ |
| 10 | 响应时间转换基于 `X-Timezone` 请求头 | ✅ |
| 11 | 认证方案基于 JWT + Spring Security，密码 BCrypt 存储 | ✅ |
| 12 | 异常类继承 `BaseException`，有 `@ControllerAdvice` 全局处理器 | ✅ |
| 13 | 命名符合约定（类名大驼峰、方法小驼峰、布尔 `is` 前缀） | ✅ |

**Gate Result**: PASS — 所有适用项通过，无违规。

## Project Structure

### Documentation (this feature)

```text
specs/002-project-scaffold-user-auth/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (API contracts)
│   └── auth-api.md
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

```text
# Backend (Spring Boot)
src/main/java/com/tradingdiary/
├── Application.java
├── config/
│   ├── SecurityConfig.java
│   └── JwtConfig.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthFilter.java
│   ├── AutoLoginFilter.java
│   ├── UserDetailsServiceImpl.java
│   └── SecurityExceptionHandler.java
├── controller/
│   └── AuthController.java
├── service/
│   ├── AuthService.java
│   └── impl/AuthServiceImpl.java
├── entity/
│   ├── SysUser.java
│   ├── SysRole.java
│   ├── SysUserRole.java
│   ├── SysPermission.java
│   └── SysRefreshToken.java
├── mapper/
│   ├── SysUserMapper.java
│   ├── SysRoleMapper.java
│   ├── SysUserRoleMapper.java
│   ├── SysPermissionMapper.java
│   └── SysRefreshTokenMapper.java
├── model/
│   ├── request/LoginRequest.java
│   ├── vo/TokenVO.java
│   ├── vo/UserInfoVO.java
│   └── ApiResponse.java
├── exception/
│   ├── BaseException.java
│   ├── BadRequestException.java
│   ├── UnauthorizedException.java
│   ├── NotFoundException.java
│   └── GlobalExceptionHandler.java
└── util/
    └── (reserved)

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-test.yml
├── application-prod.yml
└── db/migration/
    ├── V1__init_schema.sql
    └── V2__seed_admin.sql

src/test/java/com/tradingdiary/
├── controller/AuthControllerTest.java
├── service/AuthServiceTest.java
├── security/JwtTokenProviderTest.java
└── security/AutoLoginFilterTest.java

# Frontend (Next.js)
frontend/
├── package.json
├── next.config.js
├── tailwind.config.ts
├── tsconfig.json
├── playwright.config.ts
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── page.tsx
│   │   ├── login/page.tsx
│   │   └── (dashboard)/
│   │       ├── layout.tsx
│   │       └── dashboard/page.tsx
│   ├── lib/
│   │   ├── api.ts
│   │   └── auth.ts
│   ├── hooks/
│   │   └── useAuth.ts
│   └── components/
│       ├── providers/AuthProvider.tsx
│       └── layout/AuthGuard.tsx
└── e2e/ (Phase 1)

# E2E (project root, Phase 1)
playwright.config.ts
```

**Structure Decision**: 后端采用标准 Spring Boot 分层结构（controller → service → mapper），新增 `security/` 包收拢认证相关组件。前端采用 Next.js App Router，`(dashboard)/` 路由组实现认证守卫。

## Complexity Tracking

> 无宪法违规，无需记录。
