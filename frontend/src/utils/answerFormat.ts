/**
 * 识别 Agent 输出的 HTML 文档或 HTML 片段。
 * Markdown 代码块仍按 Markdown 展示，避免将示例代码误当作可执行页面。
 * Agent 也可以在 HTML 前添加 <!-- answer-format: html --> 显式声明格式。
 */
export function isHtmlAnswer(source: string): boolean {
  const text = source.trim()
  if (!text || text.startsWith('```') || text.startsWith('~~~')) return false
  if (/^<!--\s*answer-format\s*:\s*html\s*-->/i.test(text)) return true
  const withoutComments = text.replace(/^(?:\s*<!--[\s\S]*?-->\s*)+/, '')
  return /^<(?:!doctype\s+html\b|html\b|head\b|body\b|main\b|article\b|section\b|div\b|p\b|h[1-6]\b|ul\b|ol\b|table\b|pre\b|style\b|script\b|figure\b|svg\b|canvas\b|form\b|button\b|blockquote\b|details\b|summary\b|img\b|video\b)/i.test(withoutComments)
}