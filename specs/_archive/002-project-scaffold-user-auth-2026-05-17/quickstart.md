# Quickstart: 项目脚手架 + 用户认证体系

**Feature**: 002-project-scaffold-user-auth

## 前置条件

- Java 17+
- Node.js 18+
- MySQL 8.0+ (dev: H2 内存数据库可用)
- pnpm

## 快速开始

### 1. 克隆并进入分支

```bash
git checkout 002-project-scaffold-user-auth
```

### 2. 后端启动

```bash
# 初始化 Gradle wrapper（首次）
gradle wrapper

# 启动 dev 环境（H2 内存数据库，SQL 日志开启）
./gradlew bootRun --args='--spring.profiles.active=dev'
```

启动后访问 http://localhost:8080

- Flyway 自动执行 V1（建表）和 V2（种子数据）
- `AdminInitializer` 检测 admin 密码并根据 `ADMIN_INIT_PASSWORD` 加密存储
- dev 模式下 `AutoLoginFilter` 激活，所有接口自动以 admin 身份访问

### 3. 前端启动

```bash
cd frontend
pnpm install
pnpm dev
```

访问 http://localhost:3000

- dev 模式下（`NEXT_PUBLIC_DEV_AUTO_LOGIN=true`）自动以 admin 登录
- 直接进入 dashboard

### 4. 验证

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# Dev 自动登录验证（无需 token）
curl http://localhost:8080/api/v1/auth/me

# 登录接口验证
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 后端测试
./gradlew test

# 前端测试
cd frontend && pnpm test
```

### 5. 关键配置

**环境变量**:

| 变量 | 默认值 (dev) | 说明 |
|------|-------------|------|
| `ADMIN_INIT_PASSWORD` | `admin123` | 管理员初始密码，仅首次启动时用于生成 BCrypt 密文 |
| `JWT_SECRET` | (dev 内置值) | JWT 签名密钥，prod 必须从外部注入 |
| `DB_URL` | `jdbc:h2:mem:testdb` | 数据库连接 URL |
| `DB_USERNAME` | `sa` | 数据库用户名 |
| `DB_PASSWORD` | (空) | 数据库密码 |
