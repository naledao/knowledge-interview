package com.example.backend.knowledge.category.infrastructure.persistence;

import com.example.backend.knowledge.category.domain.model.KnowledgeCategory;
import com.example.backend.knowledge.category.domain.repository.KnowledgeCategoryRepository;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
/** 分类仓储的 MyBatis 实现，负责领域模型与持久化对象之间的转换。 */
public class MyBatisKnowledgeCategoryRepository implements KnowledgeCategoryRepository {
    private final KnowledgeCategoryMapper mapper;

    public MyBatisKnowledgeCategoryRepository(KnowledgeCategoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<KnowledgeCategory> findById(long id) {
        return Optional.ofNullable(mapper.findById(id)).map(KnowledgeCategoryRow::toDomain);
    }

    @Override
    public Optional<KnowledgeCategory> findByName(String name) {
        return Optional.ofNullable(mapper.findByName(name)).map(KnowledgeCategoryRow::toDomain);
    }

    @Override
    /** 在 MyBatis 查询阶段执行 SQL 分页，避免先加载全量数据。 */
    public PagedResult<KnowledgeCategory> findPage(PageRequest request) {
        PageInfo<KnowledgeCategoryRow> page = PageHelper.startPage(request.pageNum(), request.pageSize())
                .doSelectPageInfo(mapper::findAll);
        return new PagedResult<>(page.getPageNum(), page.getPageSize(), page.getTotal(),
                page.getPages(), page.isHasNextPage(),
                page.getList().stream().map(KnowledgeCategoryRow::toDomain).toList());
    }

    @Override
    public List<KnowledgeCategory> findAll() {
        return mapper.findAll().stream().map(KnowledgeCategoryRow::toDomain).toList();
    }

    @Override
    /** 保存分类并返回生成的主键。 */
    public long insert(KnowledgeCategory category) {
        KnowledgeCategoryRow row = new KnowledgeCategoryRow(category);
        mapper.insert(row);
        if (row.getId() == null) {
            throw new IllegalStateException("创建分类后未获取到主键");
        }
        return row.getId();
    }

    @Override
    public int update(KnowledgeCategory category) {
        return mapper.update(new KnowledgeCategoryRow(category));
    }

    @Override
    public int deleteById(long id) {
        return mapper.deleteById(id);
    }
}
