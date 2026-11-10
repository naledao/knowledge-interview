/**
 * 把 Markdown 粗略转换为单行纯文本，用于卡片、列表等预览位置。
 * 只需去掉标记符号、保留可读文字，因此不引入 Markdown 解析器，避免首页加载额外的依赖。
 * 规则与后端 MarkdownText 保持一致，修改时请同步。
 */
export function markdownToPlainText(source: string | null | undefined): string {
  if (!source) return ''
  return source
    .replace(/^\s*(```|~~~).*$/gm, ' ') // 代码块围栏，保留其中的代码
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1') // 图片 → 替代文字
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1') // 链接 → 链接文字
    .replace(/^\s{0,3}#{1,6}\s+/gm, '') // 标题
    .replace(/^\s{0,3}>\s?/gm, '') // 引用
    .replace(/^\s*(?:[-*+]|\d+[.)])\s+(?:\[[ xX]\]\s+)?/gm, '') // 列表、任务列表
    .replace(/^\s*\|?(?:\s*:?-{3,}:?\s*\|)+\s*:?-{3,}:?\s*\|?\s*$/gm, ' ') // 表格分隔行
    .replace(/\|/g, ' ') // 表格竖线
    .replace(/^\s*(?:[-*_]\s*){3,}$/gm, ' ') // 分隔线
    .replace(/(\*\*|__)(.+?)\1/g, '$2') // 粗体
    .replace(/(^|[^\w*])[*_]([^*_\s][^*_]*?)[*_](?=[^\w*]|$)/g, '$1$2') // 斜体
    .replace(/~~(.+?)~~/g, '$1') // 删除线
    .replace(/`+([^`]+?)`+/g, '$1') // 行内代码
    .replace(/\s+/g, ' ')
    .trim()
}
