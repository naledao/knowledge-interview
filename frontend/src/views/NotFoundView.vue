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
.not-found-page { position: relative; display: flex; align-items: center; justify-content: center; min-height: min(800px, calc(100vh - 148px)); padding: 30px 20px 82px; overflow: hidden; color: #0f172a; }
.not-found-page::before { content: ''; position: absolute; top: 5%; left: 50%; width: min(100%, 780px); height: 490px; transform: translateX(-50%); pointer-events: none; background: radial-gradient(ellipse, rgba(219, 234, 254, .68) 0%, rgba(237, 233, 254, .3) 42%, transparent 72%); }
.not-found-content { position: relative; z-index: 1; width: min(100%, 660px); text-align: center; }
.eyebrow { display: inline-flex; align-items: center; gap: 9px; margin: 0; color: #2563eb; font-size: 12px; font-weight: 800; letter-spacing: .16em; }
.eyebrow-dot, .caption-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; background: #3b82f6; }
.error-illustration { position: relative; display: grid; place-items: center; height: 260px; margin: 8px auto 6px; isolation: isolate; }
.decoration-circle { position: absolute; left: 50%; top: 42%; z-index: -1; transform: translate(-50%, -50%); border: 1px solid rgba(148, 163, 184, .18); border-radius: 50%; }
.circle-outer { width: 330px; height: 220px; }
.circle-inner { width: 253px; height: 158px; background: rgba(255, 255, 255, .32); }
.error-code { display: flex; align-items: center; justify-content: center; margin-top: -24px; color: #2563eb; font-size: clamp(106px, 22vw, 166px); font-weight: 850; letter-spacing: -.09em; line-height: 1; text-shadow: 0 16px 40px rgba(59, 130, 246, .14); }
.zero { position: relative; color: #8b5cf6; }
.zero-spark { position: absolute; top: 38%; left: 45%; transform: translate(-50%, -50%); color: white; font-size: 32px; font-weight: 400; text-shadow: none; }
.floating-star { position: absolute; color: #a78bfa; font-size: 29px; }
.star-left { top: 47px; left: 15%; }
.star-right { top: 95px; right: 13%; color: #60a5fa; font-size: 21px; }
.illustration-caption { position: absolute; bottom: 9px; display: flex; align-items: center; gap: 9px; padding: 9px 16px; border: 1px solid #e2e8f0; border-radius: 999px; background: rgba(255, 255, 255, .92); box-shadow: 0 8px 25px rgba(15, 23, 42, .04); color: #64748b; font-size: 12px; }
.caption-dot { width: 6px; height: 6px; background: #34d399; }
h1 { margin: 12px 0 15px; font-size: clamp(28px, 5vw, 39px); font-weight: 800; letter-spacing: -.03em; }
.description { margin: 0 auto; color: #64748b; font-size: 15px; line-height: 1.9; }
.primary-actions { display: flex; justify-content: center; flex-wrap: wrap; gap: 12px; margin: 28px 0 48px; }
.primary-actions a { display: inline-flex; align-items: center; justify-content: center; gap: 10px; min-height: 46px; padding: 0 23px; border-radius: 11px; font-size: 14px; font-weight: 700; text-decoration: none; transition: transform .2s, background .2s, box-shadow .2s; }
.primary-actions a:hover { transform: translateY(-2px); }
.home-action { background: #2563eb; color: white; box-shadow: 0 8px 20px rgba(37, 99, 235, .22); }
.home-action:hover { background: #1d4ed8; box-shadow: 0 12px 26px rgba(37, 99, 235, .27); }
.home-action svg { width: 17px; height: 17px; }
.browse-action { border: 1px solid #cbd5e1; background: white; color: #334155; }
.browse-action:hover { border-color: #93c5fd; background: #eff6ff; }
.search-card { width: min(100%, 548px); margin: 0 auto; padding: 25px 27px; border: 1px solid #e2e8f0; border-radius: 20px; background: rgba(255, 255, 255, .93); box-shadow: 0 15px 46px rgba(15, 23, 42, .045); text-align: left; }
.search-card-heading { display: flex; align-items: center; gap: 13px; margin-bottom: 20px; }
.search-icon { display: grid; place-items: center; width: 43px; height: 43px; flex-shrink: 0; border-radius: 12px; background: #eff6ff; color: #2563eb; font-size: 29px; }
.search-card-heading strong { font-size: 15px; }
.search-card-heading p { margin: 6px 0 0; color: #64748b; font-size: 12px; line-height: 1.5; }
.search-form { display: flex; gap: 8px; }
.search-form input { flex: 1; min-width: 0; height: 44px; padding: 0 14px; border: 1px solid #cbd5e1; border-radius: 9px; outline: none; background: #fff; color: #0f172a; font: inherit; font-size: 13px; }
.search-form input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, .11); }
.search-form button { min-width: 109px; padding: 0 14px; border: none; border-radius: 9px; background: #0f172a; color: white; font: inherit; font-size: 13px; font-weight: 700; cursor: pointer; }
.search-form button:hover:not(:disabled) { background: #1e40af; }
.search-form button:disabled { opacity: .5; cursor: not-allowed; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
a:focus-visible, button:focus-visible { outline: 3px solid #93c5fd; outline-offset: 3px; }
@media (max-width: 640px) { .not-found-page { min-height: calc(100vh - 135px); padding: 24px 0 48px; } .error-illustration { height: 220px; } .circle-outer { width: 270px; height: 190px; } .circle-inner { width: 210px; height: 138px; } .error-code { font-size: clamp(104px, 26vw, 145px); } .star-left { left: 8%; } .star-right { right: 7%; } .illustration-caption { bottom: 3px; } .description { font-size: 14px; } .desktop-break { display: none; } .primary-actions { margin: 25px 0 36px; } .search-card { padding: 21px 18px; border-radius: 16px; } .search-form { gap: 7px; } .search-form button { min-width: 85px; padding: 0 10px; } }
@media (max-width: 360px) { .primary-actions a { padding: 0 17px; } .search-card-heading p { font-size: 11px; } }
@media (prefers-reduced-motion: reduce) { .primary-actions a { transition: none; } }

/* 页面图标增强：404引导和快捷搜索。 */
.zero-spark { display: inline-block; vertical-align: middle; }
.floating-star { width: 23px; height: 23px; }
.primary-actions a, .search-form button { display: inline-flex; align-items: center; justify-content: center; gap: 9px; }
.primary-actions a > svg, .search-form button > svg { flex-shrink: 0; }
.search-card { border-color: #dce6f6; box-shadow: 0 16px 35px #1d4ed80a; }
.search-icon { background: linear-gradient(135deg, #eaf2ff, #f1edff); }
.search-form button { white-space: nowrap; }
@media (max-width: 640px) { .primary-actions a { gap: 6px; } .search-form button { gap: 5px; } }
</style>