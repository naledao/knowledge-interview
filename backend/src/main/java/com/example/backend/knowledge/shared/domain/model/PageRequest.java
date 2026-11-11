package com.example.backend.knowledge.shared.domain.model;

/** MCP 列表查询的分页请求，统一限制页码和单页数量。 */
public record PageRequest(int pageNum, int pageSize) {
    public PageRequest {
        if (pageNum < 1) throw new IllegalArgumentException("页码必须大于等于1");
        if (pageSize < 1 || pageSize > 6) throw new IllegalArgumentException("每页条数必须在1到6之间");
    }

    /** 未提供分页参数时默认查询第一页，每页6条。 */
    public static PageRequest of(Integer pageNum, Integer pageSize) {
        return new PageRequest(pageNum == null ? 1 : pageNum, pageSize == null ? 6 : pageSize);
    }
}
