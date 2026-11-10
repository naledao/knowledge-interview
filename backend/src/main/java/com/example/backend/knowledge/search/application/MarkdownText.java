package com.example.backend.knowledge.search.application;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 把 Markdown 粗略转换为单行纯文本，供搜索摘要使用：去掉标记符号，只保留可读文字。
 * 摘要来自数据库截取的正文窗口，可能从代码块或列表中间开始，因此按行用正则处理，不做完整解析。
 * 规则与前端 utils/plainText.ts 保持一致，修改时请同步。
 */
final class MarkdownText {
    private record Rule(Pattern pattern, String replacement) { }

    private static final List<Rule> RULES = List.of(
            rule("(?m)^\\s*(```|~~~).*$", " "),                                  // 代码块围栏，保留其中的代码
            rule("!\\[([^\\]]*)\\]\\([^)]*\\)", "$1"),                            // 图片 → 替代文字
            rule("\\[([^\\]]+)\\]\\([^)]*\\)", "$1"),                             // 链接 → 链接文字
            rule("(?m)^\\s{0,3}#{1,6}\\s+", ""),                                  // 标题
            rule("(?m)^\\s{0,3}>\\s?", ""),                                       // 引用
            rule("(?m)^\\s*(?:[-*+]|\\d+[.)])\\s+(?:\\[[ xX]\\]\\s+)?", ""),      // 列表、任务列表
            rule("(?m)^\\s*\\|?(?:\\s*:?-{3,}:?\\s*\\|)+\\s*:?-{3,}:?\\s*\\|?\\s*$", " "), // 表格分隔行
            rule("\\|", " "),                                                     // 表格竖线
            rule("(?m)^\\s*(?:[-*_]\\s*){3,}$", " "),                             // 分隔线
            rule("(\\*\\*|__)(.+?)\\1", "$2"),                                    // 粗体
            rule("(^|[^\\w*])[*_]([^*_\\s][^*_]*?)[*_](?=[^\\w*]|$)", "$1$2"),    // 斜体
            rule("~~(.+?)~~", "$1"),                                              // 删除线
            rule("`+([^`]+?)`+", "$1"),                                           // 行内代码
            rule("\\s+", " "));

    private MarkdownText() { }

    private static Rule rule(String regex, String replacement) {
        return new Rule(Pattern.compile(regex), replacement);
    }

    static String toPlainText(String markdown) {
        if (markdown == null || markdown.isEmpty()) return "";
        String text = markdown;
        for (Rule rule : RULES) text = rule.pattern().matcher(text).replaceAll(rule.replacement());
        return text.strip();
    }
}
