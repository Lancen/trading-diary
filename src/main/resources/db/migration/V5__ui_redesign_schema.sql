-- V5: UI 重构 — 日志字段 + margin_daily 变化 + index_daily 表

-- 1. data_collection_log 增加地址/参数/备注字段
ALTER TABLE data_collection_log
    ADD COLUMN request_url    VARCHAR(512) NULL COMMENT '外部 API 请求地址',
    ADD COLUMN request_params VARCHAR(512) NULL COMMENT '请求参数',
    ADD COLUMN remark         VARCHAR(256) NULL COMMENT '备注';

-- 2. margin_daily 增加环比变化字段
ALTER TABLE margin_daily
    ADD COLUMN margin_change DECIMAL(16,2) NULL COMMENT '融资余额较上一交易日变化',
    ADD COLUMN short_change  DECIMAL(16,2) NULL COMMENT '融券余额较上一交易日变化';

-- 3. 新建指数日线表
CREATE TABLE index_daily
(
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    index_code  VARCHAR(10)   NOT NULL COMMENT '指数代码(sh000001/sz399001)',
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
    is_deleted  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_date (index_code, trade_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '指数日线数据表';
