package com.example.backend.knowledge.question.domain.repository;

import com.example.backend.knowledge.question.domain.model.KnowledgeQuestion;
import com.example.backend.knowledge.shared.domain.model.PageRequest;
import com.example.backend.knowledge.shared.domain.model.PagedResult;
import java.util.List;
import java.util.Optional;

/** 问题领域仓储接口：定义持久化能力，不依赖具体数据库实现。 */
public interface KnowledgeQuestionRepository {
    /** 根据主键查询问题。 */
    Optional<KnowledgeQuestion> findById(long id);
    /** 查询全部问题。 */
    /** 数据库分页查询，返回当前页及总数。 */
    PagedResult<KnowledgeQuestion> findPage(Long topicId, PageRequest request);
    List<KnowledgeQuestion> findAll();
    /** 查询指定题目下的问题。 */
    List<KnowledgeQuestion> findByTopicId(long topicId);
    /** 查询同一主题中按业务排序紧邻的上一题和下一题。 */
    Optional<KnowledgeQuestion> findPrevious(long topicId, int sortOrder, long id);
    Optional<KnowledgeQuestion> findNext(long topicId, int sortOrder, long id);
    /** 随机查询一道题的主键，只读取主键以避免加载 HTML 答案；excludeId 非空时排除该题，没有可选题目时返回空。 */
    Optional<Long> findRandomId(Long excludeId);
    /** 创建问题并返回数据库生成的主键。 */
    long insert(KnowledgeQuestion question);
    /** 更新问题并返回受影响行数。 */
    int update(KnowledgeQuestion question);
    /** 根据主键删除问题并返回受影响行数。 */
    int deleteById(long id);
}
