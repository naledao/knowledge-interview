package com.example.backend.knowledge.topic.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.*;

/** 题目 MyBatis Mapper，负责实际的 SQL 增删改查及字段映射。 */
@Mapper
public interface KnowledgeTopicMapper {
    /** 根据主键查询题目，显式映射数据库下划线字段。 */
    @Select("SELECT id, category_id, title, description, sort_order, created_at, updated_at FROM knowledge_topic WHERE id = #{id}")
    @Results(id = "topicResult", value = {
        @Result(id = true, column = "id", property = "id"),
        @Result(column = "category_id", property = "categoryId"),
        @Result(column = "title", property = "title"),
        @Result(column = "description", property = "description"),
        @Result(column = "sort_order", property = "sortOrder"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    KnowledgeTopicRow findById(@Param("id") long id);

    /** 同一分类下按标题精确查找题目。 */
    @Select("SELECT id, category_id, title, description, sort_order, created_at, updated_at FROM knowledge_topic WHERE category_id = #{categoryId} AND title = #{title} LIMIT 1")
    @ResultMap("topicResult")
    KnowledgeTopicRow findByCategoryIdAndTitle(@Param("categoryId") long categoryId, @Param("title") String title);

    /** 查询全部题目，先按分类再按排序值排序。 */
    @Select("SELECT id, category_id, title, description, sort_order, created_at, updated_at FROM knowledge_topic ORDER BY category_id, sort_order, id")
    @ResultMap("topicResult")
    List<KnowledgeTopicRow> findAll();

    /** 查询指定分类下的题目。 */
    @Select("SELECT id, category_id, title, description, sort_order, created_at, updated_at FROM knowledge_topic WHERE category_id = #{categoryId} ORDER BY sort_order, id")
    @ResultMap("topicResult")
    List<KnowledgeTopicRow> findByCategoryId(@Param("categoryId") long categoryId);

    /** 插入题目并回填数据库生成的自增主键。 */
    @Insert("INSERT INTO knowledge_topic (category_id, title, description, sort_order) VALUES (#{categoryId}, #{title}, #{description}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeTopicRow row);

    /** 更新题目，允许调整所属分类。 */
    @Update("UPDATE knowledge_topic SET category_id = #{categoryId}, title = #{title}, description = #{description}, sort_order = #{sortOrder} WHERE id = #{id}")
    int update(KnowledgeTopicRow row);

    /** 删除题目，存在关联问题时由数据库外键阻止删除。 */
    @Delete("DELETE FROM knowledge_topic WHERE id = #{id}")
    int deleteById(@Param("id") long id);
}
