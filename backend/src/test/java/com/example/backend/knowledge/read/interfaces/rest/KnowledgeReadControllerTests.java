package com.example.backend.knowledge.read.interfaces.rest;

import com.example.backend.knowledge.category.application.KnowledgeCategoryApplicationService;
import com.example.backend.knowledge.category.domain.model.KnowledgeCategory;
import com.example.backend.knowledge.question.application.KnowledgeQuestionApplicationService;
import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import com.example.backend.knowledge.topic.application.KnowledgeTopicApplicationService;
import com.example.backend.knowledge.topic.domain.model.KnowledgeTopic;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KnowledgeReadControllerTests {
    private KnowledgeCategoryApplicationService categoryService;
    private KnowledgeTopicApplicationService topicService;
    private KnowledgeQuestionApplicationService questionService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        categoryService = mock(KnowledgeCategoryApplicationService.class);
        topicService = mock(KnowledgeTopicApplicationService.class);
        questionService = mock(KnowledgeQuestionApplicationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new KnowledgeReadController(categoryService, topicService, questionService)).build();
    }

    @Test
    void listsCategories() throws Exception {
        when(categoryService.list()).thenReturn(List.of(new KnowledgeCategory(2L, "Backend", "Description", 1, null, null)));
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Backend"))
                .andExpect(jsonPath("$[0].sortOrder").value(1));
        verify(categoryService).list();
    }

    @Test
    void listsAllTopics() throws Exception {
        when(topicService.list(null)).thenReturn(List.of(new KnowledgeTopic(9L, 2L, "Spring", null, 0, null, null)));
        mvc.perform(get("/api/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].categoryId").value(2));
        verify(topicService).list(null);
    }

    @Test
    void filtersTopicsByCategoryId() throws Exception {
        when(topicService.list(2L)).thenReturn(List.of());
        mvc.perform(get("/api/topics").param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(topicService).list(2L);
    }
    @Test
    void listsAllQuestions() throws Exception {
        when(questionService.list(null)).thenReturn(List.of(
                new KnowledgeQuestion(7L, 9L, "什么是依赖注入？", "由容器提供依赖", 0, null, null)));
        mvc.perform(get("/api/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].topicId").value(9))
                .andExpect(jsonPath("$[0].questionText").value("什么是依赖注入？"));
        verify(questionService).list(null);
    }

    @Test
    void filtersQuestionsByTopicId() throws Exception {
        when(questionService.list(9L)).thenReturn(List.of());
        mvc.perform(get("/api/questions").param("topicId", "9"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(questionService).list(9L);
    }
}