package com.example.backend.config.mcp;

import java.util.Optional;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * 修正 Spring AI WebMVC Streamable HTTP 传输在 GET /mcp 上的阻塞问题。
 *
 * <p>当环境配置显式启用 STREAMABLE 协议时，Spring AI 2.0.2 的
 * WebMvcStreamableServerTransportProvider 在 GET /mcp 建立空闲 SSE 监听流时不会立即刷出 HTTP
 * 响应头，导致同步等待 GET 响应头的 MCP 客户端无限期卡在连接阶段。本服务不主动向客户端推送通知，
 * 按 MCP Streamable HTTP 规范对 GET /mcp 直接返回 405 Method Not Allowed。
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "protocol", havingValue = "STREAMABLE")
public class McpStreamableGetEndpointConfig {
    /** 注册高优先级 GET /mcp 路由，仅在 Streamable HTTP 传输启用时生效。 */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public RouterFunction<ServerResponse> mcpStreamableGetFallbackRouterFunction(
            ObjectProvider<WebMvcStreamableServerTransportProvider> streamableTransportProvider,
            @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp}") String mcpEndpoint,
            @Value("${spring.ai.mcp.server.streamable-http.disallow-delete:false}") boolean disallowDelete) {
        if (streamableTransportProvider.getIfAvailable() == null) {
            return request -> Optional.empty();
        }
        String allowedMethods = disallowDelete
                ? HttpMethod.POST.name()
                : HttpMethod.POST.name() + ", " + HttpMethod.DELETE.name();
        return RouterFunctions.route()
                .GET(mcpEndpoint, request -> ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED)
                        .header(HttpHeaders.ALLOW, allowedMethods)
                        .build())
                .build();
    }
}
