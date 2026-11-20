package com.example.backend.knowledge.search.interfaces.rest;

import com.example.backend.knowledge.search.application.HotSearchKeywordService;
import com.example.backend.knowledge.search.application.KnowledgeSearchResult;
import com.example.backend.knowledge.search.application.KnowledgeSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 知识搜索只读接口：支持普通关键词搜索与精确搜索（目前支持问题 ID），由服务端完成匹配、排序与分页。 */
@RestController
@RequestMapping("/api")
public class KnowledgeSearchController {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchController.class);

    private final KnowledgeSearchService searchService;
    private final HotSearchKeywordService hotKeywordService;

    public KnowledgeSearchController(KnowledgeSearchService searchService, HotSearchKeywordService hotKeywordService) {
        this.searchService = searchService;
        this.hotKeywordService = hotKeywordService;
    }

    /**
     * GET /api/search?q=redis 持久化&mode=keyword&type=question&page=1&size=10
     * mode=keyword 时按空白分隔多词 AND 模糊匹配；mode=exact 时按问题 ID 精确查找。
     */
    @GetMapping("/search")
    public KnowledgeSearchResult search(@RequestParam("q") String keyword,
                                        @RequestParam(defaultValue = "keyword") String mode,
                                        @RequestParam(defaultValue = "all") String type,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        KnowledgeSearchResult result = searchService.search(keyword, mode, type, page, size);
        recordHotKeyword(result);
        return result;
    }

    /**
     * 统计热门搜索词：仅在普通关键词模式下“全部类型、第一页且有结果”时计数，精确 ID 查询、翻页和切换类型不计入热门词；
     * 统计失败只记录日志，不影响搜索结果的返回。
     */
    private void recordHotKeyword(KnowledgeSearchResult result) {
        if (!"keyword".equals(result.mode()) || result.counts().all() == 0 || result.page() != 1 || !"all".equals(result.type())) {
            return;
        }
        try {
            hotKeywordService.recordSearch(result.keyword());
        } catch (RuntimeException ex) {
            log.warn("记录热门搜索词失败: {}", result.keyword(), ex);
        }
    }

    /** 参数校验失败返回 400，消息可直接展示给用户。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}