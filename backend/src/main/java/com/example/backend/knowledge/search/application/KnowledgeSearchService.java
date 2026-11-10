package com.example.backend.knowledge.search.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识搜索应用服务：数据库负责 AND 初筛和正文窗口截取，本服务负责相关度打分、排序、类型筛选和分页。
 * 个人知识库数据量有限，单类型候选上限用于兜住 "a" 这类宽泛关键词带来的大结果集。
 */
@Service
public class KnowledgeSearchService {
    public static final int MAX_KEYWORD_LENGTH = 100;
    public static final int MAX_TERMS = 5;
    public static final int MAX_PAGE_SIZE = 50;
    static final int CANDIDATE_LIMIT = 500;
    static final int EXCERPT_LENGTH = 160;

    private final KnowledgeSearchRepository repository;

    public KnowledgeSearchService(KnowledgeSearchRepository repository) {
        this.repository = repository;
    }

    /**
     * 多词 AND 搜索分类、主题、问题及答案。
     *
     * @param type all（或空）/ category / topic / question；只影响返回的列表，计数始终覆盖全部类型
     * @param page 从 1 开始；超过末页时返回末页
     */
    @Transactional(readOnly = true)
    public KnowledgeSearchResult search(String keyword, String type, int page, int size) {
        String normalized = keyword == null ? "" : keyword.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) throw new IllegalArgumentException("搜索关键词不能为空");
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("搜索关键词不能超过 " + MAX_KEYWORD_LENGTH + " 个字符");
        }
        KnowledgeSearchKind filter = parseType(type);
        if (page < 1) throw new IllegalArgumentException("页码必须大于 0");
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页条数必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }

        List<String> terms = parseTerms(normalized);
        String lead = terms.stream().max(Comparator.comparingInt(String::length)).orElseThrow();

        Map<KnowledgeSearchKind, List<KnowledgeSearchResult.Hit>> byKind = new EnumMap<>(KnowledgeSearchKind.class);
        boolean truncated = false;
        for (KnowledgeSearchKind kind : KnowledgeSearchKind.values()) {
            List<KnowledgeSearchCandidate> candidates = repository.search(kind, terms, lead, CANDIDATE_LIMIT);
            truncated |= candidates.size() >= CANDIDATE_LIMIT;
            byKind.put(kind, candidates.stream().map(candidate -> toHit(candidate, terms)).toList());
        }

        List<KnowledgeSearchResult.Hit> matched = new ArrayList<>();
        for (KnowledgeSearchKind kind : KnowledgeSearchKind.values()) {
            if (filter == null || filter == kind) matched.addAll(byKind.get(kind));
        }
        // 先按类型拼接、候选内部已按 sort_order, id 排序；List.sort 是稳定排序，同分时沿用该顺序。
        matched.sort(Comparator.comparingInt(KnowledgeSearchResult.Hit::score).reversed());

        int total = matched.size();
        int totalPages = Math.max(1, (total + size - 1) / size);
        int currentPage = Math.min(page, totalPages);
        int from = (currentPage - 1) * size;
        List<KnowledgeSearchResult.Hit> items = List.copyOf(matched.subList(from, Math.min(total, from + size)));

        int categories = byKind.get(KnowledgeSearchKind.CATEGORY).size();
        int topics = byKind.get(KnowledgeSearchKind.TOPIC).size();
        int questions = byKind.get(KnowledgeSearchKind.QUESTION).size();
        KnowledgeSearchResult.Counts counts = new KnowledgeSearchResult.Counts(
                categories + topics + questions, categories, topics, questions);
        return new KnowledgeSearchResult(normalized, terms, filter == null ? "all" : filter.value(),
                currentPage, size, total, totalPages, counts, truncated, items);
    }

    /** 按空白分词、转小写并去重；超出上限的词直接忽略，避免生成过长的 SQL。 */
    static List<String> parseTerms(String keyword) {
        return new LinkedHashSet<>(Arrays.asList(keyword.toLowerCase(Locale.ROOT).split(" "))).stream()
                .filter(term -> !term.isEmpty())
                .limit(MAX_TERMS)
                .toList();
    }

    private static KnowledgeSearchKind parseType(String type) {
        if (type == null || type.isBlank() || "all".equalsIgnoreCase(type.strip())) return null;
        try {
            return KnowledgeSearchKind.valueOf(type.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的搜索类型: " + type, ex);
        }
    }

    /** 标题完全匹配 > 前缀匹配 > 包含 > 仅正文命中。 */
    private static KnowledgeSearchResult.Hit toHit(KnowledgeSearchCandidate candidate, List<String> terms) {
        String title = candidate.title() == null ? "" : candidate.title();
        String heading = title.toLowerCase(Locale.ROOT);
        String details = candidate.body() == null ? "" : candidate.body().toLowerCase(Locale.ROOT);
        int score = 0;
        boolean bodyMatch = false;
        for (String term : terms) {
            if (heading.equals(term)) score += 120;
            else if (heading.startsWith(term)) score += 90;
            else if (heading.contains(term)) score += 65;
            else {
                // 数据库已保证每个词命中标题或正文；标题未命中（含排序规则差异）时按正文命中计分。
                score += 20;
                bodyMatch = true;
            }
            bodyMatch |= details.contains(term);
        }
        String breadcrumb = candidate.breadcrumb() == null || candidate.breadcrumb().isBlank()
                ? defaultBreadcrumb(candidate.kind()) : candidate.breadcrumb();
        return new KnowledgeSearchResult.Hit(candidate.kind().value(), candidate.id(), title,
                excerpt(candidate, terms), breadcrumb,
                candidate.kind() == KnowledgeSearchKind.QUESTION && bodyMatch, score);
    }

    private static String defaultBreadcrumb(KnowledgeSearchKind kind) {
        return switch (kind) {
            case CATEGORY -> "知识分类";
            case TOPIC -> "学习主题";
            case QUESTION -> "面试问题";
        };
    }

    /**
     * 以首个命中词为中心截取摘要，并结合窗口在完整正文中的位置补齐省略号。
     * 正文按 Markdown 存储，先去掉标记符号，避免摘要里出现 **、#、``` 等字符。
     */
    static String excerpt(KnowledgeSearchCandidate candidate, List<String> terms) {
        String raw = candidate.body() == null ? "" : candidate.body();
        boolean cutBefore = candidate.bodyOffset() > 1;
        boolean cutAfter = candidate.bodyOffset() - 1 + raw.length() < candidate.bodyLength();
        String text = MarkdownText.toPlainText(raw);
        if (text.isEmpty()) return "";
        String lower = text.toLowerCase(Locale.ROOT);
        int first = terms.stream().mapToInt(lower::indexOf).filter(index -> index >= 0).min().orElse(0);
        int start = Math.max(0, Math.min(first - 35, text.length() - EXCERPT_LENGTH));
        int end = Math.min(text.length(), start + EXCERPT_LENGTH);
        return (cutBefore || start > 0 ? "…" : "") + text.substring(start, end)
                + (cutAfter || end < text.length() ? "…" : "");
    }
}
