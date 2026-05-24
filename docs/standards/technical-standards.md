# Trading Diary 技术规范

本文档是项目宪法的配套技术规范，包含日常开发中需要查阅的具体规则。宪法（`specs/_governance/constitution.md`）定义决策原则，本文档定义执行细节。

**适用阶段标记**：
- 🟢 Phase 0：项目初始化阶段，立即生效
- 🟡 Phase 1：业务代码开发阶段
- 🔴 Phase 2：部署上线阶段

---

## 1. 项目结构与命名 🟢

### 1.1 包结构

```text
src/main/java/com/tradingdiary/
├── config/          # 配置类（MyBatis、安全、Swagger 等）
├── controller/      # 控制器层（仅处理 HTTP 请求/响应）
├── service/         # 服务层接口
│   ├── impl/        # 服务实现类
│   └── dto/         # 服务间数据传输对象
├── mapper/          # MyBatis Mapper 接口
├── entity/          # 实体类（对应数据库表，使用 MyBatis-Plus 注解）
├── model/
│   ├── vo/          # 视图对象（API 响应）
│   └── request/     # 请求对象（API 参数）
├── exception/       # 自定义异常体系
├── aspect/          # AOP 切面（日志、权限、事务）
└── util/            # 工具类（必须无状态）

src/main/resources/
├── mapper/          # MyBatis XML（复杂 SQL）
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

- 包路径严格遵循上述结构，禁止私自添加顶层包
- 简单 CRUD 使用 MyBatis-Plus `BaseMapper`，无需 XML
- 复杂 SQL（联表、报表）写在 `resources/mapper/*.xml` 中
- 工具类必须无状态（所有方法 static），禁止持有成员变量
- AOP 切面仅用于横切关注点（日志、权限、事务），不得包含业务逻辑

### 1.2 命名约定

#### 后端 Java

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | 大驼峰 | `TradeService`、`TradeController` |
| Mapper 接口 | 实体名 + `Mapper` | `TradeMapper` |
| 方法名 | 小驼峰，动词开头 | `getTradeById()`、`createTrade()` |
| 变量名 | 小驼峰，体现意图 | `totalProfit`（非 `tp`） |
| 常量 | 全大写 + 下划线 | `MAX_RETRY_COUNT` |
| 布尔变量/字段 | `is`/`has`/`can` 开头 | `isClosed`、`hasNotes` |

#### 前端

| 类型 | 规范 | 示例 |
|------|------|------|
| 组件文件 | 大驼峰 `.tsx` | `TradeChart.tsx`、`ProfitCard.tsx` |
| shadcn/ui 组件 | 小写 kebab-case `.tsx` | `button.tsx`、`card.tsx`、`input.tsx`（保持上游约定） |
| 页面文件 | Next.js 约定 | `page.tsx`、`layout.tsx`、`loading.tsx` |
| 工具函数 | 小驼峰 `.ts` | `formatCurrency.ts`、`calcProfit.ts` |
| hooks | `use` 前缀 + 小驼峰 `.ts` | `useTrades.ts`、`useUser.ts` |
| 目录名 | 小写 kebab-case | `trade-chart/`、`common-ui/` |

### 1.3 代码格式

- 每行不超过 120 字符，4 空格缩进，禁止 Tab
- 不可变字段使用 `final` 修饰
- 可能为 null 的返回值使用 `Optional` 包装
- 方法长度不超过 50 行，类长度不超过 500 行
- 禁止使用 Lombok `@Data`（包括实体类——`@Data` 生成的 `equals/hashCode` 在 ORM 代理场景下行为不可控）。实体类使用 `@Getter` + `@Setter`，构造器使用 `@Builder` 或 `@NoArgsConstructor`

### 1.4 MyBatis-Plus 实体规范

```java
@Getter
@Setter
@TableName("trade_record")
public class Trade {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("trade_date")
    private LocalDate tradeDate;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic  // 软删除，MP 自动处理
    @TableField("is_deleted")
    private Boolean isDeleted;
}
```

- 实体类字段使用包装类型（`Long` 非 `long`，`BigDecimal` 非 `double`）
- 时间字段使用 `LocalDateTime` / `LocalDate`，通过 `@TableField(fill = ...)` 自动填充
- 金额字段使用 `BigDecimal`，`@TableField` 不指定精度（精度由 DDL 控制）

---

## 2. 数据库设计 🟢

### 2.1 建表规约

- 表名、字段名：全小写，下划线分隔，如 `trade_record`
- 布尔字段：`is_` 前缀，`tinyint(1)` 类型，1 表示是，0 表示否
- 主键：统一使用 `bigint` 自增，命名为 `id`
- 时间字段：`created_at`、`updated_at`，`datetime` 类型
- 金额字段：`decimal(20,8)`，见宪法领域约束 §1
- 所有业务表 MUST 包含 `user_id`（`bigint`，索引）、`is_deleted`（`tinyint(1)`，默认 0）
- 纯日志表（如 `data_collection_log`）可只含 `created_at`，不需要 `updated_at` 和 `is_deleted`
- 原始数据暂存表（如 `raw_data`）同理，按实际场景判断
- 禁止使用数据库保留字作为字段名
- 所有表 MUST 有 `COMMENT`，格式：`COMMENT='表名_业务场景；生命周期说明'`
- 所有字段 MUST 有 `COMMENT`，格式：内联在列定义末尾 `COMMENT '字段名_业务含义说明'`
- 索引如需注释说明作用：`INDEX ... COMMENT '作用说明'`
- 字符集：`utf8mb4`，排序规则 `utf8mb4_unicode_ci`

**标准建表模板**：

```sql
CREATE TABLE trade_record (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT       NOT NULL COMMENT '用户 ID',
    -- 业务字段...
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除标记',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_user_id_created_at (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易记录表_用户记录实盘交易；创建→持仓→平仓→归档';
```

### 2.2 索引规范

| 索引类型 | 命名格式 | 示例 |
|----------|----------|------|
| 唯一索引 | `uk_字段名` | `uk_trade_date` |
| 普通索引 | `idx_字段名` | `idx_user_id` |
| 联合索引 | `idx_字段1_字段2` | `idx_user_id_created_at` |

- 查询频繁的字段建立索引
- `user_id` MUST 在所有业务表中建立索引（最常见查询条件）
- 联合索引遵循最左前缀原则设计字段顺序
- 每个索引必须有明确的查询场景支撑，禁止冗余索引

### 2.3 SQL 编写

- 禁止 `SELECT *`，必须明确列出字段
- MyBatis XML 中参数使用 `#{}` 预编译占位符，禁止 `${}`（除表名/排序字段等无法预编译的场景）
- 关联查询不超过 3 张表，超出则拆分在 Service 层组装
- 大数据量查询（> 1000 行）必须分页（MyBatis-Plus `Page<T>`）
- 单个事务中 SQL 数量控制在 5 个以内

### 2.4 数据库设计文档 🟡

- 复杂模块（关联 ≥ 3 张表）MUST 提供实体关系图（ERD），置于 `specs/[###-feature]/data-model.md`
- 每个新表在 `data-model.md` 中说明：业务场景、生命周期、核心字段、与其他表的关系
- 已有表结构变更时同步更新 `data-model.md`，确保 ERD 与 DDL 一一对应

**标准生命周期说明格式**（写在 `data-model.md` 表定义中）：

```text
trade_record
  业务场景：用户记录一笔实盘交易，含开仓/平仓价格、数量、方向、盈亏
  生命周期：创建 → 持仓中 → 已平仓 → 归档（可选，平仓 30 天后）
  记录保留：永久保留，用户可软删除，物理删除需 30 天缓冲期
```

---

## 3. API 设计 🟢

### 3.1 RESTful 规范

- 资源 URL 使用复数名词：`/api/v1/trades`
- HTTP 方法语义化：GET 查询、POST 创建、PUT 全量更新、PATCH 部分更新、DELETE 删除
- 版本控制：URL 路径包含版本号 `/api/v1/`

### 3.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-05-09T16:00:00Z"
}
```

- 所有 API 响应使用统一格式 `ApiResponse<T>`，禁止直接返回实体对象
- 分页响应额外包含 `page`、`size`、`total` 字段
- 分页请求参数：`page`（页码，从 0 起，默认 0）+ `size`（每页条数，默认 20，最大 100）
  - MyBatis-Plus 默认参数为 `current`/`size`，需配置 `page-param-mapping: { current: "page" }` 统一对外接口
- 时间戳使用 ISO 8601 格式，时区由请求头 `X-Timezone` 决定（默认 UTC）
- 请求头示例：`X-Timezone: Asia/Shanghai`，无此头时按 UTC 输出

### 3.3 错误码规范

- 业务错误码：6 位数字，格式 `MMSSEE`
  - 前 2 位（MM）：业务模块，10-认证、20-用户、30-交易、40-统计、50-系统
  - 中间 2 位（SS）：模块内子域，00 保留为模块级通用错误
  - 后 2 位（EE）：具体错误编号，从 01 起
  - 示例：`300101` = 交易模块(30) + 基本信息子域(01) + 记录不存在(01)
- 每个错误码必须有中文和英文错误消息
- HTTP 状态码：2xx 成功、4xx 客户端错误、5xx 服务器错误

**异常响应格式**：所有异常统一使用 `ApiResponse<T>` 结构，`data` 字段为 `null`：

```json
// 业务异常示例（HTTP 400）
{
  "code": 300101,
  "message": "交易记录不存在",
  "data": null,
  "timestamp": "2026-05-09T16:00:00Z"
}
```

- `@ControllerAdvice` 全局异常处理器 MUST 将 `BaseException` 及其子类映射到此格式
- 禁止异常响应格式与正常响应不一致——前者用 `data:null` 且在 ControllerAdvice 中统一处理

---

## 4. Spring Boot 配置 🟢

### 4.1 多环境配置

```yaml
application.yml          # 公共配置
application-dev.yml      # 开发环境
application-test.yml     # 测试环境
application-prod.yml     # 生产环境
```

- 敏感信息通过环境变量或外部配置中心注入，不得写入配置文件
- 开发环境 MyBatis SQL 日志：`logging.level.com.tradingdiary.mapper=DEBUG`
- 生产环境关闭 SQL 日志

### 4.2 数据库迁移（Flyway）

- 迁移脚本命名：`V<版本号>__<描述>.sql`，如 `V1__init_schema.sql`、`V2__add_trade_table.sql`
- 版本号：从 `1` 开始递增，禁止跳跃（`V1` → `V2` → `V3`），不允许 `V1` → `V3`
- 描述：英文，下划线分隔单词，首字母小写
- 修复脚本（回退后重跑）：`V<版本号>__<描述>.sql`，版本号使用下一个可用序号，**禁止**修改已发布脚本
- 每条迁移 MUST 包含对应的 `COMMENT` 和索引定义

### 4.3 数据库连接池（HikariCP）

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 4.4 MyBatis-Plus 配置

```yaml
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.tradingdiary.entity
  global-config:
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

---

## 5. 安全规范 🟡

### 5.1 输入验证

- 所有 API 参数使用 JSR-380 注解验证（`@NotNull`、`@Size`、`@Pattern` 等）
- 敏感参数（用户 ID、资源 ID）必须验证当前用户的操作权限
- 禁止信任客户端传入的分页参数，服务端做上限校验（max page size = 100）

### 5.2 数据安全

- 密码使用 BCrypt 加盐哈希存储（cost ≥ 12）
- 敏感信息（密码、token、身份证号、手机号）禁止出现在日志中
- SQL 参数使用 `#{}` 预编译占位符，禁止字符串拼接和 `${}`
- 生产环境关闭 Swagger UI（Phase 2 阶段执行一次即可）
- CORS 配置使用白名单模式，禁止 `allowedOrigins("*")`

### 5.3 API 限流 🟡

- 登录接口：每分钟 5 次/IP（防暴力破解）
- 普通 API：每分钟 60 次/用户
- 使用 Spring AOP + Redis 实现，或集成 Bucket4j
- 限流触发时返回 HTTP 429 + 统一响应格式：
  ```json
  // 登录限流：100002 = 认证(10) 通用(00) 限流(02)
  // 普通API限流：500001 = 系统(50) 通用(00) 限流(01)
  {
    "code": 100002,
    "message": "请求过于频繁，请稍后再试",
    "data": null,
    "timestamp": "2026-05-09T17:00:00Z"
  }
  ```

---

## 6. 事务管理 🟡

- `@Transactional` 注解放在 Service 层，禁止放在 Controller 层
- 默认使用 `@Transactional(rollbackFor = Exception.class)`
- 只读查询使用 `@Transactional(readOnly = true)`
- 禁止在事务中执行 RPC 调用、文件 IO 等耗时操作
- 事务方法必须是 `public` 方法

| 场景 | 传播级别 |
|------|----------|
| 新增/修改数据 | `REQUIRED` |
| 查询操作 | `REQUIRED`（默认即可，避免后续加入写操作时忘记调整） |
| 嵌套事务 | `NESTED`（需 InnoDB，慎用） |
| 独立事务 | `REQUIRES_NEW`（如审计日志，失败不影响主业务） |

---

## 7. 日志规范 🟡

| 级别 | 用途 | 环境 |
|------|------|------|
| `ERROR` | 系统错误，需立即处理 | 全环境 |
| `WARN` | 潜在问题，需关注 | 全环境 |
| `INFO` | 重要业务流程节点 | 全环境 |
| `DEBUG` | 调试信息（含 MyBatis SQL） | 开发/测试环境 |

- 日志使用 JSON 格式输出，便于日志收集系统解析
- 每条日志包含：时间戳、级别、线程名、类名、消息体、traceId，格式如下：
  ```json
  {"timestamp":"2026-05-09T17:00:00.000Z","level":"INFO","thread":"http-nio-8080-exec-1","logger":"com.tradingdiary.service.TradeService","message":"交易创建成功","traceId":"a1b2c3d4"}
  ```
- 禁止 `System.out.println` 和 `e.printStackTrace()`
- MyBatis SQL 日志通过 `logging.level.com.tradingdiary.mapper=DEBUG` 控制，不开启 MyBatis 内置 StdOutImpl

---

## 8. 测试规范 🟡

### 8.1 测试原则 🟡

- 后端每个新 API 端点 MUST 有对应的 API 集成测试（MockMvc 或 `@SpringBootTest`）
- 前端每个关键用户流程 MUST 有 E2E 测试（Playwright）
- 先写测试再写实现（TDD），或至少测试与代码同 PR 提交
- 测试覆盖正常路径、异常路径、边界值

### 8.2 后端测试

| 层级 | 范围 | 框架 |
|------|------|------|
| 单元测试 | 单个方法或类 | JUnit 5 + Mockito |
| Mapper 测试 | SQL/ORM 映射 | `@MybatisPlusTest` + H2 |
| API 测试 | Controller + HTTP 交互 | `@WebMvcTest` + MockMvc |
| 集成测试 | 完整 Spring 上下文 | `@SpringBootTest` + TestRestTemplate |

**覆盖率要求**：
- 单元测试行覆盖率 ≥ 80%，分支覆盖率 ≥ 70%
- Service 层测试覆盖所有业务场景（正常 + 异常 + 边界值）
- Controller 测试使用 `@WebMvcTest`，Mock Service 层
- Mapper 测试使用 H2 内存数据库，每个 Mapper 至少测试基本的 CRUD

### 8.3 前端测试

| 层级 | 范围 | 框架 |
|------|------|------|
| 单元/组件测试 | React 组件、hooks、工具函数 | Vitest + @testing-library/react |
| E2E 测试 | 关键用户流程（登录→创建交易→查看统计） | Playwright |

**Playwright 配置**：
- 配置文件：`playwright.config.ts`（项目根目录）
- 测试目录：`e2e/`
- 执行：`npx playwright test`
- CI 中使用 headless 模式，本地开发可用 `--ui` 模式

**E2E 覆盖范围**（Phase 1 最小集）：
- 用户注册 + 登录 + 登出
- 创建一笔交易记录
- 查看交易列表 + 筛选
- 盈亏统计页面

### 8.4 阶段说明

- 🟢 Phase 0：后端 API 测试框架搭建（JUnit 5 + MockMvc 就位）
- 🟡 Phase 1：后端覆盖率达标 + 前端 Vitest + Playwright E2E 最小集
- 🔴 Phase 2：完整 E2E 回归套件 + CI 集成

---

## 9. 部署规范 🔴

- 应用使用 Docker 容器化部署
- Dockerfile 使用多阶段构建减小镜像体积
- 基础镜像建议使用 Alpine Linux（JDK 21+ 兼容）
- 镜像打版本标签，禁止 `latest` 发布到生产
- 集成 Spring Boot Actuator，分离 readiness 和 liveness 检查
- 生产环境 Actuator 端点通过 Spring Security 保护

---

## 10. 性能与监控 🔴

### 10.1 性能基线

| 指标 | 目标 |
|------|------|
| API P95 响应时间 | < 200ms |
| 单表数据库查询 | < 50ms |
| 复杂查询（2-3 表联查） | < 500ms |
| JVM 进程内存 | < 2GB |

### 10.2 必须监控的指标

- 应用健康状态（UP/DOWN）
- JVM 内存使用率（堆/非堆）
- 数据库连接池状态（活跃/空闲/等待）
- API 响应时间和 QPS
- 错误率和异常数量
- 限流触发次数

### 10.3 告警规则

| 条件 | 响应时间 |
|------|----------|
| 应用不可用 | 立即告警 |
| 错误率 > 1% | 15 分钟内告警 |
| 响应时间 P95 > 500ms | 30 分钟内告警 |
| 内存使用率 > 80% | 1 小时内告警 |
| 限流触发率 > 5% | 1 小时内告警 |

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 0.0.1 | 2026-05-09 | 初始版本：项目结构与命名、数据库设计、API 设计、Spring Boot 配置、安全、事务、日志、测试、部署、性能监控 |
