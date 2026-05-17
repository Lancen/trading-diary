CREATE TABLE margin_macro (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_date  DATE NOT NULL,
    exchange    VARCHAR(4) NOT NULL COMMENT 'SSE / SZSE',
    margin_buy  DECIMAL(24,2) COMMENT '融资买入额(元)',
    margin_balance DECIMAL(24,2) COMMENT '融资余额(元)',
    short_sell_vol  BIGINT COMMENT '融券卖出量(股)',
    short_remain_vol BIGINT COMMENT '融券余量(股)',
    short_balance   DECIMAL(24,2) COMMENT '融券余额(元)',
    total_balance   DECIMAL(24,2) COMMENT '融资融券余额(元)',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date_exchange (trade_date, exchange)
);
