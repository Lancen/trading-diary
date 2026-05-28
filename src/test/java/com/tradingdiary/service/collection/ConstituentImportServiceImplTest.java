package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockConcept;
import com.tradingdiary.entity.StockIndustry;
import com.tradingdiary.mapper.StockConceptMapper;
import com.tradingdiary.mapper.StockIndustryMapper;
import com.tradingdiary.service.collection.impl.ConstituentImportServiceImpl;
import com.tradingdiary.util.BatchSqlRunner;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 板块成分股导入服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class ConstituentImportServiceImplTest {

    private StockIndustryMapper stockIndustryMapper;
    private StockConceptMapper stockConceptMapper;
    private BatchSqlRunner batchSqlRunner;
    private ObjectMapper objectMapper;
    private ConstituentImportServiceImpl service;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, StockIndustry.class);
        TableInfoHelper.initTableInfo(assistant, StockConcept.class);
    }

    @BeforeEach
    void setUp() {
        stockIndustryMapper = mock(StockIndustryMapper.class);
        stockConceptMapper = mock(StockConceptMapper.class);
        batchSqlRunner = mock(BatchSqlRunner.class);
        objectMapper = new ObjectMapper();
        service = new ConstituentImportServiceImpl(
                stockIndustryMapper, stockConceptMapper, batchSqlRunner, objectMapper, tempDir.toString());
    }

    @Test
    void shouldListFilesReturnEmptyWhenDirNotExists() {
        ConstituentImportServiceImpl svc = new ConstituentImportServiceImpl(
                stockIndustryMapper, stockConceptMapper, batchSqlRunner, objectMapper,
                "/nonexistent/path");

        List<Map<String, Object>> files = svc.listFiles();

        assertThat(files).isEmpty();
    }

    @Test
    void shouldListFilesReturnJsonFiles() throws Exception {
        String jsonContent = "{\"fetched_date\": \"2026-05-20\", \"industries\": [{\"code\": \"BK001\", \"stocks\": [\"000001\"]}], \"concepts\": [{\"code\": \"GN001\", \"stocks\": [\"000002\"]}]}";
        Path file = tempDir.resolve("constituents_20260520.json");
        Files.writeString(file, jsonContent);

        when(stockIndustryMapper.selectCount(any())).thenReturn(0L);

        List<Map<String, Object>> files = service.listFiles();

        assertThat(files).hasSize(1);
        assertThat(files.get(0).get("filename")).isEqualTo("constituents_20260520.json");
        assertThat(files.get(0).get("industryCount")).isEqualTo(1);
        assertThat(files.get(0).get("conceptCount")).isEqualTo(1);
    }

    @Test
    void shouldImportFromFileSuccessfully() throws Exception {
        String jsonContent = "{\"fetched_date\": \"2026-05-20\"," +
                "\"industries\": [{\"code\": \"BK001\", \"stocks\": [\"000001\", \"000002\"]}]," +
                "\"concepts\": [{\"code\": \"GN001\", \"stocks\": [\"000001\"]}]}";
        Path file = tempDir.resolve("constituents_20260520.json");
        Files.writeString(file, jsonContent);

        when(stockIndustryMapper.selectList(any())).thenReturn(List.of());
        when(stockConceptMapper.selectList(any())).thenReturn(List.of());
        when(batchSqlRunner.batchInsert(any(List.class))).thenAnswer(invocation -> {
            List<?> list = invocation.getArgument(0);
            return list.size();
        });

        Map<String, Object> result = service.importFromFile("constituents_20260520.json");

        assertThat(result.get("status")).isEqualTo("success");
        assertThat(result.get("industryRelations")).isEqualTo(2);
        assertThat(result.get("conceptRelations")).isEqualTo(1);
    }

    @Test
    void shouldReturnFailedWhenFileNotExists() {
        Map<String, Object> result = service.importFromFile("nonexistent.json");

        assertThat(result.get("status")).isEqualTo("failed");
        assertThat(result.get("error").toString()).contains("文件不存在");
    }

    @Test
    void shouldRejectPathTraversal() {
        assertThatThrownBy(() -> service.importFromFile("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法文件路径");
    }
}
