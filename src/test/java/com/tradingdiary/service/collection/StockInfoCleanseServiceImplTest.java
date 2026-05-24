package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockInfo;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.service.collection.impl.StockInfoCleanseServiceImpl;
import com.tradingdiary.util.BatchSqlRunner;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 股票基础信息清洗服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class StockInfoCleanseServiceImplTest {

    private static final LocalDate SNAP_DATE = LocalDate.of(2026, 5, 20);

    private StockInfoMapper stockInfoMapper;
    private BatchSqlRunner batchSqlRunner;
    private ObjectMapper objectMapper;
    private StockInfoCleanseServiceImpl service;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, StockInfo.class);
    }

    @BeforeEach
    void setUp() {
        stockInfoMapper = mock(StockInfoMapper.class);
        batchSqlRunner = mock(BatchSqlRunner.class);
        objectMapper = new ObjectMapper();
        service = new StockInfoCleanseServiceImpl(stockInfoMapper, batchSqlRunner, objectMapper);
    }

    @Test
    void shouldCleanseSuccessfully() {
        String rawJson = "[" +
                "{\"代码\": \"000001\", \"名称\": \"平安银行\", \"最新价\": \"12.80\", \"涨跌幅\": \"2.40\"," +
                " \"涨跌额\": \"0.30\", \"成交量\": \"52340100\", \"成交额\": \"660000000\"," +
                " \"换手率\": \"0.27\", \"量比\": \"1.15\", \"市盈率-动态\": \"6.50\"," +
                " \"市净率\": \"0.68\", \"总市值\": \"248000000000\", \"流通市值\": \"248000000000\"}," +
                "{\"代码\": \"000002\", \"名称\": \"万科A\", \"最新价\": \"8.30\", \"涨跌幅\": \"-1.20\"," +
                " \"涨跌额\": \"-0.10\", \"成交量\": \"30120000\", \"成交额\": \"250000000\"," +
                " \"换手率\": \"0.15\", \"量比\": \"0.80\", \"市盈率-动态\": \"8.20\"," +
                " \"市净率\": \"0.55\", \"总市值\": \"97000000000\", \"流通市值\": \"97000000000\"}" +
                "]";

        when(stockInfoMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(2);

        int result = service.cleanse(rawJson, SNAP_DATE);

        assertThat(result).isEqualTo(2);
        verify(batchSqlRunner).batchInsert(any());
    }

    @Test
    void shouldReturnZeroWhenEmptyData() {
        String rawJson = "[]";

        int result = service.cleanse(rawJson, SNAP_DATE);

        assertThat(result).isEqualTo(0);
        verify(batchSqlRunner, never()).batchInsert(any());
        verify(batchSqlRunner, never()).batchUpdate(any());
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String invalidJson = "not a json";

        assertThatThrownBy(() -> service.cleanse(invalidJson, SNAP_DATE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("解析股票基础信息数据失败");
    }

    @Test
    void shouldUpdateExistingRecords() {
        String rawJson = "[" +
                "{\"代码\": \"000001\", \"名称\": \"平安银行\", \"最新价\": \"12.80\", \"涨跌幅\": \"2.40\"," +
                " \"涨跌额\": \"0.30\", \"成交量\": \"52340100\", \"成交额\": \"660000000\"," +
                " \"换手率\": \"0.27\", \"量比\": \"1.15\", \"市盈率-动态\": \"6.50\"," +
                " \"市净率\": \"0.68\", \"总市值\": \"248000000000\", \"流通市值\": \"248000000000\"}" +
                "]";

        StockInfo existing = new StockInfo();
        existing.setId(1L);
        existing.setCode("000001");
        existing.setSnapshotDate(SNAP_DATE);

        when(stockInfoMapper.selectList(any())).thenReturn(List.of(existing));
        when(batchSqlRunner.batchUpdate(any())).thenReturn(1);

        int result = service.cleanse(rawJson, SNAP_DATE);

        assertThat(result).isEqualTo(1);
        verify(batchSqlRunner).batchUpdate(any());
    }

    @Test
    void shouldReturnZeroWhenNonArrayJson() {
        String rawJson = "{\"key\": \"value\"}";

        int result = service.cleanse(rawJson, SNAP_DATE);

        assertThat(result).isEqualTo(0);
    }
}
