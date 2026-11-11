package com.example.backend.knowledge.question.interfaces.mcp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 验证 MCP 新答案格式要求，同时保持无答案、清空答案与历史数据兼容。 */
class McpHtmlAnswerValidatorTests {
    private static final String VALID_HTML = """
            <!DOCTYPE html>
            <html lang="zh-CN"><head><meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <style>body { background: white; color: #334155; }</style></head>
            <body><h2>知识答案</h2><p>解释</p><script>document.body.dataset.ready = 'yes'</script></body></html>
            """;

    @Test
    void acceptsFullHtmlDocument() {
        assertDoesNotThrow(() -> McpHtmlAnswerValidator.validate(VALID_HTML));
    }

    @Test
    void allowsMissingOrClearedAnswer() {
        assertDoesNotThrow(() -> McpHtmlAnswerValidator.validate(null));
        assertDoesNotThrow(() -> McpHtmlAnswerValidator.validate(""));
        assertDoesNotThrow(() -> McpHtmlAnswerValidator.validate("  "));
    }

    @Test
    void rejectsMarkdownAndEscapedHtml() {
        assertThrows(IllegalArgumentException.class, () -> McpHtmlAnswerValidator.validate("```html\n" + VALID_HTML + "\n```"));
        assertThrows(IllegalArgumentException.class, () -> McpHtmlAnswerValidator.validate("&lt;!DOCTYPE html&gt;"));
        assertThrows(IllegalArgumentException.class, () -> McpHtmlAnswerValidator.validate("普通 Markdown 答案"));
    }

    @Test
    void requiresInlineStyleAndViewport() {
        var noStyle = VALID_HTML.replace("<style>body { background: white; color: #334155; }</style>", "");
        var noViewport = VALID_HTML.replace("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">", "");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> McpHtmlAnswerValidator.validate(noStyle)).getMessage().contains("CSS"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> McpHtmlAnswerValidator.validate(noViewport)).getMessage().contains("viewport"));
    }

    @Test
    void rejectsExternalDependencies() {
        var external = VALID_HTML.replace("</head>", "<script src=\"https://example.com/lib.js\"></script></head>");
        assertThrows(IllegalArgumentException.class, () -> McpHtmlAnswerValidator.validate(external));
    }
}