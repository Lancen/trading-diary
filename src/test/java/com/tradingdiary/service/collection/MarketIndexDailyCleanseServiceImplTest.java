package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.MarketIndexDaily;
import com.tradingdiary.mapper.MarketIndexDailyMapper;
import com.tradingdiary.service.collection.impl.MarketIndexDailyCleanseServiceImpl;
import com.tradingdiary.util.BatchSqlRunner;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

@ExtendWith(MockitoExtension.class)
class MarketIndexDailyCleanseServiceImplTest {

    private MarketIndexDailyMapper marketIndexDailyMapper;
    private BatchSqlRunner batchSqlRunner;
    private ObjectMapper objectMapper;
    private MarketIndexDailyCleanseServiceImpl service;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, MarketIndexDaily.class);
    }

    @BeforeEach
    void setUp() {
        marketIndexDailyMapper = mock(MarketIndexDailyMapper.class);
        batchSqlRunner = mock(BatchSqlRunner.class);
        objectMapper = new ObjectMapper();
        service = new MarketIndexDailyCleanseServiceImpl(marketIndexDailyMapper, batchSqlRunner, objectMapper);
    }

    @Test
    void shouldCleanseWithValidJson() {
        String rawJson = "[" +
                "{\"date\": \"2026-05-20\", \"open\": \"3200.50\", \"high\": \"3250.00\", \"low\": \"3180.30\"," +
                " \"close\": \"3230.80\", \"volume\": \"350000000\", \"amount\": \"45000000000\"}," +
                "{\"date\": \"2026-05-19\", \"open\": \"3180.00\", \"high\": \"3210.50\", \"low\": \"3170.20\"," +
                " \"close\": \"3200.50\", \"volume\": \"320000000\", \"amount\": \"41000000000\"}" +
                "]";

        when(marketIndexDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(2);

        int result = service.cleanse(rawJson, "sh000001");

        assertThat(result).isEqualTo(2);
        verify(batchSqlRunner).batchInsert(any());
    }

    @Test
    void shouldReturnZeroWhenEmptyData() {
        String rawJson = "[]";

        int result = service.cleanse(rawJson, "sh000001");

        assertThat(result).isEqualTo(0);
        verify(batchSqlRunner, never()).batchInsert(any());
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String invalidJson = "not a json";

        assertThatThrownBy(() -> service.cleanse(invalidJson, "sh000001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("解析宽基指数日线数据失败");
    }

    @Test
    void shouldComputeChangePctFromPreviousDay() {
        MarketIndexDaily prev = new MarketIndexDaily();
        prev.setIndexCode("sh000001");
        prev.setTradeDate(LocalDate.of(2026, 5, 19));
        prev.setClose(new BigDecimal("3200.50"));

        when(marketIndexDailyMapper.selectList(any())).thenReturn(List.of(prev));
        when(batchSqlRunner.batchInsert(any())).thenAnswer(invocation -> {
            List<MarketIndexDaily> list = invocation.getArgument(0);
            assertThat(list.get(0).getChangePct()).isEqualByComparingTo(new BigDecimal("0.95"));
            return list.size();
        });

        String rawJson = "[" +
                "{\"date\": \"2026-05-20\", \"open\": \"3200.50\", \"high\": \"3250.00\", \"low\": \"3180.30\"," +
                " \"close\": \"3230.80\", \"volume\": \"350000000\", \"amount\": \"45000000000\"}" +
                "]";

        int result = service.cleanse(rawJson, "sh000001");

        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldSetChangePctNullWhenNoPreviousDay() {
        when(marketIndexDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenAnswer(invocation -> {
            List<MarketIndexDaily> list = invocation.getArgument(0);
            assertThat(list.get(0).getChangePct()).isNull();
            return list.size();
        });

        String rawJson = "[" +
                "{\"date\": \"2026-05-20\", \"open\": \"3200.50\", \"high\": \"3250.00\", \"low\": \"3180.30\"," +
                " \"close\": \"3230.80\", \"volume\": \"350000000\", \"amount\": \"45000000000\"}" +
                "]";

        int result = service.cleanse(rawJson, "sh000001");

        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldUpdateExistingRecords() {
        MarketIndexDaily existing = new MarketIndexDaily();
        existing.setId(1L);
        existing.setIndexCode("sh000001");
        existing.setTradeDate(LocalDate.of(2026, 5, 20));
        existing.setClose(new BigDecimal("3200.00"));

        when(marketIndexDailyMapper.selectList(any())).thenReturn(List.of(existing));
        when(batchSqlRunner.batchUpdate(any())).thenReturn(1);

        String rawJson = "[" +
                "{\"date\": \"2026-05-20\", \"open\": \"3200.50\", \"high\": \"3250.00\", \"low\": \"3180.30\"," +
                " \"close\": \"3230.80\", \"volume\": \"350000000\", \"amount\": \"45000000000\"}" +
                "]";

        int result = service.cleanse(rawJson, "sh000001");

        assertThat(result).isEqualTo(1);
        verify(batchSqlRunner).batchUpdate(any());
    }
}
