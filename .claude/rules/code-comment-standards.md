# 代码注释规范

所有 Controller、Service、Mapper 接口和 XML mapper 文件必须有中文注释，便于开发人员理解代码。所有用户可见文本必须为中文。

## 规则

### 1. 类级 Javadoc（中文）

每个 Controller、Service 接口、Mapper 接口、Scheduler、Client 等类必须有类级 Javadoc，用一句话说明该类的职责。

```java
/**
 * 数据采集查询服务，封装采集状态、日志和交易日历等查询逻辑
 */
public interface CollectionQueryService {
```

### 2. 方法级 Javadoc（中文）

- **Controller**：每个 public 方法必须有 `@Operation(summary=)` 中文描述
- **Service**：每个 public 方法必须有中文 Javadoc（含 `@param`、`@return`）
- **Mapper**：每个方法必须有中文 Javadoc 说明查询/操作目的

### 3. 行内注释（中文）

所有业务逻辑注释必须使用中文。技术术语可保留英文（如 `FETCH`、`CLEANSE`、`JWT`）。

```java
// 创建 FETCH 日志
DataCollectionLog fetchLog = new DataCollectionLog();
```

### 4. XML mapper 注释（中文）

每个 XML mapper 文件：
- 文件顶部必须有 `<!-- -->` 注释说明该 mapper 的用途
- 每个 `<select>`/`<insert>`/`<update>`/`<delete>` 节点前必须有 `<!-- -->` 注释说明该 SQL 的用途

```xml
<!-- 股票基本信息 Mapper，提供股票行情数据的查询 -->
<mapper namespace="com.tradingdiary.mapper.StockInfoMapper">

    <!-- 查询股票行情表的最大交易日期 -->
    <select id="selectMaxTradeDate" resultType="java.time.LocalDate">
        SELECT MAX(trade_date) FROM stock_info
    </select>
</mapper>
```

### 5. 用户可见文本（中文）

- 后端异常消息、错误提示 → 中文（如 `"服务器内部错误"` 而非 `"Internal Server Error"`）
- 前端 UI 文本（按钮、标题、提示）→ 中文
- 日志消息 → 可保留英文（项目规则"日志除外"）
- API 路径、JSON key → 保留英文

### 6. 前端注释（中文）

前端 `.tsx`/`.ts` 文件中的业务逻辑注释使用中文。

```typescript
// 用新令牌重试原始请求
return ky(originalRequest);
```

## Why

- 代码被阅读的次数远多于编写的次数，中文注释降低理解成本
- Mapper 方法名通常只表达"查什么"，注释补充"为什么查"和"业务含义"
- XML mapper 中的 SQL 对不熟悉业务的人尤其难懂，注释是唯一的人可读说明
- 用户可见英文文本降低产品专业度
