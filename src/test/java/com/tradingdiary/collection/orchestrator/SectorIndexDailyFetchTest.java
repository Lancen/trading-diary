package com.tradingdiary.collection.orchestrator;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.client.TushareClient;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.service.collection.ConceptCleanseService;
import com.tradingdiary.service.collection.IndustryCleanseService;
import com.tradingdiary.service.collection.MarginCleanseService;
import com.tradingdiary.service.collection.MarginMacroCleanseService;
import com.tradingdiary.service.collection.MarketIndexDailyCleanseService;
import com.tradingdiary.service.collection.SectorIndexDailyCleanseService;
import com.tradingdiary.service.collection.StockDailyCleanseService;
import com.tradingdiary.service.collection.StockInfoCleanseService;
import com.tradingdiary.service.collection.TradeCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectorIndexDailyFetchTest {

    private AKToolsClient aktoolsClient;
    private RawDataMapper rawDataMapper;
    private IndustryMapper industryMapper;
    private ConceptMapper conceptMapper;
    private CollectionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        rawDataMapper = mock(RawDataMapper.class);
        industryMapper = mock(IndustryMapper.class);
        conceptMapper = mock(ConceptMapper.class);

        TushareClient tushareClient = mock(TushareClient.class);
        DataCollectionLogMapper logMapper = mock(DataCollectionLogMapper.class);
        StockInfoCleanseService stockInfoCleanseService = mock(StockInfoCleanseService.class);
        StockDailyCleanseService stockDailyCleanseService = mock(StockDailyCleanseService.class);
        IndustryCleanseService industryCleanseService = mock(IndustryCleanseService.class);
        ConceptCleanseService conceptCleanseService = mock(ConceptCleanseService.class);
        MarginCleanseService marginCleanseService = mock(MarginCleanseService.class);
        MarginMacroCleanseService marginMacroCleanseService = mock(MarginMacroCleanseService.class);
        MarketIndexDailyCleanseService marketIndexDailyCleanseService = mock(MarketIndexDailyCleanseService.class);
        SectorIndexDailyCleanseService sectorIndexDailyCleanseService = mock(SectorIndexDailyCleanseService.class);
        TradeCalendarService tradeCalendarService = mock(TradeCalendarService.class);
        TradeCalendarMapper tradeCalendarMapper = mock(TradeCalendarMapper.class);
        StockInfoMapper stockInfoMapper = mock(StockInfoMapper.class);

        orchestrator = new CollectionOrchestrator(
                aktoolsClient, tushareClient, logMapper, rawDataMapper,
                stockInfoCleanseService, stockDailyCleanseService,
                industryCleanseService, conceptCleanseService,
                marginCleanseService, marginMacroCleanseService, marketIndexDailyCleanseService,
                sectorIndexDailyCleanseService, tradeCalendarService,
                tradeCalendarMapper, industryMapper, conceptMapper,
                stockInfoMapper
        );
    }

    @Test
    void shouldFetchIndustryIndexDailyForAllSectors() {
        Industry bank = new Industry();
        bank.setCode("BK0475");
        bank.setName("银行");
        Industry semiconductor = new Industry();
        semiconductor.setCode("BK1036");
        semiconductor.setName("半导体");

        when(industryMapper.selectList(any())).thenReturn(List.of(bank, semiconductor));
        when(aktoolsClient.fetchIndustryIndexDaily(eq("银行"), any(), any()))
                .thenReturn("[{\"日期\":\"2026-05-20\",\"开盘价\":\"1200\"}]");
        when(aktoolsClient.fetchIndustryIndexDaily(eq("半导体"), any(), any()))
                .thenReturn("[{\"日期\":\"2026-05-20\",\"开盘价\":\"2500\"}]");

        LocalDate tradeDate = LocalDate.of(2026, 5, 20);
        int result = orchestrator.fetchSectorIndexDaily("INDUSTRY", tradeDate, tradeDate);

        assertThat(result).isEqualTo(2);

        ArgumentCaptor<RawData> captor = ArgumentCaptor.forClass(RawData.class);
        verify(rawDataMapper, org.mockito.Mockito.times(2)).insert(captor.capture());

        List<RawData> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getSectorCode()).isEqualTo("BK0475");
        assertThat(saved.get(1).getSectorCode()).isEqualTo("BK1036");
        assertThat(saved.get(0).getDataType()).isEqualTo("INDUSTRY_INDEX_DAILY");
        assertThat(saved.get(1).getDataType()).isEqualTo("INDUSTRY_INDEX_DAILY");
    }

    @Test
    void shouldFetchConceptIndexDailyForAllSectors() {
        Concept ai = new Concept();
        ai.setCode("GN091");
        ai.setName("ChatGPT");

        when(conceptMapper.selectList(any())).thenReturn(List.of(ai));
        when(aktoolsClient.fetchConceptIndexDaily(eq("ChatGPT"), any(), any()))
                .thenReturn("[{\"日期\":\"2026-05-20\",\"开盘价\":\"800\"}]");

        LocalDate tradeDate = LocalDate.of(2026, 5, 20);
        int result = orchestrator.fetchSectorIndexDaily("CONCEPT", tradeDate, tradeDate);

        assertThat(result).isEqualTo(1);

        ArgumentCaptor<RawData> captor = ArgumentCaptor.forClass(RawData.class);
        verify(rawDataMapper).insert(captor.capture());

        RawData saved = captor.getValue();
        assertThat(saved.getSectorCode()).isEqualTo("GN091");
        assertThat(saved.getDataType()).isEqualTo("CONCEPT_INDEX_DAILY");
    }

    @Test
    void shouldThrowWhenIndustryTableIsEmpty() {
        when(industryMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() ->
                orchestrator.fetchSectorIndexDaily("INDUSTRY", LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("行业");
    }

    @Test
    void shouldThrowWhenConceptTableIsEmpty() {
        when(conceptMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() ->
                orchestrator.fetchSectorIndexDaily("CONCEPT", LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("概念");
    }

    @Test
    void shouldContinueWhenSingleSectorFails() {
        Industry bank = new Industry();
        bank.setCode("BK0475");
        bank.setName("银行");
        Industry semiconductor = new Industry();
        semiconductor.setCode("BK1036");
        semiconductor.setName("半导体");

        when(industryMapper.selectList(any())).thenReturn(List.of(bank, semiconductor));
        when(aktoolsClient.fetchIndustryIndexDaily(eq("银行"), any(), any()))
                .thenThrow(new RuntimeException("API error"));
        when(aktoolsClient.fetchIndustryIndexDaily(eq("半导体"), any(), any()))
                .thenReturn("[{\"日期\":\"2026-05-20\",\"开盘价\":\"2500\"}]");

        LocalDate tradeDate = LocalDate.of(2026, 5, 20);
        int result = orchestrator.fetchSectorIndexDaily("INDUSTRY", tradeDate, tradeDate);

        assertThat(result).isEqualTo(1);

        ArgumentCaptor<RawData> captor = ArgumentCaptor.forClass(RawData.class);
        verify(rawDataMapper).insert(captor.capture());

        RawData saved = captor.getValue();
        assertThat(saved.getSectorCode()).isEqualTo("BK1036");
    }
}
