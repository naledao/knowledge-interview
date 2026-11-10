package com.example.backend.knowledge.search.interfaces.rest;

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
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(KnowledgeSearchService.class);
        mvc = MockMvcBuilders.standaloneSetup(new KnowledgeSearchController(service)).build();
    }

    @Test
    void returnsPagedResults() throws Exception {
        when(service.search("redis", "question", 2, 5)).thenReturn(new KnowledgeSearchResult(
                "redis", List.of("redis"), "question", 2, 5, 6, 2,
                new KnowledgeSearchResult.Counts(7, 1, 0, 6), false,
                List.of(new KnowledgeSearchResult.Hit("question", 11, "Redis 为什么快？", "单线程…",
                        "数据库 / Redis", true, 65))));

        mvc.perform(get("/api/search").param("q", "redis").param("type", "question")
                        .param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.counts.question").value(6))
                .andExpect(jsonPath("$.terms[0]").value("redis"))
                .andExpect(jsonPath("$.items[0].kind").value("question"))
                .andExpect(jsonPath("$.items[0].answerMatch").value(true))
                .andExpect(jsonPath("$.items[0].breadcrumb").value("数据库 / Redis"));
    }

    @Test
    void appliesDefaults() throws Exception {
        when(service.search("vue", "all", 1, 10)).thenReturn(new KnowledgeSearchResult(
                "vue", List.of("vue"), "all", 1, 10, 0, 1,
                new KnowledgeSearchResult.Counts(0, 0, 0, 0), false, List.of()));
        mvc.perform(get("/api/search").param("q", "vue")).andExpect(status().isOk());
        verify(service).search("vue", "all", 1, 10);
    }

    @Test
    void mapsValidationErrorsToBadRequest() throws Exception {
        when(service.search(" ", "all", 1, 10)).thenThrow(new IllegalArgumentException("搜索关键词不能为空"));
        mvc.perform(get("/api/search").param("q", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("搜索关键词不能为空"));
    }
}
