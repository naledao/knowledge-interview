package com.example.backend.config.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/** 验证 MCP 在 STATELESS 与 STREAMABLE 两种模式下均不会在 GET /mcp 阻塞，并可完成完整握手。 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class McpEndpointIntegrationTests {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    @LocalServerPort
    private int port;

    @Value("${mcp.auth.api-key}")
    private String apiKey;

    @Nested
    @TestPropertySource(properties = "spring.ai.mcp.server.protocol=STATELESS")
    class StatelessProtocolTests {
        @Test
        void handshakeAndGetMethodNotAllowedSucceedWithoutBlocking() throws Exception {
            verifyMcpHandshake(port, apiKey, false);
        }
    }

    @Nested
    @TestPropertySource(properties = "spring.ai.mcp.server.protocol=STREAMABLE")
    class StreamableProtocolTests {
        @Test
        void handshakeAndGetMethodNotAllowedSucceedWithoutBlocking() throws Exception {
            verifyMcpHandshake(port, apiKey, true);
        }
    }

    /** 模拟真实 MCP 客户端顺序校验 401、initialize、GET /mcp、notifications/initialized 与列表查询。 */
    private static void verifyMcpHandshake(int port, String apiKey, boolean expectSessionId) throws Exception {
        URI mcpUri = URI.create("http://127.0.0.1:" + port + "/mcp");
        String authHeader = "Bearer " + apiKey;

        HttpResponse<String> unauthorizedGet = HTTP_CLIENT.send(
                HttpRequest.newBuilder(mcpUri)
                        .timeout(REQUEST_TIMEOUT)
                        .header(HttpHeaders.ACCEPT, "text/event-stream")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(unauthorizedGet.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        String initializeBody = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"integration-test","version":"1.0"}}}
                """;
        HttpResponse<String> initResponse = postMcp(mcpUri, authHeader, null, initializeBody);
        assertThat(initResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(initResponse.body()).contains("knowledge-mcp");

        String sessionId = initResponse.headers().firstValue("Mcp-Session-Id").orElse(null);
        if (expectSessionId) {
            assertThat(sessionId).isNotBlank();
        } else {
            assertThat(sessionId).isNull();
        }

        HttpRequest.Builder getBuilder = HttpRequest.newBuilder(mcpUri)
                .timeout(REQUEST_TIMEOUT)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header(HttpHeaders.ACCEPT, "text/event-stream")
                .GET();
        if (sessionId != null) {
            getBuilder.header("Mcp-Session-Id", sessionId);
        }
        HttpResponse<String> getResponse = HTTP_CLIENT.send(getBuilder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(getResponse.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());

        HttpResponse<String> initializedResponse = postMcp(
                mcpUri,
                authHeader,
                sessionId,
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}");
        assertThat(initializedResponse.statusCode()).isEqualTo(HttpStatus.ACCEPTED.value());

        HttpResponse<String> toolsResponse = postMcp(
                mcpUri,
                authHeader,
                sessionId,
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}");
        assertThat(toolsResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(toolsResponse.body()).contains("list_knowledge_categories");

        HttpResponse<String> promptsResponse = postMcp(
                mcpUri,
                authHeader,
                sessionId,
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"prompts/list\",\"params\":{}}");
        assertThat(promptsResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(promptsResponse.body()).contains("organize_knowledge");
    }

    private static HttpResponse<String> postMcp(URI mcpUri, String authHeader, String sessionId, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(mcpUri)
                .timeout(REQUEST_TIMEOUT)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.ACCEPT, "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (sessionId != null) {
            builder.header("Mcp-Session-Id", sessionId);
        }
        return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
