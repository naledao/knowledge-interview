<script setup lang="ts">
import { computed } from 'vue'
import 'highlight.js/styles/github.css'
import { renderMarkdown } from '../utils/markdown'

/**
 * 渲染知识内容中的 Markdown。renderMarkdown 已禁用原始 HTML 并校验链接，
 * 输出可直接交给 v-html；调用方不要绕过该函数自行拼接 HTML。
 */
const props = withDefaults(defineProps<{
  source: string | null | undefined
  /** compact：用于描述等次要正文，字号和间距更小。 */
  size?: 'default' | 'compact'
}>(), { size: 'default' })

const html = computed(() => renderMarkdown(props.source))
</script>

<template>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div :class="['markdown-body', `markdown-${size}`]" v-html="html"></div>
</template>

<style scoped>
.markdown-body { color: #334155; font-size: 16px; line-height: 1.9; overflow-wrap: anywhere; }
.markdown-compact { font-size: 15px; line-height: 1.85; }
.markdown-body > :first-child { margin-top: 0; }
.markdown-body > :last-child { margin-bottom: 0; }
.markdown-body :deep(p) { margin: 0 0 1em; }
.markdown-body :deep(:is(h3, h4, h5, h6)) { margin: 1.6em 0 .6em; color: #0f172a; line-height: 1.45; font-weight: 750; }
.markdown-body :deep(h3) { font-size: 1.22em; }
.markdown-body :deep(h4) { font-size: 1.1em; }
.markdown-body :deep(:is(h5, h6)) { font-size: 1em; }
.markdown-body :deep(:is(ul, ol)) { margin: 0 0 1em; padding-left: 1.6em; }
.markdown-body :deep(li) { margin: .25em 0; }
.markdown-body :deep(li > :is(ul, ol)) { margin: .25em 0 0; }
.markdown-body :deep(li > p) { margin: 0; }
.markdown-body :deep(strong) { color: #0f172a; font-weight: 700; }
.markdown-body :deep(a) { color: #2563eb; text-decoration: underline; text-underline-offset: 3px; }
.markdown-body :deep(blockquote) { margin: 0 0 1em; padding: .4em 1em; border-left: 4px solid #bfdbfe; border-radius: 0 8px 8px 0; background: #f8fafc; color: #475569; }
.markdown-body :deep(blockquote > :last-child) { margin-bottom: 0; }
.markdown-body :deep(hr) { margin: 1.6em 0; border: 0; border-top: 1px solid #e2e8f0; }
.markdown-body :deep(code) { font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace; font-size: .88em; }
.markdown-body :deep(:not(pre) > code) { padding: .15em .4em; border-radius: 5px; background: #f1f5f9; color: #be185d; overflow-wrap: anywhere; }
.markdown-body :deep(pre) { margin: 0 0 .8em; padding: 14px 16px; overflow-x: auto; border: 1px solid #e2e8f0; border-radius: 10px; background: #f8fafc; line-height: 1.6; }
.markdown-body :deep(pre code) { display: block; padding: 0; background: none; color: #1e293b; white-space: pre; overflow-wrap: normal; }
.markdown-body :deep(.md-table) { margin: 0 0 .8em; overflow-x: auto; }
.markdown-body :deep(table) { min-width: 100%; border-collapse: collapse; font-size: .92em; line-height: 1.6; }
.markdown-body :deep(:is(th, td)) { padding: 8px 12px; border: 1px solid #e2e8f0; text-align: left; vertical-align: top; }
.markdown-body :deep(th) { background: #f8fafc; color: #0f172a; font-weight: 700; white-space: nowrap; }
.markdown-body :deep(img) { max-width: 100%; height: auto; border-radius: 8px; }
@media (max-width: 720px) {
  .markdown-body { font-size: 15px; }
  .markdown-compact { font-size: 14px; }
  .markdown-body :deep(pre) { padding: 12px; font-size: 13px; }
}
</style>
