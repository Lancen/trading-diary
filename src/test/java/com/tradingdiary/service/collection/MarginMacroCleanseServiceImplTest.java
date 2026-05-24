package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.MarginMacro;
import com.tradingdiary.mapper.MarginMacroMapper;
import com.tradingdiary.service.collection.impl.MarginMacroCleanseServiceImpl;
import com.tradingdiary.util.BatchSqlRunner;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * 两融总量清洗服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class MarginMacroCleanseServiceImplTest {

    private MarginMacroMapper mapper;
    private BatchSqlRunner batchSqlRunner;
    private ObjectMapper objectMapper;
    private MarginMacroCleanseServiceImpl service;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, MarginMacro.class);
    }

    @BeforeEach
    void setUp() {
        mapper = mock(MarginMacroMapper.class);
        batchSqlRunner = mock(BatchSqlRunner.class);
        objectMapper = new ObjectMapper();
        service = new MarginMacroCleanseServiceImpl(mapper, batchSqlRunner, objectMapper);
    }

    @Test
    void shouldCleanseSuccessfully() {
        String rawJson = "[" +
                "{\"日期\": \"2026-05-20\", \"融资买入额\": \"50000000\", \"融资余额\": \"8000000000\"," +
                " \"融券卖出量\": \"1000000\", \"融券余量\": \"5000000\", \"融券余额\": \"600000000\"," +
                " \"融资融券余额\": \"8600000000\"}," +
                "{\"日期\": \"2026-05-19\", \"融资买入额\": \"45000000\", \"融资余额\": \"7800000000\"," +
                " \"融券卖出量\": \"900000\", \"融券余量\": \"4800000\", \"融券余额\": \"580000000\"," +
                " \"融资融券余额\": \"8380000000\"}" +
                "]";

        when(mapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(2);

        int result = service.cleanse(rawJson, "SSE");

        assertThat(result).isEqualTo(2);
        verify(batchSqlRunner).batchInsert(any());
    }

    @Test
    void shouldReturnZeroWhenEmptyData() {
        String rawJson = "[]";

        int result = service.cleanse(rawJson, "SSE");

        assertThat(result).isEqualTo(0);
        verify(batchSqlRunner, never()).batchInsert(any());
        verify(batchSqlRunner, never()).batchUpdate(any());
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String invalidJson = "not a json";

        assertThatThrownBy(() -> service.cleanse(invalidJson, "SSE"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("解析两融总量数据失败");
    }

    @Test
    void shouldUpdateExistingRecords() {
        String rawJson = "[" +
                "{\"日期\": \"2026-05-20\", \"融资买入额\": \"50000000\", \"融资余额\": \"8000000000\"," +
                " \"融券卖出量\": \"1000000\", \"融券余量\": \"5000000\", \"融券余额\": \"600000000\"," +
                " \"融资融券余额\": \"8600000000\"}" +
                "]";

        MarginMacro existing = new MarginMacro();
        existing.setId(1L);
        existing.setTradeDate(java.time.LocalDate.of(2026, 5, 20));
        existing.setExchange("SSE");

        when(mapper.selectList(any())).thenReturn(List.of(existing));
        when(batchSqlRunner.batchUpdate(any())).thenReturn(1);

        int result = service.cleanse(rawJson, "SSE");

        assertThat(result).isEqualTo(1);
        verify(batchSqlRunner).batchUpdate(any());
    }

    @Test
    void shouldSkipRecordsWithInvalidDate() {
        String rawJson = "[" +
                "{\"日期\": \"invalid-date\", \"融资买入额\": \"50000000\", \"融资余额\": \"8000000000\"," +
                " \"融券卖出量\": \"1000000\", \"融券余量\": \"5000000\", \"融券余额\": \"600000000\"," +
                " \"融资融券余额\": \"8600000000\"}" +
                "]";

        int result = service.cleanse(rawJson, "SSE");

        assertThat(result).isEqualTo(0);
    }
}
