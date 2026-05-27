package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.service.collection.impl.IndustryCleanseServiceImpl;
import com.tradingdiary.util.AkToolsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * IndustryIndexDailyHandler 单元测试，验证板块指数 FETCH 返回 MULTI_SECTOR 结果
 */
@ExtendWith(MockitoExtension.class)
class IndustryIndexDailyHandlerTest {

    @Mock
    private AkToolsClient akToolsClient;

    @Mock
    private DataCollectionLogMapper logMapper;

    @Mock
    private RawDataMapper rawDataMapper;

    @Mock
    private IndustryCleanseServiceImpl industryCleanseService;

    @InjectMocks
    private IndustryIndexDailyHandler handler;

    // 测试流程: 验证 dataType() 返回 INDUSTRY_INDEX_DAILY
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("INDUSTRY_INDEX_DAILY");
    }

    // 测试流程: Given mock client 返回空列表, When 调用 fetch(), Then 返回 MULTI_SECTOR 结果
    @Test
    void shouldReturnMultiSectorResultOnEmptyResponse() {
        when(akToolsClient.fetchIndustryList()).thenReturn("[]");
        // 模拟 MyBatis-Plus insert 后自动回填 ID
        doAnswer(invocation -> {
            DataCollectionLog log = invocation.getArgument(0);
            log.setId(42L);
            return 1;
        }).when(logMapper).insert(any(DataCollectionLog.class));

        FetchResult result = handler.fetch(LocalDate.of(2024, 1, 15));

        assertThat(result.getType()).isEqualTo(FetchResult.Type.MULTI_SECTOR);
        assertThat(result.getCollectionLogId()).isEqualTo(42L);
    }

    // 测试流程: 验证 requiresCalendar() 返回 true
    @Test
    void shouldRequireCalendar() {
        assertThat(handler.requiresCalendar()).isTrue();
    }
}