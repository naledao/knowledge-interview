package com.example.backend.knowledge.shared.domain.model;

import java.util.List;

/** 分页结果包含当前页数据和翻页元信息，不暴露具体持久化框架。 */
public record PagedResult<T>(int pageNum, int pageSize, long total, int pages, boolean hasNext, List<T> items) {
    public PagedResult {
        items = List.copyOf(items);
    }
}
