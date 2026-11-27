<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter, type LocationQueryRaw } from 'vue-router'
import { rememberedSearchMode, rememberSearchMode, SEARCH_MODE_OPTIONS } from './utils/searchModeMemory'
import SearchModeSelect from './components/SearchModeSelect.vue'
import type { SearchMode } from './api/knowledge'
import { BookOpenText, House, Compass, Search, KeyRound, Menu, X, RefreshCw, ServerCog, ChevronRight, Bot } from '@lucide/vue'

/** 全站导航：PC 展示横向导航与快捷搜索，移动端使用可收起菜单。 */
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const searchKeyword = ref('')
/** 当前搜索模式（状态 A），与搜索页共用；顶部搜索框据此搜索并展示模式。 */
const searchMode = rememberedSearchMode
const searchPlaceholder = computed(() => (searchMode.value === 'exact' ? '输入问题 ID' : '搜索题库'))
/** 顶部搜索框的模式下拉选项；选择后写入共用的状态 A，搜索页会同步。 */
const searchModeModel = computed<SearchMode>({
  get: () => searchMode.value,
  set: (value) => rememberSearchMode(value),
})
/** 仅内置安卓 WebView 展示服务器配置入口。 */
const isNativeAndroid = navigator.userAgent.includes('KnowledgeInterviewAndroid/')

const pageRefreshing = ref(false)

/** 移动端 WebView 没有浏览器刷新入口，点击按钮直接重新加载当前页面。 */
function refreshPage() {
  if (pageRefreshing.value) return
  pageRefreshing.value = true
  closeMenu()
  window.location.reload()
}

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
  // 带上当前记住的搜索模式（状态 A）；关键词模式沿用省略 mode 的写法。
  const query: LocationQueryRaw = {}
  if (keyword) query.q = keyword
  if (searchMode.value !== 'keyword') query.mode = searchMode.value
  void router.push({ name: 'search', query })
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
            <BookOpenText :size="23" :stroke-width="1.9" />
          </span>
          <span class="brand-name">知识<span>题库</span></span>
        </RouterLink>

        <nav class="desktop-nav" aria-label="主导航">
          <RouterLink to="/" :class="{ 'is-active': activeSection === 'home' }"><House :size="17" />首页</RouterLink>
          <RouterLink to="/search" :class="{ 'is-active': activeSection === 'explore' }"><Compass :size="17" />探索</RouterLink>
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/ai" @click="closeMenu"><Bot :size="17" />AI 助手</a>
        </nav>

        <form class="header-search" role="search" @submit.prevent="submitSearch">
          <label for="header-search-input" class="visually-hidden">搜索知识点</label>
          <Search class="search-icon" :size="18" aria-hidden="true" />
          <SearchModeSelect v-model="searchModeModel" :options="SEARCH_MODE_OPTIONS" variant="header" />
          <input id="header-search-input" v-model="searchKeyword" type="search" :placeholder="searchPlaceholder" :inputmode="searchMode === 'exact' ? 'numeric' : 'search'" autocomplete="off" />
          <button type="submit" aria-label="搜索知识">搜索</button>
        </form>

        <div class="header-actions">
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/ai" class="mobile-menu-toggle native-ai-shortcut" aria-label="AI 助手" title="AI 助手" @click="closeMenu"><Bot :size="22" aria-hidden="true" /></a>
          <button type="button" class="page-refresh-button" :class="{ 'is-refreshing': pageRefreshing }" aria-label="刷新当前页面" title="刷新当前页面" @click="refreshPage">
            <RefreshCw :size="19" aria-hidden="true" />
          </button>
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/update" class="native-server-link" aria-label="检查更新" title="检查更新" @click="closeMenu">
            <RefreshCw :size="19" aria-hidden="true" />
          </a>
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/backend" class="native-server-link" aria-label="服务器设置" title="服务器设置" @click="closeMenu">
            <ServerCog :size="19" aria-hidden="true" />
          </a>
          <RouterLink to="/api-key" class="settings-link" :class="{ 'is-active': activeSection === 'settings' }" @click="closeMenu">
            <KeyRound :size="17" aria-hidden="true" />
            <span>设置</span>
          </RouterLink>
          <button type="button" class="mobile-menu-toggle" :aria-expanded="menuOpen" aria-controls="mobile-navigation" :aria-label="menuOpen ? '关闭导航菜单' : '打开导航菜单'" @click="menuOpen = !menuOpen">
            <X v-if="menuOpen" :size="22" aria-hidden="true" />
            <Menu v-else :size="22" aria-hidden="true" />
          </button>
        </div>
      </div>
      <Transition name="mobile-nav-transition">
        <nav v-if="menuOpen" id="mobile-navigation" class="mobile-nav" aria-label="移动端导航">
          <RouterLink to="/" :class="{ 'is-active': activeSection === 'home' }" @click="closeMenu"><span class="mobile-nav-label"><House :size="18" />首页</span><ChevronRight :size="17" aria-hidden="true" /></RouterLink>
          <RouterLink to="/search" :class="{ 'is-active': activeSection === 'explore' }" @click="closeMenu"><span class="mobile-nav-label"><Compass :size="18" />探索</span><ChevronRight :size="17" aria-hidden="true" /></RouterLink>
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/ai" @click="closeMenu"><span class="mobile-nav-label"><Bot :size="18" />AI 助手</span><ChevronRight :size="17" aria-hidden="true" /></a>
          <RouterLink to="/api-key" :class="{ 'is-active': activeSection === 'settings' }" @click="closeMenu"><span class="mobile-nav-label"><KeyRound :size="18" />设置</span><ChevronRight :size="17" aria-hidden="true" /></RouterLink>
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/backend" @click="closeMenu">服务器设置<span aria-hidden="true">→</span></a>
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/proxy" @click="closeMenu">代理设置<span aria-hidden="true">→</span></a>
          <a v-if="isNativeAndroid" href="knowledgeinterview://settings/update" @click="closeMenu">检查更新<span aria-hidden="true">→</span></a>
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