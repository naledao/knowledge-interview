package com.example.backend.knowledge.read.interfaces.rest;

import com.example.backend.knowledge.category.application.KnowledgeCategoryApplicationService;
import com.example.backend.knowledge.category.domain.model.KnowledgeCategory;
import com.example.backend.knowledge.question.application.KnowledgeQuestionApplicationService;
import com.example.backend.knowledge.question.application.dto.QuestionNeighbors;
import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import com.example.backend.knowledge.topic.application.KnowledgeTopicApplicationService;
import com.example.backend.knowledge.topic.domain.model.KnowledgeTopic;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 验证前端 REST 分页、条件筛选、详情查询和错误响应。 */
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
        mvc = MockMvcBuilders.standaloneSetup(
                new KnowledgeReadController(categoryService, topicService, questionService)).build();
    }

    @Test
    void categoriesDefaultToSixPerPage() throws Exception {
        var category = new KnowledgeCategory(2L, "Backend", "Description", 1, null, null);
        when(categoryService.list(new PageRequest(1, 6))).thenReturn(
                new PagedResult<>(1, 6, 1, 1, false, List.of(category)));
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNum").value(1))
                .andExpect(jsonPath("$.pageSize").value(6))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Backend"));
        verify(categoryService).list(new PageRequest(1, 6));
    }

    @Test
    void topicsSupportFilteringAndPaging() throws Exception {
        var topic = new KnowledgeTopic(9L, 2L, "Spring", null, 0, null, null);
        when(topicService.list(2L, new PageRequest(2, 3))).thenReturn(
                new PagedResult<>(2, 3, 10, 4, true, List.of(topic)));
        mvc.perform(get("/api/topics").param("categoryId", "2").param("pageNum", "2").param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNum").value(2))
                .andExpect(jsonPath("$.pages").value(4))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.items[0].categoryId").value(2));
        verify(topicService).list(2L, new PageRequest(2, 3));
    }

    @Test
    void questionsSupportFilteringAndPaging() throws Exception {
        var question = new KnowledgeQuestion(7L, 9L, "什么是依赖注入？", "答案", 0, null, null);
        when(questionService.list(9L, new PageRequest(1, 6))).thenReturn(
                new PagedResult<>(1, 6, 1, 1, false, List.of(question)));
        mvc.perform(get("/api/questions").param("topicId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].questionText").value("什么是依赖注入？"));
        verify(questionService).list(9L, new PageRequest(1, 6));
    }

    @Test
    void detailEndpointsUseDirectIdLookups() throws Exception {
        when(categoryService.get(2L)).thenReturn(new KnowledgeCategory(2L, "Backend", null, 0, null, null));
        when(topicService.get(9L)).thenReturn(new KnowledgeTopic(9L, 2L, "Spring", null, 0, null, null));
        when(questionService.get(7L)).thenReturn(new KnowledgeQuestion(7L, 9L, "问题", "答案", 0, null, null));
        mvc.perform(get("/api/categories/2")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(2));
        mvc.perform(get("/api/topics/9")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(9));
        mvc.perform(get("/api/questions/7")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        verify(categoryService).get(2L);
        verify(topicService).get(9L);
        verify(questionService).get(7L);
    }

    @Test
    void questionNeighborsSupportCrossPageNavigation() throws Exception {
        when(questionService.neighbors(7L)).thenReturn(new QuestionNeighbors(
                new QuestionNeighbors.QuestionLink(6L, "上一题"),
                new QuestionNeighbors.QuestionLink(8L, "下一题")));
        mvc.perform(get("/api/questions/7/neighbors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous.id").value(6))
                .andExpect(jsonPath("$.next.id").value(8));
        verify(questionService).neighbors(7L);
    }

    @Test
    void invalidPageSizeReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/categories").param("pageSize", "7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("每页条数必须在1到6之间"));
        verifyNoInteractions(categoryService);
    }

    @Test
    void missingCategoryReturnsNotFound() throws Exception {
        when(categoryService.get(999L)).thenThrow(new NoSuchElementException("分类不存在: 999"));
        mvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound());
    }
}