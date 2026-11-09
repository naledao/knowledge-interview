package com.example.backend.knowledge.question.infrastructure.persistence;

import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import com.example.backend.knowledge.question.domain.repository.KnowledgeQuestionRepository;
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
    public List<KnowledgeQuestion> findAll() {
        return mapper.findAll().stream().map(KnowledgeQuestionRow::toDomain).toList();
    }

    @Override
    public List<KnowledgeQuestion> findByTopicId(long topicId) {
        return mapper.findByTopicId(topicId).stream().map(KnowledgeQuestionRow::toDomain).toList();
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
