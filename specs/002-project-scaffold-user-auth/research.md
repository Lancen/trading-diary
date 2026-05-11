# Research: 项目脚手架 + 用户认证体系

**Feature**: 002-project-scaffold-user-auth | **Date**: 2026-05-11

## 技术选型与决策

### 1. 认证方案：JWT + Spring Security

**Decision**: 使用 Spring Security Filter Chain + 无状态 JWT（jjwt 库）

**Rationale**:
- 宪法 Phase 0 明确要求 JWT + Spring Security
- 无状态认证适合 SaaS 水平扩展
- jjwt 是 Spring 生态主流 JWT 库，文档完善

**Alternatives considered**:
- Session-based 认证：有状态，需要 sticky session，不适合 SaaS
- OAuth2 完整实现：过度设计，当前无第三方登录需求

### 2. 开发环境自动登录：Profile 条件 Filter

**Decision**: `@Profile("dev")` + `OncePerRequestFilter` 在 JwtAuthFilter 之前执行

**Rationale**:
- 利用 Spring 的 profile 机制，dev 不存在时此 filter 不参与 SecurityFilterChain
- 不需要代码修改或 if-else，由运行时配置决定
- 在 dev profile 下完全透明，前端无需感知

**Alternatives considered**:
- 前端 dev 模式判断 + 自动调用 login 接口：多一次网络调用，且后端仍需处理 auth
- 启动时打印 token 到控制台：需要手动复制，降低效率

### 3. 管理员初始化：Flyway 种子数据 + 环境变量

**Decision**: Flyway 迁移脚本插入 admin 用户，密码从 `ADMIN_INIT_PASSWORD` 环境变量读取

**Rationale**:
- 与项目现有的 Flyway 数据库迁移体系一致
- 密码不在代码或配置文件中，安全可控
- V2 迁移脚本幂等：`INSERT ... WHERE NOT EXISTS`

**Alternatives considered**:
- `application-dev.yml` 直接配置密码：密码会进 git，不安全
- 独立 JSON 文件：额外维护一种配置格式
- CommandLineRunner 代码初始化：逻辑混在启动流程中，不够明确

### 4. 令牌模型：Access + Refresh Token 轮换

**Decision**: Access Token 15min + Refresh Token 7d，刷新时同时轮换两个 token

**Rationale**:
- 宪法 §5 要求 Access Token 15min，Refresh Token 7d
- Token 轮换防止 refresh token 被盗用后长期有效
- 登出时标记 refresh token 为失效（DB 记录或内存黑名单）

**Alternatives considered**:
- 单 token 长有效期：一旦泄漏无法撤销
- 不轮换 refresh token：登出后旧 token 仍可用

### 5. 密码存储：BCrypt

**Decision**: BCrypt，cost = 12

**Rationale**:
- 宪法 §5 和 Phase 0 明文要求
- cost 12 在安全性和性能间取得平衡

### 6. 前端认证：ky + zustand + TanStack Query

**Decision**: ky 实例自动注入 Authorization 头，zustand 管理 token 状态，TanStack Query 管理刷新逻辑

**Rationale**:
- 与 CLAUDE.md 技术栈一致
- ky 拦截器可在收到 401 时自动尝试刷新 token
- zustand 轻量，适合 token 状态管理

## 无待澄清项

所有技术选择已在 brainstorming 阶段确定，无需 NEEDS CLARIFICATION。
