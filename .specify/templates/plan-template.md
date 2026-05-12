# 实施计划：[FEATURE]

**分支**: `[###-feature-name]` | **日期**: [DATE] | **规范**: [link]
**输入**: 功能规范来自 `/specs/[###-feature-name]/spec.md`

**说明**: 此模板由 `/speckit-plan` 命令填充。执行流程见 `.specify/templates/plan-template.md`。

## 概述

[Extract from feature spec: primary requirement + technical approach from research]

## 技术上下文

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]  
**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]  
**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]  
**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]  
**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]
**Project Type**: [e.g., library/cli/web-service/mobile-app/compiler/desktop-app or NEEDS CLARIFICATION]  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## 宪法检查

*门禁: 必须在阶段 0 研究前通过。阶段 1 设计后重新检查。*

[Gates determined based on constitution file]

## 项目结构

### 文档（本功能）

```text
specs/[###-feature]/
├── plan.md              # 本文件 (/speckit-plan 命令输出)
├── research.md          # 阶段 0 输出 (/speckit-plan 命令)
├── data-model.md        # 阶段 1 输出 (/speckit-plan 命令)
├── quickstart.md        # 阶段 1 输出 (/speckit-plan 命令)
├── contracts/           # 阶段 1 输出 (/speckit-plan 命令)
└── tasks.md             # 阶段 2 输出 (/speckit-tasks 命令 - 不由 /speckit-plan 创建)
```

### 源代码（仓库根目录）
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**结构决策**: [Document the selected structure and reference the real
directories captured above]

## 复杂度追踪

> **仅当宪法检查有必须解释的违规时才填写**

| 违规 | 为什么需要 | 为什么更简单方案行不通 |
|------|-----------|---------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
