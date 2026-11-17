package com.example.backend.knowledge.search.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchKeywordTests {
    @Test
    void normalizesWhitespaceBeforeRecording() {
        assertEquals(Optional.of("redis 持久化"), SearchKeyword.normalizeForRecord("  redis   持久化 "));
    }

    @Test
    void ignoresBlankAndTooLongKeywords() {
        assertTrue(SearchKeyword.normalizeForRecord(null).isEmpty());
        assertTrue(SearchKeyword.normalizeForRecord("   ").isEmpty());
        assertTrue(SearchKeyword.normalizeForRecord("a".repeat(SearchKeyword.MAX_RECORDABLE_LENGTH + 1)).isEmpty());
        assertTrue(SearchKeyword.normalizeForRecord("a".repeat(SearchKeyword.MAX_RECORDABLE_LENGTH)).isPresent());
    }

    @Test
    void rejectsInvalidModel() {
        assertThrows(IllegalArgumentException.class, () -> new SearchKeyword(" ", 0, false, 0, true));
        assertThrows(IllegalArgumentException.class, () -> new SearchKeyword("Vue", -1, false, 0, true));
    }
}
