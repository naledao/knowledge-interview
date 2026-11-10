package com.example.backend.knowledge.search.infrastructure.persistence;

import com.example.backend.knowledge.search.application.KnowledgeSearchKind;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MyBatisKnowledgeSearchRepositoryTests {
    @Test
    void escapesLikeWildcards() {
        assertThat(MyBatisKnowledgeSearchRepository.escapeLike("100%_done!")).isEqualTo("100!%!_done!!");
    }

    @Test
    void dispatchesByKindWithEscapedPatterns() {
        KnowledgeSearchMapper mapper = mock(KnowledgeSearchMapper.class);
        KnowledgeSearchRow row = new KnowledgeSearchRow();
        row.setId(5L);
        row.setTitle("Spring");
        when(mapper.searchTopics(List.of("%a!_b%"), "a_b", 10)).thenReturn(List.of(row));

        var result = new MyBatisKnowledgeSearchRepository(mapper)
                .search(KnowledgeSearchKind.TOPIC, List.of("a_b"), "a_b", 10);

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.kind()).isEqualTo(KnowledgeSearchKind.TOPIC);
            assertThat(candidate.id()).isEqualTo(5L);
            assertThat(candidate.bodyOffset()).isEqualTo(1);
            assertThat(candidate.bodyLength()).isZero();
        });
        verify(mapper, never()).searchQuestions(anyList(), anyString(), anyInt());
    }
}
