package com.example.backend.knowledge.category.domain.model;

import java.time.LocalDateTime;

/** 知识分类领域模型，封装分类数据及基本业务约束。 */
public record KnowledgeCategory(Long id, String name, String description, int sortOrder,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
    /** 校验并规范化分类名称、描述及排序值。 */
    public KnowledgeCategory {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        name = name.trim();
        if (name.length() > 128) {
            throw new IllegalArgumentException("分类名称不能超过128个字符");
        }
        if (sortOrder < 0) {
            throw new IllegalArgumentException("排序值不能为负数");
        }
        if (description != null && description.isBlank()) {
            description = null;
        }
    }
}
