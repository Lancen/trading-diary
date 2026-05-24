# 数据库批量写入

超过 1 条的循环写入必须使用 `BatchSqlRunner`，禁止逐条调用 mapper。

**Why:** `BatchSqlRunner` 基于 `JdbcTemplate` 拼接多值 INSERT / CASE-WHEN UPDATE，配合 JDBC URL 参数 `rewriteBatchedStatements=true`，比 MyBatis BATCH 模式少了 SqlSession 开销，性能更优。2 条记录分开写 = 2 次网络往返，合并写 = 1 次。数据库在代码侧是本地资源，网络才是瓶颈。

**How to apply:**
- 任何循环内调 `mapper.insert()` / `mapper.updateById()` 的场景 → 收集到 List 后调用 `batchSqlRunner.batchInsert()` / `batchSqlRunner.batchUpdate()`
- 批量大小用常量（参考 `CollectionConstants.DB_BATCH_SIZE`），禁止硬编码
- `@Transactional` 确保同一事务内批处理生效
- `BatchSqlRunner` 通过反射自动推断表名和字段，无需手写 SQL
