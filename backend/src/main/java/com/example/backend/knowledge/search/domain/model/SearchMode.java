package com.example.backend.knowledge.search.domain.model;

import java.util.Locale;

/**
 * 搜索模式枚举：区分普通关键词模糊搜索与精确搜索。
 */
public enum SearchMode {
    /** 普通关键词搜索：按空格分词进行多词 AND 模糊匹配。 */
    KEYWORD("keyword"),
    /** 精确搜索：目前仅支持按问题主键 ID 精确查找。 */
    EXACT("exact");

    private final String value;

    SearchMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /**
     * 解析外部传入的搜索模式参数，缺省时默认为普通关键词搜索。
     */
    public static SearchMode fromValue(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return KEYWORD;
        }
        String normalized = rawMode.strip().toLowerCase(Locale.ROOT);
        for (SearchMode mode : values()) {
            if (mode.value.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("不支持的搜索模式: " + rawMode);
    }
}