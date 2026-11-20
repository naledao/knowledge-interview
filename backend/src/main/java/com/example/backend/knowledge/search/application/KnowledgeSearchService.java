package com.example.backend.knowledge.search.application;

import com.example.backend.knowledge.search.domain.model.SearchMode;
import com.example.backend.knowledge.search.domain.model.SearchQuery;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识搜索应用服务：区分普通关键词模糊搜索与精确搜索（目前支持按问题 ID 精确查找），并负责相关度打分、排序、类型筛选和分页。
 * 个人知识库数据量有限，单类型候选上限用于兜住 "a" 这类宽泛关键词带来的大结果集。
 */
@Service
public class KnowledgeSearchService {
    public static final int MAX_KEYWORD_LENGTH = SearchQuery.MAX_KEYWORD_LENGTH;
    public static final int MAX_TERMS = SearchQuery.MAX_TERMS;
    public static final int MAX_PAGE_SIZE = 50;
    static final int CANDIDATE_LIMIT = 500;
    static final int EXCERPT_LENGTH = 160;
    /** 精确匹配命中的固定分值。 */
    static final int EXACT_MATCH_SCORE = 10_000;

    private final KnowledgeSearchRepository repository;

    public KnowledgeSearchService(KnowledgeSearchRepository repository) {
        this.repository = repository;
    }

    /**
     * 使用默认关键词搜索模式执行检索。
     */
    @Transactional(readOnly = true)
    public KnowledgeSearchResult search(String keyword, String type, int page, int size) {
        return search(keyword, SearchMode.KEYWORD.value(), type, page, size);
    }

    /**
     * 按指定搜索模式检索知识库内容。
     *
     * @param keyword 搜索关键词或精确查询值
     * @param mode    搜索模式：keyword（默认，多词模糊匹配）/ exact（精确搜索，目前支持问题 ID）
     * @param type    all（或空）/ category / topic / question；只影响返回的列表，计数始终覆盖全部类型
     * @param page    从 1 开始；超过末页时返回末页
     * @param size    每页条数
     */
    @Transactional(readOnly = true)
    public KnowledgeSearchResult search(String keyword, String mode, String type, int page, int size) {
        SearchMode searchMode = SearchMode.fromValue(mode);
        SearchQuery query = SearchQuery.of(keyword, searchMode);
        KnowledgeSearchKind filter = parseType(type);
        if (page < 1) throw new IllegalArgumentException("页码必须大于 0");
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页条数必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }

        if (searchMode == SearchMode.EXACT) {
            return searchExact(query, filter, page, size);
        }
        return searchByKeywords(query, filter, page, size);
    }

    /**
     * 执行精确搜索：目前仅按问题主键 ID 精确查询，不混入普通模糊匹配结果。
     */
    private KnowledgeSearchResult searchExact(SearchQuery query, KnowledgeSearchKind filter, int page, int size) {
        long questionId = query.exactQuestionId().orElseThrow();
        Optional<KnowledgeSearchCandidate> found = repository.findQuestionById(questionId);
        List<KnowledgeSearchResult.Hit> questionHits = found.map(candidate -> List.of(toExactHit(candidate)))
                .orElseGet(List::of);

        List<KnowledgeSearchResult.Hit> matched =
                (filter == null || filter == KnowledgeSearchKind.QUESTION) ? questionHits : List.of();
        int total = matched.size();
        int totalPages = Math.max(1, (total + size - 1) / size);
        int currentPage = Math.min(page, totalPages);
        int from = (currentPage - 1) * size;
        List<KnowledgeSearchResult.Hit> items = List.copyOf(matched.subList(from, Math.min(total, from + size)));

        int questionCount = questionHits.size();
        KnowledgeSearchResult.Counts counts = new KnowledgeSearchResult.Counts(questionCount, 0, 0, questionCount);
        return new KnowledgeSearchResult(query.normalizedKeyword(), SearchMode.EXACT.value(), List.of(),
                filter == null ? "all" : filter.value(), currentPage, size, total, totalPages, counts, false, items);
    }

    /**
     * 执行普通关键词多词 AND 模糊搜索与相关度排序。
     */
    private KnowledgeSearchResult searchByKeywords(SearchQuery query, KnowledgeSearchKind filter, int page, int size) {
        List<String> terms = query.terms();
        String lead = query.leadTerm();
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
        return new KnowledgeSearchResult(query.normalizedKeyword(), SearchMode.KEYWORD.value(), terms,
                filter == null ? "all" : filter.value(), currentPage, size, total, totalPages, counts, truncated, items);
    }

    /** 按空白分词、转小写并去重；超出上限的词直接忽略，避免生成过长的 SQL。 */
    static List<String> parseTerms(String keyword) {
        return SearchQuery.parseTerms(keyword);
    }

    private static KnowledgeSearchKind parseType(String type) {
        if (type == null || type.isBlank() || "all".equalsIgnoreCase(type.strip())) return null;
        try {
            return KnowledgeSearchKind.valueOf(type.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的搜索类型: " + type, ex);
        }
    }

    /** 构建精确命中的搜索结果，返回完整标题与正文开头摘要。 */
    private static KnowledgeSearchResult.Hit toExactHit(KnowledgeSearchCandidate candidate) {
        String title = candidate.title() == null ? "" : candidate.title();
        String breadcrumb = candidate.breadcrumb() == null || candidate.breadcrumb().isBlank()
                ? defaultBreadcrumb(candidate.kind()) : candidate.breadcrumb();
        return new KnowledgeSearchResult.Hit(candidate.kind().value(), candidate.id(), title,
                excerpt(candidate, List.of()), breadcrumb, false, EXACT_MATCH_SCORE);
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