package com.example.backend.knowledge.search.application;

import com.example.backend.knowledge.search.domain.model.SearchKeyword;
import com.example.backend.knowledge.search.domain.repository.SearchKeywordRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 热门搜索词应用服务：负责统计有结果的搜索词，并提供搜索页的推荐词列表。 */
@Service
public class HotSearchKeywordService {
    public static final int DEFAULT_LIMIT = 5;
    public static final int MAX_LIMIT = 20;

    private final SearchKeywordRepository repository;

    public HotSearchKeywordService(SearchKeywordRepository repository) {
        this.repository = repository;
    }

    /** 记录一次有结果的搜索；空白或过长的关键词直接忽略。 */
    @Transactional
    public void recordSearch(String keyword) {
        SearchKeyword.normalizeForRecord(keyword).ifPresent(repository::recordSearch);
    }

    /**
     * 查询热门搜索词文本。
     *
     * @param limit 返回数量，范围 1 到 {@value #MAX_LIMIT}
     */
    @Transactional(readOnly = true)
    public List<String> listHotKeywords(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("热门词数量必须在 1 到 " + MAX_LIMIT + " 之间");
        }
        return repository.findHot(limit).stream().map(SearchKeyword::keyword).toList();
    }
}
