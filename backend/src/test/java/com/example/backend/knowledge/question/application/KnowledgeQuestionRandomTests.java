package com.example.backend.knowledge.question.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.knowledge.question.domain.repository.KnowledgeQuestionRepository;
import com.example.backend.knowledge.topic.domain.repository.KnowledgeTopicRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 随机抽题应用服务测试：验证排除当前题、仅一题时的兜底和题库为空的处理。 */
class KnowledgeQuestionRandomTests {
    private KnowledgeQuestionRepository repository;
    private KnowledgeQuestionApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeQuestionRepository.class);
        service = new KnowledgeQuestionApplicationService(repository, mock(KnowledgeTopicRepository.class));
    }

    @Test
    void returnsRandomQuestionExcludingCurrentOne() {
        when(repository.findRandomId(7L)).thenReturn(Optional.of(9L));
        assertEquals(9L, service.random(7L).id());
        verify(repository, never()).findRandomId(isNull());
    }

    @Test
    void fallsBackToSameQuestionWhenItIsTheOnlyOne() {
        when(repository.findRandomId(7L)).thenReturn(Optional.empty());
        when(repository.findRandomId(null)).thenReturn(Optional.of(7L));
        assertEquals(7L, service.random(7L).id());
    }

    @Test
    void throwsWhenLibraryIsEmpty() {
        when(repository.findRandomId(null)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.random(null));
    }
}
