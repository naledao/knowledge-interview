package com.example.backend.knowledge.read.interfaces.rest;

import com.example.backend.knowledge.category.application.KnowledgeCategoryApplicationService;
import com.example.backend.knowledge.category.domain.model.KnowledgeCategory;
import com.example.backend.knowledge.question.application.KnowledgeQuestionApplicationService;
import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import com.example.backend.knowledge.topic.application.KnowledgeTopicApplicationService;
import com.example.backend.knowledge.topic.domain.model.KnowledgeTopic;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 网站公开只读接口，复用现有应用服务，不暴露 MCP 写操作。 */
@RestController
@RequestMapping("/api")
public class KnowledgeReadController {
    private final KnowledgeCategoryApplicationService categories;
    private final KnowledgeTopicApplicationService topics;
    private final KnowledgeQuestionApplicationService questions;

    public KnowledgeReadController(KnowledgeCategoryApplicationService categories,
                                   KnowledgeTopicApplicationService topics,
                                   KnowledgeQuestionApplicationService questions) {
        this.categories = categories;
        this.topics = topics;
        this.questions = questions;
    }

    @GetMapping("/categories")
    public List<KnowledgeCategory> listCategories() {
        return categories.list();
    }

    @GetMapping("/topics")
    public List<KnowledgeTopic> listTopics(@RequestParam(required = false) Long categoryId) {
        return topics.list(categoryId);
    }
    /** 按主题查询问题列表，复用问题应用服务中的关联校验和只读事务。 */
    @GetMapping("/questions")
    public List<KnowledgeQuestion> listQuestions(@RequestParam(required = false) Long topicId) {
        return questions.list(topicId);
    }
}