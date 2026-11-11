package com.example.backend.knowledge.topic.infrastructure.persistence;

import com.example.backend.knowledge.topic.domain.model.KnowledgeTopic;
import com.example.backend.knowledge.topic.domain.repository.KnowledgeTopicRepository;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 题目仓储的 MyBatis 实现，负责领域模型与数据库记录之间的转换。 */
@Repository
public class MyBatisKnowledgeTopicRepository implements KnowledgeTopicRepository {
    private final KnowledgeTopicMapper mapper;

    public MyBatisKnowledgeTopicRepository(KnowledgeTopicMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<KnowledgeTopic> findById(long id) {
        return Optional.ofNullable(mapper.findById(id)).map(KnowledgeTopicRow::toDomain);
    }

    @Override
    public Optional<KnowledgeTopic> findByCategoryIdAndTitle(long categoryId, String title) {
        return Optional.ofNullable(mapper.findByCategoryIdAndTitle(categoryId, title)).map(KnowledgeTopicRow::toDomain);
    }

    @Override
    /** 在 MyBatis 查询阶段执行 SQL 分页，避免先加载全量数据。 */
    public PagedResult<KnowledgeTopic> findPage(Long categoryId, PageRequest request) {
        PageInfo<KnowledgeTopicRow> page = PageHelper.startPage(request.pageNum(), request.pageSize())
                .doSelectPageInfo(() -> { if (categoryId == null) mapper.findAll(); else mapper.findByCategoryId(categoryId); });
        return new PagedResult<>(page.getPageNum(), page.getPageSize(), page.getTotal(),
                page.getPages(), page.isHasNextPage(),
                page.getList().stream().map(KnowledgeTopicRow::toDomain).toList());
    }

    @Override
    public List<KnowledgeTopic> findAll() {
        return mapper.findAll().stream().map(KnowledgeTopicRow::toDomain).toList();
    }

    @Override
    public List<KnowledgeTopic> findByCategoryId(long categoryId) {
        return mapper.findByCategoryId(categoryId).stream().map(KnowledgeTopicRow::toDomain).toList();
    }

    @Override
    public long insert(KnowledgeTopic topic) {
        KnowledgeTopicRow row = new KnowledgeTopicRow(topic);
        mapper.insert(row);
        if (row.getId() == null) throw new IllegalStateException("创建题目后未获取到主键");
        return row.getId();
    }

    @Override
    public int update(KnowledgeTopic topic) {
        return mapper.update(new KnowledgeTopicRow(topic));
    }

    @Override
    public int deleteById(long id) {
        return mapper.deleteById(id);
    }
}
