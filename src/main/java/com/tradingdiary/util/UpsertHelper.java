package com.tradingdiary.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Upsert 分区辅助工具，将新实体列表按 key 与已有记录分区为 toInsert/toUpdate 后批量写入
 */
public final class UpsertHelper {

    private UpsertHelper() {}

    /**
     * Upsert 分区结果
     */
    public static class PartitionResult<T> {
        private final List<T> toInsert;
        private final List<T> toUpdate;

        public PartitionResult(List<T> toInsert, List<T> toUpdate) {
            this.toInsert = toInsert;
            this.toUpdate = toUpdate;
        }

        public List<T> getToInsert() { return toInsert; }
        public List<T> getToUpdate() { return toUpdate; }
        public boolean hasInserts() { return !toInsert.isEmpty(); }
        public boolean hasUpdates() { return !toUpdate.isEmpty(); }
    }

    /**
     * 将新实体列表与已有记录按 key 分区为 toInsert/toUpdate
     *
     * @param newEntities    新解析的实体列表
     * @param existingList   已有记录列表
     * @param keyExtractor   从实体提取业务 key 的函数（如 MarginDaily::getStockCode）
     * @param existingKeyFn  从已有实体提取 key 的函数（通常与 keyExtractor 相同）
     * @param idCopier       将已有实体的 ID 复制到新实体的函数（用于更新场景）
     * @return 分区结果
     */
    public static <T, K> PartitionResult<T> partition(List<T> newEntities,
                                                       List<T> existingList,
                                                       Function<T, K> keyExtractor,
                                                       Function<T, K> existingKeyFn,
                                                       IdCopier<T> idCopier) {
        Map<K, T> existingByKey = existingList.stream()
                .collect(Collectors.toMap(existingKeyFn, e -> e, (a, b) -> a));

        List<T> toInsert = new ArrayList<>();
        List<T> toUpdate = new ArrayList<>();

        for (T entity : newEntities) {
            K key = keyExtractor.apply(entity);
            T existingEntity = existingByKey.get(key);
            if (existingEntity != null) {
                idCopier.copyId(existingEntity, entity);
                toUpdate.add(entity);
            } else {
                toInsert.add(entity);
            }
        }

        return new PartitionResult<>(toInsert, toUpdate);
    }

    /**
     * 分区并批量写入（简化版，key 提取函数与已有实体 key 函数相同）
     *
     * @param newEntities    新解析的实体列表
     * @param existingList   已有记录列表
     * @param keyExtractor   从实体提取业务 key 的函数
     * @param idCopier       将已有实体的 ID 复制到新实体的函数
     * @param batchSqlRunner 批量写入执行器
     * @return 写入的总行数
     */
    public static <T, K> int partitionAndSave(List<T> newEntities,
                                               List<T> existingList,
                                               Function<T, K> keyExtractor,
                                               IdCopier<T> idCopier,
                                               BatchSqlRunner batchSqlRunner) {
        PartitionResult<T> result = partition(newEntities, existingList, keyExtractor, keyExtractor, idCopier);
        int count = 0;
        if (result.hasInserts()) count += batchSqlRunner.batchInsert(result.getToInsert());
        if (result.hasUpdates()) count += batchSqlRunner.batchUpdate(result.getToUpdate());
        return count;
    }

    /**
     * ID 复制函数接口，将已有实体的主键复制到新实体（用于更新场景）
     */
    @FunctionalInterface
    public interface IdCopier<T> {
        void copyId(T source, T target);
    }
}