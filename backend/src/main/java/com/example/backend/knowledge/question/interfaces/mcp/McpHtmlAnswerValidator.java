package com.example.backend.knowledge.question.interfaces.mcp;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * MCP 问题答案的输入格式校验：仅约束 Agent 新提交的非空答案。
 * 该规则属于 MCP 接口契约，不改变领域模型与其他接口对历史 Markdown 的兼容性。
 */
final class McpHtmlAnswerValidator {
    private static final Pattern DOCUMENT = Pattern.compile(
            "(?is)^<!doctype\\s+html\\s*>\\s*<html\\b[^>]*>\\s*<head\\b[^>]*>.*?</head>\\s*<body\\b[^>]*>.*?</body>\\s*</html>\\s*$");
    private static final Pattern STYLE = Pattern.compile("(?is)<style\\b[^>]*>.*?</style>");
    private static final Pattern EXTERNAL_ELEMENTS = Pattern.compile(
            "(?is)<(?:script\\b[^>]*\\bsrc\\s*=|link\\b[^>]*\\bhref\\s*=|iframe\\b|object\\b|embed\\b)");

    private McpHtmlAnswerValidator() {
    }

    /**
     * 创建时允许不提供答案；更新时允许 null（保持原值）或空字符串（清空答案）。
     * 非空答案必须为独立 HTML 文档，错误信息直接指导 Agent 修正后重试。
     */
    static void validate(String answerText) {
        if (answerText == null || answerText.isBlank()) {
            return;
        }
        String html = answerText.strip();
        if (html.startsWith("\uFEFF")) {
            html = html.substring(1).stripLeading();
        }
        if (!DOCUMENT.matcher(html).matches()) {
            throw new IllegalArgumentException("answerText 格式不符合要求：请直接传入从 <!DOCTYPE html> 开始、包含完整 <html><head>...</head><body>...</body></html> 的 HTML 文档；不要使用 Markdown 代码围栏、HTML 实体整体转义或额外说明。");
        }
        String lower = html.toLowerCase(Locale.ROOT);
        if (!STYLE.matcher(html).find()) {
            throw new IllegalArgumentException("answerText 缺少内联 CSS：请在 <head> 中使用 <style> 定义浅色主题，优先白色、浅灰、浅蓝色，确保文字可读。");
        }
        if (!lower.contains("name=\"viewport\"") && !lower.contains("name='viewport'")) {
            throw new IllegalArgumentException("answerText 缺少移动端 viewport：请在 <head> 添加 <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"> 并使用响应式布局。");
        }
        if (EXTERNAL_ELEMENTS.matcher(html).find() || lower.contains("@import")) {
            throw new IllegalArgumentException("answerText 禁止外部脚本、样式或嵌套页面：请使用内联 <style>、按需内联 <script>，不要依赖 CDN 或网络资源。");
        }
    }
}