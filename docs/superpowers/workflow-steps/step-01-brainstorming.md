# Step 1: 探索（brainstorming）

**执行**: `/superpowers:brainstorming`

## 做什么

- 读取项目现有代码、文档、规范
- 评估需求规模，过大的拆分为多个子项目
- 逐个提问澄清关键设计决策（一次一个问题，多项选择优先）
- 提出 2-3 种方案及权衡，给出推荐
- 逐节展示设计并获取用户批准

## 产出 — design doc 两段式结构

由固定标题分隔：

```markdown
## 数据库设计           ← 技术方案章节（WHAT + HOW）
## 后端设计             ← 数量任意，顺序任意
## 前端设计             ← speckit-plan 读取这些
## ...

## 功能描述（供 speckit-specify 使用）   ← 固定标题，不可改名
> 纯 WHAT 摘要，无任何技术术语          ← speckit-specify 读取这个
```

| 区域 | 消费者 | 约束 |
|------|--------|------|
| `## 功能描述` 之前的章节 | speckit-plan | WHAT + HOW 混合，可写任何技术细节 |
| `## 功能描述` 及之后 | speckit-specify | 纯 WHAT，自然语言，禁止框架名/库名/代码结构 |

## 关键规则

- "功能描述" 标题是接口契约，改名会破坏下游工具链
- 功能描述只描述用户能做什么、系统提供什么
- brainstorming 结束必须 commit design doc
