package com.tradingdiary.service;

import com.tradingdiary.collection.model.ConceptIndustryVO;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.service.impl.MarketDataServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 市场数据查询服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class MarketDataServiceImplTest {

    private ConceptMapper conceptMapper;
    private IndustryMapper industryMapper;
    private MarketDataServiceImpl service;

    @BeforeEach
    void setUp() {
        conceptMapper = mock(ConceptMapper.class);
        industryMapper = mock(IndustryMapper.class);
        service = new MarketDataServiceImpl(conceptMapper, industryMapper);
    }

    @Test
    void shouldReturnPaginatedConcepts() {
        ConceptIndustryVO vo = new ConceptIndustryVO();
        vo.setCode("GN001");
        vo.setName("MSCI");
        vo.setStockCount(268);
        vo.setMarginBalance(new BigDecimal("68050000000.00"));

        when(conceptMapper.selectConceptList(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(vo));
        when(conceptMapper.countConceptList(any(), any())).thenReturn(1L);

        Map<String, Object> result = service.listConcepts(null, null, "marginBalance", "desc", 1, 50);

        assertThat(result.get("total")).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        List<ConceptIndustryVO> records = (List<ConceptIndustryVO>) result.get("records");
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getName()).isEqualTo("MSCI");
    }

    @Test
    void shouldReturnPaginatedIndustries() {
        ConceptIndustryVO vo = new ConceptIndustryVO();
        vo.setCode("BK001");
        vo.setName("银行");
        vo.setStockCount(42);

        when(industryMapper.selectIndustryList(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(vo));
        when(industryMapper.countIndustryList(any(), any())).thenReturn(1L);

        Map<String, Object> result = service.listIndustries(null, null, "stockCount", "asc", 1, 20);

        assertThat(result.get("total")).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        List<ConceptIndustryVO> records = (List<ConceptIndustryVO>) result.get("records");
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getName()).isEqualTo("银行");
    }

    @Test
    void shouldUseDefaultSortWhenInvalidColumn() {
        when(conceptMapper.selectConceptList(any(), any(), eq("marginBalance"), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(conceptMapper.countConceptList(any(), any())).thenReturn(0L);

        Map<String, Object> result = service.listConcepts(null, null, "invalid_column", "desc", 1, 50);

        assertThat(result.get("total")).isEqualTo(0L);
    }

    @Test
    void shouldUseAscDirectionWhenSpecified() {
        when(industryMapper.selectIndustryList(any(), any(), any(), eq("ASC"), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(industryMapper.countIndustryList(any(), any())).thenReturn(0L);

        Map<String, Object> result = service.listIndustries(null, null, "stockCount", "asc", 1, 20);

        assertThat(result.get("total")).isEqualTo(0L);
    }
}
