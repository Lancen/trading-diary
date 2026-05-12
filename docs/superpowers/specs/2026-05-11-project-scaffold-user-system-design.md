# 子项目 1：项目脚手架 + 用户体系 — 设计文档

**日期**: 2026-05-11 | **状态**: 待实施

## 背景

trading-diary 项目处于 Phase 0（初始化），无业务代码。需要先搭建项目骨架和认证体系，为后续数据采集、股票查询、两融分析等子项目提供基础。

## 项目分解

整体 SaaS 平台拆分为以下子项目，按依赖顺序构建：

1. **项目脚手架 + 用户体系**（本文档） ← 当前
2. 数据采集层（Akshare 集成、定时/手动拉取）
3. 股票信息查询
4. 两融分析 + 标签管理

## 技术决策

| 决策点 | 选择 | 依据 |
|--------|------|------|
| 管理员账号存储 | Flyway 种子数据（DB） | 与宪法 Phase 0 体系一致 |
| Dev 自动登录 | 后端 DevFilter（AutoLoginFilter） | 最简方案，dev profile 不存在即失效 |
| 认证体系范围 | 完整 JWT + RBAC（USER/ADMIN） | 宪法 Phase 0 要求，一次到位避免返工 |
| 密码初始化 | 环境变量 `ADMIN_INIT_PASSWORD`，默认值在 dev.yml | 安全且灵活 |

## §1 数据库设计

### 新增表

**sys_user** — 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键自增 |
| username | varchar(50) | 唯一索引，登录用 |
| password | varchar(255) | BCrypt 密文 |
| nickname | varchar(50) | 显示名 |
| status | tinyint | 1=正常, 0=禁用 |
| created_at / updated_at / is_deleted | — | 标准字段 |

**sys_role** — 角色表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键自增 |
| code | varchar(30) | 唯一，如 `ADMIN`、`USER` |
| name | varchar(50) | 显示名 |

**sys_user_role** — 用户-角色关联表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键自增 |
| user_id | bigint | FK → sys_user.id |
| role_id | bigint | FK → sys_role.id |

**sys_permission** — 权限表（为 Phase 1 预留）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键自增 |
| code | varchar(50) | 唯一，如 `trade:read` |
| name | varchar(50) | 显示名 |

### 种子数据

- 1 个 ADMIN 角色（`code=ADMIN`）
- 1 个管理员用户（`username=admin`，密码从 `ADMIN_INIT_PASSWORD` 环境变量读取，默认值 `admin123`，BCrypt 加密存储）
- 关联 admin → ADMIN 角色

### Flyway 迁移

```text
V1__init_schema.sql     # sys_user, sys_role, sys_user_role, sys_permission 建表
V2__seed_admin.sql      # 管理员种子数据
```

---

## §2 后端设计

### 认证流程

```
请求 → AutoLoginFilter（dev profile，注入 admin）
     → JwtAuthFilter（验签 + 注入 SecurityContext，prod 用）
     → Controller → Service
```

AutoLoginFilter 在 `JwtAuthFilter` 之前执行。dev profile 下前端无需传 `Authorization` 头。

### 包结构（新增）

```text
com.tradingdiary/
├── config/
│   ├── SecurityConfig.java          # SecurityFilterChain, 端点权限, CORS
│   └── JwtConfig.java               # JWT 密钥、过期时间（@ConfigurationProperties）
├── security/                         # ← 新增包
│   ├── JwtTokenProvider.java        # 签发/验证/解析 JWT
│   ├── JwtAuthFilter.java           # OncePerRequestFilter，验签注入 SecurityContext
│   ├── AutoLoginFilter.java         # @Profile("dev")，注入 admin 身份
│   ├── UserDetailsServiceImpl.java  # 从 DB 加载用户（UserDetailsService）
│   └── SecurityExceptionHandler.java # AuthenticationException → ApiResponse
├── controller/
│   └── AuthController.java          # POST login, refresh, logout
├── service/
│   ├── AuthService.java
│   └── impl/AuthServiceImpl.java
├── entity/
│   ├── SysUser.java                 # @TableName("sys_user")
│   ├── SysRole.java                 # @TableName("sys_role")
│   ├── SysUserRole.java             # @TableName("sys_user_role")
│   └── SysPermission.java           # @TableName("sys_permission")
├── mapper/
│   ├── SysUserMapper.java
│   ├── SysRoleMapper.java
│   ├── SysUserRoleMapper.java
│   └── SysPermissionMapper.java
└── model/
    ├── request/
    │   └── LoginRequest.java        # username + password
    └── vo/
        ├── TokenVO.java             # accessToken + refreshToken + expiresIn
        └── UserInfoVO.java          # id, username, nickname, roles
```

### SecurityConfig 端点权限

| 端点 | 权限 |
|------|------|
| `POST /api/v1/auth/login` | 匿名 |
| `POST /api/v1/auth/refresh` | 匿名（用 refresh token） |
| `POST /api/v1/auth/logout` | 需认证 |
| 其他所有 `/api/**` | 需认证 |
| `/actuator/health` | 匿名 |

### JWT 配置

- Access Token: 15 分钟有效期（宪法 §5）
- Refresh Token: 7 天有效期
- 密钥: `jwt.secret` 配置属性，dev 环境有默认值，prod 通过环境变量注入
- Token 载荷: `sub`(userId), `roles`(角色列表), `iat`, `exp`

