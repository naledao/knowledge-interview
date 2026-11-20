package com.example.backend.knowledge.search.interfaces.mcp;

import com.example.backend.knowledge.search.application.KnowledgeSearchResult;
import com.example.backend.knowledge.search.application.KnowledgeSearchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpTool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** 验证 AI 关键词搜索工具的名称、默认分页、参数传递及分页上限。 */
class KnowledgeSearchToolsTests {
    @Test
    void exposesExpectedMcpToolName() throws Exception {
        McpTool annotation = KnowledgeSearchTools.class
                .getMethod("search", String.class, String.class, Integer.class, Integer.class)
                .getAnnotation(McpTool.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("search_knowledge");
    }

    @Test
    void defaultsToAllTypesAndSixItemsPerPage() {
        KnowledgeSearchService service = mock(KnowledgeSearchService.class);
        KnowledgeSearchTools tool = new KnowledgeSearchTools(service);
        KnowledgeSearchResult expected = emptyResult(1, 6, "all");
        when(service.search("Redis", null, 1, 6)).thenReturn(expected);

        assertThat(tool.search("Redis", null, null, null)).isSameAs(expected);
        verify(service).search("Redis", null, 1, 6);
        verifyNoMoreInteractions(service);
    }

    @Test
    void passesTypeAndCustomValidPagination() {
        KnowledgeSearchService service = mock(KnowledgeSearchService.class);
        KnowledgeSearchTools tool = new KnowledgeSearchTools(service);
        KnowledgeSearchResult expected = emptyResult(3, 4, "question");
        when(service.search("Vue", "question", 3, 4)).thenReturn(expected);

        assertThat(tool.search("Vue", "question", 3, 4)).isSameAs(expected);
        verify(service).search("Vue", "question", 3, 4);
        verifyNoMoreInteractions(service);
    }

    @Test
    void rejectsInvalidPaginationBeforeSearching() {
        KnowledgeSearchService service = mock(KnowledgeSearchService.class);
        KnowledgeSearchTools tool = new KnowledgeSearchTools(service);

        assertThatThrownBy(() -> tool.search("Redis", null, 0, 6))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.search("Redis", null, 1, 7))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.search("Redis", null, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(service);
    }

    private static KnowledgeSearchResult emptyResult(int page, int size, String type) {
        return new KnowledgeSearchResult("Redis", "keyword", List.of("redis"), type, page, size,
                0, 1, new KnowledgeSearchResult.Counts(0, 0, 0, 0), false, List.of());
    }
}