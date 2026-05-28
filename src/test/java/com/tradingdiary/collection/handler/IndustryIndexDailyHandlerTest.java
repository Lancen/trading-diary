package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.service.collection.SectorIndexDailyCleanseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndustryIndexDailyHandlerTest {

    @Mock
    private com.tradingdiary.collection.client.AKToolsClient aktoolsClient;

    @Mock
    private SectorIndexDailyCleanseService sectorIndexDailyCleanseService;

    @Mock
    private IndustryMapper industryMapper;

    @Mock
    private RawDataMapper rawDataMapper;

    @Mock
    private DataCollectionLogMapper logMapper;

    @InjectMocks
    private IndustryIndexDailyHandler handler;

    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("INDUSTRY_INDEX_DAILY");
    }

    @Test
    void shouldReturnSingleResultWhenIndustryListIsEmpty() {
        doAnswer(invocation -> {
            DataCollectionLog log = invocation.getArgument(0);
            log.setId(42L);
            return 1;
        }).when(logMapper).insert(any(DataCollectionLog.class));
        when(industryMapper.selectList(any())).thenReturn(Collections.emptyList());

        FetchResult result = handler.fetch(LocalDate.of(2024, 1, 15));

        assertThat(result.getType()).isEqualTo(FetchResult.Type.SINGLE);
    }

    @Test
    void shouldFetchIndustryIndexDailyForEachSector() {
        doAnswer(invocation -> {
            DataCollectionLog log = invocation.getArgument(0);
            log.setId(100L);
            return 1;
        }).when(logMapper).insert(any(DataCollectionLog.class));

        Industry sector = new Industry();
        sector.setCode("INDUSTRY_001");
        sector.setName("银行");
        when(industryMapper.selectList(any())).thenReturn(List.of(sector));
        when(aktoolsClient.fetchIndustryIndexDaily(eq("银行"), any(), any())).thenReturn("[{}]");
        when(rawDataMapper.insert(any(RawData.class))).thenReturn(1);

        FetchResult result = handler.fetch(LocalDate.of(2024, 1, 15));

        assertThat(result.getType()).isEqualTo(FetchResult.Type.MULTI_SECTOR);
        assertThat(result.getSectorCount()).isEqualTo(1);
        verify(aktoolsClient).sleepBetweenCalls();
    }

    @Test
    void shouldRequireCalendar() {
        assertThat(handler.requiresCalendar()).isTrue();
    }
}
