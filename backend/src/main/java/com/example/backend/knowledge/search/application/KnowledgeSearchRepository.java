package com.example.backend.knowledge.search.application;

import java.util.List;

/** 搜索读模型仓储：按关键词 AND 匹配初筛候选，相关度排序由应用服务完成。 */
public interface KnowledgeSearchRepository {
    /**
     * 查询指定类型的候选，结果按 sort_order、id 升序。
     *
     * @param terms 已规范化的关键词，每个词都必须命中标题或正文
     * @param lead  用于定位正文窗口的关键词
     * @param limit 最多返回的候选数量
     */
    List<KnowledgeSearchCandidate> search(KnowledgeSearchKind kind, List<String> terms, String lead, int limit);
}
