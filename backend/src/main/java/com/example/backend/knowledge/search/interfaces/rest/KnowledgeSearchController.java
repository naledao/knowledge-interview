package com.example.backend.knowledge.search.interfaces.rest;

import com.example.backend.knowledge.search.application.KnowledgeSearchResult;
import com.example.backend.knowledge.search.application.KnowledgeSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 知识搜索只读接口：服务端完成匹配、排序与分页，前端无需拉取全量数据。 */
@RestController
@RequestMapping("/api")
public class KnowledgeSearchController {
    private final KnowledgeSearchService searchService;

    public KnowledgeSearchController(KnowledgeSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET /api/search?q=redis 持久化&type=question&page=1&size=10
     * 多个关键词以空白分隔，需同时命中（AND）。
     */
    @GetMapping("/search")
    public KnowledgeSearchResult search(@RequestParam("q") String keyword,
                                        @RequestParam(defaultValue = "all") String type,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return searchService.search(keyword, type, page, size);
    }

    /** 参数校验失败返回 400，消息可直接展示给用户。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
