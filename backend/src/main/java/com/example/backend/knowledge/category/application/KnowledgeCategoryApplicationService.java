package com.example.backend.knowledge.category.application;

import com.example.backend.knowledge.category.domain.model.KnowledgeCategory;
import com.example.backend.knowledge.category.domain.repository.KnowledgeCategoryRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 分类应用服务，编排增删改查、事务及分类名称唯一性校验。 */
public class KnowledgeCategoryApplicationService {
    private final KnowledgeCategoryRepository repository;

    public KnowledgeCategoryApplicationService(KnowledgeCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    /** 创建分类，校验业务规则并返回完整持久化结果。 */
    public KnowledgeCategory create(String name, String description, Integer sortOrder) {
        KnowledgeCategory category = new KnowledgeCategory(null, name, description,
                sortOrder == null ? 0 : sortOrder, null, null);
        ensureNameAvailable(category.name(), null);
        try {
            long id = repository.insert(category);
            return get(id);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("分类名称已存在", ex);
        }
    }

    @Transactional(readOnly = true)
    /** 根据 ID 获取分类，不存在时抛出异常。 */
    public KnowledgeCategory get(long id) {
        checkId(id);
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("分类不存在: " + id));
    }

    @Transactional(readOnly = true)
    /** 获取全部分类。 */
    public List<KnowledgeCategory> list() {
        return repository.findAll();
    }

    @Transactional
    /** 更新分类，未提供的可选字段保留原值。 */
    public KnowledgeCategory update(long id, String name, String description, Integer sortOrder) {
        KnowledgeCategory old = get(id);
        KnowledgeCategory category = new KnowledgeCategory(id, name,
                description == null ? old.description() : description,
                sortOrder == null ? old.sortOrder() : sortOrder,
                old.createdAt(), old.updatedAt());
        ensureNameAvailable(category.name(), id);
        try {
            repository.update(category);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("分类名称已存在", ex);
        }
        return get(id);
    }

    @Transactional
    /** 删除分类；存在关联题目时拒绝删除。 */
    public boolean delete(long id) {
        get(id);
        try {
            return repository.deleteById(id) > 0;
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("分类下存在题目，不能直接删除分类", ex);
        }
    }

    /** 检查分类名称是否被其他分类占用。 */
    private void ensureNameAvailable(String name, Long currentId) {
        repository.findByName(name).ifPresent(existing -> {
            if (currentId == null || !currentId.equals(existing.id())) {
                throw new IllegalArgumentException("分类名称已存在: " + name);
            }
        });
    }

    /** 校验分类主键是否合法。 */
    private void checkId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("分类ID必须大于0");
        }
    }
}
