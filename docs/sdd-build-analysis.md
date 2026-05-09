# trading-diary SDD 构建步骤

> 原文：https://juejin.cn/post/7630755707556118562
>
> OpenSpec 管 Spec 生命周期（propose → apply → archive），Superpowers 管单次变更的 设计→TDD→Review 循环。

---

## 一、项目初始化（一次性）

### 1.1 需求与架构敲定

```
/superpowers:brainstorming
```

聚焦：用户画像、MVP 功能边界、技术栈、领域模型、关键页面 UI Mockup

输出 Design Doc 到 `docs/design/`。

> 没有 Superpowers 时，直接用 Claude Code 对话完成，分步讨论：先产品定位 → 再技术选型 → 再 UI 草图。

### 1.2 初始化 OpenSpec

```bash
openspec init
```

`openspec/config.yaml` 核心内容：

```yaml
project: trading-diary
coding_standards:
  - UI 组件必须有 unit test
  - API 必须有 integration test
  - 关键路径必须有 E2E test
test_strategy:
  unit: vitest
  integration: supertest
  e2e: playwright
```

### 1.3 建立文档骨架

```
docs/
├── design/    # Brainstorming 输出的 Design Spec
├── log/       # 每日开发日志 YYYY-MM-DD.md
└── sdd-build-analysis.md
```

`CLAUDE.md` 追加规则：

- 每个 Session 结束后在 `docs/log/` 追加日志
- 每次代码变更前先写测试

### 1.4 MVP 功能切分

按优先级排序，建议 MVP 范围：

1. 交易记录 CRUD
2. 标签/分类系统
3. 盈亏统计面板
4. 日/周/月视图

---

## 二、v2+ 版本迭代

v2 和冷启动的关键区别是：**OpenSpec 现在有东西可以锚定了**。

### 2.1 先盘点现有资产

```bash
# v1 积累的长期 Spec 库
ls openspec/specs/

# v1 期间踩过的坑（已固化为规则）
cat openspec/config.yaml

# 最近的开发日志
ls docs/log/
```

### 2.2 按需求清晰度选入口

| 情况 | 入口 | 说明 |
|------|------|------|
| v2 需求清晰，是 v1 的自然延伸 | `opsx:propose` | 有现有 Spec 做锚点，直接生成 proposal |
| v2 是新大功能，需求还不明确 | `/superpowers:brainstorming` | 结构化提问把需求敲实 |
| v2 是 UI 重做或新增大量页面 | brainstorming + Visual Companion | 生成 Mockup，对齐视觉预期 |

> 原则：不太清楚要做什么 → brainstorming；很清楚要做什么 → propose。v2 有历史资产做锚点，propose 的起点高很多。

### 2.3 Feature 循环流程不变

流程和 v1 完全一样，区别只在 `opsx:archive` 时 delta spec 是**合并进已有 Spec 树**，而非从零构建。

### 2.4 v1→v2 的本质变化

- **OpenSpec 从弱项变强项**：冷启动帮不上忙，增量变更正是它的主场
- **config.yaml 的复利开始体现**：v1 每个坑已固化成规则，v2 不会再犯
- **Brainstorming 从必跑变可选**：需求说清楚了就直接 propose

---

## 三、每个 Feature 的标准循环

```
1. [可选] /superpowers:brainstorming          → Design Spec（UI 重时必跑，用 Visual Companion）
2. opsx:propose                               → proposal.md + design.md + tasks.md
3. Superpowers TDD 分批执行                   → 写测试 → 实现 → 测试套件 → Review → 提交
4. 手工 Sanity Check
5. opsx:apply                                 → 验证所有 Task 关掉了
6. Deploy
7. opsx:archive                               → delta spec 合并回 openspec/specs/
8. 更新 CLAUDE.md + config.yaml + README
9. Commit & Push
```

---

## 四、必须遵守的纪律

1. **不清楚就 brainstorming，清楚就 propose** — v1 冷启动必须 brainstorming；v2+ 视需求清晰度选入口
2. **UI 变更走 Visual Companion** — 20 分钟 Mockup 省几小时返工
3. **每个错误写进 config.yaml** — 错误变规则，下次不再犯
4. **每日写 Log** — `docs/log/YYYY-MM-DD.md`，低成本回顾
5. **TDD 交给 Superpowers，归档交给 OpenSpec** — 各司其职

---

## 五、参考

- OpenSpec: https://github.com/Fission-AI/OpenSpec
- Superpowers: https://github.com/obra/superpowers
- tennis-lineup 项目: https://github.com/austinxyz/tennis-lineup
