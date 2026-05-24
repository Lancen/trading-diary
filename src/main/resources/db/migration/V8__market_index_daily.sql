CREATE TABLE market_index_daily
(
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    index_code  VARCHAR(10)   NOT NULL COMMENT '指数代码（如 sh000001）',
    trade_date  DATE          NOT NULL COMMENT '交易日期',
    open        DECIMAL(10,3) NULL     COMMENT '开盘价',
    high        DECIMAL(10,3) NULL     COMMENT '最高价',
    low         DECIMAL(10,3) NULL     COMMENT '最低价',
    close       DECIMAL(10,3) NULL     COMMENT '收盘价',
    volume      BIGINT        NULL     COMMENT '成交量',
    amount      DECIMAL(16,2) NULL     COMMENT '成交额',
    change_pct  DECIMAL(6,2)  NULL     COMMENT '涨跌幅(%)',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_date (index_code, trade_date),
    INDEX idx_code_date (index_code, trade_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '宽基指数日线数据表_存储上证指数/沪深300等宽基指数每日OHLCV行情';
