package com.example.backend.knowledge.search.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 测试搜索查询值对象的规范化、分词与问题 ID 精确匹配识别逻辑。 */
class SearchQueryTests {
    @Test
    void recognizesPlainNumericAndPrefixedQuestionIds() {
        assertThat(SearchQuery.of("42").exactQuestionId()).contains(42L);
        assertThat(SearchQuery.of("  #108  ").exactQuestionId()).contains(108L);
        assertThat(SearchQuery.of("id:7").exactQuestionId()).contains(7L);
        assertThat(SearchQuery.of("ID： 99").exactQuestionId()).contains(99L);
    }

    @Test
    void ignoresZeroNegativeOverflowAndTextQueriesForExactId() {
        assertThat(SearchQuery.parseExactQuestionId("0")).isEmpty();
        assertThat(SearchQuery.parseExactQuestionId("-5")).isEmpty();
        assertThat(SearchQuery.parseExactQuestionId("#0")).isEmpty();
        assertThat(SearchQuery.parseExactQuestionId("99999999999999999999999999999")).isEmpty();
        assertThat(SearchQuery.of("Redis 42").exactQuestionId()).isEmpty();
        assertThat(SearchQuery.of("HTTP/2").exactQuestionId()).isEmpty();
        assertThat(SearchQuery.parseExactQuestionId(null)).isEmpty();
        assertThat(SearchQuery.parseExactQuestionId("   ")).isEmpty();
    }

    @Test
    void rejectsBlankOrTooLongKeywords() {
        assertThatThrownBy(() -> SearchQuery.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchQuery.of("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchQuery.of("a".repeat(SearchQuery.MAX_KEYWORD_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}