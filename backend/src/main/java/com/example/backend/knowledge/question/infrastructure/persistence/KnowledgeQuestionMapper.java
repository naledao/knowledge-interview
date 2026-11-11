package com.example.backend.knowledge.question.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.*;

/** 问题 MyBatis Mapper：执行增删改查 SQL，并完成数据库字段映射。 */
@Mapper
public interface KnowledgeQuestionMapper {
    /** 根据 ID 查询问题，显式处理下划线字段映射。 */
    @Select("SELECT id, topic_id, question_text, answer_text, sort_order, created_at, updated_at FROM knowledge_question WHERE id = #{id}")
    @Results(id = "questionResult", value = {
        @Result(id = true, column = "id", property = "id"),
        @Result(column = "topic_id", property = "topicId"),
        @Result(column = "question_text", property = "questionText"),
        @Result(column = "answer_text", property = "answerText"),
        @Result(column = "sort_order", property = "sortOrder"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    KnowledgeQuestionRow findById(@Param("id") long id);

    /** 查询全部问题，按题目、排序值和主键升序排列。 */
    @Select("SELECT id, topic_id, question_text, answer_text, sort_order, created_at, updated_at FROM knowledge_question ORDER BY topic_id, sort_order, id")
    @ResultMap("questionResult")
    List<KnowledgeQuestionRow> findAll();

    /** 查询指定题目下的问题。 */
    @Select("SELECT id, topic_id, question_text, answer_text, sort_order, created_at, updated_at FROM knowledge_question WHERE topic_id = #{topicId} ORDER BY sort_order, id")
    @ResultMap("questionResult")
    List<KnowledgeQuestionRow> findByTopicId(@Param("topicId") long topicId);

    /** 查询同一主题的上一题，仅返回导航字段，避免读取完整 HTML 答案。 */
    @Select("SELECT id, topic_id, question_text, NULL AS answer_text, sort_order, created_at, updated_at FROM knowledge_question WHERE topic_id = #{topicId} AND (sort_order < #{sortOrder} OR (sort_order = #{sortOrder} AND id < #{id})) ORDER BY sort_order DESC, id DESC LIMIT 1")
    @ResultMap("questionResult")
    KnowledgeQuestionRow findPrevious(@Param("topicId") long topicId, @Param("sortOrder") int sortOrder, @Param("id") long id);

    /** 查询同一主题的下一题，避免加载整个问题列表。 */
    @Select("SELECT id, topic_id, question_text, NULL AS answer_text, sort_order, created_at, updated_at FROM knowledge_question WHERE topic_id = #{topicId} AND (sort_order > #{sortOrder} OR (sort_order = #{sortOrder} AND id > #{id})) ORDER BY sort_order ASC, id ASC LIMIT 1")
    @ResultMap("questionResult")
    KnowledgeQuestionRow findNext(@Param("topicId") long topicId, @Param("sortOrder") int sortOrder, @Param("id") long id);
    /** 插入问题，并回填数据库自增主键。 */
    @Insert("INSERT INTO knowledge_question (topic_id, question_text, answer_text, sort_order) VALUES (#{topicId}, #{questionText}, #{answerText}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeQuestionRow row);

    /** 修改问题内容、答案、所属题目和排序值。 */
    @Update("UPDATE knowledge_question SET topic_id = #{topicId}, question_text = #{questionText}, answer_text = #{answerText}, sort_order = #{sortOrder} WHERE id = #{id}")
    int update(KnowledgeQuestionRow row);

    /** 根据主键删除问题。 */
    @Delete("DELETE FROM knowledge_question WHERE id = #{id}")
    int deleteById(@Param("id") long id);
}
