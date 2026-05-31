-- V13: 财务报表数据表 — 利润表、资产负债表、现金流量表、财务指标、每日估值

CREATE TABLE stock_income
(
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code        VARCHAR(10)   NOT NULL COMMENT '股票代码（6位纯数字）',
    report_date       DATE          NOT NULL COMMENT '报告期（如2025-03-31）',
    report_type       VARCHAR(4)    NOT NULL COMMENT '报告类型：Q1/H1/Q3/Annual',
    total_revenue     DECIMAL(18,2) NULL COMMENT '营业总收入',
    operating_revenue DECIMAL(18,2) NULL COMMENT '营业收入',
    total_cost        DECIMAL(18,2) NULL COMMENT '营业总成本',
    operating_cost    DECIMAL(18,2) NULL COMMENT '营业成本',
    rd_expense        DECIMAL(18,2) NULL COMMENT '研发费用',
    selling_expense   DECIMAL(18,2) NULL COMMENT '销售费用',
    admin_expense     DECIMAL(18,2) NULL COMMENT '管理费用',
    finance_expense   DECIMAL(18,2) NULL COMMENT '财务费用',
    operating_profit  DECIMAL(18,2) NULL COMMENT '营业利润',
    total_profit      DECIMAL(18,2) NULL COMMENT '利润总额',
    net_profit        DECIMAL(18,2) NULL COMMENT '净利润',
    np_parent         DECIMAL(18,2) NULL COMMENT '归母净利润',
    np_minority       DECIMAL(18,2) NULL COMMENT '少数股东损益',
    deducted_np       DECIMAL(18,2) NULL COMMENT '扣非净利润',
    basic_eps         DECIMAL(10,4) NULL COMMENT '基本每股收益',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_report (stock_code, report_date, report_type),
    INDEX idx_report_date (report_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '利润表数据';

CREATE TABLE stock_balance_sheet
(
    id                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code             VARCHAR(10)   NOT NULL COMMENT '股票代码（6位纯数字）',
    report_date            DATE          NOT NULL COMMENT '报告期',
    report_type            VARCHAR(4)    NOT NULL COMMENT '报告类型：Q1/H1/Q3/Annual',
    total_assets           DECIMAL(18,2) NULL COMMENT '资产总计',
    current_assets         DECIMAL(18,2) NULL COMMENT '流动资产合计',
    cash                   DECIMAL(18,2) NULL COMMENT '货币资金',
    accounts_receivable    DECIMAL(18,2) NULL COMMENT '应收账款',
    inventory              DECIMAL(18,2) NULL COMMENT '存货',
    fixed_assets           DECIMAL(18,2) NULL COMMENT '固定资产',
    goodwill               DECIMAL(18,2) NULL COMMENT '商誉',
    total_liabilities      DECIMAL(18,2) NULL COMMENT '负债合计',
    current_liabilities    DECIMAL(18,2) NULL COMMENT '流动负债合计',
    interest_bearing_debt  DECIMAL(18,2) NULL COMMENT '有息负债',
    total_equity           DECIMAL(18,2) NULL COMMENT '所有者权益合计',
    equity_parent          DECIMAL(18,2) NULL COMMENT '归母所有者权益',
    minority_interest      DECIMAL(18,2) NULL COMMENT '少数股东权益',
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at             DATETIME      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted             TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_report (stock_code, report_date, report_type),
    INDEX idx_report_date (report_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '资产负债表数据';

CREATE TABLE stock_cash_flow
(
    id                          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code                  VARCHAR(10)   NOT NULL COMMENT '股票代码（6位纯数字）',
    report_date                 DATE          NOT NULL COMMENT '报告期',
    report_type                 VARCHAR(4)    NOT NULL COMMENT '报告类型：Q1/H1/Q3/Annual',
    operating_cash_flow         DECIMAL(18,2) NULL COMMENT '经营活动现金流净额',
    investing_cash_flow         DECIMAL(18,2) NULL COMMENT '投资活动现金流净额',
    financing_cash_flow         DECIMAL(18,2) NULL COMMENT '筹资活动现金流净额',
    capex                       DECIMAL(18,2) NULL COMMENT '资本开支（购建固定资产等）',
    depreciation                DECIMAL(18,2) NULL COMMENT '折旧与摊销',
    cash_dividend_paid          DECIMAL(18,2) NULL COMMENT '分配股利支付的现金',
    free_cash_flow              DECIMAL(18,2) NULL COMMENT '自由现金流（经营-资本开支）',
    created_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at                  DATETIME      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted                  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_report (stock_code, report_date, report_type),
    INDEX idx_report_date (report_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '现金流量表数据';

CREATE TABLE stock_financial_indicator
(
    id                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code             VARCHAR(10)   NOT NULL COMMENT '股票代码（6位纯数字）',
    report_date            DATE          NOT NULL COMMENT '报告期',
    report_type            VARCHAR(4)    NOT NULL COMMENT '报告类型：Q1/H1/Q3/Annual',
    gross_margin           DECIMAL(8,4)  NULL COMMENT '毛利率(%)',
    net_margin             DECIMAL(8,4)  NULL COMMENT '净利率(%)',
    roe                    DECIMAL(8,4)  NULL COMMENT '净资产收益率ROE(%)',
    roa                    DECIMAL(8,4)  NULL COMMENT '总资产收益率ROA(%)',
    debt_ratio             DECIMAL(8,4)  NULL COMMENT '资产负债率(%)',
    current_ratio          DECIMAL(10,4) NULL COMMENT '流动比率',
    quick_ratio            DECIMAL(10,4) NULL COMMENT '速动比率',
    ar_turnover_days       DECIMAL(10,2) NULL COMMENT '应收账款周转天数',
    inventory_turnover_days DECIMAL(10,2) NULL COMMENT '存货周转天数',
    ocf_to_np              DECIMAL(10,4) NULL COMMENT '经营现金流/净利润',
    revenue_yoy            DECIMAL(8,4)  NULL COMMENT '营收同比增长率(%)',
    np_yoy                 DECIMAL(8,4)  NULL COMMENT '净利润同比增长率(%)',
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at             DATETIME      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted             TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_report (stock_code, report_date, report_type),
    INDEX idx_report_date (report_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '财务指标数据';

CREATE TABLE stock_daily_valuation
(
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stock_code      VARCHAR(10)   NOT NULL COMMENT '股票代码（6位纯数字）',
    trade_date      DATE          NOT NULL COMMENT '交易日期',
    pe_ttm          DECIMAL(10,2) NULL COMMENT '滚动市盈率TTM',
    pe_static       DECIMAL(10,2) NULL COMMENT '静态市盈率',
    pb              DECIMAL(10,2) NULL COMMENT '市净率',
    ps_ttm          DECIMAL(10,2) NULL COMMENT '市销率TTM',
    total_mv        DECIMAL(18,2) NULL COMMENT '总市值(元)',
    circ_mv         DECIMAL(18,2) NULL COMMENT '流通市值(元)',
    turnover_rate   DECIMAL(8,4)  NULL COMMENT '换手率(%)',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_date (stock_code, trade_date),
    INDEX idx_trade_date (trade_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '每日估值数据';
