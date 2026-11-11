package com.example.backend.knowledge.question.application.dto;

/** 问题相邻导航只返回标题和主键，不传输完整 HTML 答案。 */
public record QuestionNeighbors(QuestionLink previous, QuestionLink next) {
    public record QuestionLink(long id, String questionText) {}
}
