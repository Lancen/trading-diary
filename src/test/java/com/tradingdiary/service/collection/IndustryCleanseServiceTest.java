package com.tradingdiary.service.collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.ClassificationChangeLog;
import com.tradingdiary.entity.StockIndustry;
import com.tradingdiary.mapper.ClassificationChangeLogMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.mapper.StockIndustryMapper;
import com.tradingdiary.util.BatchSqlRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustryCleanseServiceTest {

    private static final String INDUSTRY_CODE = "BK001";
    private static final LocalDate SNAP_DATE = LocalDate.of(2026, 5, 15);

    private IndustryMapper industryMapper;
    private StockIndustryMapper stockIndustryMapper;
    private ClassificationChangeLogMapper changeLogMapper;
    private BatchSqlRunner batchSqlRunner;
    private ObjectMapper objectMapper;
    private IndustryCleanseService service;

    @BeforeEach
    void setUp() {
        industryMapper = mock(IndustryMapper.class);
        stockIndustryMapper = mock(StockIndustryMapper.class);
        changeLogMapper = mock(ClassificationChangeLogMapper.class);
        batchSqlRunner = mock(BatchSqlRunner.class);
        objectMapper = new ObjectMapper();
        service = new IndustryCleanseService(
                industryMapper, stockIndustryMapper, changeLogMapper, batchSqlRunner, objectMapper);
    }

    // ──────────────────────────────────────────────
    // Test: ADD — new stocks in today's data, not in DB
    // ──────────────────────────────────────────────

    @Test
    void shouldInsertNewStocksAndLogAdd() {
        List<StockIndustry> dbRelations = Arrays.asList(
                buildDbRelation("000001"),
                buildDbRelation("000002")
        );
        when(stockIndustryMapper.selectList(any())).thenReturn(dbRelations);

        String rawJson = jsonArray(jsonStock("000001"), jsonStock("000002"), jsonStock("000003"));

        int result = service.cleanseCons(rawJson, INDUSTRY_CODE, SNAP_DATE);

        assertThat(result).isEqualTo(1);

        // 1 ADD change log
        ArgumentCaptor<ClassificationChangeLog> logCaptor = ArgumentCaptor.forClass(ClassificationChangeLog.class);
        verify(changeLogMapper, times(1)).insert(logCaptor.capture());
        ClassificationChangeLog log = logCaptor.getValue();
        assertThat(log.getStockCode()).isEqualTo("000003");
        assertThat(log.getClassificationType()).isEqualTo("INDUSTRY");
        assertThat(log.getSectorCode()).isEqualTo(INDUSTRY_CODE);
        assertThat(log.getAction()).isEqualTo("ADD");
        assertThat(log.getSnapDate()).isEqualTo(SNAP_DATE);
    }

    // ──────────────────────────────────────────────
    // Test: REMOVE — stocks in DB but not in today's data
    // ──────────────────────────────────────────────

    @Test
    void shouldSoftDeleteRemovedStocksAndLogRemove() {
        StockIndustry db1 = buildDbRelation("000001");
        StockIndustry db2 = buildDbRelation("000002");
        StockIndustry db3 = buildDbRelation("000003");
        List<StockIndustry> dbRelations = Arrays.asList(db1, db2, db3);
        when(stockIndustryMapper.selectList(any())).thenReturn(dbRelations);

        String rawJson = jsonArray(jsonStock("000001"), jsonStock("000002"));

        int result = service.cleanseCons(rawJson, INDUSTRY_CODE, SNAP_DATE);

        assertThat(result).isEqualTo(1);

        // 1 REMOVE change log
        ArgumentCaptor<ClassificationChangeLog> logCaptor = ArgumentCaptor.forClass(ClassificationChangeLog.class);
        verify(changeLogMapper, times(1)).insert(logCaptor.capture());
        ClassificationChangeLog log = logCaptor.getValue();
        assertThat(log.getStockCode()).isEqualTo("000003");
        assertThat(log.getClassificationType()).isEqualTo("INDUSTRY");
        assertThat(log.getSectorCode()).isEqualTo(INDUSTRY_CODE);
        assertThat(log.getAction()).isEqualTo("REMOVE");
        assertThat(log.getSnapDate()).isEqualTo(SNAP_DATE);
    }

    // ──────────────────────────────────────────────
    // Test: Mixed — some ADD, some REMOVE, some unchanged
    // ──────────────────────────────────────────────

    @Test
    void shouldHandleMixedAddRemoveUnchanged() {
        StockIndustry db1 = buildDbRelation("000001");
        StockIndustry db2 = buildDbRelation("000002");
        StockIndustry db3 = buildDbRelation("000003");
        List<StockIndustry> dbRelations = Arrays.asList(db1, db2, db3);
        when(stockIndustryMapper.selectList(any())).thenReturn(dbRelations);

        String rawJson = jsonArray(jsonStock("000001"), jsonStock("000003"), jsonStock("000004"));

        int result = service.cleanseCons(rawJson, INDUSTRY_CODE, SNAP_DATE);

        // 2 changes: 1 ADD (000004), 1 REMOVE (000002)
        assertThat(result).isEqualTo(2);

        // 2 change logs: one ADD + one REMOVE
        verify(changeLogMapper, times(2)).insert(anyLog());
    }

    // ──────────────────────────────────────────────
    // Test: No-change — all stocks unchanged
    // ──────────────────────────────────────────────

    @Test
    void shouldSkipWhenNoChanges() {
        List<StockIndustry> dbRelations = Arrays.asList(
                buildDbRelation("000001"),
                buildDbRelation("000002")
        );
        when(stockIndustryMapper.selectList(any())).thenReturn(dbRelations);

        String rawJson = jsonArray(jsonStock("000001"), jsonStock("000002"));

        int result = service.cleanseCons(rawJson, INDUSTRY_CODE, SNAP_DATE);

        assertThat(result).isEqualTo(0);

        // No change logs inserted
        verify(changeLogMapper, never()).insert(anyLog());
    }

    // ──────────────────────────────────────────────
    // Test: All new — DB is empty, everything is ADD
    // ──────────────────────────────────────────────

    @Test
    void shouldInsertAllWhenDbEmpty() {
        when(stockIndustryMapper.selectList(any())).thenReturn(Collections.emptyList());

        String rawJson = jsonArray(jsonStock("000001"), jsonStock("000002"), jsonStock("000003"));

        int result = service.cleanseCons(rawJson, INDUSTRY_CODE, SNAP_DATE);

        assertThat(result).isEqualTo(3);

        // 3 ADD change logs
        verify(changeLogMapper, times(3)).insert(anyLog());
    }

    // ──────────────────────────────────────────────
    // Test: All removed — DB has stocks, today empty
    // ──────────────────────────────────────────────

    @Test
    void shouldReturnZeroWhenNoConstituentsParsed() {
        String rawJson = "[]";

        int result = service.cleanseCons(rawJson, INDUSTRY_CODE, SNAP_DATE);

        assertThat(result).isEqualTo(0);
        verify(changeLogMapper, never()).insert(anyLog());
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private static String jsonArray(String... items) {
        return "[" + String.join(",", items) + "]";
    }

    private static String jsonStock(String code) {
        return "{\"代码\": \"" + code + "\"}";
    }

    private StockIndustry buildDbRelation(String stockCode) {
        StockIndustry si = new StockIndustry();
        si.setId((long) stockCode.hashCode());
        si.setStockCode(stockCode);
        si.setIndustryCode(INDUSTRY_CODE);
        si.setSnapDate(SNAP_DATE.minusDays(1));
        si.setIsDeleted(false);
        return si;
    }

    /** Typed matcher to avoid MyBatis-Plus BaseMapper overload ambiguity. */
    private static ClassificationChangeLog anyLog() {
        return org.mockito.ArgumentMatchers.any(ClassificationChangeLog.class);
    }
}
