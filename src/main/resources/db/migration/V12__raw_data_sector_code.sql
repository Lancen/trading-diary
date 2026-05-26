ALTER TABLE raw_data ADD COLUMN sector_code VARCHAR(20) NULL COMMENT '板块编码（板块指数K线专用）' AFTER source;
CREATE INDEX idx_raw_sector_code ON raw_data (sector_code);
