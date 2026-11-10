import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import dockerfile from 'highlight.js/lib/languages/dockerfile'
import go from 'highlight.js/lib/languages/go'
import ini from 'highlight.js/lib/languages/ini'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import kotlin from 'highlight.js/lib/languages/kotlin'
import plaintext from 'highlight.js/lib/languages/plaintext'
import protobuf from 'highlight.js/lib/languages/protobuf'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'

/**
 * 知识内容的 Markdown 渲染。
 * - html: false：内容中的原始 HTML 会被转义为文本，配合 markdown-it 默认的链接校验
 *   （拒绝 javascript: / vbscript: / file: 等协议），输出可安全用于 v-html，无需额外清洗。
 * - breaks: true：保留单个换行，兼容已有的纯文本答案。
 * - 只注册面试题中常见的语言，避免把 highlight.js 全量语言打进包里。
 */
const languages = {
  bash, css, dockerfile, go, ini, java, javascript, json, kotlin, plaintext,
  protobuf, python, sql, typescript, xml, yaml,
}
for (const [name, language] of Object.entries(languages)) hljs.registerLanguage(name, language)
hljs.registerAliases(['sh', 'shell', 'zsh', 'console'], { languageName: 'bash' })
hljs.registerAliases(['html', 'vue', 'svg'], { languageName: 'xml' })
hljs.registerAliases(['properties', 'toml', 'conf'], { languageName: 'ini' })
hljs.registerAliases(['proto'], { languageName: 'protobuf' })
hljs.registerAliases(['text', 'txt'], { languageName: 'plaintext' })

const md: MarkdownIt = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code, lang) {
    const language = lang.trim().toLowerCase()
    if (language && hljs.getLanguage(language)) {
      try {
        return hljs.highlight(code, { language, ignoreIllegals: true }).value
      } catch {
        // 高亮失败时回退为转义后的纯文本。
      }
    }
    return ''
  },
})

/** 正文标题整体下移两级：页面已有 h1（问题）和 h2（区块标题），答案中的 # 从 h3 开始。 */
md.renderer.rules.heading_open = (tokens, idx, options, _env, self) => {
  const token = tokens[idx]!
  token.tag = `h${Math.min(6, Number(token.tag.slice(1)) + 2)}`
  return self.renderToken(tokens, idx, options)
}
md.renderer.rules.heading_close = (tokens, idx, options, _env, self) => {
  const open = tokens[idx - 2]
  if (open?.type === 'heading_open') tokens[idx]!.tag = open.tag
  return self.renderToken(tokens, idx, options)
}

/** 站外链接在新窗口打开，并切断 opener 引用。 */
md.renderer.rules.link_open = (tokens, idx, options, _env, self) => {
  const token = tokens[idx]!
  if (/^https?:\/\//i.test(token.attrGet('href') ?? '')) {
    token.attrSet('target', '_blank')
    token.attrSet('rel', 'noopener noreferrer')
  }
  return self.renderToken(tokens, idx, options)
}

/** 宽表格放进可横向滚动的容器，避免在手机上撑破页面。 */
md.renderer.rules.table_open = (tokens, idx, options, _env, self) =>
  `<div class="md-table">${self.renderToken(tokens, idx, options)}`
md.renderer.rules.table_close = (tokens, idx, options, _env, self) =>
  `${self.renderToken(tokens, idx, options)}</div>`

md.renderer.rules.image = (tokens, idx, options, env, self) => {
  tokens[idx]!.attrSet('loading', 'lazy')
  return self.renderToken(tokens, idx, options)
}

export function renderMarkdown(source: string | null | undefined): string {
  return source ? md.render(source) : ''
}
