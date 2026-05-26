ALTER TABLE stock_info
    ADD COLUMN market VARCHAR(4) NULL COMMENT '市场标识: SH/SZ/BJ' AFTER code,
    ADD COLUMN stock_code VARCHAR(6) NULL COMMENT '纯数字股票代码' AFTER market;

UPDATE stock_info
SET market   = UPPER(SUBSTRING(code, 1, 2)),
    stock_code = SUBSTRING(code, 3)
WHERE code REGEXP '^(sh|sz|bj)[0-9]{6}$';

UPDATE stock_info
SET market   = 'OTHER',
    stock_code = code
WHERE stock_code IS NULL;

ALTER TABLE stock_info
    MODIFY COLUMN market VARCHAR(4) NOT NULL COMMENT '市场标识: SH/SZ/BJ/OTHER',
    MODIFY COLUMN stock_code VARCHAR(6) NOT NULL COMMENT '纯数字股票代码';

CREATE INDEX idx_stock_info_stock_code ON stock_info (stock_code);
