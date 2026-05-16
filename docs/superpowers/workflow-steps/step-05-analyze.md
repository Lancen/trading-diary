# Step 5: 检查（speckit-analyze）

**执行**: `/speckit-analyze`

## 为什么必须在实施前做

tasks.md 基于 spec.md 生成，但 spec 不包含所有技术细节。analyze 在 spec/plan/tasks 之间做交叉检查，发现的缺口必须在实施前修复，否则边写边补会导致返工。

## analyze MUST 额外检查 design doc

逐条对照 design doc "功能描述"之前章节的技术方案，确认每个技术决策在 tasks.md 中有对应条目。设计有两层策略（日常增量 + 全量回填）但 tasks.md 只有一个模糊任务的，必须拆分。

## 常见发现

| 类型 | 示例 | 修复方式 |
|------|------|---------|
| data-model 缺表/缺字段 | 刷新令牌失效需要 `sys_refresh_token` 表未建模 | 补充到 data-model.md、plan.md、tasks.md |
| tasks 遗漏关键步骤 | token 刷新/登出的实现任务被拆分到另一阶段 | 合并或补充任务 |
| 成功标准无验证手段 | 性能指标无对应测试任务 | 标注验证阶段 |
| 术语不一致 | spec 用"凭证"，plan 用"token" | 确认是可接受的还是需要统一 |
