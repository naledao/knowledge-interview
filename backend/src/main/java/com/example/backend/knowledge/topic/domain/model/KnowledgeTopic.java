package com.example.backend.knowledge.topic.domain.model;

import java.time.LocalDateTime;

/** 知识题目领域模型：一个分类下可以包含多个题目。 */
public record KnowledgeTopic(Long id, Long categoryId, String title, String description,
                             int sortOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {
    /** 校验题目的分类归属、标题长度及排序值。 */
    public KnowledgeTopic {
        if (id != null && id <= 0) throw new IllegalArgumentException("题目ID必须大于0");
        if (categoryId == null || categoryId <= 0) throw new IllegalArgumentException("分类ID必须大于0");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("题目标题不能为空");
        title = title.trim();
        if (title.length() > 255) throw new IllegalArgumentException("题目标题不能超过255个字符");
        if (sortOrder < 0) throw new IllegalArgumentException("排序值不能为负数");
        if (description != null && description.isBlank()) description = null;
    }
}
