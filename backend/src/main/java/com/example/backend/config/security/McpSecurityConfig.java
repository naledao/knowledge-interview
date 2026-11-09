package com.example.backend.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** MCP 访问安全配置：无状态 API Key 认证，默认拒绝其它未授权访问。 */
@Configuration
public class McpSecurityConfig {
    @Bean
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http,
            @Value("${mcp.auth.api-key}") String apiKey) throws Exception {
        McpApiKeyAuthenticationFilter apiKeyFilter = new McpApiKeyAuthenticationFilter(apiKey);
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/mcp", "/mcp/**").hasRole("MCP_AGENT")
                       .requestMatchers(HttpMethod.GET, "/api/categories", "/api/topics").hasRole("MCP_AGENT")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(
                        (request, response, exception) -> response.sendError(HttpStatus.UNAUTHORIZED.value())))
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
