package com.example.backend.knowledge.topic.interfaces.mcp;

import com.example.backend.knowledge.topic.application.KnowledgeTopicApplicationService;
import com.example.backend.knowledge.topic.domain.model.KnowledgeTopic;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/** 面向 AI Agent 的知识题目工具，只执行数据操作，不参与知识理解与分类决策。 */
@Component
public class KnowledgeTopicTools {
    private final KnowledgeTopicApplicationService service;

    public KnowledgeTopicTools(KnowledgeTopicApplicationService service) {
        this.service = service;
    }

    /** 创建指定分类下的题目。 */
    @McpTool(name = "create_knowledge_topic", description = "在已有分类下创建知识专题（如Java→集合框架）；主题用于组织一组相关问题，不存放完整问答；创建前先查询该分类已有主题")
    public KnowledgeTopic create(
            @McpToolParam(description = "所属分类ID", required = true) long categoryId,
            @McpToolParam(description = "题目标题，最多255字符", required = true) String title,
            @McpToolParam(description = "题目描述，可选", required = false) String description,
            @McpToolParam(description = "排序值，非负整数，默认0", required = false) Integer sortOrder) {
        return service.create(categoryId, title, description, sortOrder);
    }

    /** 根据 ID 查询题目。 */
    @McpTool(name = "get_knowledge_topic", description = "根据题目ID查询知识题目")
    public KnowledgeTopic get(@McpToolParam(description = "题目ID", required = true) long id) {
        return service.get(id);
    }

    /** 列出全部题目或指定分类下的题目。 */
    @McpTool(name = "list_knowledge_topics", description = "分页查询，默认第一页6条、每页最多6条；返回total、pages和hasNext，按需查询后续页，避免一次读取全部数据；创建前检查已有内容")
    public PagedResult<KnowledgeTopic> list(
            @McpToolParam(description = "可选的分类ID，不提供则查询全部", required = false) Long categoryId,
            @McpToolParam(description = "页码，从1开始，默认1", required = false) Integer pageNum,
            @McpToolParam(description = "每页条数，默认6，最大6", required = false) Integer pageSize) {
        return service.list(categoryId, PageRequest.of(pageNum, pageSize));
    }

    /** 更新题目，可以修改标题、归属分类、描述和排序值。 */
    @McpTool(name = "update_knowledge_topic", description = "更新题目；仅修改提供的字段，描述传空字符串可以清除")
    public KnowledgeTopic update(
            @McpToolParam(description = "需要更新的题目ID", required = true) long id,
            @McpToolParam(description = "新的分类ID，可选", required = false) Long categoryId,
            @McpToolParam(description = "新的题目标题，可选", required = false) String title,
            @McpToolParam(description = "新的描述，可选", required = false) String description,
            @McpToolParam(description = "新的排序值，可选", required = false) Integer sortOrder) {
        return service.update(id, categoryId, title, description, sortOrder);
    }

    /** 删除题目，存在关联问题时拒绝删除。 */
    @McpTool(name = "delete_knowledge_topic", description = "删除指定知识题目；如果存在关联问题则拒绝删除")
    public boolean delete(@McpToolParam(description = "题目ID", required = true) long id) {
        return service.delete(id);
    }
}
