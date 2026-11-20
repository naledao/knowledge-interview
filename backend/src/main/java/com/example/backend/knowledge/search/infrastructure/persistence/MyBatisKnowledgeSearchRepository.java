package com.example.backend.knowledge.search.infrastructure.persistence;

import com.example.backend.knowledge.search.application.KnowledgeSearchCandidate;
import com.example.backend.knowledge.search.application.KnowledgeSearchKind;
import com.example.backend.knowledge.search.application.KnowledgeSearchRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 搜索仓储的 MyBatis 实现：负责 LIKE 模式转义、按类型分派查询以及按主键精确查询问题。 */
@Repository
public class MyBatisKnowledgeSearchRepository implements KnowledgeSearchRepository {
    private final KnowledgeSearchMapper mapper;

    public MyBatisKnowledgeSearchRepository(KnowledgeSearchMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<KnowledgeSearchCandidate> search(KnowledgeSearchKind kind, List<String> terms, String lead, int limit) {
        List<String> patterns = terms.stream().map(term -> "%" + escapeLike(term) + "%").toList();
        List<KnowledgeSearchRow> rows = switch (kind) {
            case CATEGORY -> mapper.searchCategories(patterns, lead, limit);
            case TOPIC -> mapper.searchTopics(patterns, lead, limit);
            case QUESTION -> mapper.searchQuestions(patterns, lead, limit);
        };
        return rows.stream().map(row -> row.toCandidate(kind)).toList();
    }

    @Override
    public Optional<KnowledgeSearchCandidate> findQuestionById(long id) {
        KnowledgeSearchRow row = mapper.findQuestionById(id);
        return Optional.ofNullable(row).map(r -> r.toCandidate(KnowledgeSearchKind.QUESTION));
    }

    /** 转义 LIKE 通配符，使用户输入的 %、_ 按字面匹配；转义符与 SQL 中的 ESCAPE '!' 保持一致。 */
    static String escapeLike(String term) {
        return term.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}