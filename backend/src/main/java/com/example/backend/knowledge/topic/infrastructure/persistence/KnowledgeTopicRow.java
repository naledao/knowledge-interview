package com.example.backend.knowledge.topic.infrastructure.persistence;

import com.example.backend.knowledge.topic.domain.model.KnowledgeTopic;
import java.time.LocalDateTime;

/** MyBatis 持久化对象，对应 knowledge_topic 数据表。 */
public class KnowledgeTopicRow {
    private Long id;
    private Long categoryId;
    private String title;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KnowledgeTopicRow() { }

    /** 将领域模型转换为持久化对象。 */
    public KnowledgeTopicRow(KnowledgeTopic topic) {
        this.id = topic.id();
        this.categoryId = topic.categoryId();
        this.title = topic.title();
        this.description = topic.description();
        this.sortOrder = topic.sortOrder();
        this.createdAt = topic.createdAt();
        this.updatedAt = topic.updatedAt();
    }

    /** 将数据库记录转换为领域模型。 */
    public KnowledgeTopic toDomain() {
        return new KnowledgeTopic(id, categoryId, title, description, sortOrder, createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
