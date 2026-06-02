package com.tradingdiary.service.market;

import com.tradingdiary.mapper.CrowdednessMapper;
import com.tradingdiary.service.market.CrowdednessService.CrowdednessDaily;
import com.tradingdiary.service.market.impl.CrowdednessServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * CrowdednessServiceImpl 单元测试，验证拥挤度计算逻辑
 */
@ExtendWith(MockitoExtension.class)
class CrowdednessServiceTest {

    @Mock
    private CrowdednessMapper crowdednessMapper;

    @InjectMocks
    private CrowdednessServiceImpl service;

    // 测试流程: Given mapper 返回空结果, When 查询且最新日期为 null, Then 返回空列表
    @Test
    void shouldReturnEmptyWhenNoData() {
        when(crowdednessMapper.selectLatestTradeDate()).thenReturn(null);

        List<CrowdednessDaily> result = service.query(null, null);

        assertThat(result).isEmpty();
    }

    // 测试流程: Given mapper 返回单日拥挤度数据, When 查询, Then 正确解析各字段
    @Test
    @SuppressWarnings("unchecked")
    void shouldParseCrowdednessDaily() {
        LocalDate latest = LocalDate.of(2026, 5, 30);
        when(crowdednessMapper.selectLatestTradeDate()).thenReturn(latest);

        Map<String, Object> row = Map.of(
                "trade_date", latest,
                "total_amount", new BigDecimal("100000000000"),
                "top_amount", new BigDecimal("43000000000"),
                "total_stocks", 5000L,
                "top_stocks", 250L,
                "crowdedness", new BigDecimal("43.00")
        );
        when(crowdednessMapper.selectCrowdednessDaily(any(), any())).thenReturn(List.of(row));

        List<CrowdednessDaily> result = service.query(null, null);

        assertThat(result).hasSize(1);
        CrowdednessDaily d = result.get(0);
        assertThat(d.tradeDate()).isEqualTo(latest);
        assertThat(d.crowdedness()).isEqualByComparingTo("43.00");
        assertThat(d.totalStocks()).isEqualTo(5000);
        assertThat(d.topStocks()).isEqualTo(250);
    }

    // 测试流程: Given 指定日期范围, When 查询, Then 使用传入的日期而非最新日期
    @Test
    void shouldUseProvidedDateRange() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        when(crowdednessMapper.selectCrowdednessDaily(start, end)).thenReturn(List.of());

        List<CrowdednessDaily> result = service.query(start, end);

        assertThat(result).isEmpty();
    }

    // 测试流程: Given 仅提供 endDate, When 查询, Then startDate 默认为 endDate 减3年
    @Test
    void shouldDefaultStartDateTo3YearsBefore() {
        LocalDate end = LocalDate.of(2026, 5, 30);
        LocalDate expectedStart = end.minusYears(3);
        when(crowdednessMapper.selectCrowdednessDaily(expectedStart, end)).thenReturn(List.of());

        service.query(null, end);
    }
}
