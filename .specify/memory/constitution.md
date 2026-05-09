<!--
  Sync Impact Report
  ==================
  Version: 0.0.1 (初始化版本)
  Ratified: 2026-05-09
  This is the initial project constitution. No prior versions.
-->

# Trading Diary 项目宪法

宪法是项目最高准则。它定义**决策原则**，而非实现细节。当两个方案冲突时，宪法告诉你选哪个。实现细节见 `docs/standards/technical-standards.md`。

> **注**：CLAUDE.md 中的 4 条行为准则（编码前先思考、简洁优先、精准修改、目标驱动执行）面向 AI（Claude Code），与本宪法的工程决策原则并行，各司其职——AI 准则约束编码过程，宪法约束系统设计。

---

## 核心原则

每条原则包含三个要素：**规则**（做什么）、**为什么**（背后的取舍）、**反模式**（具体代码示例）。

### I. 约定优于配置

Spring Boot 的默认值经过大量生产验证。只在默认约定无法满足需求时，才引入自定义配置。

**为什么**：减少决策疲劳，降低 onboarding 成本。每多一个自定义配置，就多一个需要文档解释的东西。

**反模式**：
```java
// ❌ "因为习惯"覆盖默认值
server.port=8081

// ❌ 自定义 Jackson 配置替代 spring.jackson.*
@Bean public ObjectMapper objectMapper() { ... }

// ✅ 用 Spring Boot 提供的配置项
spring.jackson.date-format=yyyy-MM-dd
```

**边界**：当默认值确实不适用时（如生产环境需要调大线程池），用配置项覆盖而非写 `@Configuration` 类。如果 Spring Boot 没有对应的配置项，可以写自定义配置并记录在 `docs/architecture/` 中说明原因。

---

### II. 分层不可逆

Controller → Service → Mapper，调用方向不可逆。Controller 不能直接调用 Mapper，Service 不能反向调用 Controller。

**为什么**：分层不可逆让你可以独立测试每一层、替换实现、控制事务边界。一旦出现反向调用，整个分层体系崩溃。

**反模式**：
```java
// ❌ Controller 直接注入 Mapper
@RestController
public class TradeController {
    @Autowired
    private TradeMapper tradeMapper;
}

// ❌ Controller 中包含业务判断
@PostMapping("/trades")
public ApiResponse<Void> create(@RequestBody TradeRequest req) {
    if (req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
        throw new BadRequestException("价格必须大于 0");
    }
    // ...
}

// ✅ Controller 只做参数绑定和调用 Service
@PostMapping("/trades")
public ApiResponse<TradeVO> create(@Valid @RequestBody TradeRequest req) {
    return ApiResponse.ok(tradeService.create(req));
}
```

**边界**：工具类（`util/`）、配置类（`config/`）、AOP 切面（`aspect/`）不受此限制，任何层都可以使用。

---

### III. 防御式编程

所有外部输入必须验证。API 参数、数据库查询结果、外部服务响应——任何跨信任边界的数据。

**为什么**：交易数据涉及真金白银，一个未验证的输入可能导致盈亏计算错误。防御式编程的成本远低于修复错误数据的成本。

**反模式**：
```java
// ❌ 信任客户端传来的分页大小
Page<Trade> page = tradeService.query(userId, page, size);

// ❌ catch 后吞掉异常
try { ... } catch (Exception e) { }

// ❌ 只打日志不处理
try { ... } catch (Exception e) { log.error("出错了", e); }

// ✅ 验证参数 + 自定义异常 + 统一处理
if (size > 100) {
    throw new BadRequestException("分页大小不能超过 100");
}
```

**边界**：内部方法之间的调用不需要防御式验证（信任内部代码）。验证集中在 Controller 层（参数格式）和 Service 层（业务规则）。

---

### IV. 渐进式优化

先实现正确性，再优化性能。性能优化必须有量化数据支撑。

**为什么**：过早优化是万恶之源。你猜的瓶颈 90% 不是真正的瓶颈，而正确的代码即使慢一点也可以工作。

**反模式**：
```java
// ❌ "反正迟早要用"——没有性能数据就引入缓存
@Cacheable("trades")
public List<TradeVO> getTrades() { ... }

// ❌ "预重构"——功能还没写完就开始拆微服务

// ✅ 先写正确的代码，通过测试后再用 JMeter/JMH 测瓶颈
// ✅ 性能优化附带 before/after 对比数据
```

