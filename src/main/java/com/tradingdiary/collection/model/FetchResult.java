package com.tradingdiary.collection.model;

/**
 * FETCH 阶段结果，区分单条 JSON 和多板块两种数据模式
 */
public class FetchResult {

    /** FETCH 结果类型 */
    public enum Type {
        /** 标准 FETCH：返回单个 JSON 字符串（由编排器保存到 raw_data） */
        SINGLE,
        /** 板块指数 FETCH：handler 已自行保存多条 raw_data，只返回 collectionLogId */
        MULTI_SECTOR
    }

    private final Type type;
    private final String rawJson;
    private final Long collectionLogId;
    private final int sectorCount;

    /** 标准 FETCH 结果：单个 JSON */
    public static FetchResult single(String rawJson) {
        return new FetchResult(Type.SINGLE, rawJson, null, 0);
    }

    /** 板块指数 FETCH 结果：已保存多条 raw_data */
    public static FetchResult multiSector(Long collectionLogId, int sectorCount) {
        return new FetchResult(Type.MULTI_SECTOR, null, collectionLogId, sectorCount);
    }

    private FetchResult(Type type, String rawJson, Long collectionLogId, int sectorCount) {
        this.type = type;
        this.rawJson = rawJson;
        this.collectionLogId = collectionLogId;
        this.sectorCount = sectorCount;
    }

    public Type getType() { return type; }
    public String getRawJson() { return rawJson; }
    public Long getCollectionLogId() { return collectionLogId; }
    public int getSectorCount() { return sectorCount; }
    public boolean isSuccess() { return type == Type.SINGLE && rawJson != null || type == Type.MULTI_SECTOR && collectionLogId != null; }
}