package com.example.backend.config.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 将前端 History 模式路由转发给 Vue，由浏览器处理页面导航。 */
@Controller
public class SpaForwardController {
    @GetMapping({"/categories/{id}", "/topics/{id}", "/questions/{id}", "/search", "/api-key"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}