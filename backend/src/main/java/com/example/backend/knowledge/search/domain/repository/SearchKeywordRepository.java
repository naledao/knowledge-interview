package com.example.backend.knowledge.search.domain.repository;

import com.example.backend.knowledge.search.domain.model.SearchKeyword;
import java.util.List;

/** 搜索关键词领域仓储接口，定义统计与查询操作，不依赖具体数据库实现。 */
public interface SearchKeywordRepository {
    /** 记录一次搜索：关键词不存在时新增，已存在时次数加一并刷新最近搜索时间。 */
    void recordSearch(String keyword);

    /** 查询热门搜索词：仅包含启用的词，置顶词优先，其次按搜索次数倒序。 */
    List<SearchKeyword> findHot(int limit);
}
