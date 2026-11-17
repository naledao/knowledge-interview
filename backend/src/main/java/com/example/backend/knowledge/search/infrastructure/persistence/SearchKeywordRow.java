package com.example.backend.knowledge.search.infrastructure.persistence;

import com.example.backend.knowledge.search.domain.model.SearchKeyword;

/** MyBatis 持久化对象，对应 knowledge_search_keyword 表，与领域模型隔离。 */
public class SearchKeywordRow {
    private String keyword;
    private Long searchCount;
    private Boolean pinned;
    private Integer sortOrder;
    private Boolean enabled;

    public SearchKeywordRow() { }

    /** 将数据库查询结果转换为领域模型。 */
    public SearchKeyword toDomain() {
        return new SearchKeyword(keyword, searchCount, pinned, sortOrder, enabled);
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Long getSearchCount() { return searchCount; }
    public void setSearchCount(Long searchCount) { this.searchCount = searchCount; }
    public Boolean getPinned() { return pinned; }
    public void setPinned(Boolean pinned) { this.pinned = pinned; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
