package com.example.backend.knowledge.shared.interfaces.mcp;

import com.example.backend.knowledge.category.application.KnowledgeCategoryApplicationService;
import com.example.backend.knowledge.category.interfaces.mcp.KnowledgeCategoryTools;
import com.example.backend.knowledge.question.application.KnowledgeQuestionApplicationService;
import com.example.backend.knowledge.question.interfaces.mcp.KnowledgeQuestionTools;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import com.example.backend.knowledge.topic.application.KnowledgeTopicApplicationService;
import com.example.backend.knowledge.topic.interfaces.mcp.KnowledgeTopicTools;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** 验证 MCP 列表工具的默认分页、筛选传递、分页边界和结果元信息。 */
class KnowledgeMcpPagingTests {
    @Test
    void categoriesUseDefaultPageAndRejectInvalidValues() {
        var service = mock(KnowledgeCategoryApplicationService.class);
        var tool = new KnowledgeCategoryTools(service);
        PagedResult<com.example.backend.knowledge.category.domain.model.KnowledgeCategory> result = new PagedResult<>(1, 6, 0L, 0, false, List.of());
        when(service.list(new PageRequest(1, 6))).thenReturn(result);

        assertThat(tool.list(null, null)).isSameAs(result);
        verify(service).list(new PageRequest(1, 6));
        assertThatThrownBy(() -> tool.list(0, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.list(1, 7)).isInstanceOf(IllegalArgumentException.class);
        verifyNoMoreInteractions(service);
    }

    @Test
    void topicsKeepCategoryFilterAndPageParameters() {
        var service = mock(KnowledgeTopicApplicationService.class);
        var tool = new KnowledgeTopicTools(service);
        PagedResult<com.example.backend.knowledge.topic.domain.model.KnowledgeTopic> result = new PagedResult<>(2, 5, 11L, 3, true, List.of());
        when(service.list(7L, new PageRequest(2, 5))).thenReturn(result);

        assertThat(tool.list(7L, 2, 5)).isSameAs(result);
        verify(service).list(7L, new PageRequest(2, 5));
        assertThat(result.total()).isEqualTo(11);
        assertThat(result.pages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void questionsKeepTopicFilterAndLimitPageSize() {
        var service = mock(KnowledgeQuestionApplicationService.class);
        var tool = new KnowledgeQuestionTools(service);
        PagedResult<com.example.backend.knowledge.question.domain.model.KnowledgeQuestion> result = new PagedResult<>(3, 6, 240L, 3, false, List.of());
        when(service.list(12L, new PageRequest(3, 6))).thenReturn(result);

        assertThat(tool.list(12L, 3, 6)).isSameAs(result);
        verify(service).list(12L, new PageRequest(3, 6));
        assertThatThrownBy(() -> tool.list(12L, 1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.list(12L, 1, 7)).isInstanceOf(IllegalArgumentException.class);
        verifyNoMoreInteractions(service);
    }
}