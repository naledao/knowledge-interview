package com.example.backend.knowledge.category.infrastructure.persistence;

import java.time.LocalDateTime;
import com.example.backend.knowledge.category.domain.model.KnowledgeCategory;

/** MyBatis 持久化对象，对应 knowledge_category 表，与领域模型隔离。 */
public class KnowledgeCategoryRow {
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KnowledgeCategoryRow() { }

    /** 将领域模型转换为数据库持久化对象。 */
    public KnowledgeCategoryRow(KnowledgeCategory category) {
        this.id = category.id();
        this.name = category.name();
        this.description = category.description();
        this.sortOrder = category.sortOrder();
        this.createdAt = category.createdAt();
        this.updatedAt = category.updatedAt();
    }

    /** 将数据库查询结果转换为领域模型。 */
    public KnowledgeCategory toDomain() {
        return new KnowledgeCategory(id, name, description, sortOrder, createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
