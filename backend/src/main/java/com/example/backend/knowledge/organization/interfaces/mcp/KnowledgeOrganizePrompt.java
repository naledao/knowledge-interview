package com.example.backend.knowledge.organization.interfaces.mcp;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** 通过 MCP Prompts 向 Agent 提供知识整理规范，不执行知识整理或数据库操作。 */
@Component
public class KnowledgeOrganizePrompt {
    private final String rules;

    /** 从 classpath 读取规则，保证规则与 MCP 服务一起打包部署。 */
    public KnowledgeOrganizePrompt() {
        try (var input = new ClassPathResource("prompts/knowledge-organize.md").getInputStream()) {
            this.rules = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("无法读取知识整理提示词资源", ex);
        }
    }

    /** 客户端通过 prompts/get 获取完整规范，由 Agent 决定如何整理。 */
    @McpPrompt(name = "organize_knowledge", title = "知识整理规范", description = "按分类→主题→问题整理知识，参考答案使用浅色主题 HTML/CSS/JS")
    public GetPromptResult organizeKnowledge() {
        return GetPromptResult.builder(List.of(
                new PromptMessage(Role.USER, TextContent.builder(rules).build())))
                .description("knowledge-mcp 知识整理规范")
                .build();
    }
}
