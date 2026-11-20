package com.example.backend.knowledge.search.application;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeSearchServiceTests {
    private KnowledgeSearchRepository repository;
    private KnowledgeSearchService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeSearchRepository.class);
        when(repository.search(any(), anyList(), anyString(), anyInt())).thenReturn(List.of());
        service = new KnowledgeSearchService(repository);
    }

    private static KnowledgeSearchCandidate candidate(KnowledgeSearchKind kind, long id, String title, String body) {
        return new KnowledgeSearchCandidate(kind, id, title, body, 1, body == null ? 0 : body.length(), null);
    }

    @Test
    void normalizesTermsAndUsesLongestAsLead() {
        service.search("  Redis   redis  持久化 ", "all", 1, 10);
        verify(repository).search(KnowledgeSearchKind.QUESTION, List.of("redis", "持久化"), "redis",
                KnowledgeSearchService.CANDIDATE_LIMIT);
    }

    @Test
    void ranksTitleMatchesAboveBodyMatchesAcrossKinds() {
        when(repository.search(eq(KnowledgeSearchKind.QUESTION), anyList(), anyString(), anyInt())).thenReturn(List.of(
                candidate(KnowledgeSearchKind.QUESTION, 1, "什么是缓存击穿？", "Redis 可以用互斥锁解决"),
                candidate(KnowledgeSearchKind.QUESTION, 2, "Redis 为什么快？", null)));
        when(repository.search(eq(KnowledgeSearchKind.CATEGORY), anyList(), anyString(), anyInt())).thenReturn(List.of(
                candidate(KnowledgeSearchKind.CATEGORY, 3, "Redis", "")));

        KnowledgeSearchResult result = service.search("redis", null, 1, 10);

        assertThat(result.items()).extracting(KnowledgeSearchResult.Hit::id).containsExactly(3L, 2L, 1L);
        assertThat(result.items().get(0).breadcrumb()).isEqualTo("知识分类");
        assertThat(result.items().get(2).answerMatch()).isTrue();
        assertThat(result.items().get(1).answerMatch()).isFalse();
        assertThat(result.counts()).isEqualTo(new KnowledgeSearchResult.Counts(3, 1, 0, 2));
        assertThat(result.type()).isEqualTo("all");
    }

    @Test
    void filtersByTypeButKeepsAllCountsAndClampsPage() {
        when(repository.search(eq(KnowledgeSearchKind.TOPIC), anyList(), anyString(), anyInt())).thenReturn(List.of(
                candidate(KnowledgeSearchKind.TOPIC, 1, "Vue 响应式", null),
                candidate(KnowledgeSearchKind.TOPIC, 2, "Vue 路由", null),
                candidate(KnowledgeSearchKind.TOPIC, 3, "Vue 组件", null)));
        when(repository.search(eq(KnowledgeSearchKind.CATEGORY), anyList(), anyString(), anyInt())).thenReturn(List.of(
                candidate(KnowledgeSearchKind.CATEGORY, 9, "Vue", null)));

        KnowledgeSearchResult result = service.search("vue", "TOPIC", 99, 2);

        assertThat(result.type()).isEqualTo("topic");
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.items()).extracting(KnowledgeSearchResult.Hit::id).containsExactly(3L);
        assertThat(result.counts().all()).isEqualTo(4);
    }

    @Test
    void flagsTruncationWhenCandidateLimitReached() {
        List<KnowledgeSearchCandidate> many = java.util.stream.LongStream.rangeClosed(1, KnowledgeSearchService.CANDIDATE_LIMIT)
                .mapToObj(id -> candidate(KnowledgeSearchKind.QUESTION, id, "a" + id, null)).toList();
        when(repository.search(eq(KnowledgeSearchKind.QUESTION), anyList(), anyString(), anyInt())).thenReturn(many);
        assertThat(service.search("a", "all", 1, 10).truncated()).isTrue();
    }

    @Test
    void rejectsInvalidArguments() {
        assertThatThrownBy(() -> service.search("  ", "all", 1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search("x".repeat(101), "all", 1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search("vue", "answer", 1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search("vue", "all", 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search("vue", "all", 1, 51)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void limitsTermCount() {
        assertThat(KnowledgeSearchService.parseTerms("a b c d e f g")).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    void excerptCentersOnFirstHitAndMarksTruncatedWindow() {
        String body = "x".repeat(300) + " Redis 持久化 " + "y".repeat(300);
        // 模拟数据库窗口：从第 251 个字符开始，完整正文更长。
        String window = body.substring(250, 450);
        KnowledgeSearchCandidate windowed = new KnowledgeSearchCandidate(
                KnowledgeSearchKind.QUESTION, 1, "t", window, 251, body.length(), null);

        String excerpt = KnowledgeSearchService.excerpt(windowed, List.of("redis"));

        assertThat(excerpt).startsWith("…").endsWith("…").contains("Redis 持久化");
        assertThat(excerpt.length()).isEqualTo(KnowledgeSearchService.EXCERPT_LENGTH + 2);
    }

    @Test
    void shortExcerptIsReturnedWhole() {
        assertThat(KnowledgeSearchService.excerpt(candidate(KnowledgeSearchKind.TOPIC, 1, "t", "  短\n描述 "), List.of("描述")))
                .isEqualTo("短 描述");
    }

    @Test
    void prioritizesExactQuestionIdMatchAndDeduplicates() {
        KnowledgeSearchCandidate exact = new KnowledgeSearchCandidate(
                KnowledgeSearchKind.QUESTION, 42, "什么是 MVCC？", "多版本并发控制原理", 1, 9, "MySQL / 事务与锁");
        KnowledgeSearchCandidate textMatchedSameId = candidate(KnowledgeSearchKind.QUESTION, 42, "什么是 MVCC？", "包含 42 的文本");
        KnowledgeSearchCandidate textMatchedOther = candidate(KnowledgeSearchKind.QUESTION, 7, "第 42 条优化建议", "正文");

        when(repository.findQuestionById(42L)).thenReturn(java.util.Optional.of(exact));
        when(repository.search(eq(KnowledgeSearchKind.QUESTION), anyList(), anyString(), anyInt()))
                .thenReturn(List.of(textMatchedSameId, textMatchedOther));

        KnowledgeSearchResult result = service.search("#42", "all", 1, 10);

        assertThat(result.items()).extracting(KnowledgeSearchResult.Hit::id).containsExactly(42L, 7L);
        assertThat(result.items().get(0).score()).isEqualTo(KnowledgeSearchService.EXACT_ID_SCORE);
        assertThat(result.items().get(0).breadcrumb()).isEqualTo("MySQL / 事务与锁");
        assertThat(result.counts().question()).isEqualTo(2);
        assertThat(result.counts().all()).isEqualTo(2);
    }

    @Test
    void fallsBackToTextSearchWhenExactQuestionIdDoesNotExist() {
        when(repository.findQuestionById(999L)).thenReturn(java.util.Optional.empty());
        when(repository.search(eq(KnowledgeSearchKind.QUESTION), anyList(), anyString(), anyInt()))
                .thenReturn(List.of(candidate(KnowledgeSearchKind.QUESTION, 12, "错误码 999 排查", null)));

        KnowledgeSearchResult result = service.search("999", "question", 1, 10);

        assertThat(result.items()).extracting(KnowledgeSearchResult.Hit::id).containsExactly(12L);
        assertThat(result.counts().question()).isEqualTo(1);
    }
}