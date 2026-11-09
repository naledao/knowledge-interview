package com.example.backend.knowledge.question.infrastructure.persistence;

import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import java.time.LocalDateTime;

/** MyBatis 持久化对象，对应 knowledge_question 表，与领域模型隔离。 */
public class KnowledgeQuestionRow {
    private Long id;
    private Long topicId;
    private String questionText;
    private String answerText;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KnowledgeQuestionRow() { }

    /** 将领域模型转换为数据库持久化对象。 */
    public KnowledgeQuestionRow(KnowledgeQuestion question) {
        this.id = question.id();
        this.topicId = question.topicId();
        this.questionText = question.questionText();
        this.answerText = question.answerText();
        this.sortOrder = question.sortOrder();
        this.createdAt = question.createdAt();
        this.updatedAt = question.updatedAt();
    }

    /** 将数据库查询结果转换为领域模型。 */
    public KnowledgeQuestion toDomain() {
        return new KnowledgeQuestion(id, topicId, questionText, answerText, sortOrder, createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
