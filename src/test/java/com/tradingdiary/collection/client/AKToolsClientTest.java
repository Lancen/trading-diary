package com.tradingdiary.collection.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AKToolsClient}.
 * <p>
 * SC-002: Verifies that HTTP call failures in AKToolsClient propagate as exceptions
 * within the 30s timing bound (retry backoff 2s/4s/8s = 14s sleep + call time).
 */
@ExtendWith(MockitoExtension.class)
class AKToolsClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AKToolsClient client;

    @BeforeEach
    void setUp() {
        client = new AKToolsClient("http://localhost:9999");
        ReflectionTestUtils.setField(client, "restClient", restClient);

        // Mock the RestClient chain: get() -> uri() -> retrieve() -> body()
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        // Use Object[].class to disambiguate varargs from Map and Function overloads
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void fetchStockSpot_shouldThrowWhenHttpCallFails_withinTimingBound() {
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

        long start = System.currentTimeMillis();
        assertThrows(RuntimeException.class, () -> client.fetchStockSpot());
        long elapsed = System.currentTimeMillis() - start;

        // SC-002: retry backoff 2s/4s/8s = 14s, total should be ≤ 30s
        // A single failed call completes far faster, but this asserts the bound
        assertThat(elapsed).isLessThan(30_000L);
    }

    @Test
    void fetchIndustryNames_shouldThrowWhenHttpCallFails() {
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(RuntimeException.class, () -> client.fetchIndustryNames());
    }

    @Test
    void fetchConceptNames_shouldThrowWhenHttpCallFails() {
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(RuntimeException.class, () -> client.fetchConceptNames());
    }

    @Test
    void fetchTradeCalendar_shouldThrowWhenHttpCallFails() {
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(RuntimeException.class, () -> client.fetchTradeCalendar());
    }

    @Test
    void fetchMarginDetailSse_shouldThrowWhenHttpCallFails() {
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(RuntimeException.class, () -> client.fetchMarginDetailSse("2026-05-15"));
    }

    @Test
    void fetchMarginDetailSzse_shouldThrowWhenHttpCallFails() {
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(RuntimeException.class, () -> client.fetchMarginDetailSzse("2026-05-15"));
    }
}
