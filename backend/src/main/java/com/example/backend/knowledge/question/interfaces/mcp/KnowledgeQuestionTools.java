package com.example.backend.knowledge.question.interfaces.mcp;

import com.example.backend.knowledge.question.application.KnowledgeQuestionApplicationService;
import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/** 面向 AI Agent 的问题操作工具，只负责数据操作，不负责知识提取或整理决策。 */
@Component
public class KnowledgeQuestionTools {
    private final KnowledgeQuestionApplicationService service;

    public KnowledgeQuestionTools(KnowledgeQuestionApplicationService service) {
        this.service = service;
    }

    /** 在指定题目下创建问题，可同时保存答案。 */
    @McpTool(name = "create_knowledge_question", description = "在指定知识题目下添加一个问题，答案可选")
    public KnowledgeQuestion create(
            @McpToolParam(description = "所属题目ID", required = true) long topicId,
            @McpToolParam(description = "具体问题内容", required = true) String questionText,
            @McpToolParam(description = "问题答案或知识说明，可选", required = false) String answerText,
            @McpToolParam(description = "排序值，非负整数，默认0", required = false) Integer sortOrder) {
        return service.create(topicId, questionText, answerText, sortOrder);
    }

    /** 根据问题 ID 查询问题及其答案。 */
    @McpTool(name = "get_knowledge_question", description = "根据问题ID获取问题及答案")
    public KnowledgeQuestion get(@McpToolParam(description = "问题ID", required = true) long id) {
        return service.get(id);
    }

    /** 列出全部问题，或按所属题目筛选。 */
    @McpTool(name = "list_knowledge_questions", description = "查询知识问题，可按题目ID筛选；不提供题目ID时返回全部")
    public List<KnowledgeQuestion> list(
            @McpToolParam(description = "可选的题目ID，不提供则返回全部问题", required = false) Long topicId) {
        return service.list(topicId);
    }

    /** 修改问题内容、所属题目、答案和排序值。 */
    @McpTool(name = "update_knowledge_question", description = "更新已有问题，仅修改提供的字段；答案传空字符串可以清除")
    public KnowledgeQuestion update(
            @McpToolParam(description = "需要更新的问题ID", required = true) long id,
            @McpToolParam(description = "新的所属题目ID，可选", required = false) Long topicId,
            @McpToolParam(description = "新的问题内容，可选", required = false) String questionText,
            @McpToolParam(description = "新的答案，可选；空字符串表示清除答案", required = false) String answerText,
            @McpToolParam(description = "新的排序值，可选", required = false) Integer sortOrder) {
        return service.update(id, topicId, questionText, answerText, sortOrder);
    }

    /** 根据问题 ID 删除问题。 */
    @McpTool(name = "delete_knowledge_question", description = "根据问题ID删除知识问题")
    public boolean delete(@McpToolParam(description = "问题ID", required = true) long id) {
        return service.delete(id);
    }
}
