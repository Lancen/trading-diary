# Speckit + Superpowers 混合使用流程

**版本**: 2.4.0 | **日期**: 2026-05-17

适用于任何软件项目，从模糊想法到可交付代码的完整开发链路。

## 定位

| 工具 | 角色 | 输入 | 输出 |
|------|------|------|------|
| **Superpowers** | 前期探索 + 后期执行 | 模糊想法 | design doc → 可运行代码 |
| **Speckit** | 中期结构化 | 明确的需求描述 | spec.md → plan.md → tasks.md |

## 完整流程

```
模糊想法
    │
    ▼
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│1. 探索    │ → │2. 规范    │ → │3. 计划    │ → │4. 拆解    │ → │5. 检查    │ → │6. 准备    │
│brainstorm│   │specify   │   │plan      │   │tasks     │   │analyze   │   │环境      │
└──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘
     │              │              │              │              │              │
     ▼              ▼              ▼              ▼              ▼              ▼
 design doc     spec.md        plan.md        tasks.md       分析报告      编译通过
 (WHAT+HOW)    (纯 WHAT)     research.md                   修复提交      pnpm install
                             data-model.md
                             contracts/
                             quickstart.md
                                          │
                                          ▼
┌──────────────────────────────────────────────────────────────────┐
│ 7. 执行 ── 循环审查门禁                                           │
│                                                                  │
│  tasks.md 按 Phase 拆批 → 每批: TDD → 双审查                      │
│                                                                  │
│  全部 Phase 通过 → verify（AC 验收）→ 收尾审查                     │
└──────────────────────────────────────────────────────────────────┘
                                          │
                                          ▼
┌──────────┐
│8. 归档    │
│archive   │
└──────────┘
     │
     ▼
 可交付代码 + 归档 spec
```

## 步骤速查

| 步骤 | 执行命令 | 前置产物 | 产出物 | 详细说明 |
|------|---------|---------|--------|---------|
| 1. 探索 | `/superpowers:brainstorming` | 无 | design doc（两段式） | [step-01](workflow-steps/step-01-brainstorming.md) |
| 2. 规范 | `/speckit-specify <纯WHAT文本>` | design doc | spec.md + checklists/ | [step-02](workflow-steps/step-02-specify.md) |
| 3. 计划 | `/speckit-plan` | spec.md | plan.md + data-model.md + contracts/ | [step-03](workflow-steps/step-03-plan.md) |
| 4. 拆解 | `/speckit-tasks` | spec.md + plan.md | tasks.md | [step-04](workflow-steps/step-04-tasks.md) |
| 5. 检查 | `/speckit-analyze` | spec.md + plan.md + tasks.md | 分析报告 + 修复提交 | [step-05](workflow-steps/step-05-analyze.md) |
| 6. 准备 | `scripts/check-env.sh` | tasks.md | 编译通过、依赖安装 | [step-06](workflow-steps/step-06-environment.md) |
| 7. 执行 | `/superpowers:subagent-driven-development` | tasks.md + 编译通过 | 可交付代码 | [step-07](workflow-steps/step-07-execution.md) |
| 8. 归档 | `/superpowers:finishing-a-development-branch` | 步骤 7 全部通过 | 归档 spec + 合并 PR | [step-08](workflow-steps/step-08-archive.md) |

**AI 规则**: 收到开发指令后，匹配上表确定当前步骤，**只加载对应步骤的详细文件**。不确定当前步骤时加载精简版（本文档）即可。
步骤完成后，提示当前产物并主动列出下一个可用命令。流程结束时更新 `specs/_feature-status.md`。

## 接口契约

| 上游 | 下游 | 传递方式 | 关键约束 |
|------|------|---------|---------|
| brainstorming | speckit-specify | 复制 "功能描述" 文本 | 纯 WHAT，不传文件路径 |
| brainstorming | speckit-plan | AI 主动读取 "功能描述" 之前的章节 | speckit 不会自动读 design doc |
| speckit-analyze | 修复 | 分析报告 → 修复 spec/plan/tasks | 修复后才能执行 |

## 工具链选择

| 用这个 | 不用那个 | 理由 |
|--------|---------|------|
| brainstorming | speckit-clarify | brainstorming 已充分澄清，clarify 冗余 |
| speckit-plan | writing-plans | speckit-plan 与 spec/tasks 联动，artifact 一致性更好 |
| speckit-analyze | speckit-checklist | analyze 跨 artifact 检查，checklist 仅在单个 spec 内 |
| subagent-driven-development | executing-plans | 同一 session，快速迭代；任务量大时选 executing-plans |

## 反模式

| 反模式 | 后果 | 预防 |
|--------|------|------|
| 跳过 brainstorming 直接 speckit-specify | 需求未探索，spec 偏离意图 | 永远从 brainstorming 开始 |
| speckit-specify 传入 design doc 文件路径 | 技术细节渗入 spec | 只复制 "功能描述" 段落文本 |
| speckit-plan 不读 design doc | 重复讨论已决策方案 | plan 前主动读取 design doc |
| 跳过 speckit-analyze | data-model 缺口带入实施 | analyze → 修复 → 再执行 |
| 跳过环境准备 | agent 批量失败 | 编译通过是派发 agent 的前置条件 |
| 编译不过继续加代码 | 错误复合 | 每批提交后立即编译 |
| 僵硬按 tasks.md 阶段执行 | 相邻阶段重复实现 | 执行时识别自然合并 |
| agent 文件创建了没编译 | 代码质量无保证 | "没编译 = BLOCKED，不是 DONE" |
| 跳过 spec 审查直接做代码审查 | spec 偏离未发现 | 先 spec 后质量 |
| implementer 自审查替代正式审查 | 自查不出所有问题 | 审查必须由独立 subagent 执行 |
| design doc 多层策略合成一个模糊任务 | agent 遗漏端到端入口 | analyze 阶段对照 design doc 逐条检查 |
