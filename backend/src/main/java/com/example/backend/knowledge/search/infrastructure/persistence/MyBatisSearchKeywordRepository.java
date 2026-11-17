package com.example.backend.knowledge.search.infrastructure.persistence;

import com.example.backend.knowledge.search.domain.model.SearchKeyword;
import com.example.backend.knowledge.search.domain.repository.SearchKeywordRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 搜索关键词仓储的 MyBatis 实现，负责领域模型与持久化对象之间的转换。 */
@Repository
public class MyBatisSearchKeywordRepository implements SearchKeywordRepository {
    private final SearchKeywordMapper mapper;

    public MyBatisSearchKeywordRepository(SearchKeywordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void recordSearch(String keyword) {
        mapper.upsertSearch(keyword);
    }

    @Override
    public List<SearchKeyword> findHot(int limit) {
        return mapper.findHot(limit).stream().map(SearchKeywordRow::toDomain).toList();
    }
}
