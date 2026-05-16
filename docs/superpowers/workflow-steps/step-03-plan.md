# Step 3: 计划（speckit-plan）

**执行**: `/speckit-plan`

## 重要

speckit-plan 不会自动读取 design doc。执行时 AI MUST 主动读取 `docs/superpowers/specs/` 下对应的 design doc，将 "功能描述" 标题之前所有章节的技术决策作为技术上下文填充。不做这一步 = 重复讨论已决策的方案。

## 产出

- `plan.md`
- `research.md`
- `data-model.md`
- `contracts/`
- `quickstart.md`
