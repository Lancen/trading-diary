# Step 6: 环境准备

## 为什么必须

AI agent 在沙箱中通常无法执行包管理器、系统级安装命令。这些必须在派发 agent 之前由人（或主 session）完成。

| 检查项 | 验证命令 | agent 能做吗 |
|--------|---------|-------------|
| 语言运行时版本 | `java -version`、`node -v` | ❌ 不能安装 |
| 包管理器 | `gradle --version`、`pnpm --version` | ❌ 不能安装 |
| 首次编译通过 | `./gradlew compileJava` | ⚠️ 需要 JDK 路径 |
| 前端依赖安装 | `cd frontend && pnpm install` | ❌ 沙箱限制 |
| 敏感配置 | `.env` 文件就位 | ✅ 但不会自动创建 |

> **核心原则：编译通过是派发 agent 的前置条件。不要在编译不过的情况下继续加代码——问题会复合。**

额外建议：
- 统一配置方式（所有环境通过 `.env` + `application.yml` 默认值管理）
- 一个能跑通的最小启动流程，确保 agent 生成的代码不会因环境问题批量失败
