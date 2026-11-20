package com.example.backend.knowledge.search.interfaces.rest;

import com.example.backend.knowledge.search.application.HotSearchKeywordService;
import com.example.backend.knowledge.search.application.KnowledgeSearchResult;
import com.example.backend.knowledge.search.application.KnowledgeSearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KnowledgeSearchControllerTests {
    private KnowledgeSearchService service;
    private HotSearchKeywordService hotKeywordService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(KnowledgeSearchService.class);
        hotKeywordService = mock(HotSearchKeywordService.class);
        mvc = MockMvcBuilders.standaloneSetup(new KnowledgeSearchController(service, hotKeywordService)).build();
    }

    @Test
    void returnsPagedResults() throws Exception {
        when(service.search("redis", "keyword", "question", 2, 5)).thenReturn(new KnowledgeSearchResult(
                "redis", "keyword", List.of("redis"), "question", 2, 5, 6, 2,
                new KnowledgeSearchResult.Counts(7, 1, 0, 6), false,
                List.of(new KnowledgeSearchResult.Hit("question", 11, "Redis 为什么快？", "单线程…",
                        "数据库 / Redis", true, 65))));

        mvc.perform(get("/api/search").param("q", "redis").param("type", "question")
                        .param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("keyword"))
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.counts.question").value(6))
                .andExpect(jsonPath("$.terms[0]").value("redis"))
                .andExpect(jsonPath("$.items[0].kind").value("question"))
                .andExpect(jsonPath("$.items[0].answerMatch").value(true))
                .andExpect(jsonPath("$.items[0].breadcrumb").value("数据库 / Redis"));
    }

    @Test
    void appliesDefaults() throws Exception {
        when(service.search("vue", "keyword", "all", 1, 10)).thenReturn(new KnowledgeSearchResult(
                "vue", "keyword", List.of("vue"), "all", 1, 10, 0, 1,
                new KnowledgeSearchResult.Counts(0, 0, 0, 0), false, List.of()));
        mvc.perform(get("/api/search").param("q", "vue")).andExpect(status().isOk());
        verify(service).search("vue", "keyword", "all", 1, 10);
    }

    @Test
    void supportsExactModeWithoutRecordingHotKeyword() throws Exception {
        when(service.search("42", "exact", "all", 1, 10)).thenReturn(new KnowledgeSearchResult(
                "42", "exact", List.of(), "all", 1, 10, 1, 1,
                new KnowledgeSearchResult.Counts(1, 0, 0, 1), false,
                List.of(new KnowledgeSearchResult.Hit("question", 42, "什么是 MVCC？", "摘要",
                        "MySQL / 事务", false, 10000))));

        mvc.perform(get("/api/search").param("q", "42").param("mode", "exact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("exact"))
                .andExpect(jsonPath("$.items[0].id").value(42));

        verify(service).search("42", "exact", "all", 1, 10);
        verify(hotKeywordService, never()).recordSearch(anyString());
    }

    private static KnowledgeSearchResult resultOf(String keyword, int page, int all) {
        return new KnowledgeSearchResult(keyword, "keyword", List.of(keyword), "all", page, 10, all, 1,
                new KnowledgeSearchResult.Counts(all, all, 0, 0), false, List.of());
    }

    @Test
    void recordsHotKeywordWhenFirstPageHasResults() throws Exception {
        when(service.search("vue", "keyword", "all", 1, 10)).thenReturn(resultOf("vue", 1, 3));
        mvc.perform(get("/api/search").param("q", "vue")).andExpect(status().isOk());
        verify(hotKeywordService).recordSearch("vue");
    }

    @Test
    void skipsHotKeywordWhenNoResultOrNotFirstPage() throws Exception {
        when(service.search("zzz", "keyword", "all", 1, 10)).thenReturn(resultOf("zzz", 1, 0));
        when(service.search("vue", "keyword", "all", 2, 10)).thenReturn(resultOf("vue", 2, 20));
        mvc.perform(get("/api/search").param("q", "zzz")).andExpect(status().isOk());
        mvc.perform(get("/api/search").param("q", "vue").param("page", "2")).andExpect(status().isOk());
        verify(hotKeywordService, never()).recordSearch(anyString());
    }

    @Test
    void searchStillSucceedsWhenRecordingFails() throws Exception {
        when(service.search("vue", "keyword", "all", 1, 10)).thenReturn(resultOf("vue", 1, 3));
        doThrow(new IllegalStateException("db down")).when(hotKeywordService).recordSearch("vue");
        mvc.perform(get("/api/search").param("q", "vue")).andExpect(status().isOk());
    }

    @Test
    void mapsValidationErrorsToBadRequest() throws Exception {
        when(service.search(" ", "keyword", "all", 1, 10)).thenThrow(new IllegalArgumentException("搜索关键词不能为空"));
        mvc.perform(get("/api/search").param("q", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("搜索关键词不能为空"));
    }
}