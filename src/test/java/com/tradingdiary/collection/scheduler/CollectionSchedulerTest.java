package com.tradingdiary.collection.scheduler;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tradingdiary.collection.orchestrator.CollectionOrchestrator;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 数据采集调度器单元测试
 */
@ExtendWith(MockitoExtension.class)
class CollectionSchedulerTest {

    private CollectionOrchestrator orchestrator;
    private TradeCalendarMapper tradeCalendarMapper;
    private RawDataMapper rawDataMapper;
    private CollectionScheduler scheduler;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, TradeCalendar.class);
    }

    @BeforeEach
    void setUp() {
        orchestrator = mock(CollectionOrchestrator.class);
        tradeCalendarMapper = mock(TradeCalendarMapper.class);
        rawDataMapper = mock(RawDataMapper.class);
        scheduler = new CollectionScheduler(orchestrator, tradeCalendarMapper, rawDataMapper);
    }

    @Test
    void shouldReturnTrueWhenDateIsTradeDay() throws Exception {
        when(tradeCalendarMapper.selectCount(any())).thenReturn(1L);

        boolean result = invokeIsTradeDay(LocalDate.of(2026, 5, 20));

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenDateIsNotTradeDay() throws Exception {
        when(tradeCalendarMapper.selectCount(any())).thenReturn(0L);

        boolean result = invokeIsTradeDay(LocalDate.of(2026, 5, 17));

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenCountIsNull() throws Exception {
        when(tradeCalendarMapper.selectCount(any())).thenReturn(null);

        boolean result = invokeIsTradeDay(LocalDate.of(2026, 5, 20));

        assertThat(result).isFalse();
    }

    private boolean invokeIsTradeDay(LocalDate date) throws Exception {
        Method method = CollectionScheduler.class.getDeclaredMethod("isTradeDay", LocalDate.class);
        method.setAccessible(true);
        return (boolean) method.invoke(scheduler, date);
    }
}
