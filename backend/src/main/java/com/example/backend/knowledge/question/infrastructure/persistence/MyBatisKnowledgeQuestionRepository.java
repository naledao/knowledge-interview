package com.example.backend.knowledge.question.infrastructure.persistence;

import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import com.example.backend.knowledge.question.domain.repository.KnowledgeQuestionRepository;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 问题仓储的 MyBatis 实现，负责持久化对象与领域模型之间的转换。 */
@Repository
public class MyBatisKnowledgeQuestionRepository implements KnowledgeQuestionRepository {
    private final KnowledgeQuestionMapper mapper;

    public MyBatisKnowledgeQuestionRepository(KnowledgeQuestionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<KnowledgeQuestion> findById(long id) {
        return Optional.ofNullable(mapper.findById(id)).map(KnowledgeQuestionRow::toDomain);
    }

    @Override
    /** 在 MyBatis 查询阶段执行 SQL 分页，避免先加载全量数据。 */
    public PagedResult<KnowledgeQuestion> findPage(Long topicId, PageRequest request) {
        PageInfo<KnowledgeQuestionRow> page = PageHelper.startPage(request.pageNum(), request.pageSize())
                .doSelectPageInfo(() -> { if (topicId == null) mapper.findAll(); else mapper.findByTopicId(topicId); });
        return new PagedResult<>(page.getPageNum(), page.getPageSize(), page.getTotal(),
                page.getPages(), page.isHasNextPage(),
                page.getList().stream().map(KnowledgeQuestionRow::toDomain).toList());
    }

    @Override
    public List<KnowledgeQuestion> findAll() {
        return mapper.findAll().stream().map(KnowledgeQuestionRow::toDomain).toList();
    }

    @Override
    public List<KnowledgeQuestion> findByTopicId(long topicId) {
        return mapper.findByTopicId(topicId).stream().map(KnowledgeQuestionRow::toDomain).toList();
    }

    /** 查询同一主题的上一题。 */
    @Override
    public Optional<KnowledgeQuestion> findPrevious(long topicId, int sortOrder, long id) {
        return Optional.ofNullable(mapper.findPrevious(topicId, sortOrder, id)).map(KnowledgeQuestionRow::toDomain);
    }

    /** 查询同一主题的下一题。 */
    @Override
    public Optional<KnowledgeQuestion> findNext(long topicId, int sortOrder, long id) {
        return Optional.ofNullable(mapper.findNext(topicId, sortOrder, id)).map(KnowledgeQuestionRow::toDomain);
    }
    /** 随机查询一道题的主键，excludeId 非空时排除该题。 */
    @Override
    public Optional<Long> findRandomId(Long excludeId) {
        return Optional.ofNullable(mapper.findRandomId(excludeId));
    }

    /** 保存问题并返回生成的主键。 */
    @Override
    public long insert(KnowledgeQuestion question) {
        KnowledgeQuestionRow row = new KnowledgeQuestionRow(question);
        mapper.insert(row);
        if (row.getId() == null) throw new IllegalStateException("创建问题后未获取到主键");
        return row.getId();
    }

    @Override
    public int update(KnowledgeQuestion question) {
        return mapper.update(new KnowledgeQuestionRow(question));
    }

    @Override
    public int deleteById(long id) {
        return mapper.deleteById(id);
    }
}
