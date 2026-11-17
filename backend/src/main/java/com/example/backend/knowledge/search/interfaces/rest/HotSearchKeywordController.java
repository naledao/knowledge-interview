package com.example.backend.knowledge.search.interfaces.rest;

import com.example.backend.knowledge.search.application.HotSearchKeywordService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 热门搜索词只读接口：供搜索页展示推荐词。 */
@RestController
@RequestMapping("/api/search")
public class HotSearchKeywordController {
    private final HotSearchKeywordService hotKeywordService;

    public HotSearchKeywordController(HotSearchKeywordService hotKeywordService) {
        this.hotKeywordService = hotKeywordService;
    }

    /** GET /api/search/hot-keywords?limit=5 返回热门搜索词文本数组，没有数据时为空数组。 */
    @GetMapping("/hot-keywords")
    public List<String> hotKeywords(@RequestParam(defaultValue = "5") int limit) {
        return hotKeywordService.listHotKeywords(limit);
    }

    /** 参数校验失败返回 400，消息可直接展示给用户。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
