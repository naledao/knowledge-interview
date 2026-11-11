package com.example.backend.knowledge.question.interfaces.mcp;

import com.example.backend.knowledge.question.application.KnowledgeQuestionApplicationService;
import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
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
    @McpTool(name = "create_knowledge_question", description = "在已有主题下创建具体知识问题；questionText保存提问，answerText保存浅色主题的 HTML/CSS/JS 参考答案；不得将问答塞入分类或主题描述，创建前检查重复问题")
    public KnowledgeQuestion create(
            @McpToolParam(description = "所属题目ID", required = true) long topicId,
            @McpToolParam(description = "具体问题内容", required = true) String questionText,
            @McpToolParam(description = "参考答案，完整 HTML 文档，采用浅色主题并内联 CSS 和必要的 JavaScript；不使用 Markdown 代码围栏，可选", required = false) String answerText,
            @McpToolParam(description = "排序值，非负整数，默认0", required = false) Integer sortOrder) {
        McpHtmlAnswerValidator.validate(answerText);
        return service.create(topicId, questionText, answerText, sortOrder);
    }

    /** 根据问题 ID 查询问题及其答案。 */
    @McpTool(name = "get_knowledge_question", description = "根据问题ID获取问题及答案")
    public KnowledgeQuestion get(@McpToolParam(description = "问题ID", required = true) long id) {
        return service.get(id);
    }

    /** 列出全部问题，或按所属题目筛选。 */
    @McpTool(name = "list_knowledge_questions", description = "分页查询，默认第一页6条、每页最多6条；返回total、pages和hasNext，按需查询后续页，避免一次读取全部数据；创建前检查已有内容")
    public PagedResult<KnowledgeQuestion> list(
            @McpToolParam(description = "可选的主题ID，不提供则查询全部", required = false) Long topicId,
            @McpToolParam(description = "页码，从1开始，默认1", required = false) Integer pageNum,
            @McpToolParam(description = "每页条数，默认6，最大6", required = false) Integer pageSize) {
        return service.list(topicId, PageRequest.of(pageNum, pageSize));
    }

    /** 修改问题内容、所属题目、答案和排序值。 */
    @McpTool(name = "update_knowledge_question", description = "更新已有问题，仅修改提供的字段；答案传空字符串可以清除")
    public KnowledgeQuestion update(
            @McpToolParam(description = "需要更新的问题ID", required = true) long id,
            @McpToolParam(description = "新的所属题目ID，可选", required = false) Long topicId,
            @McpToolParam(description = "新的问题内容，可选", required = false) String questionText,
            @McpToolParam(description = "新的参考答案，完整 HTML 文档（内联 CSS 与必要的 JavaScript），可选；空字符串表示清除答案", required = false) String answerText,
            @McpToolParam(description = "新的排序值，可选", required = false) Integer sortOrder) {
        McpHtmlAnswerValidator.validate(answerText);
        return service.update(id, topicId, questionText, answerText, sortOrder);
    }

    /** 根据问题 ID 删除问题。 */
    @McpTool(name = "delete_knowledge_question", description = "根据问题ID删除知识问题")
    public boolean delete(@McpToolParam(description = "问题ID", required = true) long id) {
        return service.delete(id);
    }
}
