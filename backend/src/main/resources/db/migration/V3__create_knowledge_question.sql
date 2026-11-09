-- 具体问题：一个题目包含多个问题，可选保存答案
CREATE TABLE IF NOT EXISTS knowledge_question (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    topic_id BIGINT UNSIGNED NOT NULL COMMENT '所属题目',
    question_text TEXT NOT NULL COMMENT '问题内容',
    answer_text LONGTEXT NULL COMMENT '答案或知识说明，可为空',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_question_topic_sort (topic_id, sort_order, id),
    CONSTRAINT fk_question_topic FOREIGN KEY (topic_id) REFERENCES knowledge_topic (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识问题表';
