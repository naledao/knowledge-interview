<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

/**
 * 将 Agent 生成的 HTML/CSS/JS 放在独立的不透明源 iframe 内运行。
 * 不使用 allow-same-origin，隔离主页面 Cookie、localStorage 和 API Key。
 * CSP 禁止外部资源和网络连接，仅允许内联脚本与样式。
 */
const props = defineProps<{ source: string }>()
const iframeRef = ref<HTMLIFrameElement | null>(null)
const frameHeight = ref(1)
const MESSAGE_TYPE = 'knowledge-answer-frame-resize'
const MIN_HEIGHT = 1
const MAX_HEIGHT = 100000

/** 防止内容通过 HTML 属性破坏 iframe 文档结构。 */
function escapeAttribute(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

/**
 * 先使用不执行脚本的 DOMParser 解析完整文档和片段，再重组为独立页面。
 * 安全策略放在用户内容之前，避免原文中的 head/script 提前执行。
 */
const srcdoc = computed(() => {
  const parsed = new DOMParser().parseFromString(props.source, 'text/html')
  // 参考答案不允许通过 meta refresh、base 标签改变隔离页的导航与资源解析方式。
  parsed.querySelectorAll('base, meta[http-equiv="refresh"]').forEach((node) => node.remove())
  const bodyAttributes = Array.from(parsed.body.attributes)
    .filter((attribute) => ['id', 'class', 'style', 'dir'].includes(attribute.name))
    .map((attribute) => ` ${attribute.name}="${escapeAttribute(attribute.value)}"`)
    .join('')
  const lang = escapeAttribute(parsed.documentElement.getAttribute('lang') || 'zh-CN')
  const csp = "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data: blob:; media-src data: blob:; font-src data:; connect-src 'none'; frame-src 'none'; object-src 'none'; form-action 'none'; base-uri 'none'"
const resizeScript = `<script>(function(){var pending=false;function send(){if(pending)return;pending=true;requestAnimationFrame(function(){pending=false;var body=document.body;if(!body)return;var height=Math.ceil(Math.max(body.scrollHeight,body.offsetHeight,body.getBoundingClientRect().height));parent.postMessage({type:'${MESSAGE_TYPE}',height:height},'*')})}function init(){send();if('ResizeObserver' in window){var observer=new ResizeObserver(send);observer.observe(document.body);observer.observe(document.documentElement)}if('MutationObserver' in window){new MutationObserver(send).observe(document.body,{subtree:true,childList:true,attributes:true,characterData:true})}window.addEventListener('load',send);window.addEventListener('resize',send);if(document.fonts){document.fonts.ready.then(send)}setTimeout(send,100)}if(document.readyState==='loading'){document.addEventListener('DOMContentLoaded',init,{once:true})}else{init()}})();<\/script>`
  return `<!doctype html><html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="Content-Security-Policy" content="${csp}"><style>*,*::before,*::after{box-sizing:border-box}html{overflow-x:hidden}body{margin:0;padding:0;min-width:0;overflow-wrap:anywhere;font-family:system-ui,-apple-system,'Segoe UI',sans-serif;line-height:1.7;color:#1e293b}img,video,svg,canvas{max-width:100%}pre{max-width:100%;overflow:auto}</style>${resizeScript}${parsed.head.innerHTML}<style>html,body{overflow:hidden!important}</style></head><body${bodyAttributes}>${parsed.body.innerHTML}</body></html>`
})

/** 只接受当前 iframe 发出的尺寸消息，限制高度以避免恶意内容撑破页面。 */
function handleMessage(event: MessageEvent) {
  if (!iframeRef.value || event.source !== iframeRef.value.contentWindow || event.origin !== 'null') return
  const data = event.data
  if (!data || data.type !== MESSAGE_TYPE || typeof data.height !== 'number' || !Number.isFinite(data.height)) return
  frameHeight.value = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, Math.ceil(data.height)))
}

watch(() => props.source, () => { frameHeight.value = 1 })
onMounted(() => window.addEventListener('message', handleMessage))
onUnmounted(() => window.removeEventListener('message', handleMessage))
</script>

<template>
  <div class="html-answer-renderer">
    <iframe
      ref="iframeRef"
      class="answer-frame"
      :srcdoc="srcdoc"
      :style="{ height: `${frameHeight}px` }"
      sandbox="allow-scripts"
      scrolling="no"
      referrerpolicy="no-referrer"
      title="交互式参考答案"
    ></iframe>
  </div>
</template>

<style scoped>
.html-answer-renderer { width: 100%; min-width: 0; }
.answer-frame { display: block; width: 100%; min-width: 0; border: 0; padding: 0; background: transparent; overflow: hidden; }
</style>