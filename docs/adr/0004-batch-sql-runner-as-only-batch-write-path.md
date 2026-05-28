# ADR-0004: BatchSqlRunner 是唯一批量写入路径

## 状态

已接受

## 日期

2026-05-28

## 语境

项目需要批量写入数据到 MySQL，有两条路径可选：
- **MyBatis-Plus IService.saveBatch()**：框架原生批量操作，自动分批，依赖 MP 元数据解析
- **BatchSqlRunner**：基于 JdbcTemplate 自定义拼接 `INSERT INTO ... VALUES (...), (...), ...` 和 `CASE-WHEN` 批量更新

## 决策

**所有业务数据的批量写入必须使用 `BatchSqlRunner`，禁止混用 MyBatis BATCH 模式。**

`BatchSqlRunner` 的 `batchInsert()` 和 `batchUpdate()` 是项目的标准批量写入接口：
- `batchInsert(List<T>, int batchSize)`：多行 VALUES 语法，单次 SQL 写入一个批次
- `batchUpdate(List<T>, int batchSize)`：CASE-WHEN 语法，单次 SQL 更新一个批次

## 理由

### BatchSqlRunner 的定制能力无法被 MyBatis-Plus 替代

1. **CASE-WHEN 批量更新语法**：MP 的 `updateBatchById()` 是逐条 UPDATE，批量更新需 N 次 SQL 调用。`BatchSqlRunner.batchUpdate()` 用单条 SQL 完成批量更新，性能差异显著
2. **rewriteBatchedStatements=true 的局限**：该参数对 `INSERT INTO ... VALUES` 有效，但对 `UPDATE` 无效，无法用 MyBatis BATCH 模式实现单 SQL 批量更新
3. **EntityMeta 元数据抽象**：`BatchSqlRunner` 通过 `EntityMeta` 统一处理字段名映射（驼峰→下划线）、特殊字段（createdAt/updatedAt）、非数据库字段过滤（`@TableField(exist=false)`），所有批量写入共享同一套元数据逻辑

### 维护单一路径的优势

1. **一致的事务边界**：所有批量写入通过 `BatchSqlRunner`，事务控制逻辑统一
2. **可预测的性能**：所有批量操作使用统一的 `DB_BATCH_SIZE`（1000），避免不同路径的 batchSize 不一致
3. **故障排查简单**：出现批量写入问题时，只需检查 `BatchSqlRunner` 一处代码

### MyBatis BATCH 模式的问题

1. **rewriteBatchedStatements=true 只优化 INSERT**：对 UPDATE 无效
2. **N+1 问题**：MyBatis BATCH 模式执行 N 条 UPDATE 仍是 N 次 JDBC 调用
3. **行为不一致**：项目中 `BatchSqlRunner` 已稳定运行，替换为 MyBatis BATCH 会引入不可预期的行为差异

## 被否决的方案

| 方案 | 否决理由 |
|------|---------|
| MyBatis BATCH 模式 | rewriteBatchedStatements=true 只优化 INSERT，对 UPDATE 无效；MyBatis BATCH 仍是 N 次 JDBC 调用 |
| 混用 BatchSqlRunner 和 MyBatis BATCH | 两套元数据解析逻辑，行为不一致，故障排查困难 |
| 移除 BatchSqlRunner，统一用 MP | MP 的 CASE-WHEN 批量更新能力不如 BatchSqlRunner 精细 |

## 后果

- 所有 CleanseService 的批量写入必须通过 `BatchSqlRunner`
- 新增实体类时，只需确保有 `@TableName` 和 `@TableId(type = IdType.AUTO)`，BatchSqlRunner 自动处理元数据
- `EntityMeta.buildMeta()` 必须正确过滤 `@TableField(exist=false)` 的字段（已实现）
- 未来如需支持 JSON 字段或计算字段，通过扩展 `toJdbcValue()` 或自定义 `FieldProcessor` 实现
