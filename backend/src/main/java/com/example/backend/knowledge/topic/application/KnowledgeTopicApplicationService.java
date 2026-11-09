package com.example.backend.knowledge.topic.application;

import com.example.backend.knowledge.category.domain.repository.KnowledgeCategoryRepository;
import com.example.backend.knowledge.topic.domain.model.KnowledgeTopic;
import com.example.backend.knowledge.topic.domain.repository.KnowledgeTopicRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 题目应用服务，负责增删改查、分类关联校验及分类内标题唯一性。 */
@Service
public class KnowledgeTopicApplicationService {
    private final KnowledgeTopicRepository repository;
    private final KnowledgeCategoryRepository categoryRepository;

    public KnowledgeTopicApplicationService(KnowledgeTopicRepository repository,
                                            KnowledgeCategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    /** 创建题目，必须关联已存在的分类，同一分类内标题不能重复。 */
    @Transactional
    public KnowledgeTopic create(long categoryId, String title, String description, Integer sortOrder) {
        KnowledgeTopic topic = new KnowledgeTopic(null, categoryId, title, description,
                sortOrder == null ? 0 : sortOrder, null, null);
        ensureCategoryExists(topic.categoryId());
        ensureTitleAvailable(topic.categoryId(), topic.title(), null);
        try {
            long id = repository.insert(topic);
            return get(id);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("题目标题在分类内已存在，或所属分类无效", ex);
        }
    }

    /** 根据 ID 查询题目，不存在时抛出异常。 */
    @Transactional(readOnly = true)
    public KnowledgeTopic get(long id) {
        checkId(id);
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("题目不存在: " + id));
    }

    /** 查询题目：提供分类 ID 时按分类筛选，否则返回全部题目。 */
    @Transactional(readOnly = true)
    public List<KnowledgeTopic> list(Long categoryId) {
        if (categoryId == null) return repository.findAll();
        ensureCategoryExists(categoryId);
        return repository.findByCategoryId(categoryId);
    }

    /** 更新题目；未提供的字段保留原值，描述传空字符串可清除。 */
    @Transactional
    public KnowledgeTopic update(long id, Long categoryId, String title, String description, Integer sortOrder) {
        KnowledgeTopic old = get(id);
        KnowledgeTopic topic = new KnowledgeTopic(id,
                categoryId == null ? old.categoryId() : categoryId,
                title == null ? old.title() : title,
                description == null ? old.description() : description,
                sortOrder == null ? old.sortOrder() : sortOrder,
                old.createdAt(), old.updatedAt());
        ensureCategoryExists(topic.categoryId());
        ensureTitleAvailable(topic.categoryId(), topic.title(), id);
        try {
            if (repository.update(topic) == 0) throw new NoSuchElementException("题目不存在: " + id);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("题目标题在分类内已存在，或所属分类无效", ex);
        }
        return get(id);
    }

    /** 删除题目；如果下面已有问题，则禁止直接删除。 */
    @Transactional
    public boolean delete(long id) {
        get(id);
        try {
            return repository.deleteById(id) > 0;
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("题目下存在问题，不能直接删除题目", ex);
        }
    }

    /** 验证所属分类存在，避免产生无归属题目。 */
    private void ensureCategoryExists(long categoryId) {
        if (categoryId <= 0) throw new IllegalArgumentException("分类ID必须大于0");
        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw new NoSuchElementException("分类不存在: " + categoryId);
        }
    }

    /** 检查同一分类内的标题是否被其它题目占用。 */
    private void ensureTitleAvailable(long categoryId, String title, Long currentId) {
        repository.findByCategoryIdAndTitle(categoryId, title).ifPresent(existing -> {
            if (currentId == null || !currentId.equals(existing.id())) {
                throw new IllegalArgumentException("该分类下已存在同名题目: " + title);
            }
        });
    }

    /** 验证题目 ID 合法。 */
    private void checkId(long id) {
        if (id <= 0) throw new IllegalArgumentException("题目ID必须大于0");
    }
}
