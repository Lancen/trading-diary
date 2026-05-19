package com.tradingdiary.util;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tradingdiary.collection.CollectionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class BatchSqlRunner {

    private static final Logger log = LoggerFactory.getLogger(BatchSqlRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final ConcurrentMap<Class<?>, EntityMeta> metaCache = new ConcurrentHashMap<>();

    public BatchSqlRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public <T> int batchInsert(List<T> entities) {
        return batchInsert(entities, CollectionConstants.DB_BATCH_SIZE);
    }

    public <T> int batchInsert(List<T> entities, int batchSize) {
        if (entities.isEmpty()) return 0;
        EntityMeta meta = getMeta(entities.get(0).getClass());
        int total = 0;
        for (int i = 0; i < entities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entities.size());
            List<T> chunk = entities.subList(i, end);
            total += doInsertChunk(chunk, meta);
        }
        return total;
    }

    public <T> int batchUpdate(List<T> entities) {
        return batchUpdate(entities, CollectionConstants.DB_BATCH_SIZE);
    }

    public <T> int batchUpdate(List<T> entities, int batchSize) {
        if (entities.isEmpty()) return 0;
        EntityMeta meta = getMeta(entities.get(0).getClass());
        int total = 0;
        for (int i = 0; i < entities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entities.size());
            List<T> chunk = entities.subList(i, end);
            total += doUpdateChunk(chunk, meta);
        }
        return total;
    }

    private <T> int doInsertChunk(List<T> chunk, EntityMeta meta) {
        String sql = buildInsertSql(meta, chunk.size());
        Object[] params = buildInsertParams(chunk, meta);
        return jdbcTemplate.update(sql, params);
    }

    private <T> int doUpdateChunk(List<T> chunk, EntityMeta meta) {
        String sql = buildUpdateSql(meta, chunk.size());
        Object[] params = buildUpdateParams(chunk, meta);
        int rows = jdbcTemplate.update(sql, params);
        if (rows != chunk.size()) {
            log.warn("批量更新返回行数({})与预期({})不一致", rows, chunk.size());
        }
        return rows;
    }

    String buildInsertSql(EntityMeta meta, int rowCount) {
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(meta.tableName).append(" (");

        for (int i = 0; i < meta.insertFields.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(camelToSnake(meta.insertFields.get(i).getName()));
        }
        if (meta.createdAtField != null) sb.append(", created_at");
        if (meta.updatedAtField != null) sb.append(", updated_at");

        sb.append(") VALUES ");
        for (int r = 0; r < rowCount; r++) {
            if (r > 0) sb.append(", ");
            sb.append("(");
            int paramCount = meta.insertFields.size()
                    + (meta.createdAtField != null ? 1 : 0)
                    + (meta.updatedAtField != null ? 1 : 0);
            for (int p = 0; p < paramCount; p++) {
                if (p > 0) sb.append(", ");
                sb.append("?");
            }
            sb.append(")");
        }
        return sb.toString();
    }

    Object[] buildInsertParams(List<?> chunk, EntityMeta meta) {
        int paramsPerRow = meta.insertFields.size()
                + (meta.createdAtField != null ? 1 : 0)
                + (meta.updatedAtField != null ? 1 : 0);
        LocalDateTime now = LocalDateTime.now();
        Object[] params = new Object[chunk.size() * paramsPerRow];
        int idx = 0;
        for (Object entity : chunk) {
            for (Field field : meta.insertFields) {
                params[idx++] = toJdbcValue(getFieldValue(entity, field));
            }
            if (meta.createdAtField != null) {
                params[idx++] = now;
            }
            if (meta.updatedAtField != null) {
                params[idx++] = now;
            }
        }
        return params;
    }

    String buildUpdateSql(EntityMeta meta, int rowCount) {
        StringBuilder sb = new StringBuilder("UPDATE ");
        sb.append(meta.tableName).append(" SET ");

        List<Field> caseFields = meta.updateCaseFields;
        for (int ci = 0; ci < caseFields.size(); ci++) {
            if (ci > 0) sb.append(", ");
            String col = camelToSnake(caseFields.get(ci).getName());
            sb.append(col).append(" = CASE ").append(camelToSnake(meta.idField.getName()));
            for (int r = 0; r < rowCount; r++) {
                sb.append(" WHEN ? THEN ?");
            }
            sb.append(" END");
        }

        if (meta.updatedAtField != null) {
            sb.append(", updated_at = ?");
        }

        sb.append(" WHERE ").append(camelToSnake(meta.idField.getName())).append(" IN (");
        for (int r = 0; r < rowCount; r++) {
            if (r > 0) sb.append(", ");
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    Object[] buildUpdateParams(List<?> chunk, EntityMeta meta) {
        List<Field> caseFields = meta.updateCaseFields;
        int idParamCount = chunk.size();
        int caseParamCount = caseFields.size() * chunk.size() * 2;
        int updatedAtParamCount = meta.updatedAtField != null ? 1 : 0;
        Object[] params = new Object[caseParamCount + updatedAtParamCount + idParamCount];
        int idx = 0;

        for (Field field : caseFields) {
            for (Object entity : chunk) {
                params[idx++] = toJdbcValue(getFieldValue(entity, meta.idField));
                params[idx++] = toJdbcValue(getFieldValue(entity, field));
            }
        }

        if (meta.updatedAtField != null) {
            params[idx++] = LocalDateTime.now();
        }

        for (Object entity : chunk) {
            params[idx++] = toJdbcValue(getFieldValue(entity, meta.idField));
        }

        return params;
    }

    String camelToSnake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    Object toJdbcValue(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b ? 1 : 0;
        if (value instanceof LocalDate date) return java.sql.Date.valueOf(date);
        if (value instanceof LocalDateTime dt) return Timestamp.valueOf(dt);
        return value;
    }

    private EntityMeta getMeta(Class<?> clazz) {
        return metaCache.computeIfAbsent(clazz, BatchSqlRunner::buildMeta);
    }

    static EntityMeta buildMeta(Class<?> clazz) {
        TableName tableAnno = clazz.getAnnotation(TableName.class);
        if (tableAnno == null || tableAnno.value().isEmpty()) {
            throw new IllegalArgumentException(clazz.getName() + " 缺少 @TableName 注解");
        }

        Field[] declared = clazz.getDeclaredFields();
        Field idField = null;
        Field createdAtField = null;
        Field updatedAtField = null;
        List<Field> insertFields = new ArrayList<>();
        List<Field> updateCaseFields = new ArrayList<>();

        for (Field f : declared) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if ("serialVersionUID".equals(f.getName())) continue;

            if (f.isAnnotationPresent(TableId.class) && f.getAnnotation(TableId.class).type() == IdType.AUTO) {
                idField = f;
                continue;
            }

            if ("createdAt".equals(f.getName())) {
                createdAtField = f;
                continue;
            } else if ("updatedAt".equals(f.getName())) {
                updatedAtField = f;
                continue;
            }

            insertFields.add(f);
            updateCaseFields.add(f);
        }

        if (idField == null) {
            throw new IllegalArgumentException(clazz.getName() + " 缺少 @TableId(type = IdType.AUTO) 注解");
        }

        return new EntityMeta(
                tableAnno.value(),
                idField,
                insertFields,
                updateCaseFields,
                createdAtField,
                updatedAtField
        );
    }

    private Object getFieldValue(Object entity, Field field) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("无法读取字段 " + field.getName(), e);
        }
    }

    record EntityMeta(
            String tableName,
            Field idField,
            List<Field> insertFields,
            List<Field> updateCaseFields,
            Field createdAtField,
            Field updatedAtField
    ) {}
}