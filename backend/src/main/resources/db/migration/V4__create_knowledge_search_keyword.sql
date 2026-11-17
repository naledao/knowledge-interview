-- 搜索关键词：记录搜索次数，用于搜索页的热门搜索词推荐
CREATE TABLE IF NOT EXISTS knowledge_search_keyword (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    keyword VARCHAR(100) NOT NULL COMMENT '搜索关键词，保存首次出现时的写法；唯一键不区分大小写',
    search_count BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计搜索次数（仅统计有结果的搜索）',
    pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶推荐：0 否，1 是',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '置顶词的排序值，越小越靠前',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否参与推荐：0 屏蔽，1 启用',
    last_searched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次搜索时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_search_keyword_text (keyword),
    KEY idx_search_keyword_hot (enabled, pinned, sort_order, search_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索关键词统计表';
