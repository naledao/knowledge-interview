package com.example.backend.knowledge.category.interfaces.mcp;

import com.example.backend.knowledge.category.application.KnowledgeCategoryApplicationService;
import com.example.backend.knowledge.category.domain.model.KnowledgeCategory;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
/** 对远程 AI Agent 暴露分类操作工具，仅处理调用参数和结果，不负责知识整理决策。 */
public class KnowledgeCategoryTools {
    private final KnowledgeCategoryApplicationService service;

    public KnowledgeCategoryTools(KnowledgeCategoryApplicationService service) {
        this.service = service;
    }

    @McpTool(name = "create_knowledge_category", description = "创建知识分类，只存储Agent给出的分类信息，不自动整理知识")
    // MCP 工具：创建分类。
    public KnowledgeCategory create(
            @McpToolParam(description = "分类名称，最多128字符", required = true) String name,
            @McpToolParam(description = "分类描述，可选", required = false) String description,
            @McpToolParam(description = "排序值，非负整数，默认0", required = false) Integer sortOrder) {
        return service.create(name, description, sortOrder);
    }

    @McpTool(name = "get_knowledge_category", description = "根据ID查询知识分类")
    // MCP 工具：查询单个分类。
    public KnowledgeCategory get(@McpToolParam(description = "分类ID", required = true) long id) {
        return service.get(id);
    }

    @McpTool(name = "list_knowledge_categories", description = "列出全部知识分类，按排序值及ID升序排列；创建前可用来检查是否已有同类分类")
    // MCP 工具：列出全部分类。
    public List<KnowledgeCategory> list() {
        return service.list();
    }

    @McpTool(name = "update_knowledge_category", description = "更新分类名称、描述和排序；描述为空值时保留原描述，传空字符串可清除描述")
    // MCP 工具：修改指定分类。
    public KnowledgeCategory update(
            @McpToolParam(description = "分类ID", required = true) long id,
            @McpToolParam(description = "新的分类名称", required = true) String name,
            @McpToolParam(description = "新的描述，可选", required = false) String description,
            @McpToolParam(description = "新的排序值，可选", required = false) Integer sortOrder) {
        return service.update(id, name, description, sortOrder);
    }

    @McpTool(name = "delete_knowledge_category", description = "删除指定分类；如果分类下存在题目则拒绝删除")
    // MCP 工具：删除指定分类。
    public boolean delete(@McpToolParam(description = "分类ID", required = true) long id) {
        return service.delete(id);
    }
}
