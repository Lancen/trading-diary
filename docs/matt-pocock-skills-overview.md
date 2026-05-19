# Matt Pocock 工程技能概览

这套技能覆盖了从需求到交付的完整工程流程，核心思想是**把 AI 当作工程团队成员来约束**，而不是对话 bot。

---

## 需求 → 任务

| 技能 | 用途 | 典型触发 |
|------|------|---------|
| `/to-prd` | 把对话上下文整理成 PRD，发布到 issue tracker | "把这个需求写成 PRD" |
| `/to-issues` | 把 plan/spec 拆成独立可领取的 issue | "把 plan 拆成 issue" |
| `/speckit-specify` | 从自然语言描述生成 feature spec | "我要做一个 XX 功能" |
| `/speckit-plan` | 基于 spec 生成实现计划 | spec 写好后自动进入 |
| `/speckit-tasks` | 基于 plan 生成依赖排序的任务列表 | plan 写好后自动进入 |

## 执行前 — 理解与对齐

| 技能 | 用途 | 典型触发 |
|------|------|---------|
| `/blueprint` | 把一句话目标展开为多 session、多 agent 的施工计划 | "帮我规划这个大型功能" |
| `/grill-with-docs` | 用领域语言挑战你的设计，输出 `CONTEXT.md` + ADR | 领域术语模糊、架构决策需要记录时 |
| `/grill-me` | 反复追问你的设计方案直到双方完全对齐 | "帮我审查这个设计" |

## 执行中 — 质量保障

| 技能 | 用途 | 典型触发 |
|------|------|---------|
| `/triage` | 按状态机处理 issue（待评估 → 等待反馈 → 可执行 → 人工 → 关闭） | "帮我 triage 一下 issue" |
| `/tdd` | 强制红-绿-重构循环 | 写新功能或修 bug 时 |
| `/diagnose` | 系统化调试：复现 → 最小化 → 假设 → 验证 | 遇到难 bug 时 |
| `/improve-codebase-architecture` | 基于 `CONTEXT.md` 的领域语言审查架构 | "审查一下 XX 模块的架构" |
| `/browser-qa` | 浏览器自动化视觉测试 | 前端部署后验证 UI |

---

## 常规用法

### 推荐流程（speckit + superpowers 混合开发）

```
/speckit-specify  →  /speckit-plan  →  /speckit-tasks  →  /to-issues  →  /triage
     需求描述           实现计划           任务分解          创建 issue      分类排期
```

### 日常典型组合

1. **接需求时**：`/to-prd` 或 `/speckit-specify` 把模糊需求结构化
2. **动手前**：`/grill-me` 审查设计是否有盲区，复杂决策走 `/grill-with-docs` 产出 `CONTEXT.md` + ADR
3. **写代码时**：`/tdd` 强制先写测试再写实现
4. **遇到 bug**：`/diagnose` 系统化定位，不要猜
5. **收尾时**：`/triage` 关闭已完成的 issue，`/to-issues` 创建遗留任务

### 关键区别

这些技能不是在"帮 AI 写代码"，而是在**约束 AI 按工程纪律做事**。比如 `/tdd` 不让你跳过红灯直接写实现，`/diagnose` 不让你猜原因直接改代码。

---

## Issue Tracker 切换

当前使用 GitHub Issues。如需切换，可通过以下方式：

| 方式 | 说明 |
|------|------|
| 重新运行 `/setup-matt-pocock-skills` | Section A 选其他 tracker 类型，自动更新配置 |
| 直接编辑文件 | 修改 `docs/agents/issue-tracker.md` 和 `CLAUDE.md` 中对应描述 |
| Other（自定义） | Jira、Linear 等，运行 skill 时选 Other 后描述工作流 |

支持的 tracker 类型：GitHub（`gh` CLI）、GitLab（`glab` CLI）、本地 Markdown（`.scratch/`）、其他（自由格式）。