### AutoLoginFilter 机制

- `@Profile("dev")` + `@Order(BEFORE_JWT_FILTER)` 确保仅在 dev profile 且先于 JWT 过滤器执行
- 从配置读取 admin username，查 DB 获取用户，注入 `UsernamePasswordAuthenticationToken` 到 `SecurityContext`
- 不依赖密码验证（种子数据已保证 admin 用户存在）

### 全局组件

- `ApiResponse<T>` — 统一响应格式（已在技术规范 §3.2 定义）
- `BaseException` + `@ControllerAdvice` — 统一异常处理（已在宪法 Phase 0 要求）

---

## §3 前端设计

### 初始化

`pnpm create next-app@latest frontend --typescript --tailwind --src-dir --app --import-alias "@/*"`

### 页面结构

```text
frontend/src/app/
├── layout.tsx              # 根布局（Providers: AuthProvider, QueryClient, Theme）
├── page.tsx                # 首页（仪表盘，Phase 1 实现）
├── login/
│   └── page.tsx            # 登录页（prod 使用）
├── (dashboard)/            # 需认证的页面组
│   ├── layout.tsx          # 侧边栏 + 顶栏 + AuthGuard
│   └── dashboard/
│       └── page.tsx        # 仪表盘占位
```

### 认证机制

**Dev 模式**:
- `AuthProvider` 在初始化时检测配置（`NEXT_PUBLIC_DEV_AUTO_LOGIN=true`）
- 后端 AutoLoginFilter 直接注入 admin 身份
- 前端不渲染登录页，直接进 dashboard

**Prod 模式**:
- 未认证重定向 `/login`
- 登录成功存储 token（内存，TanStack Query 管理刷新）
- `ky` 实例自动注入 `Authorization: Bearer <token>`

### 关键前端文件

```text
frontend/src/
├── lib/
│   ├── api.ts              # ky 实例（base URL: localhost:8080，自动注入 token）
│   └── auth.ts             # login/logout/refresh API 封装
├── hooks/
│   └── useAuth.ts          # 认证状态管理（zustand）
├── components/
│   ├── providers/
│   │   └── AuthProvider.tsx # 认证上下文，初始化判断 dev/prod
│   └── layout/
│       └── AuthGuard.tsx    # 需认证路由守卫
```

---

## §4 脚手架产出物

### 后端

| 产出 | 说明 |
|------|------|
| `build.gradle` | Spring Boot 3.2.x + MyBatis-Plus 3.5+ + Spring Security + Flyway + JWT + SpringDoc |
| `settings.gradle` | 项目名 `trading-diary` |
| `Application.java` | Spring Boot 入口 |
| `application.yml` | 公共配置 |
| `application-dev.yml` | dev 环境（DB 连接、SQL 日志、JWT secret 默认值） |
| `application-test.yml` | test 环境（H2） |
| 异常体系 | `BaseException`, `BadRequestException`, `UnauthorizedException`, `NotFoundException` |
| `ApiResponse<T>` | 统一响应格式 |
| `GlobalExceptionHandler` | `@ControllerAdvice` |

### 前端

| 产出 | 说明 |
|------|------|
| `package.json` | Next.js 14 + shadcn/ui + ky + zustand + TanStack Query + TailwindCSS |
| `tailwind.config.ts` | shadcn/ui 主题 |
| 基础 shadcn 组件 | button, card, input, dropdown-menu, avatar |

---

## §5 不在此范围

- 用户注册/注册入口
- 修改密码/忘记密码
- 权限管理后台（仅建表 + RBAC 骨架）
- OAuth2/第三方登录
- 前端 E2E 测试（Phase 1）
- Docker 部署（Phase 2）
- API 限流（Phase 1）

---

## §6 宪法合规（Phase 0 检查清单）

- [ ] 包路径在 `com.tradingdiary` 下
- [ ] Controller 未直接注入 Mapper（分层不可逆）
- [ ] Controller 方法未包含业务逻辑
- [ ] API 响应使用 `ApiResponse<T>` 包装
- [ ] 数据库表包含 `id`, `user_id`（sys_user 不需要）, `created_at`, `updated_at`, `is_deleted`
- [ ] 认证方案基于 JWT + Spring Security，密码 BCrypt 存储
- [ ] 异常类继承 `BaseException`，有 `@ControllerAdvice` 全局处理器
- [ ] 命名符合约定

---

## 功能描述（供 speckit-specify 使用）

> 此章节是纯 WHAT 摘要，不含技术实现细节，专门作为 speckit 工具链的输入接口。

搭建项目脚手架和用户认证体系。管理员通过 Flyway 种子数据初始化（用户名 admin，密码从环境变量配置）。系统支持账号密码登录，验证成功后签发短期访问令牌和长期刷新令牌。令牌过期后可刷新，登出后刷新令牌失效。所有密码 BCrypt 加密存储。预置 ADMIN 和 USER 两种角色用于权限控制。开发阶段（dev 模式）启动时自动以管理员身份登录，开发者无需手动输入密码即可访问所有接口。生产模式下自动登录失效，所有接口必须携带有效令牌。
