package com.example.backend.knowledge.question.application;

import com.example.backend.knowledge.question.application.dto.QuestionNeighbors;
import com.example.backend.knowledge.question.application.dto.RandomQuestion;
import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import com.example.backend.knowledge.question.domain.repository.KnowledgeQuestionRepository;
import com.example.backend.knowledge.topic.domain.repository.KnowledgeTopicRepository;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 问题应用服务：编排增删改查、题目关联校验及事务。 */
@Service
public class KnowledgeQuestionApplicationService {
    private final KnowledgeQuestionRepository repository;
    private final KnowledgeTopicRepository topicRepository;

    public KnowledgeQuestionApplicationService(KnowledgeQuestionRepository repository,
                                               KnowledgeTopicRepository topicRepository) {
        this.repository = repository;
        this.topicRepository = topicRepository;
    }

    /** 创建问题，所属题目必须已存在，答案允许为空。 */
    @Transactional
    public KnowledgeQuestion create(long topicId, String questionText, String answerText, Integer sortOrder) {
        KnowledgeQuestion question = new KnowledgeQuestion(null, topicId, questionText, answerText,
                sortOrder == null ? 0 : sortOrder, null, null);
        ensureTopicExists(question.topicId());
        try {
            long id = repository.insert(question);
            return get(id);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("问题保存失败，请检查所属题目和数据库约束", ex);
        }
    }

    /** 根据 ID 查询问题，不存在时抛出异常。 */
    @Transactional(readOnly = true)
    public KnowledgeQuestion get(long id) {
        checkId(id);
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("问题不存在: " + id));
    }

    /** 查询问题：指定题目时只返回该题目的问题，否则返回全部问题。 */
    @Transactional(readOnly = true)
    public PagedResult<KnowledgeQuestion> list(Long topicId, PageRequest request) {
        if (topicId != null) ensureTopicExists(topicId);
        return repository.findPage(topicId, request);
    }

    /** 保留网站 REST 接口原有列表查询行为，MCP 工具只使用分页方法。 */
    @Transactional(readOnly = true)
    public List<KnowledgeQuestion> list(Long topicId) {
        if (topicId == null) return repository.findAll(); ensureTopicExists(topicId); return repository.findByTopicId(topicId);
    }

    /** 数据库定向查询上一题和下一题，避免读取整个题目列表及 HTML 答案。 */
    @Transactional(readOnly = true)
    public QuestionNeighbors neighbors(long id) {
        KnowledgeQuestion current = get(id);
        var previous = repository.findPrevious(current.topicId(), current.sortOrder(), id)
                .map(item -> new QuestionNeighbors.QuestionLink(item.id(), item.questionText())).orElse(null);
        var next = repository.findNext(current.topicId(), current.sortOrder(), id)
                .map(item -> new QuestionNeighbors.QuestionLink(item.id(), item.questionText())).orElse(null);
        return new QuestionNeighbors(previous, next);
    }

    /**
     * 随机抽取一道题，仅返回主键。
     * excludeId 非空时优先排除该题，避免连续抽到同一道；题库只有这一题时仍返回它，题库为空时抛出异常。
     */
    @Transactional(readOnly = true)
    public RandomQuestion random(Long excludeId) {
        return repository.findRandomId(excludeId)
                .or(() -> repository.findRandomId(null))
                .map(RandomQuestion::new)
                .orElseThrow(() -> new NoSuchElementException("暂无题目"));
    }
    /** 更新问题；未提供的字段保留原值，答案传空字符串可清除。 */
    @Transactional
    public KnowledgeQuestion update(long id, Long topicId, String questionText,
                                    String answerText, Integer sortOrder) {
        KnowledgeQuestion old = get(id);
        KnowledgeQuestion question = new KnowledgeQuestion(id,
                topicId == null ? old.topicId() : topicId,
                questionText == null ? old.questionText() : questionText,
                answerText == null ? old.answerText() : answerText,
                sortOrder == null ? old.sortOrder() : sortOrder,
                old.createdAt(), old.updatedAt());
        ensureTopicExists(question.topicId());
        try {
            if (repository.update(question) == 0) {
                throw new NoSuchElementException("问题不存在: " + id);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("问题更新失败，请检查所属题目和数据库约束", ex);
        }
        return get(id);
    }

    /** 根据 ID 删除问题。 */
    @Transactional
    public boolean delete(long id) {
        get(id);
        return repository.deleteById(id) > 0;
    }

    /** 验证所属题目存在，防止问题引用不存在的题目。 */
    private void ensureTopicExists(long topicId) {
        if (topicId <= 0) throw new IllegalArgumentException("题目ID必须大于0");
        if (topicRepository.findById(topicId).isEmpty()) {
            throw new NoSuchElementException("题目不存在: " + topicId);
        }
    }

    /** 验证问题主键合法。 */
    private void checkId(long id) {
        if (id <= 0) throw new IllegalArgumentException("问题ID必须大于0");
    }
}
