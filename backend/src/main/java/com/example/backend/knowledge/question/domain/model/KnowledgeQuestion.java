package com.example.backend.knowledge.question.domain.model;

import java.time.LocalDateTime;

/** 知识问题领域模型：一个题目下可以包含多个问题，每个问题可以保存答案。 */
public record KnowledgeQuestion(Long id, Long topicId, String questionText, String answerText,
                                int sortOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {
    /** 校验问题内容、所属题目及排序值，并规范化可选答案。 */
    public KnowledgeQuestion {
        if (id != null && id <= 0) throw new IllegalArgumentException("问题ID必须大于0");
        if (topicId == null || topicId <= 0) throw new IllegalArgumentException("题目ID必须大于0");
        if (questionText == null || questionText.isBlank()) throw new IllegalArgumentException("问题内容不能为空");
        questionText = questionText.trim();
        if (sortOrder < 0) throw new IllegalArgumentException("排序值不能为负数");
        if (answerText != null && answerText.isBlank()) answerText = null;
    }
}
