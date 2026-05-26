# 数据库批量写入

超过 1 条的循环写入必须使用 `BatchSqlRunner`，禁止逐条调用 mapper。

**Why:** `BatchSqlRunner` 基于 `JdbcTemplate` 拼接多值 INSERT / CASE-WHEN UPDATE，配合 JDBC URL 参数 `rewriteBatchedStatements=true`，比 MyBatis BATCH 模式少了 SqlSession 开销，性能更优。2 条记录分开写 = 2 次网络往返，合并写 = 1 次。数据库在代码侧是本地资源，网络才是瓶颈。

**How to apply:**
- 任何循环内调 `mapper.insert()` / `mapper.updateById()` 的场景 → 收集到 List 后调用 `batchSqlRunner.batchInsert()` / `batchSqlRunner.batchUpdate()`
- 批量大小用常量（参考 `CollectionConstants.DB_BATCH_SIZE`），禁止硬编码
- `@Transactional` 确保同一事务内批处理生效
- `BatchSqlRunner` 通过反射自动推断表名和字段，无需手写 SQL

## 违规模式（必须避免）

```java
// ❌ 违规：循环内逐条写入
for (JsonNode stock : board.get("stocks")) {
    mapper.insert(entity);  // 每条1次网络往返
}

// ❌ 违规：逐条 upsert
for (String code : codes) {
    Entity existing = mapper.selectOne(...);
    if (existing != null) {
        mapper.updateById(existing);  // 逐条更新
    } else {
        mapper.insert(entity);  // 逐条插入
    }
}
```

## 合规模式（必须使用）

```java
// ✅ 合规：批量查询 + 分离 + 批量写入
List<Entity> existing = mapper.selectList(...);  // 1次查询
Set<String> existingKeys = existing.stream()
    .map(e -> e.getKey()).collect(Collectors.toSet());

List<Entity> toInsert = new ArrayList<>();
List<Entity> toUpdate = new ArrayList<>();
for (String code : codes) {
    if (existingKeys.contains(code)) {
        // 设置更新字段，加入 toUpdate
    } else {
        // 构建新实体，加入 toInsert
    }
}

if (!toInsert.isEmpty()) batchSqlRunner.batchInsert(toInsert);
if (!toUpdate.isEmpty()) batchSqlRunner.batchUpdate(toUpdate);
```

## 自检清单

写完任何涉及数据库写入的代码后，必须自检：
1. 是否存在 `for` / `while` 循环内调用 `mapper.insert()` / `mapper.updateById()` / `mapper.deleteById()`？
2. 如果是 → 必须改为收集到 List 后使用 `BatchSqlRunner`
3. 批量写入方法是否加了 `@Transactional`？
