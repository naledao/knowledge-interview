package com.example.backend.knowledge.category.domain.repository;

import com.example.backend.knowledge.category.domain.model.KnowledgeCategory;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import java.util.List;
import java.util.Optional;

/** 分类领域仓储接口，定义持久化操作，不依赖具体数据库实现。 */
public interface KnowledgeCategoryRepository {
    /** 根据主键查询分类。 */
    Optional<KnowledgeCategory> findById(long id);
    /** 根据名称查询分类，用于唯一性校验。 */
    Optional<KnowledgeCategory> findByName(String name);
    /** 查询全部分类。 */
    /** 数据库分页查询，返回当前页及总数。 */
    PagedResult<KnowledgeCategory> findPage(PageRequest request);
    List<KnowledgeCategory> findAll();
    /** 新增分类并返回数据库生成的主键。 */
    long insert(KnowledgeCategory category);
    /** 更新分类，返回受影响行数。 */
    int update(KnowledgeCategory category);
    /** 根据主键删除分类，返回受影响行数。 */
    int deleteById(long id);
}
