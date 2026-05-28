# 后端分层与数据访问规范

## 分层约束（不可逆）

Controller → Service → Mapper，严格单向依赖，禁止反向或跨层调用。

### 违规模式

```java
// ❌ Controller 直接注入 Mapper
@RestController
class FooController {
    @Autowired private FooMapper fooMapper;  // 跨层
}

// ❌ Controller 内嵌 JdbcTemplate 业务逻辑
@RestController
class BarController {
    @Autowired private JdbcTemplate jdbc;  // 跨层 + 绕过 ORM
    public List<X> query() {
        return jdbc.query("SELECT ...", ...);  // SQL 在 Controller
    }
}
```

### 合规模式

```java
// ✅ Controller 只调 Service
@RestController
class FooController {
    @Autowired private FooService fooService;
    public List<X> query() { return fooService.query(); }
}

// ✅ Service 注入 Mapper
@Service
class FooServiceImpl implements FooService {
    @Autowired private FooMapper fooMapper;
    public List<X> query() { return fooMapper.selectList(...); }
}
```

## ORM 规范：MyBatis-Plus XML 优先

- 所有 SQL 必须写在 `src/main/resources/mapper/*.xml`，禁止 `@Select`/`@Update` 注解拼接
- 简单 CRUD 用 MyBatis-Plus 内置方法（`selectList`、`insert`、`updateById`），无需写 XML
- 复杂查询（JOIN、窗口函数、聚合）必须写 XML
- 动态表名/列名路由必须用 `<choose>` 白名单，禁止 `${}` 字符串插值

### `<choose>` 白名单模式

```xml
<!-- 安全：白名单枚举合法值 -->
<choose>
    <when test="sectorType == 'INDUSTRY'">industry</when>
    <when test="sectorType == 'CONCEPT'">concept</when>
    <otherwise>1=0</otherwise>
</choose>

<!-- 危险：${} 直接插值，SQL 注入风险 -->
FROM ${sectorType}  <!-- ❌ 禁止 -->
```

- `<otherwise>1=0</otherwise>` 是兜底安全策略：非法参数返回空结果而非报错
- 排序字段白名单同理：`<choose>` 枚举合法排序列，`<otherwise>` 用默认排序

## Service 必须接口+实现类

详见 `service-interface-impl.md`。调用方注入接口类型，不引用 `impl/` 下的具体类。

## 批量写入

详见 `batch-db-write.md`。循环内调 `mapper.insert()`/`mapper.updateById()` 必须改为 `BatchSqlRunner`。

## 工具类模式

### `UpsertHelper` — 批量 upsert 分区

```java
// 收集已有 key → 分区为 toInsert/toUpdate → 批量写入
Set<String> existingKeys = existing.stream().map(e -> e.getKey()).collect(Collectors.toSet());
UpsertHelper.PartitionResult<Entity> result = UpsertHelper.partition(
    entities, existingKeys, Entity::getKey, Entity::getId, existing::getById
);
if (!result.toInsert.isEmpty()) batchSqlRunner.batchInsert(result.toInsert);
if (!result.toUpdate.isEmpty()) batchSqlRunner.batchUpdate(result.toUpdate);
```

- `partitionAndSave()` 一步完成分区+写入，适用于无额外后处理的场景
- `IdCopier` 函数式接口用于从已有记录复制 ID 到更新记录

### `JsonNodeHelper` — null 安全 JSON 解析

```java
import static com.tradingdiary.util.JsonNodeHelper.*;
String name = safeText(node, "name");           // null → null
BigDecimal price = safeDecimal(node, "price");   // null → null
String code = stripMarketPrefix("sh600519");     // → "600519"
```

- 所有 `CleanseServiceImpl` 统一使用 `import static JsonNodeHelper.*`
- 禁止在 Service 中重复定义 `safeText`/`safeDecimal`/`safeLong`/`stripMarketPrefix`

### `RetryPolicy` — 配置化重试

```java
RetryPolicy policy = RetryPolicy.DEFAULT;  // 3 次，2s 初始，2x 退避，30s 上限
long backoffMs = policy.getBackoffMs(attempt);
```

- 禁止硬编码 `MAX_RETRIES`、`INITIAL_BACKOFF_MS` 等魔术数字
- 自定义重试策略用 `new RetryPolicy(maxRetries, initialBackoffMs, multiplier, maxBackoffMs)`

## 判别返回类型 `FetchResult`

采集 `fetch()` 返回 `FetchResult` 而非裸 `String`，通过 `Type` 枚举区分：

```java
// 单板块类型（股票、两融等）
return FetchResult.single(json);

// 多板块类型（行业/概念指数日线）
return FetchResult.multiSector(logId, sectorCount);
```

- Orchestrator 通过 `result.getType()` 路由，禁止 `instanceof` 检查
- 新增采集类型只需在 `FetchResult.Type` 加枚举值 + Orchestrator 加路由分支

## 为什么

- 分层不可逆是项目宪法，Controller 膨胀是 Spring Boot 项目最常见的架构腐化
- JdbcTemplate 散落各处导致 SQL 不可审计、不可测试、与 MyBatis-Plus 双轨维护
- `<choose>` 白名单是 `${}` 的唯一安全替代，`<otherwise>1=0` 保证非法输入不报错不泄露
- 工具类提取消除 8 个 Cleanse 服务的 ~350 行重复代码