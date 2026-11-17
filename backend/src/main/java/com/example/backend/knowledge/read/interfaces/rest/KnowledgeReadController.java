package com.example.backend.knowledge.read.interfaces.rest;

import com.example.backend.knowledge.category.application.KnowledgeCategoryApplicationService;
import com.example.backend.knowledge.category.domain.model.KnowledgeCategory;
import com.example.backend.knowledge.question.application.KnowledgeQuestionApplicationService;
import com.example.backend.knowledge.question.application.dto.QuestionNeighbors;
import com.example.backend.knowledge.question.application.dto.RandomQuestion;
import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import com.example.backend.knowledge.topic.application.KnowledgeTopicApplicationService;
import com.example.backend.knowledge.topic.domain.model.KnowledgeTopic;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 网站只读接口：列表使用数据库分页，详情通过主键查询，避免全量加载。 */
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

    /** 分类列表：默认第一页6条，最多6条。 */
    @GetMapping("/categories")
    public PagedResult<KnowledgeCategory> listCategories(@RequestParam(required = false) Integer pageNum,
                                                          @RequestParam(required = false) Integer pageSize) {
        return categories.list(PageRequest.of(pageNum, pageSize));
    }

    /** 分类详情不再依赖全量列表定位。 */
    @GetMapping("/categories/{id}")
    public KnowledgeCategory getCategory(@PathVariable long id) {
        return categories.get(id);
    }

    /** 可按分类筛选主题，保持分页与筛选同时在数据库执行。 */
    @GetMapping("/topics")
    public PagedResult<KnowledgeTopic> listTopics(@RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) Integer pageNum,
                                                   @RequestParam(required = false) Integer pageSize) {
        return topics.list(categoryId, PageRequest.of(pageNum, pageSize));
    }

    /** 通过主题 ID 查询单条数据。 */
    @GetMapping("/topics/{id}")
    public KnowledgeTopic getTopic(@PathVariable long id) {
        return topics.get(id);
    }

    /** 可按主题筛选问题，单页最多6条。 */
    @GetMapping("/questions")
    public PagedResult<KnowledgeQuestion> listQuestions(@RequestParam(required = false) Long topicId,
                                                         @RequestParam(required = false) Integer pageNum,
                                                         @RequestParam(required = false) Integer pageSize) {
        return questions.list(topicId, PageRequest.of(pageNum, pageSize));
    }

    /** 问题详情按 ID 查询，避免下载全部 HTML 答案。 */
    @GetMapping("/questions/{id}")
    public KnowledgeQuestion getQuestion(@PathVariable long id) {
        return questions.get(id);
    }

    /** 提供跨页的前后题导航信息。 */
    @GetMapping("/questions/{id}/neighbors")
    public QuestionNeighbors questionNeighbors(@PathVariable long id) {
        return questions.neighbors(id);
    }

    /**
     * 随机抽取一道题，返回其主键，前端据此跳转到详情页。
     * excludeId 用于避免连续抽到同一道；固定路径 random 优先于 {id} 匹配。
     */
    @GetMapping("/questions/random")
    public RandomQuestion randomQuestion(@RequestParam(required = false) Long excludeId) {
        return questions.random(excludeId);
    }
    /** 非法分页参数统一返回 HTTP 400。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalidArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** 不存在的分类、主题或问题返回 HTTP 404。 */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail notFound(NoSuchElementException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
