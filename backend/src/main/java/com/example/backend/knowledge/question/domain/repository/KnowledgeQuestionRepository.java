package com.example.backend.knowledge.question.domain.repository;

import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import java.util.List;
import java.util.Optional;

/** 问题领域仓储接口：定义持久化能力，不依赖具体数据库实现。 */
public interface KnowledgeQuestionRepository {
    /** 根据主键查询问题。 */
    Optional<KnowledgeQuestion> findById(long id);
    /** 查询全部问题。 */
    List<KnowledgeQuestion> findAll();
    /** 查询指定题目下的问题。 */
    List<KnowledgeQuestion> findByTopicId(long topicId);
    /** 创建问题并返回数据库生成的主键。 */
    long insert(KnowledgeQuestion question);
    /** 更新问题并返回受影响行数。 */
    int update(KnowledgeQuestion question);
    /** 根据主键删除问题并返回受影响行数。 */
    int deleteById(long id);
}
