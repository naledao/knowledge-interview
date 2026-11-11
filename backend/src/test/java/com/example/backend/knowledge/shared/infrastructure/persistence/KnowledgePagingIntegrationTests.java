package com.example.backend.knowledge.shared.infrastructure.persistence;

import com.example.backend.knowledge.category.domain.repository.KnowledgeCategoryRepository;
import com.example.backend.knowledge.question.domain.repository.KnowledgeQuestionRepository;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import com.example.backend.knowledge.topic.domain.repository.KnowledgeTopicRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** 使用真实 MyBatis 与数据库验证三个仓储的 SQL 分页，不修改业务数据。 */
@SpringBootTest
class KnowledgePagingIntegrationTests {
    @Autowired KnowledgeCategoryRepository categories;
    @Autowired KnowledgeTopicRepository topics;
    @Autowired KnowledgeQuestionRepository questions;

    @Test
    void categoriesArePagedInDatabase() {
        assertFirstPage(categories.findPage(new PageRequest(1, 1)));
    }

    @Test
    void topicsArePagedInDatabase() {
        assertFirstPage(topics.findPage(null, new PageRequest(1, 1)));
    }

    @Test
    void questionsArePagedInDatabase() {
        assertFirstPage(questions.findPage(null, new PageRequest(1, 1)));
    }

    /** 每次最多返回一条记录，分页总数和下一页标志必须一致。 */
    private void assertFirstPage(PagedResult<?> page) {
        assertThat(page.pageNum()).isEqualTo(1);
        assertThat(page.pageSize()).isEqualTo(1);
        assertThat(page.items().size()).isLessThanOrEqualTo(1);
        assertThat(page.pages()).isEqualTo((int) page.total());
        assertThat(page.hasNext()).isEqualTo(page.total() > 1);
        if (page.total() > 0) assertThat(page.items()).hasSize(1);
    }
}