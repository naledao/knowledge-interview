package com.example.backend.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** 使用 Bearer API Key 认证远程 MCP 请求，不负责用户登录或知识整理。 */
public class McpApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private final byte[] expectedKey;

    /** API Key 只从服务端配置注入，不在源码中保存真实密钥。 */
    public McpApiKeyAuthenticationFilter(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("必须通过 MCP_API_KEY 环境变量配置 API Key");
        }
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    /** 校验 MCP 和知识查询接口的 API Key，其它路径交给安全规则处理。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !"/mcp".equals(path) && !path.startsWith("/mcp/")
                && !"/api/categories".equals(path) && !"/api/topics".equals(path);
    }

    /** 比对 Authorization: Bearer <key>，验证成功后写入本次请求的安全上下文。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")
                || !MessageDigest.isEqual(expectedKey,
                        authorization.substring(7).getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid MCP API Key");
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                "mcp-agent", null, List.of(new SimpleGrantedAuthority("ROLE_MCP_AGENT"))));
        SecurityContextHolder.setContext(context);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
