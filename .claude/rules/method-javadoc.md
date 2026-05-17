# 后端方法须有接口文档说明

Controller 和 Service 的 public 方法在新增/修改时，必须添加方法说明。

## Controller

- 每个 API 端点方法必须标注 `@Operation(summary = "xxx")`（SpringDoc），一句话说明接口用途
- 修改方法行为时同步更新 `summary`

## Service

- 每个 public 方法必须有 Javadoc（一句话即可），说明方法的业务含义
- 修改方法行为时同步更新 Javadoc

**Why:** Controller 是 Swagger API 文档的数据源，Service 是业务逻辑入口。方法说明帮助后续维护者和 AI 快速理解方法职责，减少误用。

**How to apply:**
- 新增 public 方法 → 先写 `@Operation` / Javadoc 再写实现
- 修改方法 → 检查现有说明是否仍准确，不准确则更新
- code-reviewer agent 审查时会检查此项
