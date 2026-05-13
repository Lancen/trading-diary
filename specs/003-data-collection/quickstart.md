# Quickstart：数据采集层

## 前置条件

1. **子项目 1 已完成** — 项目脚手架 + 用户认证体系已就绪
2. **AKTools Docker 运行中**:
   ```bash
   docker run -d --name aktools --restart unless-stopped \
     -p 8081:8080 \
     registry.cn-shanghai.aliyuncs.com/akfamily/aktools:1.8.95
   ```
3. **MySQL 数据库可访问** — `.env` 中 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 已配置

## 启动后端

```bash
# 1. 编译
./gradlew compileJava

# 2. 运行 Flyway 迁移（首次会自动创建 V3__collection_schema.sql 定义的 12 张表）
./gradlew bootRun --args='--spring.profiles.active=dev'

# 3. 验证 AKTools 连通性
curl http://localhost:8081/api/public/tool_trade_date_hist_sina
```

## 首次使用

### 1. 初始化交易日历

启动后系统会自动拉取交易日历。也可手动触发：

```bash
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/TRADE_CALENDAR \
  -H "Authorization: Bearer <admin_token>"
```

### 2. 回填历史两融数据

通过管理后台"两融完整性"页面选择日期范围点"补采"，或直接调用 API：

```bash
curl -X POST http://localhost:8080/api/v1/admin/collection/backfill \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{"dataType":"MARGIN_DAILY_SSE","exchange":"SSE","startDate":"2024-01-01","endDate":"2024-12-31"}'
```

建议顺序：先回填最近 1 年 → 再逐步往前回溯。系统按周分片，支持中断后继续。

### 3. 触发全量采集

```bash
# 股票基础信息 + 日线
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/STOCK_INFO \
  -H "Authorization: Bearer <admin_token>"

# 行业板块
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/INDUSTRY_NAME \
  -H "Authorization: Bearer <admin_token>"

# 概念板块
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/CONCEPT_NAME \
  -H "Authorization: Bearer <admin_token>"
```

## 查看状态

```bash
# 所有数据类采集状态
curl http://localhost:8080/api/v1/admin/collection/status \
  -H "Authorization: Bearer <admin_token>"

# 两融数据完整性
curl "http://localhost:8080/api/v1/admin/collection/gaps?start=2024-01-01&end=2024-12-31&exchange=SSE" \
  -H "Authorization: Bearer <admin_token>"
```

## 管理后台入口

- 采集总览：`/admin/collection`
- 两融完整性：`/admin/collection/margin`

## Dev 模式

`application-dev.yml` 中 `auto-login.enabled=true`，开发时无需登录可直接访问后台。
