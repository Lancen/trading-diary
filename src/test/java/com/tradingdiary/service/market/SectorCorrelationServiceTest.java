package com.tradingdiary.service.market;

import com.tradingdiary.mapper.SectorCorrelationMapper;
import com.tradingdiary.service.market.impl.SectorCorrelationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * SectorCorrelationServiceImpl 单元测试，验证 Jaccard 相似度计算和过滤逻辑
 */
@ExtendWith(MockitoExtension.class)
class SectorCorrelationServiceTest {

    @Mock
    private SectorCorrelationMapper sectorCorrelationMapper;

    @InjectMocks
    private SectorCorrelationServiceImpl service;

    // 测试流程: 验证无效 sectorType 抛出 IllegalArgumentException
    @Test
    void shouldRejectInvalidSectorType() {
        assertThatThrownBy(() -> service.compute("INVALID", "001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sectorType");
    }

    // 测试流程: Given mapper 返回交集数据, When 计算 Jaccard, Then 过滤掉低于 0.05 的结果
    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterLowJaccardResults() {
        Map<String, Object> highCorrelation = Map.of(
                "relatedCode", "002", "relatedName", "板块B",
                "intersectionCount", 50L, "sourceCount", 80L);
        Map<String, Object> lowCorrelation = Map.of(
                "relatedCode", "003", "relatedName", "板块C",
                "intersectionCount", 2L, "sourceCount", 100L);

        when(sectorCorrelationMapper.selectIntersectionRanking(eq("INDUSTRY"), eq("001"), anyInt()))
                .thenReturn(List.of(highCorrelation, lowCorrelation));
        // highCorrelation target: 假设 targetCount=70, union=80+70-50=100, jaccard=50/100=0.5
        when(sectorCorrelationMapper.selectStockCount("INDUSTRY", "002")).thenReturn(70L);
        // lowCorrelation target: 假设 targetCount=30, union=100+30-2=128, jaccard=2/128=0.0156
        when(sectorCorrelationMapper.selectStockCount("INDUSTRY", "003")).thenReturn(30L);

        var results = service.compute("INDUSTRY", "001");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).relatedCode()).isEqualTo("002");
        assertThat(results.get(0).jaccard()).isEqualByComparingTo("0.5000");
    }

    // 测试流程: Given mapper 无交集数据, When 计算, Then 返回空列表
    @Test
    void shouldReturnEmptyWhenNoIntersections() {
        when(sectorCorrelationMapper.selectIntersectionRanking(eq("CONCEPT"), eq("001"), anyInt()))
                .thenReturn(List.of());

        var results = service.compute("CONCEPT", "001");

        assertThat(results).isEmpty();
    }
}