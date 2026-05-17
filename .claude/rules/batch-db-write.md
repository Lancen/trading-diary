# 数据库批量写入

超过 1 条的循环写入必须使用 MyBatis BATCH 模式，禁止逐条调用 mapper。

**Why:** 代码侧开 BATCH SqlSession 的开销远低于逐条 SQL 的网络往返延迟。2 条记录分开写 = 2 次网络往返，合并写 = 1 次。数据库在代码侧是本地资源，网络才是瓶颈。

**How to apply:**
- 任何循环内调 `mapper.insert()` / `mapper.updateById()` 的场景 → 改为 `ExecutorType.BATCH` + `flushStatements()`
- 配合 JDBC URL 参数 `rewriteBatchedStatements=true`，MySQL 驱动自动合并多条 INSERT 为单条多值语句
- `@Transactional` 确保同一事务内批处理生效
- 批量大小用常量（参考 `CollectionConstants.DB_BATCH_SIZE`），禁止硬编码