**边界**：领域约束中的规则（如 BigDecimal 精度、UTC 时区、认证验证）不属于"过早优化"，它们是**正确性**的一部分，必须在 Phase 0 就遵守。

---

## 领域约束

以下规则源于交易日记 SaaS 产品的领域特性，优先级等同于核心原则。

### 1. 金额精度

交易金额、价格、盈亏等涉及金钱的字段，一律使用 `BigDecimal`，禁止 `float`/`double`。

**数据库映射**：`decimal(20,8)`，8 位小数精度可覆盖加密货币和外汇的精度需求。

**计算规则**：所有 `BigDecimal` 除法运算 MUST 指定 `RoundingMode.HALF_UP` 和精度，禁止使用无参 `divide()`：

```java
// ❌ double profit = exitPrice - entryPrice;
// ❌ profit.divide(totalCost);  // 无限小数时 ArithmeticException

// ✅
BigDecimal profit = exitPrice.subtract(entryPrice);
BigDecimal roi = profit.divide(totalCost, 8, RoundingMode.HALF_UP);
```

### 2. 时区一致性

所有时间字段 UTC 存储，响应按用户时区转换。

**传递机制**：客户端通过请求头 `X-Timezone` 声明时区（如 `Asia/Shanghai`、`America/New_York`），响应中所有时间戳按此时区格式化。

**为什么**：交易者可能跨时区使用系统。服务器 UTC + 请求头声明时区转换是唯一不会出错的方式。请求头比用户配置更灵活——用户可以在 PC 看 UTC 时间，在手机看本地时间。

```java
// 请求头：X-Timezone: Asia/Shanghai
// 响应时间按 Asia/Shanghai 格式化："2026-05-09T16:00:00+08:00"

// 数据库：datetime 类型，存储 UTC 时间
// Entity：LocalDateTime
// API 响应：按 X-Timezone 头转换后返回
// 默认：请求无 X-Timezone 头时，按 UTC (Z) 输出
```

### 3. 数据归属

任何交易数据必须关联 `user_id`。所有查询必须带用户隔离条件。

**为什么**：交易数据是敏感个人信息。一处遗漏就可能导致用户看到别人的交易记录。

```java
// ❌ 不带用户隔离的查询
List<Trade> trades = tradeMapper.selectList(null);

// ✅ Mapper 方法签名强制传入 userId
List<Trade> selectByUserId(@Param("userId") Long userId);

// ✅ 或通过拦截器在 SQL 层自动追加 WHERE user_id = ?
```

### 4. 交易日概念

区分**交易日**（T）和**自然日**。期货夜盘、加密货币 24h 市场等场景下，交易日可能跨越自然日。

- `trade_date`：交易日，业务操作以此为准
- `created_at`：记录创建时间，审计用途
- 结算、统计功能以 `trade_date` 为准

### 5. 认证与授权

SaaS 产品必须以 JWT + Spring Security 实现无状态认证。

**令牌模型**：
- Access Token：短期（15 分钟），承载在 `Authorization: Bearer <token>` 头
- Refresh Token：长期（7 天），仅用于换取新的 Access Token
- 登出时 Refresh Token 失效

**密码策略**：
- 密码 MUST 使用 BCrypt 加盐哈希存储（cost ≥ 12）
- 密码最小长度 8 位，MUST 含数字和字母

**授权模型**：
- 默认采用 RBAC（角色-权限），用户 → 角色 → 权限
- 初期角色：USER（普通用户）、ADMIN（管理员）
- 每个 API 端点 MUST 声明所需权限，`@PreAuthorize` 注解控制

**为什么**：交易数据属于高度敏感个人信息。认证是数据隔离（领域约束 §3）的前置条件——没有认证，user_id 隔离无从谈起。

---

## 阶段化治理

规则按项目阶段分级。Constitution Check 只检查**当前阶段**的规则。

### Phase 0 — 立即生效（项目初始化 → 第一行代码）

在写任何业务代码之前，以下规则必须遵守：

