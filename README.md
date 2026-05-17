# trading-diary

交易日记应用 —— 记录和管理实盘交易。

**当前阶段：Phase 1（业务开发）**

## 技术栈

| 层 | 选型 |
|------|------|
| 语言 | Java 17+ |
| 框架 | Spring Boot 3.2.x |
| ORM | MyBatis-Plus 3.5+ |
| 数据库 | MySQL 8.0 |
| 构建 | Gradle |
| 前端 | Next.js 14+ / TypeScript / TailwindCSS / shadcn/ui |

完整技术栈见 [CLAUDE.md](CLAUDE.md)。

## 快速开始

```bash
# 环境检查
scripts/check-env.sh

# AKTools（行情数据源，需先 pip3 install aktools）
python3 -m aktools --host 127.0.0.1 --port 8081 &

# 后端
./gradlew bootRun --args='--spring.profiles.active=dev'

# 前端
cd frontend && pnpm install && pnpm dev
```

AKTools 提供 1000+ 行情数据接口，是数据采集模块的依赖。启动后验证：

```bash
curl http://localhost:8081/version
```

## 项目结构

```
trading-diary/
├── src/main/java/com/tradingdiary/   # 后端
├── frontend/                          # 前端
├── specs/                             # 功能规范
│   └── _feature-status.md             # 当前 feature 进度
├── docs/
│   ├── standards/                     # 技术规范
│   └── superpowers/                   # 开发流程
├── scripts/                           # 工具脚本
└── e2e/                               # E2E 测试
```

## 文档

| 文档 | 说明 |
|------|------|
| [CLAUDE.md](CLAUDE.md) | AI 编码准则 |
| [项目宪法](specs/_governance/constitution.md) | 工程决策原则 |
| [技术规范](docs/standards/technical-standards.md) | 代码标准、数据库、API、安全等 |
| [开发流程](docs/superpowers/speckit-superpowers-workflow.md) | Speckit + Superpowers 混合流程 |
| [Feature 状态](specs/_feature-status.md) | 当前 feature 进度 |

## 工程原则

1. **约定优于配置** — 优先使用 Spring Boot 默认约定
2. **分层不可逆** — Controller → Service → Mapper
3. **防御式编程** — 所有外部输入验证
4. **渐进式优化** — 先正确，再优化
