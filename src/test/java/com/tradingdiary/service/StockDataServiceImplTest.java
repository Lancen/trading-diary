package com.tradingdiary.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tradingdiary.collection.model.StockDetailVO;
import com.tradingdiary.collection.model.StockListVO;
import com.tradingdiary.entity.MarginDaily;
import com.tradingdiary.entity.StockConcept;
import com.tradingdiary.entity.StockDaily;
import com.tradingdiary.entity.StockIndustry;
import com.tradingdiary.entity.StockInfo;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.StockConceptMapper;
import com.tradingdiary.mapper.StockDailyMapper;
import com.tradingdiary.mapper.StockIndustryMapper;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.service.impl.StockDataServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 股票数据查询服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class StockDataServiceImplTest {

    private StockInfoMapper stockInfoMapper;
    private StockDailyMapper stockDailyMapper;
    private MarginDailyMapper marginDailyMapper;
    private StockIndustryMapper stockIndustryMapper;
    private StockConceptMapper stockConceptMapper;
    private StockDataServiceImpl service;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, StockInfo.class);
        TableInfoHelper.initTableInfo(assistant, StockDaily.class);
        TableInfoHelper.initTableInfo(assistant, MarginDaily.class);
        TableInfoHelper.initTableInfo(assistant, StockIndustry.class);
        TableInfoHelper.initTableInfo(assistant, StockConcept.class);
    }

    @BeforeEach
    void setUp() {
        stockInfoMapper = mock(StockInfoMapper.class);
        stockDailyMapper = mock(StockDailyMapper.class);
        marginDailyMapper = mock(MarginDailyMapper.class);
        stockIndustryMapper = mock(StockIndustryMapper.class);
        stockConceptMapper = mock(StockConceptMapper.class);
        service = new StockDataServiceImpl(
                stockInfoMapper, stockDailyMapper, marginDailyMapper,
                stockIndustryMapper, stockConceptMapper);
    }

    @Test
    void shouldReturnPaginatedStocks() {
        StockListVO vo = new StockListVO();
        vo.setStockCode("000001");
        vo.setStockName("平安银行");
        vo.setClose(new BigDecimal("12.80"));
        vo.setChangePct(new BigDecimal("2.40"));

        when(stockInfoMapper.selectStockList(any(), any(), any(), any(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(vo));
        when(stockInfoMapper.countStockList(any(), any(), any(), any())).thenReturn(1L);

        Map<String, Object> result = service.listStocks(null, null, null, null, "changePct", "desc", 1, 50);

        assertThat(result.get("total")).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        List<StockListVO> records = (List<StockListVO>) result.get("records");
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getStockCode()).isEqualTo("000001");
    }

    @Test
    void shouldReturnStockDetail() {
        StockInfo info = new StockInfo();
        info.setCode("000001");
        info.setName("平安银行");
        info.setLatestPrice(new BigDecimal("12.80"));
        info.setChangePct(new BigDecimal("2.40"));
        info.setVolume(52340100L);
        info.setIsDeleted(false);

        StockIndustry industry = new StockIndustry();
        industry.setIndustryCode("BK001");
        industry.setIsDeleted(false);

        StockConcept concept = new StockConcept();
        concept.setConceptCode("GN001");
        concept.setIsDeleted(false);

        StockDaily daily = new StockDaily();
        daily.setStockCode("000001");
        daily.setTradeDate(LocalDate.of(2026, 5, 20));
        daily.setOpen(new BigDecimal("12.50"));
        daily.setHigh(new BigDecimal("13.00"));
        daily.setLow(new BigDecimal("12.30"));
        daily.setClose(new BigDecimal("12.80"));
        daily.setVolume(52340100L);
        daily.setIsDeleted(false);

        MarginDaily margin = new MarginDaily();
        margin.setStockCode("000001");
        margin.setTradeDate(LocalDate.of(2026, 5, 20));
        margin.setMarginBalance(new BigDecimal("3258000000"));
        margin.setMarginBuy(new BigDecimal("50000000"));
        margin.setShortBalance(new BigDecimal("15000000"));
        margin.setTotalBalance(new BigDecimal("3273000000"));
        margin.setIsDeleted(false);

        when(stockInfoMapper.selectOne(any())).thenReturn(info);
        when(stockIndustryMapper.selectOne(any())).thenReturn(industry);
        when(stockConceptMapper.selectList(any())).thenReturn(List.of(concept));
        when(stockDailyMapper.selectList(any())).thenReturn(List.of(daily));
        when(marginDailyMapper.selectList(any())).thenReturn(List.of(margin));

        StockDetailVO result = service.getStockDetail("000001", null, null);

        assertThat(result.getStockCode()).isEqualTo("000001");
        assertThat(result.getStockName()).isEqualTo("平安银行");
        assertThat(result.getIndustry()).isEqualTo("BK001");
        assertThat(result.getConcepts()).containsExactly("GN001");
        assertThat(result.getLatestQuote()).isNotNull();
        assertThat(result.getLatestQuote().getClose()).isEqualByComparingTo("12.80");
        assertThat(result.getDailyKlines()).hasSize(1);
        assertThat(result.getLatestMargin()).isNotNull();
        assertThat(result.getLatestMargin().getMarginBalance()).isEqualByComparingTo("3258000000");
    }

    @Test
    void shouldReturnEmptyDetailWhenStockNotFound() {
        when(stockInfoMapper.selectOne(any())).thenReturn(null);

        StockDetailVO result = service.getStockDetail("999999", null, null);

        assertThat(result.getStockCode()).isEqualTo("999999");
        assertThat(result.getStockName()).isNull();
    }

    @Test
    void shouldFallbackToDefaultSortColumn() {
        when(stockInfoMapper.selectStockList(any(), any(), any(), any(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(stockInfoMapper.countStockList(any(), any(), any(), any())).thenReturn(0L);

        Map<String, Object> result = service.listStocks(null, null, null, null, "invalid_column", "desc", 1, 50);

        assertThat(result.get("total")).isEqualTo(0L);
    }
}
