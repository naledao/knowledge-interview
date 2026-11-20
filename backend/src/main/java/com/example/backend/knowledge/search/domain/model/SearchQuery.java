package com.example.backend.knowledge.search.domain.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 搜索查询值对象：封装关键词规范化、分词规则以及问题 ID 精确匹配意图识别。
 */
public record SearchQuery(String normalizedKeyword, List<String> terms, Optional<Long> exactQuestionId) {
    /** 搜索关键词最大允许长度。 */
    public static final int MAX_KEYWORD_LENGTH = 100;
    /** 单次搜索最多参与匹配的分词数量。 */
    public static final int MAX_TERMS = 5;

    /** 匹配纯正整数、#ID 或 id:ID（不区分大小写，支持中英文冒号）的问题编号格式。 */
    private static final Pattern QUESTION_ID_PATTERN =
            Pattern.compile("^(?:#|(?i:id)[:：]\\s*)?([1-9][0-9]*)$");

    /**
     * 从原始用户输入构造经过校验与规范化的搜索查询值对象。
     *
     * @param rawKeyword 用户输入的原始搜索词
     * @return 规范化后的搜索查询值对象
     */
    public static SearchQuery of(String rawKeyword) {
        String normalized = rawKeyword == null ? "" : rawKeyword.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("搜索关键词不能超过 " + MAX_KEYWORD_LENGTH + " 个字符");
        }
        List<String> terms = parseTerms(normalized);
        Optional<Long> questionId = parseExactQuestionId(normalized);
        return new SearchQuery(normalized, terms, questionId);
    }

    /**
     * 提取用于数据库正文窗口定位的主导词（取最长分词）。
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
     * 识别规范化后的搜索词是否表达精确查询某道问题 ID 的意图。
     * 支持纯数字（如 "42"）、井号前缀（如 "#42"）以及前缀写法（如 "id:42"、"ID：42"）。
     */
    public static Optional<Long> parseExactQuestionId(String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = QUESTION_ID_PATTERN.matcher(normalizedKeyword.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            long id = Long.parseLong(matcher.group(1));
            return id > 0 ? Optional.of(id) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}