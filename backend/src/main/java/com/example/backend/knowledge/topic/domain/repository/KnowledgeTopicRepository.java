package com.example.backend.knowledge.topic.domain.repository;

import com.example.backend.knowledge.topic.domain.model.KnowledgeTopic;
import java.util.List;
import java.util.Optional;

/** 题目领域仓储接口：定义数据操作能力，与 MyBatis 实现解耦。 */
public interface KnowledgeTopicRepository {
    /** 根据主键查询题目。 */
    Optional<KnowledgeTopic> findById(long id);
    /** 在指定分类内根据标题查询题目，用于唯一性检查。 */
    Optional<KnowledgeTopic> findByCategoryIdAndTitle(long categoryId, String title);
    /** 查询全部题目。 */
    List<KnowledgeTopic> findAll();
    /** 查询指定分类下的题目。 */
    List<KnowledgeTopic> findByCategoryId(long categoryId);
    /** 新建题目并返回数据库生成的主键。 */
    long insert(KnowledgeTopic topic);
    /** 更新题目并返回影响行数。 */
    int update(KnowledgeTopic topic);
    /** 删除题目并返回影响行数。 */
    int deleteById(long id);
}
