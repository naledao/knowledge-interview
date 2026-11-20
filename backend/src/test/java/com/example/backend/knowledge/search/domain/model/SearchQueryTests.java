package com.example.backend.knowledge.search.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 测试搜索模式解析及搜索查询值对象的校验、分词与精确问题 ID 解析逻辑。 */
class SearchQueryTests {
    @Test
    void parsesSearchModeWithKeywordDefault() {
        assertThat(SearchMode.fromValue(null)).isEqualTo(SearchMode.KEYWORD);
        assertThat(SearchMode.fromValue("   ")).isEqualTo(SearchMode.KEYWORD);
        assertThat(SearchMode.fromValue("keyword")).isEqualTo(SearchMode.KEYWORD);
        assertThat(SearchMode.fromValue(" KEYWORD ")).isEqualTo(SearchMode.KEYWORD);
        assertThat(SearchMode.fromValue("exact")).isEqualTo(SearchMode.EXACT);
        assertThat(SearchMode.fromValue(" EXACT ")).isEqualTo(SearchMode.EXACT);
        assertThatThrownBy(() -> SearchMode.fromValue("fuzzy"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keepsNumericAndPrefixedQueriesAsNormalTermsInKeywordMode() {
        SearchQuery numeric = SearchQuery.of("42");
        assertThat(numeric.mode()).isEqualTo(SearchMode.KEYWORD);
        assertThat(numeric.terms()).containsExactly("42");
        assertThat(numeric.exactQuestionId()).isEmpty();

        SearchQuery prefixed = SearchQuery.of("  #108  ", SearchMode.KEYWORD);
        assertThat(prefixed.mode()).isEqualTo(SearchMode.KEYWORD);
        assertThat(prefixed.terms()).containsExactly("#108");
        assertThat(prefixed.exactQuestionId()).isEmpty();
    }

    @Test
    void parsesPositiveIntegerQuestionIdInExactMode() {
        SearchQuery query = SearchQuery.of("  42  ", SearchMode.EXACT);
        assertThat(query.mode()).isEqualTo(SearchMode.EXACT);
        assertThat(query.normalizedKeyword()).isEqualTo("42");
        assertThat(query.terms()).isEmpty();
        assertThat(query.exactQuestionId()).contains(42L);
    }

    @Test
    void rejectsInvalidQuestionIdsInExactMode() {
        assertThatThrownBy(() -> SearchQuery.of("0", SearchMode.EXACT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchQuery.of("-5", SearchMode.EXACT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchQuery.of("#42", SearchMode.EXACT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchQuery.of("id:7", SearchMode.EXACT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchQuery.of("Redis", SearchMode.EXACT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchQuery.of("99999999999999999999999999999", SearchMode.EXACT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankOrTooLongKeywords() {
        assertThatThrownBy(() -> SearchQuery.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchQuery.of("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchQuery.of("   ", SearchMode.EXACT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchQuery.of("a".repeat(SearchQuery.MAX_KEYWORD_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}