package com.example.backend.config.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** API Key 过滤器测试：验证有效密钥、错误密钥与缺少密钥的处理。 */
class McpApiKeyAuthenticationFilterTests {
    private final McpApiKeyAuthenticationFilter filter = new McpApiKeyAuthenticationFilter("test-secret");

    @Test
    void validKeyShouldAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setServletPath("/mcp");
        request.addHeader("Authorization", "Bearer test-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        filter.doFilter(request, response, (req, res) -> {
            invoked.set(true);
            assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        });
        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void wrongKeyShouldBeRejected() throws Exception {
        assertUnauthorized("Bearer wrong-secret");
    }

    @Test
    void missingKeyShouldBeRejected() throws Exception {
        assertUnauthorized(null);
    }

    @Test
    void knowledgeApisRequireValidKey() throws Exception {
        for (String path : new String[] {"/api/categories", "/api/topics", "/api/questions"}) {
            MockHttpServletRequest missing = new MockHttpServletRequest("GET", path);
            missing.setServletPath(path);
            MockHttpServletResponse denied = new MockHttpServletResponse();
            AtomicBoolean deniedChain = new AtomicBoolean(false);
            filter.doFilter(missing, denied, (req, res) -> deniedChain.set(true));
            assertEquals(401, denied.getStatus(), path);
            assertFalse(deniedChain.get(), path);

            MockHttpServletRequest valid = new MockHttpServletRequest("GET", path);
            valid.setServletPath(path);
            valid.addHeader("Authorization", "Bearer test-secret");
            MockHttpServletResponse allowed = new MockHttpServletResponse();
            AtomicBoolean allowedChain = new AtomicBoolean(false);
            filter.doFilter(valid, allowed, (req, res) -> {
                allowedChain.set(true);
                assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
            });
            assertTrue(allowedChain.get(), path);
            assertEquals(200, allowed.getStatus(), path);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }
    private void assertUnauthorized(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setServletPath("/mcp");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        filter.doFilter(request, response, (req, res) -> invoked.set(true));
        assertEquals(401, response.getStatus());
        assertFalse(invoked.get());
    }
}
