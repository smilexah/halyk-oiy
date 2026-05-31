package kz.halyk.maqsat.summary.client;

import java.util.List;
import kz.halyk.maqsat.summary.config.OpenAiProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiClientTest {

    private MockWebServer mockWebServer;
    private OpenAiClient client;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = "http://localhost:" + mockWebServer.getPort();
        var props = new OpenAiProperties("test-key", baseUrl, "gpt-4o-mini", 1024, 0.4);
        client = new OpenAiClient(WebClient.builder().baseUrl(baseUrl).build(), props);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void complete_extractsChoicesMessageContent() throws InterruptedException {
        String responseJson = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "{\\"result\\":\\"ok\\"}"
                      }
                    }
                  ]
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        String result = client.complete("You are helpful.", List.of(
                new OpenAiClient.ChatMessage("user", "Test prompt")));

        assertThat(result).isEqualTo("{\"result\":\"ok\"}");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/v1/chat/completions");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-key");
    }
}
