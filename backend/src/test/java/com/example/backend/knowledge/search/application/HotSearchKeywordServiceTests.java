package com.example.backend.knowledge.search.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.backend.knowledge.search.domain.model.SearchKeyword;
import com.example.backend.knowledge.search.domain.repository.SearchKeywordRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HotSearchKeywordServiceTests {
    private SearchKeywordRepository repository;
    private HotSearchKeywordService service;

    @BeforeEach
    void setUp() {
        repository = mock(SearchKeywordRepository.class);
        service = new HotSearchKeywordService(repository);
    }

    @Test
    void recordsNormalizedKeyword() {
        service.recordSearch("  Vue  ");
        verify(repository).recordSearch("Vue");
    }

    @Test
    void ignoresTooLongKeyword() {
        service.recordSearch("a".repeat(SearchKeyword.MAX_RECORDABLE_LENGTH + 1));
        verifyNoInteractions(repository);
    }

    @Test
    void listsKeywordTexts() {
        when(repository.findHot(5)).thenReturn(List.of(
                new SearchKeyword("Vue", 9, true, 0, true),
                new SearchKeyword("Redis", 3, false, 0, true)));
        assertEquals(List.of("Vue", "Redis"), service.listHotKeywords(5));
    }

    @Test
    void rejectsOutOfRangeLimit() {
        assertThrows(IllegalArgumentException.class, () -> service.listHotKeywords(0));
        assertThrows(IllegalArgumentException.class,
                () -> service.listHotKeywords(HotSearchKeywordService.MAX_LIMIT + 1));
    }
}
