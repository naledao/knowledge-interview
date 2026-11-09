-- 知识题目：一个分类包含多个题目
CREATE TABLE IF NOT EXISTS knowledge_topic (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '所属分类',
    title VARCHAR(255) NOT NULL COMMENT '题目标题',
    description TEXT NULL COMMENT '题目描述或背景',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_topic_category_title (category_id, title),
    KEY idx_topic_category_sort (category_id, sort_order, id),
    CONSTRAINT fk_topic_category FOREIGN KEY (category_id) REFERENCES knowledge_category (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识题目表';
