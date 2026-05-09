# OpenSpec × Superpowers：3 小时跑完一次 SDD 实战

> 作者：AustinXu，2026-04-21
> 原文：https://juejin.cn/post/7630755707556118562
> 专栏：Vibe Coding

这是[第七章：从 Vibe Coding 到 Spec-Driven Development——OpenSpec 实战](https://juejin.cn/post/7613975134063591476)的后续。上一篇讲的是把 OpenSpec 引入一个已有的 Finance 项目；这一篇讲的是在一个全新项目里，从 Day 1 就把 OpenSpec 和 Superpowers 组合起来用。

在 Finance 项目上跑了三个月 [OpenSpec](https://github.com/Fission-AI/OpenSpec) 之后，我对它的长处和短板都心里有数了。与此同时，在一个个人知识库项目里，因为不需要编程，我一直在用 [Superpowers](https://github.com/obra/superpowers)，它的 `brainstorming`、TDD、code-review 这几个 Skill 确实好使。

最近我做了一个新项目，一个基于 UTR 的网球排兵布阵工具 [tennis-lineup](https://github.com/austinxyz/tennis-lineup)，我专门让两个工具跑在一起，看看它们怎么配合，以下是我的使用心得。

---

## OpenSpec 单独用不够在哪

OpenSpec 在**增量变更管理**上表现出色。`propose → apply → archive` 这个循环能把 Spec 库保持的简洁高效，archive 步骤会把 delta spec 沉淀，并且保留历史痕迹便于回顾。对已有代码库逐个 Feature 的迭代开发很好。

但三个月跑下来，四个问题越来越明显：

- **项目冷启动很弱。** OpenSpec 擅长驱动*变更*。一个全新项目——需要先定架构、技术栈、领域建模——`opsx:propose` 没有东西可以锚定。
- **Spec 捕捉的是意图，不是交互。** Spec 告诉 AI *要做什么*，但 UI 细节经常被低估，第一版的UI实现很少能跟我脑子里想的对上。
- **tasks.md 只写了 What，没写 How。** `tasks.md` 是一个 checkbox 列表，没有每个 Task 的实现计划，AI 能自己补空白——但是不稳定，有时候对，有时候跑偏。有的 Task 会被悄悄跳过。
- **没有 Test 纪律。** 我把 "tests first" 写进了 `config.yaml`，但代码质量还是不稳定。手工测试不断发现一些本该被严格 TDD 流程捕捉的 Bug。

---

## Superpowers 正好补上

Superpowers 是一组 Claude Code Skill。对 SDD 工作流最关键的有三个：

- `superpowers:brainstorming` —— 在你写任何 Spec 之前，用结构化问题把你过一遍。还有 **Visual Companion**，能生成可以在浏览器里点击体验的 HTML Mockup。最后输出一份 Design Spec。
- `superpowers:writing-plans` + `executing-plans` —— 把 Spec 拆成 Task，每个 Task 再拆成 red/green/refactor 的 TDD 序列，附带精确的文件路径、命令、期望 Test 输出、Commit Message。
- `superpowers:requesting-code-review` —— 每个 Task 做完自动跑一轮 Review，按 CRITICAL / HIGH / MEDIUM / LOW 分级标出问题，并给出具体修复建议。

OpenSpec 管变更管理和长期 Spec 积累；Superpowers 管前置设计、执行纪律、Review。它们在不同层面工作，不冲突，可以相互配合。

---

## 结合两个工具的 SDLC 是这样

这是我在 tennis-lineup 上最终稳定下来的工作流：

**项目初始化（一次性）：**

- 先跑 `superpowers:brainstorming`，把需求和架构敲定，输出一份 Design Doc 提交到 `docs/`。
- 再 `openspec init`，把 `config.yaml` 填好——技术栈、编码规范、Test 策略（unit + integration + e2e）一次说清楚。
- 按优先级切需求。把 `docs/log/` 当作活日志，在 `CLAUDE.md` 里强制每个 Session 都要 append 一段。

**每个 Feature：**

- 如果这个 Feature 开始我自己也不清楚需求细节，先再跑一次 `superpowers:brainstorming`，UI 重的用 Visual Companion。输出一份 Design Spec。
- 把 Design Spec 喂给 `opsx:propose`，拿到 `proposal.md`、`design.md`、`tasks.md`。
- 用 Superpowers TDD 按这些 Task 跑。它会分成几个批次，先写 Test，再实现，再跑 Test Suite，再按 Batch 跑 Code Review。
- 手工 Sanity Check 一下。基本上没什么bug发现。
- `opsx:apply` 验证所有 Task 都打完成了——通常 Superpowers 已经全部关掉了。
- Deploy。
- `opsx:archive` 把 delta spec 合并回主 Spec 库。
- 扫一遍当天的 Log，新的坑沉淀进 `CLAUDE.md` 和 `openspec/config.yaml`，顺手更新 README。
- Commit & Push，下一个 Feature。

两个工具的分工很清晰：**Superpowers 负责设计和构建，OpenSpec 负责追踪和归档。**

---

## 一个完整的 Feature：Mobile UI 优化

举个 2026-04-19 的真实 Feature 看看怎么跑。

**起手 Prompt：**

```
/superpowers:brainstorming 我想用 brainstorming 的 UI mock 功能来优化 UI，要对 Mobile 友好
1. 侧边栏可以隐藏
2. 右侧如果是两栏结构，可以隐藏一栏，比如队伍管理，选中队伍时候，可以隐藏队伍列表
3. 如果是两列排列的，在 mobile 下可以变成一列，比如已保存排阵，mobile 下每个排阵一列
```

**Step 1 —— Brainstorming + Visual Companion。** Superpowers 生成了一组 HTML Mockup，我在浏览器里直接看到mock，和它交互几轮然后它写出 [Design Spec](https://github.com/austinxyz/tennis-lineup/blob/main/docs/superpowers/specs/2026-04-19-mobile-ui-optimization-design.md)。

**Step 2 —— Propose。** `opsx:propose` 拿着 Design Spec 生成 OpenSpec 的 [proposal](https://github.com/austinxyz/tennis-lineup/blob/main/openspec/changes/archive/2026-04-19-mobile-ui-optimization/proposal.md) 和 [task list](https://github.com/austinxyz/tennis-lineup/blob/main/openspec/changes/archive/2026-04-19-mobile-ui-optimization/tasks.md)——8 组，37 个 Task。

**Step 3 —— Superpowers分 Batch 做 TDD。** 举个有代表性的 Batch：

```
第一批：全局框架（后端无改动）
提交：2e336bb

- 新增 AppHeader.vue：汉堡菜单 + 可选返回按钮 + 标题 + actions slot；lg:hidden
- 重构 MainLayout.vue：provide('sidebarOpen', ref(false))；Escape 关闭抽屉
- 决定合并 SidebarDrawer.vue 逻辑到 MainLayout（无需单独组件）

代码审查修复：
级别   问题                                              修复
HIGH   inject fallback 用普通对象（不是 ref），独立使用时静默失败   改为 inject('sidebarOpen', ref(false))
HIGH   汉堡 aria-label 固定为"打开导航"无视状态                 动态 :aria-label + :aria-expanded
HIGH   返回按钮缺 aria-label + type="button"                  补上，form 内安全
HIGH   移除 MainLayout 的 pt-14 导致内容被固定头部遮挡             恢复 pt-14 lg:pt-0
MEDIUM 抽屉无 Escape 键关闭（WCAG 2.1.2）                      MainLayout 加全局 keydown 监听
LOW    无 overflow-x-hidden 可能闪现横向滚动                     加上

测试：381 tests 全部通过（新增 16 个 AppHeader 测试）
```

这就是 Superpowers 循环的正常样子：写测试 → 实现代码 → 跑测试 → Review → 修问题 → 提交。每个 Batch 结束都是绿的。

**Step 4 —— 一次人为出错。** 在 Opponent Analysis 组件上，UI Mock 只画了 Mobile 布局，我没重新对照已有 Spec 就同意了。Superpowers 就按 Mock 实现，悄悄把已有的 Desktop 功能砍了。

修法：
- 把已有 Spec 加回去，重新跑 `superpowers:brainstorming`，UI 设计重做。
- 按修订后的设计继续 Superpowers TDD。

这条教训更新到了 `openspec/config.yaml`，下一次变更不会再犯。

**Step 5 —— Deploy、Archive、更新文档。** `opsx:archive` 把 delta spec 同步回去。`CLAUDE.md` 补了 E2E dual-render 的坑、SOCKS5 代理配置、Windows localhost dual-stack。`config.yaml` 加了 dual-render `data-testid`、backend 重启规则、TOCTOU、Deploy Smoke Test、"sync before archive" 等条目。

完整过程在 [2026-04-19 的日志](https://github.com/austinxyz/tennis-lineup/blob/main/docs/log/2026-04-19.md) 里。

---

## 数字统计

窗口：从 `5c612fe`（Design Spec）到 `ee4c3bd`（Archive）——**3 小时 7 分钟**，包含 Brainstorming、6 个实现 Batch、一次回滚重做、E2E 修复、Deploy 和 Archive。

代码变化（`git diff 3f75465..ee4c3bd`）：

| 类别 | 文件数 | +/− | 净增 |
|------|--------|-----|------|
| Vue 源码 | 8 | +741 / −297 | +444 |
| Test（unit + E2E） | 11 | +1094 / −16 | +1078 |
| 文档/Spec/Config | 22 | +2547 / −10 | +2537 |
| Brainstorming Mockup | 19 | +4348 / −0 | +4348 |
| **合计** | **60** | **+8730 / −323** | **+8407** |

Test 增量：
- Unit：+77（365 → 442）
- E2E：+9（44 → 53）
- **合计：+86 个 Test Case**

时间线：

| 耗时 | 事件 |
|------|------|
| 09:00–09:23 | Brainstorming + 设计规格 + 实施计划 |
| 09:23–09:39 | 第 1 批：AppHeader + MainLayout（+16 个测试） |
| 09:39–09:54 | 第 2 批：TeamManagerView + TeamDetail（+20 个测试） |
| 09:54–10:00 | 第 3 批：LineupCard（+12 个测试） |
| 10:00–10:15 | 第 4 批：LineupHistoryView（+9 个测试） |
| 10:15–10:36 | 第 5 批：LineupGenerator（+3 个测试） |
| 10:36–10:49 | 第 6 批：OpponentAnalysis 重写（+46）→ **用户驳回** |
| 10:49–10:50 | Revert + mobile 最小适配（批次 12） |
| 11:46 | 第 13 批：OpponentAnalysis 重设计 TDD（+15 个测试）+ E2E 修复 |
| 12:07 | 部署 fly.io + 归档 |

ROI 观察：
- 444 行净增源码，对应 86 个 Test Case（按行数 Test:Code ≈ 2.4:1）。
- 最花时间的是 Batch 6 的过度重构和回滚，教训已经固化到 `config.yaml` 里。
- 前面 20 分钟做 HTML Mockup，换来后面 2 小时零返工的实现。

---

## tasks.md 和 plan.md：各有各的用处

OpenSpec 的 `tasks.md` 和 Superpowers 的 `plan.md` 不是一回事。它们的粒度和使用者都不同。

| 维度 | OpenSpec tasks.md | Superpowers plan.md |
|------|-------------------|---------------------|
| 长度 | ~60 行 | ~1300 行 |
| 粒度 | 8 组 × 37 个 Task，每个一句话 | 11 个 Task × 4–9 Step（write test / run / implement / verify / commit） |
| 代码块 | ❌ | ✅ 完整 Vue 模板、JS、Test Case |
| 文件路径 | 只到组件名 | ✅ 精确路径（`frontend/src/components/AppHeader.vue`） |
| 命令 | `mvn test` / `npm test` | ✅ 精确命令 + 期望输出（`Expected: FAIL — ...`） |
| 测试 | "add/update tests" | ✅ 可执行的 TDD red-green-refactor |
| Commit | 每组一个（约 8 个） | ✅ 每个 Task 一条精确 Commit Message（11+ 个） |
| 自检 | Spec scenario → Task 映射隐式 | ✅ 最后有 Spec 覆盖 Checklist |
| 风险说明 | 在 design.md 里 | ✅ 内联（如 "Task 8.7 依赖 backend"） |
| 使用者假设 | 熟悉 Repo 的开发者 | 零 Context 的工程师照着也能跑 |

**各自的长处：**

`tasks.md` 适合：
- 快速扫描 Scope、做 checkbox 追踪
- 确认 "Feature 是否完成"（`applyRequires`）
- 你或 AI 已经知道实现细节的场景

`plan.md` 适合：
- 把活交给零 Context 的工程师或 Subagent
- 严格 TDD —— 每一步都把 red / green / refactor 讲清楚
- 小粒度 Commit，方便 `git bisect`
- 派给 `superpowers:subagent-driven-development`

**我实际怎么用：** `tasks.md` 是跟 OpenSpec 的 Scope 合同，`plan.md` 是 Superpowers 的执行脚本。它们共享同一份 Spec 作为 Source of Truth，只是服务于不同阶段。

---

## 那 OpenSpec 还有必要吗？

Superpowers 这么好用，是不是 OpenSpec 就多余了？

对我来说，还是必要的，三个理由：

- **小步迭代的纪律。** `propose → apply → archive` 是一个硬节奏，逼着每个变更都有明确 Scope、验收条件、和 Archive 步骤。
- **长期 Spec 库。** `opsx:archive` 会把 delta spec 同步进 `openspec/specs/` 这棵不断生长的树。几个月下来，这就成了项目的权威规格——就像我把 LLM wiki 当作核心笔记本一样。Superpowers 的 Spec 和 Plan 是 per-change 的，没法累积出项目级视图。
- **完成度的交叉校验。** OpenSpec 的 Task 和 Superpowers 的 Plan 可以互相 diff。Superpowers 跑完了但 OpenSpec 还有 Task 没关，说明漏了东西。

一句话：**OpenSpec 管 Spec 生命周期，Superpowers 管一次变更里的设计–执行循环。** 两个组合起来，既有长期结构，也有单次变更的严谨度。

---

## Token 成本

这次变更用了 ~180M tokens，主要是 Opus 4.7 加 Visual Companion（不停生成和迭代 HTML Mockup 挺烧的）。我用的是Claude Code Max Plan，没出现Token被限。按产出算——444 行源码、86 个 Test、从 Design 到 Archive 3 小时、几乎零返工——我觉得可以接受。

---

## 几个关键 Takeaway

1. **任何新项目都从 Brainstorming 开始，不是从 Propose。** OpenSpec 没法从一句话 Bootstrap 架构，Superpowers 的结构化提问可以。

2. **UI 变更一定走 Visual Companion。** 20 分钟可点击 Mockup，省掉几小时错位实现。这是整个工作流里杠杆最高的一个工具。

3. **TDD 交给 Superpowers，Archival 交给 OpenSpec。** 不要指望任何一个把两件事都做好。

4. **每个错误都要进 `config.yaml`。** Batch 6 那次过度重构现在是一条预防规则。这就是 SDD 相对 Vibe Coding 的复利——错误变成结构，而不是只沉在 Git History 里。

5. **坚持写每日 Log。** `docs/log/YYYY-MM-DD.md` 的习惯让回顾成本极低，也是 `CLAUDE.md` / `config.yaml` 更新的素材来源。

---

## References

**项目**
- [tennis-lineup on GitHub](https://github.com/austinxyz/tennis-lineup) —— 全部源码，含 `CLAUDE.md`、OpenSpec config、每日 Log
- [Mobile UI 优化变更（已 Archive）](https://github.com/austinxyz/tennis-lineup/tree/main/openspec/changes/archive/2026-04-19-mobile-ui-optimization)
- [2026-04-19 Session Log](https://github.com/austinxyz/tennis-lineup/blob/main/docs/log/2026-04-19.md)

**工具**
- [OpenSpec](https://github.com/Fission-AI/OpenSpec) —— 轻量 SDD CLI
- [Superpowers](https://github.com/obra/superpowers) —— Claude Code 上的 Brainstorming / TDD / Code Review Skill 集合

**相关**
- [第七章：从 Vibe Coding 到 Spec-Driven Development——OpenSpec 实战](https://juejin.cn/post/7613975134063591476) —— 本文的前作
