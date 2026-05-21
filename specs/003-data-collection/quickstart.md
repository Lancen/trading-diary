# Quickstart

## 一期开发启动

```bash
# 1. 切分支
git checkout 001-collection-ui-redesign

# 2. 运行 Flyway 迁移（V5 新增字段和表）
./gradlew flywayMigrate

# 3. 启动后端
./gradlew bootRun

# 4. 启动前端
cd frontend && pnpm dev

# 5. 测试新 API
curl http://localhost:8080/api/v1/admin/stocks/list
curl http://localhost:8080/api/v1/admin/market/concepts
```

## 开发顺序

1. Flyway V5 迁移
2. 实体 + Mapper（DataCollectionLog 加字段、MarginDaily 加字段、IndexDaily 新建）
3. MarginCleanseService 改造（加 change 计算）
4. 3 个新 Controller + Service（StockDataController、MarketDataController、MarginStatsController）
5. 前端 layout.tsx 侧边栏
6. Hub 页改造
7. 股票行情详情 + 成分股管理
8. 股票列表 + 详情
9. 概念列表 + 行业列表
