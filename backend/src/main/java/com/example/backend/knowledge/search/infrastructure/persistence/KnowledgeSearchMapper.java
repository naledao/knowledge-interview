package com.example.backend.knowledge.search.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 搜索 MyBatis Mapper。
 * <ul>
 *   <li>每个关键词都必须命中标题或正文（AND）；LIKE 模式由调用方转义，统一使用 '!' 作为转义符。</li>
 *   <li>表使用 utf8mb4_unicode_ci，LIKE / LOCATE 均不区分大小写。</li>
 *   <li>正文只返回 lead 关键词前 60 个字符起、共 600 个字符的窗口，避免传输整段 LONGTEXT 答案；
 *       正文为空或未命中 lead 时窗口从开头截取。</li>
 * </ul>
 * 注解值必须是编译期常量，因此每张表的片段单独声明。
 */
@Mapper
public interface KnowledgeSearchMapper {
    String CATEGORY_BODY =
            "SUBSTRING(c.description, GREATEST(1, COALESCE(LOCATE(#{lead}, c.description), 0) - 60), 600) AS body, "
            + "GREATEST(1, COALESCE(LOCATE(#{lead}, c.description), 0) - 60) AS bodyOffset, "
            + "COALESCE(CHAR_LENGTH(c.description), 0) AS bodyLength";
    String CATEGORY_MATCH = "<where><foreach collection='patterns' item='p'>"
            + " AND (c.name LIKE #{p} ESCAPE '!' OR c.description LIKE #{p} ESCAPE '!')</foreach></where>";

    String TOPIC_BODY =
            "SUBSTRING(t.description, GREATEST(1, COALESCE(LOCATE(#{lead}, t.description), 0) - 60), 600) AS body, "
            + "GREATEST(1, COALESCE(LOCATE(#{lead}, t.description), 0) - 60) AS bodyOffset, "
            + "COALESCE(CHAR_LENGTH(t.description), 0) AS bodyLength";
    String TOPIC_MATCH = "<where><foreach collection='patterns' item='p'>"
            + " AND (t.title LIKE #{p} ESCAPE '!' OR t.description LIKE #{p} ESCAPE '!')</foreach></where>";

    String QUESTION_BODY =
            "SUBSTRING(q.answer_text, GREATEST(1, COALESCE(LOCATE(#{lead}, q.answer_text), 0) - 60), 600) AS body, "
            + "GREATEST(1, COALESCE(LOCATE(#{lead}, q.answer_text), 0) - 60) AS bodyOffset, "
            + "COALESCE(CHAR_LENGTH(q.answer_text), 0) AS bodyLength";
    String QUESTION_MATCH = "<where><foreach collection='patterns' item='p'>"
            + " AND (q.question_text LIKE #{p} ESCAPE '!' OR q.answer_text LIKE #{p} ESCAPE '!')</foreach></where>";

    @Select("<script>SELECT c.id AS id, c.name AS title, " + CATEGORY_BODY + ", NULL AS breadcrumb"
            + " FROM knowledge_category c " + CATEGORY_MATCH
            + " ORDER BY c.sort_order, c.id LIMIT #{limit}</script>")
    List<KnowledgeSearchRow> searchCategories(@Param("patterns") List<String> patterns,
                                              @Param("lead") String lead, @Param("limit") int limit);

    @Select("<script>SELECT t.id AS id, t.title AS title, " + TOPIC_BODY + ", c.name AS breadcrumb"
            + " FROM knowledge_topic t JOIN knowledge_category c ON c.id = t.category_id " + TOPIC_MATCH
            + " ORDER BY c.sort_order, c.id, t.sort_order, t.id LIMIT #{limit}</script>")
    List<KnowledgeSearchRow> searchTopics(@Param("patterns") List<String> patterns,
                                          @Param("lead") String lead, @Param("limit") int limit);

    @Select("<script>SELECT q.id AS id, q.question_text AS title, " + QUESTION_BODY
            + ", CONCAT_WS(' / ', c.name, t.title) AS breadcrumb"
            + " FROM knowledge_question q"
            + " JOIN knowledge_topic t ON t.id = q.topic_id"
            + " JOIN knowledge_category c ON c.id = t.category_id " + QUESTION_MATCH
            + " ORDER BY c.sort_order, c.id, t.sort_order, t.id, q.sort_order, q.id LIMIT #{limit}</script>")
    List<KnowledgeSearchRow> searchQuestions(@Param("patterns") List<String> patterns,
                                             @Param("lead") String lead, @Param("limit") int limit);
}
