package com.example.backend.knowledge.read.interfaces.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.knowledge.category.application.KnowledgeCategoryApplicationService;
import com.example.backend.knowledge.question.application.KnowledgeQuestionApplicationService;
import com.example.backend.knowledge.question.application.dto.RandomQuestion;
import com.example.backend.knowledge.topic.application.KnowledgeTopicApplicationService;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 随机抽题接口测试：同时验证固定路径 random 不会被 /questions/{id} 误匹配。 */
class KnowledgeRandomQuestionControllerTests {
    private KnowledgeQuestionApplicationService questionService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        questionService = mock(KnowledgeQuestionApplicationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new KnowledgeReadController(
                mock(KnowledgeCategoryApplicationService.class),
                mock(KnowledgeTopicApplicationService.class), questionService)).build();
    }

    @Test
    void returnsRandomQuestionId() throws Exception {
        when(questionService.random(null)).thenReturn(new RandomQuestion(34L));
        mvc.perform(get("/api/questions/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(34));
    }

    @Test
    void passesExcludeIdToService() throws Exception {
        when(questionService.random(12L)).thenReturn(new RandomQuestion(5L));
        mvc.perform(get("/api/questions/random").param("excludeId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void returnsNotFoundWhenLibraryIsEmpty() throws Exception {
        when(questionService.random(null)).thenThrow(new NoSuchElementException("暂无题目"));
        mvc.perform(get("/api/questions/random"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("暂无题目"));
    }
}
