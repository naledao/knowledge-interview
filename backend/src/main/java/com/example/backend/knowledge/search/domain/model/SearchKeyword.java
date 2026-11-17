package com.example.backend.knowledge.search.domain.model;

import java.util.Optional;

/** 搜索关键词领域模型：记录被搜索的关键词、累计次数及推荐控制状态。 */
public record SearchKeyword(String keyword, long searchCount, boolean pinned, int sortOrder, boolean enabled) {
    /** 可作为热门词展示的最大长度，避免把整句话当成推荐词。 */
    public static final int MAX_RECORDABLE_LENGTH = 30;

    /** 校验关键词不能为空、次数不能为负数。 */
    public SearchKeyword {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        if (searchCount < 0) {
            throw new IllegalArgumentException("搜索次数不能为负数");
        }
    }

    /**
     * 规范化待统计的搜索词：去除首尾空白并合并连续空白。
     * 为空或超过长度上限时返回空，表示该搜索词不参与热门统计。
     */
    public static Optional<String> normalizeForRecord(String raw) {
        if (raw == null) return Optional.empty();
        String normalized = raw.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty() || normalized.length() > MAX_RECORDABLE_LENGTH) return Optional.empty();
        return Optional.of(normalized);
    }
}
