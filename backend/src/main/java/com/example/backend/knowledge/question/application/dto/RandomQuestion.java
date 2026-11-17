package com.example.backend.knowledge.question.application.dto;

/** 随机抽题结果：只返回问题主键，详情由前端跳转到问题详情页后再加载，避免传输 HTML 答案。 */
public record RandomQuestion(long id) { }