- 包结构符合 `com.tradingdiary` 层级（技术规范 §1.1）
- 命名符合约定（技术规范 §1.2）
- 数据库表设计符合建表规约（技术规范 §2.1），含软删除字段
- API 响应使用统一格式 `ApiResponse<T>`（技术规范 §3.2）
- 金额字段使用 `BigDecimal`，数据库字段 `decimal(20,8)`，除法指定 RoundingMode（领域约束 §1）
- 时间字段 UTC 存储（领域约束 §2）
- 所有业务表包含 `user_id` 字段（领域约束 §3）
- 认证方案：JWT + Spring Security（领域约束 §5）
- 异常体系继承 `BaseException`，有 `@ControllerAdvice` 全局处理器
- 分层架构不可逆（核心原则 II）

### Phase 1 — 业务开发（有业务代码后）

当开始写业务逻辑时，以下规则加入检查：

- 输入验证：Controller 参数 `@Valid` + Service 业务验证（核心原则 III）
- 测试覆盖率达标（技术规范 §8）
- 事务管理正确（技术规范 §6）
- 日志合规（技术规范 §7）
- 安全规范：CORS 白名单、BCrypt 密码、敏感信息脱敏日志（技术规范 §5）
- SQL 无 `SELECT *`，参数使用 `#{}` 预编译（技术规范 §2.3）
- 复杂模块含 ERD 和生命周期说明（技术规范 §2.4）
- API 限流就位（技术规范 §5.3）

### Phase 2 — 部署上线（准备发布时）

当系统准备部署到生产环境时，以下规则加入检查：

- Docker 多阶段构建（技术规范 §9）
- Actuator 健康检查分离（技术规范 §9）
- 性能基线达标（技术规范 §10.1）
- 监控指标就位（技术规范 §10.2）
- 告警规则配置（技术规范 §10.3）
- API 文档完整（SpringDoc）
- Swagger UI 关闭（技术规范 §5.2）
- 数据库迁移脚本通过 Flyway 管理，命名遵循 `V<版本号>__<描述>.sql`（技术规范 §4.2）

---

## Governance

### 宪法合规检查清单 — Phase 0

在每个 feature spec 的 Constitution Check 门禁中，逐项勾选：

- [ ] 包路径在 `com.tradingdiary` 下，未私自添加顶层包
- [ ] Controller 未直接注入 Mapper（分层不可逆）
- [ ] Controller 方法未包含业务逻辑（if/else 业务判断、金额计算）
- [ ] API 响应使用 `ApiResponse<T>` 包装，未直接返回实体对象
- [ ] 金额/价格/盈亏字段使用 `BigDecimal`，非 `float`/`double`
- [ ] `BigDecimal` 除法使用 `divide(value, scale, RoundingMode.HALF_UP)`
- [ ] 数据库金额字段定义为 `decimal(20,8)`
- [ ] 数据库表包含 `id`、`user_id`、`created_at`、`updated_at`、`is_deleted` 字段
- [ ] 时间相关字段明确按 UTC 存储
- [ ] 响应时间转换基于 `X-Timezone` 请求头（无此头时按 UTC 输出）
- [ ] 认证方案基于 JWT + Spring Security，密码 BCrypt 存储
- [ ] 异常类继承 `BaseException`，有 `@ControllerAdvice` 全局处理器
- [ ] 命名符合约定（类名大驼峰、方法小驼峰、布尔 `is` 前缀）

**Phase 1 和 Phase 2 的检查清单**在进入对应阶段时从 `docs/standards/technical-standards.md` 中对应章节提取。

### 修订流程

- 宪法修订通过 PR 提交，附带变更理由和影响评估
- 版本管理遵循语义化版本（MAJOR.MINOR.PATCH）
  - MAJOR：原则删除或重新定义
  - MINOR：新增原则/领域约束/阶段
  - PATCH：措辞澄清、错字修正、数值微调
- 每个 feature spec 的 Constitution Check 门禁必须在 Phase 0 研究前通过
- Reviewer 验证变更是否符合宪法，不符合的 PR 被拒绝

### 相关文件

| 文件 | 用途 |
|------|------|
| `docs/standards/technical-standards.md` | 技术实现规范（本文档的配套细节） |
| `CLAUDE.md` | 项目技术栈、AI 行为准则、日常命令 |
| `docs/architecture/` | 架构设计决策记录 |

---

**Version**: 0.0.1 | **Ratified**: 2026-05-09 | **Last Amended**: 2026-05-09
