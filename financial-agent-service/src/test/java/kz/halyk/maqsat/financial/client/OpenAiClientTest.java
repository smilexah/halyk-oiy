package kz.halyk.maqsat.financial.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kz.halyk.maqsat.financial.config.OpenAiProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class OpenAiClientTest {

    private MockWebServer server;
    private OpenAiClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        String baseUrl = server.url("/").toString();
        OpenAiProperties props = new OpenAiProperties(
                "test-api-key", baseUrl, "gpt-4o-mini", 1024, 0.2);
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        client = new OpenAiClient(webClient, props);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void complete_returnsContentFromResponse() {
        String cannedContent = "{\"categories\":[{\"name\":\"Продукты\",\"type\":\"MANDATORY\",\"limitAmount\":125000}],\"rationale\":\"test\"}";
        String cannedResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": %s
                      }
                    }
                  ]
                }
                """.formatted("\"" + cannedContent.replace("\"", "\\\"") + "\"");

        server.enqueue(new MockResponse()
                .setBody(cannedResponse)
                .addHeader("Content-Type", "application/json"));

        String result = client.complete(
                "You are a finance assistant.",
                List.of(new OpenAiClient.ChatMessage("user", "propose a budget")));

        assertThat(result).isEqualTo(cannedContent);
    }

    @Test
    void complete_simpleJsonResponse() {
        String content = "{\"categories\":[],\"rationale\":\"empty\"}";
        // Wrap content as a JSON string value (escaped)
        String escaped = content.replace("\"", "\\\"");
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + escaped + "\"}}]}";

        server.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        String result = client.complete("system prompt",
                List.of(new OpenAiClient.ChatMessage("user", "hello")));

        assertThat(result).isEqualTo(content);
    }
}
