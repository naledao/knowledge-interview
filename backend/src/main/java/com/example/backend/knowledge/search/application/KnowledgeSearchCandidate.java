package com.example.backend.knowledge.search.application;

/**
 * 数据库初筛命中的候选记录。
 *
 * @param body       正文窗口：只截取首个关键词附近的片段，避免把 LONGTEXT 答案整段读入内存
 * @param bodyOffset 窗口在完整正文中的起始位置（从 1 开始）
 * @param bodyLength 完整正文长度，用于判断摘要尾部是否需要省略号
 */
public record KnowledgeSearchCandidate(KnowledgeSearchKind kind, long id, String title, String body,
                                       int bodyOffset, int bodyLength, String breadcrumb) {
}
