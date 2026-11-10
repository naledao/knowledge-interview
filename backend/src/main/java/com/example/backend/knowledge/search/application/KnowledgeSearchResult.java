package com.example.backend.knowledge.search.application;

import java.util.List;

/**
 * 搜索接口响应：当前页结果、各类型命中数及分页信息。
 *
 * @param terms     实际参与匹配的关键词，前端据此高亮，保证与后端匹配规则一致
 * @param truncated 某个类型的命中数达到候选上限，计数可能偏少
 */
public record KnowledgeSearchResult(String keyword, List<String> terms, String type,
                                    int page, int size, int total, int totalPages,
                                    Counts counts, boolean truncated, List<Hit> items) {

    /** 各类型命中数，前端筛选标签直接展示，无需额外请求。 */
    public record Counts(int all, int category, int topic, int question) { }

    /**
     * 单条搜索结果，正文只返回截断后的摘要。
     *
     * @param kind        category / topic / question
     * @param answerMatch 问题类型下，摘要是否来自命中的答案
     */
    public record Hit(String kind, long id, String title, String excerpt,
                      String breadcrumb, boolean answerMatch, int score) { }
}
