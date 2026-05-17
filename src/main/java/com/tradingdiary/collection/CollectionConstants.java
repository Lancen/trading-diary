package com.tradingdiary.collection;

/**
 * 数据采集模块共享常量。
 */
public final class CollectionConstants {

    private CollectionConstants() {}

    /** 数据库批量写入每批记录数（触发 JDBC flush） */
    public static final int DB_BATCH_SIZE = 500;

    /** 补采时累计多少只股票后执行一次批量写入 */
    public static final int BACKFILL_ACCUMULATE_SIZE = 50;
}
