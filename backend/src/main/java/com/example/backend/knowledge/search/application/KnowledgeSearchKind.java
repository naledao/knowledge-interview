package com.example.backend.knowledge.search.application;

import java.util.Locale;

/** 搜索结果类型，声明顺序同时作为同分时的展示顺序：分类 → 主题 → 问题。 */
public enum KnowledgeSearchKind {
    CATEGORY, TOPIC, QUESTION;

    /** 接口中使用的小写类型名，例如 category。 */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
