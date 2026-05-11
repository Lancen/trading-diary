# Data Model: 项目脚手架 + 用户认证体系

**Feature**: 002-project-scaffold-user-auth | **Date**: 2026-05-11

## Entity Relationship Diagram

```
sys_user ──< sys_user_role >── sys_role
                                  │
                                  ▼ (Phase 1+)
                            sys_role_permission
                                  │
                                  ▼
                            sys_permission
```

## Tables

### sys_user — 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 登录用户名 |
| password | VARCHAR(255) | NOT NULL | BCrypt 密文 |
| nickname | VARCHAR(50) | NULL | 显示名称 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 1=正常, 0=禁用 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| is_deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 软删除标记 |

**索引**: `uk_username` (UNIQUE), `idx_status`

**业务场景**: 系统使用者账号。当前仅管理员，后续扩展支持普通用户注册。
**生命周期**: 创建 → 正常使用 → 禁用/软删除。物理删除需 30 天缓冲期。

### sys_role — 角色表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| code | VARCHAR(30) | UNIQUE, NOT NULL | 角色编码（ADMIN, USER） |
| name | VARCHAR(50) | NOT NULL | 角色名称 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| is_deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 软删除标记 |

**索引**: `uk_code` (UNIQUE)

**种子数据**: `{code: "ADMIN", name: "管理员"}`, `{code: "USER", name: "普通用户"}`

### sys_user_role — 用户-角色关联表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL, FK → sys_user.id | 用户 ID |
| role_id | BIGINT | NOT NULL, FK → sys_role.id | 角色 ID |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**: `uk_user_role` (UNIQUE: user_id, role_id), `idx_user_id`, `idx_role_id`

### sys_permission — 权限表（Phase 1 预留）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| code | VARCHAR(50) | UNIQUE, NOT NULL | 权限编码（如 `trade:read`） |
| name | VARCHAR(50) | NOT NULL | 权限名称 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**说明**: 本阶段仅建表，不填充数据。为 Phase 1 的 `@PreAuthorize` 权限控制预留。

### sys_role_permission — 角色-权限关联表（Phase 1 预留）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| role_id | BIGINT | NOT NULL, FK → sys_role.id | 角色 ID |
| permission_id | BIGINT | NOT NULL, FK → sys_permission.id | 权限 ID |

**说明**: 本阶段仅建表，不填充数据。

## Seed Data (V2__seed_admin.sql)

```sql
-- 幂等插入角色
INSERT INTO sys_role (code, name, created_at, updated_at)
SELECT 'ADMIN', '管理员', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'ADMIN');

INSERT INTO sys_role (code, name, created_at, updated_at)
SELECT 'USER', '普通用户', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'USER');

-- 幂等插入管理员用户（密码为 BCrypt 密文，需在应用层生成后写入）
INSERT INTO sys_user (username, password, nickname, status, created_at, updated_at)
SELECT 'admin', '${BCRYPT_HASH}', '管理员', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

-- 关联 admin → ADMIN 角色
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
```

> ⚠️ BCrypt 密文由 Java 端 `BCryptPasswordEncoder` 在迁移时生成。Flyway 不支持 Java 调用，改用应用启动时 `ApplicationRunner` 检查并初始化密码，或迁移脚本中存储 BCrypt("admin123") 的固定密文（dev 可行，prod 需通过环境变量传递密文）。

**修正方案**: 种子数据分两阶段：
1. Flyway V2 插入 admin 用户（密码为临时占位符）
2. 应用启动时 `AdminInitializer`（实现 `ApplicationRunner`）检测 admin 密码是否为占位符 → 读取 `ADMIN_INIT_PASSWORD` → BCrypt 加密 → 更新密码
