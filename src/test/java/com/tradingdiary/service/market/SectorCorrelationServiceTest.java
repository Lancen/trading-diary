package com.tradingdiary.service.market;

import com.tradingdiary.service.market.impl.SectorCorrelationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectorCorrelationServiceTest {

    private JdbcTemplate jdbcTemplate;
    private SectorCorrelationService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new SectorCorrelationServiceImpl(jdbcTemplate);
    }

    @Test
    void shouldComputeIndustryToConceptCorrelation() {
        SectorCorrelation chip = new SectorCorrelation(
                "CONCEPT", "GN091", "芯片概念",
                new BigDecimal("0.7500"), 30, 50, 40
        );
        SectorCorrelation domestic = new SectorCorrelation(
                "CONCEPT", "GN253", "国产替代",
                new BigDecimal("0.4286"), 15, 50, 20
        );

        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("BK1036"), eq("BK1036"), anyInt()))
                .thenReturn(List.of(chip, domestic));

        List<SectorCorrelation> result = service.compute("INDUSTRY", "BK1036");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).relatedType()).isEqualTo("CONCEPT");
        assertThat(result.get(0).relatedCode()).isEqualTo("GN091");
        assertThat(result.get(0).relatedName()).isEqualTo("芯片概念");
        assertThat(result.get(0).jaccard()).isEqualByComparingTo(new BigDecimal("0.75"));
        assertThat(result.get(0).intersectionCount()).isEqualTo(30);
        assertThat(result.get(0).sourceCount()).isEqualTo(50);
        assertThat(result.get(0).targetCount()).isEqualTo(40);
    }

    @Test
    void shouldComputeConceptToIndustryCorrelation() {
        SectorCorrelation semi = new SectorCorrelation(
                "INDUSTRY", "BK1036", "半导体",
                new BigDecimal("0.7500"), 30, 40, 50
        );

        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("GN091"), eq("GN091"), anyInt()))
                .thenReturn(List.of(semi));

        List<SectorCorrelation> result = service.compute("CONCEPT", "GN091");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).relatedType()).isEqualTo("INDUSTRY");
        assertThat(result.get(0).relatedCode()).isEqualTo("BK1036");
        assertThat(result.get(0).relatedName()).isEqualTo("半导体");
    }

    @Test
    void shouldFilterByMinJaccard() {
        SectorCorrelation high = new SectorCorrelation(
                "CONCEPT", "GN091", "芯片概念",
                new BigDecimal("0.7500"), 30, 50, 40
        );
        SectorCorrelation low = new SectorCorrelation(
                "CONCEPT", "GN999", "噪声概念",
                new BigDecimal("0.0200"), 1, 50, 30
        );

        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("BK1036"), eq("BK1036"), anyInt()))
                .thenReturn(List.of(high, low));

        List<SectorCorrelation> result = service.compute("INDUSTRY", "BK1036");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).relatedCode()).isEqualTo("GN091");
    }

    @Test
    void shouldReturnEmptyWhenNoConstituents() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("BK9999"), eq("BK9999"), anyInt()))
                .thenReturn(List.of());

        List<SectorCorrelation> result = service.compute("INDUSTRY", "BK9999");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowOnInvalidSectorType() {
        assertThatThrownBy(() -> service.compute("INVALID", "BK1036"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sectorType");
    }
}
