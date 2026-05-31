package kz.halyk.maqsat.summary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import kz.halyk.maqsat.summary.client.OpenAiClient;
import kz.halyk.maqsat.summary.client.UserMetricsDto;
import kz.halyk.maqsat.summary.config.OpenAiProperties;
import kz.halyk.maqsat.summary.dto.SummaryResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryLlmServiceTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void fallback_whenNoApiKey_returnsHardBuiltRussianText() {
        var props = new OpenAiProperties("", "http://localhost:9999", "gpt-4o-mini", 1024, 0.4);
        var openAiClient = new OpenAiClient(WebClient.builder()
                .baseUrl("http://localhost:9999").build(), props);
        var svc = new SummaryLlmService(props, openAiClient, new ObjectMapper().registerModule(new JavaTimeModule()), new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        var metrics = new UserMetricsDto(null, "u1", "2026-05",
                new BigDecimal("500000"), new BigDecimal("300000"),
                null, null, null, null, Instant.now(), List.of(), List.of());

        SummaryResult result = svc.summarise("u1", "2026-05", "ru", metrics, null);

        assertThat(result.language()).isEqualTo("ru");
        assertThat(result.summaryText()).contains("2026-05");
        assertThat(result.summaryText()).contains("300000");
        assertThat(result.summaryText()).contains("fallback");
        assertThat(result.highlights()).isNotEmpty();
        assertThat(result.suggestions()).isNotEmpty();
    }

    @Test
    void fallback_whenNullMetrics_returnsZeroSpent() {
        var props = new OpenAiProperties("", "http://localhost:9999", "gpt-4o-mini", 1024, 0.4);
        var openAiClient = new OpenAiClient(WebClient.builder()
                .baseUrl("http://localhost:9999").build(), props);
        var svc = new SummaryLlmService(props, openAiClient, new ObjectMapper().registerModule(new JavaTimeModule()), new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        SummaryResult result = svc.summarise("u1", "2026-05", "ru", null, null);

        assertThat(result.summaryText()).contains("0");
    }

    @Test
    void happyPath_withOpenAi_parsesJsonResponse() throws Exception {
        String openAiJson = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"language\\":\\"ru\\",\\"summaryText\\":\\"Вы потратили 300000 ₸\\",\\"highlights\\":[\\"Продукты: 150000\\"],\\"suggestions\\":[\\"Сократите расходы на рестораны\\"]}"
                      }
                    }
                  ]
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(openAiJson)
                .addHeader("Content-Type", "application/json"));

        var baseUrl = "http://localhost:" + mockWebServer.getPort();
        var props = new OpenAiProperties("test-key", baseUrl, "gpt-4o-mini", 1024, 0.4);
        var openAiClient = new OpenAiClient(WebClient.builder().baseUrl(baseUrl).build(), props);
        var svc = new SummaryLlmService(props, openAiClient, new ObjectMapper().registerModule(new JavaTimeModule()), new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        var metrics = new UserMetricsDto(null, "u1", "2026-05",
                new BigDecimal("500000"), new BigDecimal("300000"),
                null, null, null, null, Instant.now(), List.of(), List.of());

        SummaryResult result = svc.summarise("u1", "2026-05", "ru", metrics, null);

        assertThat(result.language()).isEqualTo("ru");
        assertThat(result.summaryText()).isEqualTo("Вы потратили 300000 ₸");
        assertThat(result.highlights()).containsExactly("Продукты: 150000");
        assertThat(result.suggestions()).containsExactly("Сократите расходы на рестораны");
    }
}
