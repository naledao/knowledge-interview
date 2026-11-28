<script setup lang="ts">
import { House, Compass, Search, ArrowRight, ArrowUpRight, Sparkles } from '@lucide/vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

/** 404 页面：引导用户返回首页，或通过关键词重新查找知识内容。 */
const router = useRouter()
const searchKeyword = ref('')

/** 复用现有搜索路由，不额外发起无意义的空关键词查询。 */
function searchKnowledge() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  void router.push({ name: 'search', query: { q: keyword } })
}
</script>

<template>
  <section class="not-found-page" aria-labelledby="not-found-heading">
    <div class="not-found-content">
      <p class="eyebrow"><span class="eyebrow-dot" aria-hidden="true"></span> PAGE NOT FOUND</p>

      <div class="error-illustration" aria-hidden="true">
        <div class="decoration-circle circle-outer"></div>
        <div class="decoration-circle circle-inner"></div>
        <div class="error-code">4<span class="zero">0<Sparkles class="zero-spark" :size="24" /></span>4</div>
        <Sparkles class="floating-star star-left" :size="23" />
        <Sparkles class="floating-star star-right" :size="18" />
        <div class="illustration-caption"><span class="caption-dot"></span> 探索未停止，知识仍在这里</div>
      </div>

      <h1 id="not-found-heading">哎呀，页面走丢了</h1>
      <p class="description">你访问的页面可能已被移动、删除，或者链接地址有误。<br class="desktop-break" />没关系，我们可以帮你重新找到学习方向。</p>

      <div class="primary-actions">
        <RouterLink class="home-action" to="/">
          <House :size="19" aria-hidden="true" />
          返回首页
        </RouterLink>
        <RouterLink class="browse-action" to="/search">
          <Compass :size="18" aria-hidden="true" />浏览知识库 <ArrowUpRight :size="16" aria-hidden="true" />
        </RouterLink>
      </div>

      <div class="search-card">
        <div class="search-card-heading">
          <span class="search-icon" aria-hidden="true"><Search :size="22" /></span>
          <div><strong>换个关键词试试？</strong><p>搜索分类、主题和面试问题，继续你的知识探索。</p></div>
        </div>
        <form class="search-form" role="search" @submit.prevent="searchKnowledge">
          <label for="not-found-search" class="sr-only">搜索知识点</label>
          <input id="not-found-search" v-model="searchKeyword" type="search" placeholder="输入想了解的知识点..." autocomplete="off" maxlength="200" />
          <button type="submit" :disabled="!searchKeyword.trim()">搜索知识 <ArrowRight :size="16" aria-hidden="true" /></button>
        </form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.not-found-page { position: relative; display: flex; align-items: center; justify-content: center; padding: 10px 0 20px; overflow: hidden; color: #0f172a; }
.not-found-page::before { content: ''; position: absolute; top: 0; left: 50%; width: min(100%, 680px); height: 340px; transform: translateX(-50%); pointer-events: none; background: radial-gradient(ellipse, rgba(219, 234, 254, .65) 0%, rgba(237, 233, 254, .28) 42%, transparent 72%); }
.not-found-content { position: relative; z-index: 1; width: min(100%, 600px); text-align: center; }
.eyebrow { display: inline-flex; align-items: center; gap: 8px; margin: 0; color: #2563eb; font-size: 11px; font-weight: 800; letter-spacing: .16em; }
.eyebrow-dot, .caption-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #3b82f6; }
.error-illustration { position: relative; display: grid; place-items: center; height: 164px; margin: 4px auto; isolation: isolate; }
.decoration-circle { position: absolute; left: 50%; top: 44%; z-index: -1; transform: translate(-50%, -50%); border: 1px solid rgba(148, 163, 184, .18); border-radius: 50%; }
.circle-outer { width: 240px; height: 150px; }
.circle-inner { width: 184px; height: 112px; background: rgba(255, 255, 255, .32); }
.error-code { display: flex; align-items: center; justify-content: center; margin-top: -16px; color: #2563eb; font-size: clamp(78px, 16vw, 114px); font-weight: 850; letter-spacing: -.08em; line-height: 1; text-shadow: 0 12px 28px rgba(59, 130, 246, .14); }
.zero { position: relative; color: #8b5cf6; }
.zero-spark { position: absolute; top: 38%; left: 45%; display: inline-block; vertical-align: middle; transform: translate(-50%, -50%); color: #fff; }
.floating-star { position: absolute; width: 20px; height: 20px; color: #a78bfa; }
.star-left { top: 24px; left: 18%; }
.star-right { top: 56px; right: 16%; color: #60a5fa; }
.illustration-caption { position: absolute; bottom: 2px; display: flex; align-items: center; gap: 7px; padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 999px; background: rgba(255, 255, 255, .92); box-shadow: 0 6px 18px rgba(15, 23, 42, .04); color: #64748b; font-size: 12px; }
.caption-dot { background: #34d399; }
h1 { margin: 8px 0 8px; font-size: clamp(22px, 4vw, 28px); font-weight: 800; letter-spacing: -.03em; }
.description { margin: 0 auto; color: #64748b; font-size: 14px; line-height: 1.65; }
.primary-actions { display: flex; justify-content: center; flex-wrap: wrap; gap: 10px; margin: 16px 0 20px; }
.primary-actions a { display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-height: 40px; padding: 0 18px; border-radius: 10px; font-size: 14px; font-weight: 700; text-decoration: none; transition: transform .2s, background .2s, box-shadow .2s; }
.primary-actions a > svg, .search-form button > svg { flex-shrink: 0; }
.primary-actions a:hover { transform: translateY(-2px); }
.home-action { background: #2563eb; color: #fff; box-shadow: 0 6px 16px rgba(37, 99, 235, .2); }
.home-action:hover { background: #1d4ed8; box-shadow: 0 10px 22px rgba(37, 99, 235, .25); }
.home-action svg { width: 17px; height: 17px; }
.browse-action { border: 1px solid #cbd5e1; background: #fff; color: #334155; }
.browse-action:hover { border-color: #93c5fd; background: #eff6ff; }
.search-card { width: min(100%, 520px); margin: 0 auto; padding: 16px 18px; border: 1px solid #dce6f6; border-radius: 14px; background: rgba(255, 255, 255, .94); box-shadow: 0 12px 28px #1d4ed80a; text-align: left; }
.search-card-heading { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.search-icon { display: grid; place-items: center; width: 36px; height: 36px; flex-shrink: 0; border-radius: 10px; background: linear-gradient(135deg, #eaf2ff, #f1edff); color: #2563eb; }
.search-card-heading strong { font-size: 14px; }
.search-card-heading p { margin: 3px 0 0; color: #64748b; font-size: 12px; line-height: 1.45; }
.search-form { display: flex; gap: 8px; }
.search-form input { flex: 1; min-width: 0; height: 40px; padding: 0 12px; border: 1px solid #cbd5e1; border-radius: 9px; outline: none; background: #fff; color: #0f172a; font: inherit; font-size: 13px; }
.search-form input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, .11); }
.search-form button { display: inline-flex; align-items: center; justify-content: center; gap: 6px; min-width: 98px; padding: 0 14px; border: none; border-radius: 9px; background: #0f172a; color: #fff; font: inherit; font-size: 13px; font-weight: 700; white-space: nowrap; cursor: pointer; }
.search-form button:hover:not(:disabled) { background: #1e40af; }
.search-form button:disabled { opacity: .5; cursor: not-allowed; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
a:focus-visible, button:focus-visible { outline: 3px solid #93c5fd; outline-offset: 3px; }
@media (max-width: 640px) {
  .not-found-page { padding: 4px 0 12px; }
  .error-illustration { height: 144px; }
  .circle-outer { width: 204px; height: 132px; }
  .circle-inner { width: 158px; height: 100px; }
  .star-left { left: 10%; }
  .star-right { right: 9%; }
  .description { font-size: 13px; }
  .desktop-break { display: none; }
  .primary-actions { margin: 14px 0 16px; }
  .primary-actions a { gap: 6px; padding: 0 14px; }
  .search-card { padding: 14px; }
  .search-form { gap: 6px; }
  .search-form button { gap: 5px; min-width: 84px; padding: 0 10px; }
}
@media (prefers-reduced-motion: reduce) { .primary-actions a { transition: none; } }
</style>