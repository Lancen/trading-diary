# mattpocock/skills 使用指南

> 来源：[mattpocock/skills](https://github.com/mattpocock/skills) | Stars: 80k+ | License: MIT
> 作者：Matt Pocock（TypeScript 教育家、Total TypeScript 创始人、前 Vercel/Stately 工程师）

## 概述

mattpocock/skills 是 Matt Pocock 公开的个人 `.claude/skills` 目录，包含 26 个工程级 Agent Skill。核心理念是 **"Anti-Vibe Coding"**——让 AI Agent 按照严谨的工程方法论工作，而非随意生成代码。

### 解决的四大问题

| 问题 | 解决方案 |
|------|---------|
| Agent 理解偏差 | `grill-me` / `grill-with-docs` 在写代码前穷举所有决策分支 |
| Agent 啰嗦冗长 | 通过 `CONTEXT.md` 建立共享领域语言，用 1 个词代替 20 个词 |
| 代码跑不通 | `tdd` 强制红-绿-重构循环，`diagnose` 提供结构化调试 |
| 代码腐化成泥球 | `improve-codebase-architecture` / `zoom-out` 维持设计质量 |

---

## 快速安装

```bash
# 安装全部 skills
npx skills@latest add mattpocock/skills --all

# 或按需安装单个 skill
npx skills@latest add mattpocock/skills/<skill-name>

# 首次安装后运行初始化
/setup-matt-pocock-skills
```

---

## 完整 Skill 列表

### 一、规划与设计（Planning & Design）— 9 个

| Skill | 功能 | 适用场景 |
|-------|------|---------|
| **grill-me** | 极限拷问模式 — 逐条追问直到所有决策分支被穷举 | 需求模糊时，启动任何新功能前 |
| **grill-with-docs** | 在 grill-me 基础上同步更新 `CONTEXT.md` 并实时写入 ADR | 需要沉淀领域语言和架构决策时 |
| **to-prd** | 将对话内容合成为结构化 PRD，提交为 GitHub Issue | 需求讨论完毕，需要正式文档化 |
| **to-issues** | 将 PRD/Spec 拆解为独立、可垂直切片的 GitHub Issues | 大功能拆分为可并行的开发任务 |
| **design-an-interface** | 启动并行 sub-agent 生成多个截然不同的 UI 方案并评估 | 需要多方案对比的 UI/API 设计 |
| **request-refactor-plan** | 模拟访谈 → 细粒度提交计划 → 生成 GitHub Issue | 大规模重构前的规划 |
| **domain-model** | 围绕 DDD 构建领域模型 | 复杂业务领域的建模 |
| **ubiquitous-language** | 从对话中提取通用语言词汇表，标注歧义并建议规范术语 | 团队对齐领域术语 |
| **zoom-out** | 上升一个抽象层级，输出项目架构全景图 | 不熟悉某个模块时快速建立认知 |

### 二、开发与编码（Development）— 8 个

| Skill | 功能 | 适用场景 |
|-------|------|---------|
| **tdd** | 强制红→绿→重构循环，每次只做一个垂直切片 | 新功能开发、Bug 修复 |
| **diagnose** | 6 阶段结构化调试：复现→假设→插桩→修复→回归→复盘 | 复杂 Bug、性能问题 |
| **triage-issue** | 探索代码库→根因分析→TDD 修复计划→GitHub Issue | Issue 驱动的 Bug 修复 |
| **improve-codebase-architecture** | 从 `CONTEXT.md` + ADR 中寻找深层模块改进机会 | 技术债务治理 |
| **caveman** | 超压缩通信模式，剥离所有礼貌用语，节省 60-70% token | 长对话 token 预算紧张时 |
| **prototype** | 在独立分支上构建 disposable 原型，验证设计可行性 | 技术方案验证 |
| **migrate-to-shoehorn** | 将断言迁移至 `@total-typescript/shoehorn` | TypeScript 项目测试迁移 |
| **scaffold-exercises** | 按规范生成练习目录结构 | TypeScript 教学内容生成 |

### 三、工程环境（Tooling & Safety）— 5 个

| Skill | 功能 | 适用场景 |
|-------|------|---------|
| **git-guardrails-claude-code** | 在 hook 层拦截危险 Git 命令（`push --force`、`reset --hard`、`clean -fd`、`branch -D`） | 防止 Agent 误操作破坏仓库 |
| **setup-pre-commit** | 一键配置 Husky + lint-staged + Prettier + typecheck + tests | 新项目初始化 |
| **setup-matt-pocock-skills** | 配置 Issue Tracker、Labels、文档目录 | 安装 skills 后的首次初始化 |
| **triage** (github-triage) | Issue 状态机：`unlabeled → needs-triage → needs-info / ready-for-agent / ready-for-human / wontfix` | Issue 管理 |
| **qa** | 项目质量审计 | 发布前的全面检查 |

### 四、写作与知识（Writing & Knowledge）— 4 个

| Skill | 功能 | 适用场景 |
|-------|------|---------|
| **handoff** | 生成结构化上下文交接文档，让另一个 Agent 或人类无缝接手 | 跨会话长任务、多人协作 |
| **write-a-skill** | 元技能 — 按渐进式披露结构创建新的 Skill | 扩展自己的 Skill 体系 |
| **edit-article** | 结构化编辑：重组章节、提高清晰度、压缩文字 | 技术文章优化 |
| **obsidian-vault** | 管理 Obsidian 知识库，支持 wikilinks 和索引笔记 | 个人知识管理 |

---

## 推荐使用组合

### 新手入门（5 个核心 Skill）

```bash
npx skills@latest add mattpocock/skills/grill-me
npx skills@latest add mattpocock/skills/tdd
npx skills@latest add mattpocock/skills/diagnose
npx skills@latest add mattpocock/skills/caveman
npx skills@latest add mattpocock/skills/handoff
```

### 完整工程流（12 个）

在上述 5 个基础上增加：

```bash
npx skills@latest add mattpocock/skills/grill-with-docs
npx skills@latest add mattpocock/skills/to-prd
npx skills@latest add mattpocock/skills/to-issues
npx skills@latest add mattpocock/skills/zoom-out
npx skills@latest add mattpocock/skills/git-guardrails-claude-code
npx skills@latest add mattpocock/skills/write-a-skill
npx skills@latest add mattpocock/skills/triage
```

### TypeScript 专项

```bash
npx skills@latest add mattpocock/skills/migrate-to-shoehorn
npx skills@latest add mattpocock/skills/scaffold-exercises
```

---

## 典型工作流

```
需求模糊的一句话
    ↓
/grill-me 或 /grill-with-docs    ← 穷举所有决策分支
    ↓
/to-prd                            ← 输出结构化 PRD
    ↓
/to-issues                         ← 拆解为独立 Issue
    ↓
/tdd                               ← 逐个 Issue 红→绿→重构
    ↓ (遇到 Bug)
/diagnose                          ← 6 阶段结构化调试
    ↓ (完成)
/handoff                           ← 生成上下文交接文档
```

---

## 与现有 Skill 体系的关系

本项目的 `superpowers:*` 系列 Skill 与 mattpocock/skills 有部分功能重叠，可互补使用：

| 场景 | mattpocock/skills | 本项目已有 |
|------|------------------|-----------|
| 需求澄清 | `grill-me` | `superpowers:brainstorming` |
| TDD | `tdd` | `superpowers:test-driven-development` |
| 调试 | `diagnose` | `superpowers:systematic-debugging` |
| 代码审查 | — | `superpowers:requesting-code-review` |
| 计划执行 | — | `superpowers:executing-plans` |
| 计划编写 | — | `superpowers:writing-plans` |
| 并行 Agent | — | `superpowers:dispatching-parallel-agents` |
| 上下文压缩 | `caveman` / `handoff` | — |
| Git 安全 | `git-guardrails-claude-code` | — |
| Issue 管理 | `triage` / `to-issues` | 本项目 `docs/agents/issue-tracker.md` |

**建议**：`grill-me`、`caveman`、`handoff` 这三个是本项目 Skill 体系中没有直接对应的，优先考虑引入。

---

## 注意事项

1. **Skill 优先级**：用户指令 > CLAUDE.md > Skill 指令。Skill 无法覆盖用户在配置文件中明确写定的规则
2. **独立安装**：每个 Skill 是独立文件，按需安装，避免一次性安装全部导致上下文膨胀
3. **TypeScript 倾向**：Matt Pocock 是 TypeScript 专家，部分 Skill（`migrate-to-shoehorn`、`scaffold-exercises`）是 TS 专用的
4. **兼容性**：Skill 是跨平台标准格式，可在 Claude Code、Copilot CLI、Gemini CLI 等支持 Skill 的 Agent 中使用
