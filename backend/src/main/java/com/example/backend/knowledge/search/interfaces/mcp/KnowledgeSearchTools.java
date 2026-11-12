package com.example.backend.knowledge.search.interfaces.mcp;

import com.example.backend.knowledge.search.application.KnowledgeSearchResult;
import com.example.backend.knowledge.search.application.KnowledgeSearchService;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/** 向 AI Agent 提供知识库关键词搜索，复用现有搜索应用服务。 */
@Component
public class KnowledgeSearchTools {
    private final KnowledgeSearchService service;

    public KnowledgeSearchTools(KnowledgeSearchService service) {
        this.service = service;
    }

    /** 搜索分类、主题、问题及答案；统一限制每页最多六条。 */
    @McpTool(name = "search_knowledge", description = "按关键词搜索知识库分类、主题、问题及答案，支持空格分隔的多个关键词（AND 匹配），按相关度排序；默认每页6条、最多6条。创建知识前优先搜索已有内容以避免重复；返回匹配摘要，若需完整问题答案可根据ID调用get_knowledge_question。")
    public KnowledgeSearchResult search(
            @McpToolParam(description = "搜索关键词，必填，最多100字符；多个关键词用空格分隔", required = true) String keyword,
            @McpToolParam(description = "搜索类型：all、category、topic、question，默认all", required = false) String type,
            @McpToolParam(description = "页码，从1开始，默认1", required = false) Integer pageNum,
            @McpToolParam(description = "每页条数，默认6，最大6", required = false) Integer pageSize) {
        PageRequest page = PageRequest.of(pageNum, pageSize);
        return service.search(keyword, type, page.pageNum(), page.pageSize());
    }
}
