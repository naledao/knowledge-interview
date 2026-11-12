<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

/** 全站导航：PC 展示横向导航与快捷搜索，移动端使用可收起菜单。 */
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const searchKeyword = ref('')
/** 仅内置安卓 WebView 展示服务器配置入口。 */
const isNativeAndroid = navigator.userAgent.includes('KnowledgeInterviewAndroid/')

/** 分类、主题及问题详情统一归属于知识探索导航。 */
const activeSection = computed(() => {
  if (route.name === 'home') return 'home'
  if (route.name === 'api-key') return 'settings'
  if (['search', 'category-detail', 'topic-detail', 'question-detail'].includes(String(route.name))) return 'explore'
  return ''
})

function closeMenu() {
  menuOpen.value = false
}

/** 导航快捷搜索和现有搜索结果页共用 q 查询参数。 */
function submitSearch() {
  const keyword = searchKeyword.value.trim()
  closeMenu()
  void router.push({ name: 'search', query: keyword ? { q: keyword } : {} })
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') closeMenu()
}

function handleResize() {
  if (window.innerWidth > 760) closeMenu()
}

/** 路由跳转后关闭菜单，避免覆盖新页面。 */
watch(() => route.fullPath, () => {
  closeMenu()
  if (route.name === 'search' && typeof route.query.q === 'string') {
    searchKeyword.value = route.query.q
  }
}, { immediate: true })

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="app-shell">
    <a class="skip-link" href="#main-content">跳转到主要内容</a>
    <header class="site-header">
      <div class="header-inner">
        <RouterLink to="/" class="brand" aria-label="知识题库首页" @click="closeMenu">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M12 6.5C9.8 4.9 6.8 4.5 3 5.3v13c3.8-.8 6.8-.4 9 1.2m0-13c2.2-1.6 5.2-2 9-1.2v13c-3.8-.8-6.8-.4-9 1.2m0-13v13" /></svg>
          </span>
          <span class="brand-name">知识<span>题库</span></span>
        </RouterLink>

        <nav class="desktop-nav" aria-label="主导航">
          <RouterLink to="/" :class="{ 'is-active': activeSection === 'home' }">首页</RouterLink>
          <RouterLink to="/search" :class="{ 'is-active': activeSection === 'explore' }">探索题库</RouterLink>
        </nav>

        <form class="header-search" role="search" @submit.prevent="submitSearch">
          <label for="header-search-input" class="visually-hidden">搜索知识点</label>
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m16 16 4 4"/></svg>
          <input id="header-search-input" v-model="searchKeyword" type="search" placeholder="搜索知识点、面试题..." autocomplete="off" />
          <button type="submit" aria-label="搜索知识">搜索</button>
        </form>

        <div class="header-actions">
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/update" class="native-server-link" aria-label="检查更新" title="检查更新" @click="closeMenu">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 11a8 8 0 1 0-2.4 5.7"/><path d="M20 4v7h-7"/></svg>
          </a>
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/backend" class="native-server-link" aria-label="服务器设置" title="服务器设置" @click="closeMenu">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="4" y="4" width="16" height="7" rx="2"/><rect x="4" y="13" width="16" height="7" rx="2"/><path d="M8 7.5h.01M8 16.5h.01M12 7.5h5M12 16.5h5"/></svg>
          </a>
          <RouterLink to="/api-key" class="settings-link" :class="{ 'is-active': activeSection === 'settings' }" @click="closeMenu">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="8" cy="15" r="4"/><path d="m11 12 9-9 2 2-2 2 1 2-3 3-2-1-2 2"/></svg>
            <span>密钥设置</span>
          </RouterLink>
          <button type="button" class="mobile-menu-toggle" :aria-expanded="menuOpen" aria-controls="mobile-navigation" :aria-label="menuOpen ? '关闭导航菜单' : '打开导航菜单'" @click="menuOpen = !menuOpen">
            <svg v-if="menuOpen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M5 5 19 19M19 5 5 19"/></svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16"/></svg>
          </button>
        </div>
      </div>
      <Transition name="mobile-nav-transition">
        <nav v-if="menuOpen" id="mobile-navigation" class="mobile-nav" aria-label="移动端导航">
          <RouterLink to="/" :class="{ 'is-active': activeSection === 'home' }" @click="closeMenu">首页<span aria-hidden="true">→</span></RouterLink>
          <RouterLink to="/search" :class="{ 'is-active': activeSection === 'explore' }" @click="closeMenu">探索题库<span aria-hidden="true">→</span></RouterLink>
          <RouterLink to="/api-key" :class="{ 'is-active': activeSection === 'settings' }" @click="closeMenu">密钥设置<span aria-hidden="true">→</span></RouterLink>
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/backend" @click="closeMenu">服务器设置<span aria-hidden="true">→</span></a>
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/update" @click="closeMenu">检查更新<span aria-hidden="true">→</span></a>
          <form class="mobile-search" role="search" @submit.prevent="submitSearch">
            <label for="mobile-search-input">快速搜索知识点</label>
            <div class="mobile-search-row">
              <input id="mobile-search-input" v-model="searchKeyword" type="search" placeholder="输入关键词..." autocomplete="off" />
              <button type="submit">搜索</button>
            </div>
          </form>
        </nav>
      </Transition>
    </header>
    <button v-if="menuOpen" type="button" class="mobile-nav-backdrop" tabindex="-1" aria-label="关闭导航菜单" @click="closeMenu"></button>
    <main id="main-content" class="site-main" tabindex="-1">
      <RouterView v-if="isNativeAndroid" v-slot="{ Component, route }">
        <KeepAlive>
          <component :is="Component" :key="route.fullPath" />
        </KeepAlive>
      </RouterView>
      <RouterView v-else />
    </main>
  </div>
</template>