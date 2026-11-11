package com.example.backend.knowledge.category.interfaces.mcp;

import com.example.backend.knowledge.category.application.KnowledgeCategoryApplicationService;
import com.example.backend.knowledge.category.domain.model.KnowledgeCategory;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
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

    @McpTool(name = "create_knowledge_category", description = "仅创建较宽泛、稳定的知识领域（如Java、数据库）；分类名称和描述不得存放具体问题、答案或整篇资料；创建前先调用list_knowledge_categories检查是否已有分类")
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

    @McpTool(name = "list_knowledge_categories", description = "分页查询知识数据，默认第一页6条、每页最多6条；根据total、pages、hasNext按需查询后续页面，不得一次性读取全部数据；创建前优先检查已有内容")
    // MCP 工具：列出全部分类。
    public PagedResult<KnowledgeCategory> list(
            @McpToolParam(description = "页码，从1开始，默认1", required = false) Integer pageNum,
            @McpToolParam(description = "每页条数，默认6，最大6", required = false) Integer pageSize) {
        return service.list(PageRequest.of(pageNum, pageSize));
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
