-- ============================================================
-- V3__collection_schema.sql — 数据采集模块表结构
-- ============================================================

-- 1. 数据采集日志表
CREATE TABLE data_collection_log
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    data_type     VARCHAR(30)  NOT NULL COMMENT '数据类型',
    job_type      VARCHAR(10)  NOT NULL COMMENT 'FETCH/CLEANSE',
    status        VARCHAR(15)  NOT NULL COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    trade_date    DATE         NULL     COMMENT '交易日期',
    week_start    DATE         NULL     COMMENT '周起始日期',
    week_end      DATE         NULL     COMMENT '周结束日期',
    record_count  INT          NULL     COMMENT '记录数量',
    error_msg     TEXT         NULL     COMMENT '错误信息',
    started_at    DATETIME     NULL     COMMENT '开始时间',
    completed_at  DATETIME     NULL     COMMENT '完成时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_log_type_date (data_type, trade_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '数据采集日志表_记录每次数据抓取/清洗任务的执行情况';

-- 2. 原始数据表
CREATE TABLE raw_data
(
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    collection_log_id BIGINT       NOT NULL COMMENT '采集日志ID（关联 data_collection_log.id）',
    data_type         VARCHAR(30)  NOT NULL COMMENT '数据类型',
    trade_date        DATE         NULL     COMMENT '交易日期',
    source            VARCHAR(20)  NOT NULL DEFAULT 'AKSHARE' COMMENT '数据来源',
    raw_json          LONGTEXT     NOT NULL COMMENT '原始 JSON 数据',
    fetch_at          DATETIME     NOT NULL COMMENT '抓取时间',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_raw_log (collection_log_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '原始数据表_存储从 AKShare 抓取的原始 JSON 数据，用于清洗回溯';

-- 3. 交易日历表
CREATE TABLE trade_calendar
(
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trade_date      DATE        NOT NULL COMMENT '交易日期',
    is_trading_day  TINYINT     NOT NULL DEFAULT 1 COMMENT '是否交易日：1=是 0=否',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_date (trade_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '交易日历表_A股交易日历，用于判断交易日期、补数据范围';

-- 4. 股票基本信息表
CREATE TABLE stock_info
(
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code            VARCHAR(10)   NOT NULL COMMENT '股票代码',
    name            VARCHAR(20)   NOT NULL COMMENT '股票名称',
    latest_price    DECIMAL(10,3) NULL     COMMENT '最新价',
    change_pct      DECIMAL(6,2)  NULL     COMMENT '涨跌幅(%)',
    change_amount   DECIMAL(8,3)  NULL     COMMENT '涨跌额',
    volume          BIGINT        NULL     COMMENT '成交量',
    amount          DECIMAL(16,2) NULL     COMMENT '成交额',
    turnover_rate   DECIMAL(6,2)  NULL     COMMENT '换手率(%)',
    volume_ratio    DECIMAL(6,2)  NULL     COMMENT '量比',
    pe              DECIMAL(10,3) NULL     COMMENT '市盈率',
    pb              DECIMAL(10,3) NULL     COMMENT '市净率',
    total_mv        DECIMAL(14,2) NULL     COMMENT '总市值',
    float_mv        DECIMAL(14,2) NULL     COMMENT '流通市值',
    snapshot_date   DATE          NOT NULL COMMENT '快照日期',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_date (code, snapshot_date),
    INDEX idx_code (code),
    INDEX idx_snapshot (snapshot_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '股票基本信息表_每日快照存储股票行情概要信息';

-- 5. 股票日线数据表
CREATE TABLE stock_daily
(
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code  VARCHAR(10)   NOT NULL COMMENT '股票代码',
    trade_date  DATE          NOT NULL COMMENT '交易日期',
    open        DECIMAL(10,3) NULL     COMMENT '开盘价',
    high        DECIMAL(10,3) NULL     COMMENT '最高价',
    low         DECIMAL(10,3) NULL     COMMENT '最低价',
    close       DECIMAL(10,3) NULL     COMMENT '收盘价',
    volume      BIGINT        NULL     COMMENT '成交量',
    amount      DECIMAL(16,2) NULL     COMMENT '成交额',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_date (stock_code, trade_date),
    INDEX idx_code_date (stock_code, trade_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '股票日线数据表_存储个股每日 OHLCV 行情数据';

-- 6. 行业表
CREATE TABLE industry
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code        VARCHAR(10) NOT NULL COMMENT '行业编码',
    name        VARCHAR(50) NOT NULL COMMENT '行业名称',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '行业表_行业板块编码与名称字典';

-- 7. 股票行业关联表
CREATE TABLE stock_industry
(
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code    VARCHAR(10) NOT NULL COMMENT '股票代码',
    industry_code VARCHAR(10) NOT NULL COMMENT '行业编码（关联 industry.code）',
    snap_date     DATE        NOT NULL COMMENT '快照日期',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted    TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_industry (stock_code, industry_code),
    INDEX idx_industry (industry_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '股票行业关联表_股票与行业的归属关系，快照式存储';

-- 8. 概念板块表
CREATE TABLE concept
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code        VARCHAR(10) NOT NULL COMMENT '概念编码',
    name        VARCHAR(50) NOT NULL COMMENT '概念名称',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '概念板块表_概念板块编码与名称字典';

-- 9. 股票概念关联表
CREATE TABLE stock_concept
(
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code   VARCHAR(10) NOT NULL COMMENT '股票代码',
    concept_code VARCHAR(10) NOT NULL COMMENT '概念编码（关联 concept.code）',
    snap_date    DATE        NOT NULL COMMENT '快照日期',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted   TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_concept (stock_code, concept_code),
    INDEX idx_concept (concept_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '股票概念关联表_股票与概念的归属关系，快照式存储';

-- 10. 分类变更日志表
CREATE TABLE classification_change_log
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code          VARCHAR(10) NOT NULL COMMENT '股票代码',
    classification_type VARCHAR(10) NOT NULL COMMENT 'INDUSTRY/CONCEPT',
    sector_code         VARCHAR(10) NOT NULL COMMENT '板块编码（行业/概念）',
    action              VARCHAR(10) NOT NULL COMMENT 'ADD/REMOVE',
    snap_date           DATE        NOT NULL COMMENT '快照日期',
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_stock_type_date (stock_code, classification_type, snap_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '分类变更日志表_记录股票行业/概念归属的新增与移除变更';

-- 11. 融资融券标的表
CREATE TABLE margin_stock
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code  VARCHAR(10) NOT NULL COMMENT '股票代码',
    stock_name  VARCHAR(20) NOT NULL COMMENT '股票名称',
    exchange    VARCHAR(4)  NOT NULL COMMENT 'SSE/SZSE',
    is_margin   TINYINT     NOT NULL DEFAULT 0 COMMENT '是否融资标的：1=是 0=否',
    is_short    TINYINT     NOT NULL DEFAULT 0 COMMENT '是否融券标的：1=是 0=否',
    snap_date   DATE        NOT NULL COMMENT '快照日期',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_date (stock_code, snap_date),
    INDEX idx_exchange (exchange)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '融资融券标的表_每日快照存储两融标的资格和属性';

-- 12. 融资融券每日明细表
CREATE TABLE margin_daily
(
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code       VARCHAR(10)   NOT NULL COMMENT '股票代码',
    trade_date       DATE          NOT NULL COMMENT '交易日期',
    exchange         VARCHAR(4)    NOT NULL COMMENT 'SSE/SZSE',
    margin_balance   DECIMAL(16,2) NULL     COMMENT '融资余额',
    margin_buy       DECIMAL(16,2) NULL     COMMENT '融资买入额',
    margin_repay     DECIMAL(16,2) NULL     COMMENT '融资偿还额',
    short_balance    DECIMAL(16,2) NULL     COMMENT '融券余额',
    short_sell_vol   BIGINT        NULL     COMMENT '融券卖出量',
    short_repay_vol  BIGINT        NULL     COMMENT '融券偿还量',
    short_remain_vol BIGINT        NULL     COMMENT '融券余量',
    total_balance    DECIMAL(16,2) NULL     COMMENT '两融总余额',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_date_exchange (stock_code, trade_date, exchange),
    INDEX idx_trade_date_exchange (trade_date, exchange),
    INDEX idx_stock_date (stock_code, trade_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '融资融券每日明细表_存储个股每日两融交易明细';
