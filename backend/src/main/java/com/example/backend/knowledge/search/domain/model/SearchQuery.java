package com.example.backend.knowledge.search.domain.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 搜索查询值对象：根据搜索模式封装关键词规范化、分词规则或精确问题 ID 解析。
 */
public record SearchQuery(SearchMode mode, String normalizedKeyword, List<String> terms, Optional<Long> exactQuestionId) {
    /** 搜索关键词最大允许长度。 */
    public static final int MAX_KEYWORD_LENGTH = 100;
    /** 单次搜索最多参与匹配的分词数量。 */
    public static final int MAX_TERMS = 5;

    /** 精确问题 ID 格式：仅接受正整数。 */
    private static final Pattern POSITIVE_INTEGER_PATTERN = Pattern.compile("^[1-9][0-9]*$");

    /**
     * 使用默认关键词搜索模式构造查询值对象。
     */
    public static SearchQuery of(String rawKeyword) {
        return of(rawKeyword, SearchMode.KEYWORD);
    }

    /**
     * 按指定搜索模式校验并构造查询值对象。
     *
     * @param rawKeyword 用户输入的原始搜索内容
     * @param mode       搜索模式（关键词搜索或精确搜索）
     * @return 规范化后的搜索查询值对象
     */
    public static SearchQuery of(String rawKeyword, SearchMode mode) {
        SearchMode effectiveMode = mode == null ? SearchMode.KEYWORD : mode;
        String normalized = rawKeyword == null ? "" : rawKeyword.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(effectiveMode == SearchMode.EXACT
                    ? "请输入要精确搜索的问题 ID"
                    : "搜索关键词不能为空");
        }
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("搜索关键词不能超过 " + MAX_KEYWORD_LENGTH + " 个字符");
        }
        if (effectiveMode == SearchMode.EXACT) {
            long questionId = parseExactQuestionId(normalized);
            return new SearchQuery(SearchMode.EXACT, normalized, List.of(), Optional.of(questionId));
        }
        return new SearchQuery(SearchMode.KEYWORD, normalized, parseTerms(normalized), Optional.empty());
    }

    /**
     * 提取用于数据库正文窗口定位的主导词（取最长分词，仅关键词模式使用）。
     */
    public String leadTerm() {
        return terms.stream()
                .max((a, b) -> Integer.compare(a.length(), b.length()))
                .orElseThrow();
    }

    /**
     * 按空白分词、转小写并去重；超出上限的词直接忽略，避免生成过长的 SQL。
     */
    public static List<String> parseTerms(String keyword) {
        return new LinkedHashSet<>(Arrays.asList(keyword.toLowerCase(Locale.ROOT).split(" "))).stream()
                .filter(term -> !term.isEmpty())
                .limit(MAX_TERMS)
                .toList();
    }

    /**
     * 校验并解析正整数问题 ID。
     */
    public static long parseExactQuestionId(String normalizedKeyword) {
        if (normalizedKeyword == null || !POSITIVE_INTEGER_PATTERN.matcher(normalizedKeyword).matches()) {
            throw new IllegalArgumentException("精确搜索目前仅支持输入正整数问题 ID");
        }
        try {
            long id = Long.parseLong(normalizedKeyword);
            if (id <= 0) {
                throw new IllegalArgumentException("精确搜索目前仅支持输入正整数问题 ID");
            }
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("问题 ID 超出有效范围", ex);
        }
    }
}