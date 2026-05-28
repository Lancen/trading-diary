# ADR-0005: DataTypeHandler.cleanse() 返回 int 是有意设计

## 状态

已接受

## 日期

2026-05-28

## 语境

`DataTypeHandler` 接口的 `cleanse()` 方法签名如下：

```java
public interface DataTypeHandler {
    String dataType();
    FetchResult fetch(LocalDate tradeDate);
    int cleanse(String rawJson, LocalDate tradeDate);
    default boolean requiresCalendar() { return true; }
}
```

`cleanse()` 返回 `int`（影响的记录数），而非 `List<CleanseTask>` 或其他结构化类型。在架构审查中，有建议将其改为返回"清洗任务列表"以便统一处理，但经过 grilling 后确认：**返回 int 是有意设计，不应追求统一 CleanseTask 抽象。**

## 决策

**`DataTypeHandler.cleanse()` 的返回类型保持为 `int`，不引入 CleanseTask 中间类型。**

CleanseService 层负责：
1. 将 `rawJson` 解析为实体对象
2. 调用 `BatchSqlRunner` 批量写入
3. 返回写入记录数

Handler 层的职责边界是"清洗转换"（解析 + 对象构建），而非"执行写入"。

## 理由

### 接口简洁性

`cleanse()` 返回 `int` 是 CollectionOrchestrator 更新采集日志所需的最小信息：

```java
int recordCount = handler.cleanse(rawJson, tradeDate);
cleanseLog.setRecordCount(recordCount);
```

引入 `List<CleanseTask>` 会：
1. 引入新的领域类型（CleanseTask），需要定义 targetTable、entity、metadata 等字段
2. 增加 CollectionOrchestrator 的适配逻辑（根据 targetTable 路由到不同 Service）
3. 需要设计批量窗口和事务边界

### 已有机制覆盖了需要统一的场景

1. **ADR-0003 覆盖了"行级归属"问题**：raw_data 表增加 `sector_code` 列，FETCH 时写入板块编码
2. **CleanseService 的职责已清晰**：Handler 调用 Service，Service 负责写库
3. **`BatchSqlRunner` 是统一的批量执行路径**：所有 Service 共享同一写入引擎

### `STOCK_SPOT` 和两 Service 的耦合是已知约束

`StockSpotHandler.cleanse()` 内部调用 `StockInfoCleanseService` + `StockDailyCleanseService`，这是两个数据类型硬绑的设计。但这并非架构缺陷，而是**数据来源的固有耦合**——STOCK_SPOT 数据同时更新两只表是业务需求。

若未来需要解耦，可以通过以下方式扩展：
1. Handler 返回 `Map<String, List<?>>`（表名→实体列表）
2. CollectionOrchestrator 根据表名路由到对应 Service

但在当前需求下，这是不必要的过度设计。

### safeXxx 工具方法无重复问题

所有 CleanseService 通过 `JsonNodeHelper` 的静态方法（`safeText`/`safeDecimal`/`safeLong`）读取 JSON，62 处调用的行为完全一致（null → null）。**重复的是调用点，而非行为**，无需进一步抽象。

## 被否决的方案

| 方案 | 否决理由 |
|------|---------|
| `cleanse()` 返回 `List<CleanseTask>` | 引入新类型、增加路由逻辑、需要设计批量窗口；当前需求不需要 |
| 统一 `safeXxx` 抽象 | 所有实现通过 `JsonNodeHelper`，行为完全一致，调用点重复不是问题 |
| Handler 通过 Service 层注入解耦 | 引入循环依赖风险（Handler 需要 Service，Orchestrator 需要 Handler） |

## 后果

- `DataTypeHandler.cleanse()` 接口不变
- CollectionOrchestrator 继续使用 `int` recordCount 更新日志
- 未来若有新数据类型（如外部 JSON），CleanseService 适配器模式保持不变
- SSE/SZSE 等交易所标识继续作为参数传递（不升级为枚举），除非未来出现第三个交易所
