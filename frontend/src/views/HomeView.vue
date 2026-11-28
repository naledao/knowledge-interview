<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCategories, getTopics, type KnowledgeCategory, type KnowledgeTopic } from '../api/knowledge'
import { markdownToPlainText } from '../utils/plainText'
import { readRememberedPage, rememberPage } from '../utils/pageMemory'
import { rememberedSearchMode, rememberSearchMode, SEARCH_MODE_OPTIONS } from '../utils/searchModeMemory'
import SearchModeSelect from '../components/SearchModeSelect.vue'
import type { SearchMode } from '../api/knowledge'
import ListPagination from '../components/ListPagination.vue'
import { ArrowRight, ArrowUpRight, Clock3, Layers3, Search, Shuffle, Sparkles } from '@lucide/vue'
import SmartIcon from '../components/SmartIcon.vue'
import { useRandomQuestion } from '../composables/useRandomQuestion'

const router = useRouter()
const { loading: randomLoading, errorMessage: randomError, goRandom } = useRandomQuestion()
const keyword = ref('')
/** 首页搜索框的模式下拉：与顶部搜索框、搜索页共用同一份搜索模式（状态 A）。 */
const searchModeModel = computed<SearchMode>({
  get: () => rememberedSearchMode.value,
  set: (value) => rememberSearchMode(value),
})
const searchPlaceholder = computed(() => (rememberedSearchMode.value === 'exact' ? '输入问题 ID（如 42）' : '搜索知识点、面试题'))
const loading = ref(true)
const error = ref('')
const categories = ref<KnowledgeCategory[]>([])
const recentItems = ref<{ id: number; title: string; category: string }[]>([])
const categoryPage = ref(readRememberedPage('home:categories'))
const categoryPages = ref(0)

function timestamp(value: string | null): number {
  const parsed = value ? Date.parse(value) : NaN
  return Number.isNaN(parsed) ? 0 : parsed
}

/** 首页只提供搜索、分类和最近主题三个主要入口。 */
async function loadHome() {
  loading.value = true
  error.value = ''
  try {
    const [categoryData, topicData] = await Promise.all([getCategories(categoryPage.value), getTopics()])
    categoryPages.value = categoryData.pages
    if (categoryPage.value > Math.max(1, categoryData.pages)) {
      categoryPage.value = Math.max(1, categoryData.pages)
      rememberPage('home:categories', Math.max(1, categoryData.pages))
      void loadHome()
      return
    }
    categories.value = [...categoryData.items]
      .sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
      .map((category) => ({ ...category, description: markdownToPlainText(category.description) }))
    const names = new Map(categoryData.items.map((category) => [category.id, category.name]))
    recentItems.value = [...topicData.items]
      .sort((a, b) => timestamp(b.updatedAt || b.createdAt) - timestamp(a.updatedAt || a.createdAt) || b.id - a.id)
      .slice(0, 5)
      .map((topic: KnowledgeTopic) => ({
        id: topic.id,
        title: topic.title,
        category: names.get(topic.categoryId) || '',
      }))
  } catch (cause) {
    categories.value = []
    recentItems.value = []
    error.value = '加载失败，请重试'
    console.error('首页数据加载失败', cause)
  } finally {
    loading.value = false
  }
}

function changeCategoryPage(page: number) {
  if (page < 1 || page > categoryPages.value || page === categoryPage.value) return
  rememberPage('home:categories', page)
  categoryPage.value = page
  void loadHome()
}

function search() {
  const q = keyword.value.trim()
  // 带上当前记住的搜索模式；关键词模式沿用省略 mode 的写法。
  const query: Record<string, string> = {}
  if (q) query.q = q
  if (rememberedSearchMode.value !== 'keyword') query.mode = rememberedSearchMode.value
  void router.push({ name: 'search', query })
}

onMounted(loadHome)
</script>

<template>
  <div class="home-page">
    <section class="home-hero" aria-labelledby="home-title">
      <p class="home-kicker"><Sparkles :size="15" aria-hidden="true" />知识题库 · 学习空间</p>
      <h1 id="home-title">找到你要复习的知识点</h1>
      <p class="home-subtitle">直接搜索，按分类浏览，或随机来一题。</p>
      <form class="home-search" role="search" @submit.prevent="search">
        <Search class="home-search-icon" :size="20" aria-hidden="true" />
        <SearchModeSelect v-model="searchModeModel" :options="SEARCH_MODE_OPTIONS" variant="home" />
        <input v-model="keyword" type="search" :inputmode="rememberedSearchMode === 'exact' ? 'numeric' : 'search'" aria-label="搜索知识点" :placeholder="searchPlaceholder" autocomplete="off" />
        <button type="submit"><span><span class="home-search-verb">开始</span>搜索</span><ArrowRight class="home-search-arrow" :size="17" aria-hidden="true" /></button>
      </form>
      <div class="home-random">
        <button type="button" class="random-button" :disabled="randomLoading" @click="goRandom()">
          <Shuffle :size="17" aria-hidden="true" />{{ randomLoading ? '抽取中…' : '随机一题' }}
        </button>
        <span v-if="randomError" class="random-error" role="alert">{{ randomError }}</span>
      </div>
    </section>

    <section class="home-section" aria-labelledby="category-heading">
      <div class="home-section-heading">
        <h2 id="category-heading"><Layers3 :size="22" aria-hidden="true" />知识分类</h2>
      </div>
      <p v-if="loading" class="home-status" role="status">加载中…</p>
      <div v-else-if="error" class="home-status home-error" role="alert">{{ error }} <button type="button" @click="loadHome">重试</button></div>
      <p v-else-if="!categories.length" class="home-status">暂无分类</p>
      <div v-else class="category-grid">
        <RouterLink v-for="category in categories" :key="category.id" :to="`/categories/${category.id}`" class="category-card">
          <div class="category-card-top"><SmartIcon class="category-icon" :text="category.name" :size="22" /><ArrowUpRight class="category-arrow" :size="19" aria-hidden="true" /></div><h3>{{ category.name }}</h3>
          <p v-if="category.description">{{ category.description }}</p>
        </RouterLink>
      </div>
      <ListPagination :page-num="categoryPage" :pages="categoryPages" :loading="loading" @change="changeCategoryPage" />
    </section>

    <section class="home-section recent-section" aria-labelledby="recent-heading">
      <div class="home-section-heading">
        <h2 id="recent-heading"><Clock3 :size="22" aria-hidden="true" />最近更新</h2>
        <RouterLink to="/search" class="section-link">搜索更多 <ArrowRight :size="16" aria-hidden="true" /></RouterLink>
      </div>
      <p v-if="loading" class="home-status" role="status">加载中…</p>
      <p v-else-if="!recentItems.length" class="home-status">暂无内容</p>
      <div v-else class="recent-list">
        <RouterLink v-for="item in recentItems" :key="item.id" :to="`/topics/${item.id}`" class="recent-item">
          <SmartIcon class="recent-item-icon" :text="item.title" :secondary-text="item.category" :size="18" /><span class="recent-title">{{ item.title }}</span>
          <span v-if="item.category" class="recent-category">{{ item.category }}</span>
          <ArrowRight class="recent-arrow" :size="18" aria-hidden="true" />
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page { padding: 0; color: #172439; }
.home-page * { box-sizing: border-box; }
.home-page a { text-decoration: none; }
.home-hero { position: relative; overflow: hidden; padding: 24px 28px; border: 1px solid #e1e9f4; border-radius: var(--hero-radius); background: radial-gradient(circle at 93% 8%, #dbeafe 0, transparent 34%), linear-gradient(125deg, #eef5ff, #f8faff 67%, #f0efff); box-shadow: 0 12px 28px rgba(30, 64, 175, .05); }
.home-hero::after { content: ''; position: absolute; right: -72px; top: -76px; width: 170px; height: 170px; border: 24px solid rgba(99, 102, 241, .06); border-radius: 50%; pointer-events: none; }
.home-hero > * { position: relative; z-index: 1; }
.home-kicker { display: inline-flex; align-items: center; gap: 6px; margin: 0 0 6px; padding: 4px 10px; border: 1px solid #d4e2ff; border-radius: 99px; background: #ffffffa8; color: #3367b5; font-size: 12px; font-weight: 700; }
.home-hero h1 { margin: 0; font-size: clamp(22px, 3vw, 30px); line-height: 1.3; letter-spacing: -.03em; font-weight: 800; }
.home-subtitle { margin: 8px 0 16px; color: #677992; font-size: 14px; }
.home-search { display: flex; align-items: center; gap: 10px; width: min(100%, 690px); padding: 5px 5px 5px 14px; border: 1px solid #cedaeb; border-radius: 12px; background: #fff; box-shadow: 0 4px 16px rgba(30, 64, 175, .055); }
.home-search:focus-within { border-color: #417de2; box-shadow: 0 0 0 3px #2563eb18; }
.home-search svg { width: 18px; height: 18px; flex-shrink: 0; color: #8594aa; }
.home-search input { flex: 1; min-width: 0; border: 0; outline: 0; background: none; color: #172439; font: inherit; font-size: 14px; }
.home-search input::placeholder { color: #98a5b6; }
.home-search button { display: inline-flex; align-items: center; justify-content: center; gap: 6px; flex-shrink: 0; min-height: 36px; padding: 9px 18px; border: 0; border-radius: 8px; background: #2563eb; color: #fff; font: inherit; font-size: 14px; font-weight: 700; cursor: pointer; }
.home-search button:hover { background: #1d4ed8; }
.home-random { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-top: 10px; }
.random-button { display: inline-flex; align-items: center; justify-content: center; gap: 7px; min-height: 36px; padding: 7px 14px; border: 1px solid #c7d7f5; border-radius: 9px; background: #ffffffcc; color: #2563eb; font: inherit; font-size: 13px; font-weight: 700; cursor: pointer; transition: border-color .18s, background .18s; }
.random-button:hover:not(:disabled) { border-color: #93b4ee; background: #fff; }
.random-button:disabled { cursor: progress; opacity: .7; }
.random-error { color: #b91c1c; font-size: 13px; }
.home-section, .recent-section { margin-top: var(--section-gap); }
.home-section-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: var(--section-heading-gap); }
.home-section-heading h2 { display: inline-flex; align-items: center; gap: 9px; margin: 0; font-size: 19px; line-height: 1.35; font-weight: 750; }
.home-section-heading h2 svg { width: 19px; height: 19px; color: #477ce6; }
.section-link { display: inline-flex; align-items: center; justify-content: center; gap: 5px; color: #346ac2; font-size: 13px; white-space: nowrap; }
.section-link:hover { color: #1d4ed8; }
.category-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--grid-gap); }
.category-card { display: block; min-width: 0; min-height: 96px; padding: var(--card-padding-y) var(--card-padding-x); border: 1px solid #e4eaf2; border-radius: var(--card-radius); background: #fff; color: inherit; transition: transform .18s, border-color .18s, box-shadow .18s; }
.category-card:hover { transform: translateY(-2px); border-color: #9ab9ec; box-shadow: 0 10px 24px rgba(30, 64, 175, .075); }
.category-card-top { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 10px; }
.category-icon { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 10px; background: #edf4ff; color: #3b68cd; }
.category-arrow { color: #93a5bd; transition: transform .18s, color .18s; }
.category-card:hover .category-arrow, .recent-item:hover .recent-arrow { transform: translate(2px, -2px); color: #2563eb; }
.category-card h3 { margin: 0 0 5px; font-size: 16px; line-height: 1.35; overflow-wrap: anywhere; }
.category-card p { display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; margin: 0; color: #728197; font-size: 13px; line-height: 1.55; overflow-wrap: anywhere; }
.home-status { padding: 24px 16px; border: 1px solid #e4eaf2; border-radius: var(--card-radius); background: #fff; color: #64748b; text-align: center; font-size: 13px; }
.home-error button { margin-left: 10px; padding: 5px 12px; border: 1px solid #c7d8f3; border-radius: 7px; background: #fff; color: #2563eb; cursor: pointer; }
.recent-list { border: 1px solid #e4eaf2; border-radius: var(--card-radius); overflow: hidden; background: #fff; }
.recent-item { display: flex; align-items: center; gap: 12px; min-height: 46px; padding: 10px 14px; color: #26384e; }
.recent-item + .recent-item { border-top: 1px solid #eef1f6; }
.recent-item:hover { background: #f8fbff; }
.recent-item-icon { display: grid; flex: 0 0 30px; height: 30px; place-items: center; border-radius: 8px; background: #eef4ff; color: #5378c8; }
.recent-title { flex: 1; min-width: 0; font-size: 14px; font-weight: 650; overflow-wrap: anywhere; }
.recent-category { max-width: 32%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #8190a6; font-size: 12px; }
.recent-arrow { flex-shrink: 0; color: #8ca0b9; transition: transform .18s, color .18s; }
@media (max-width: 760px) {
  .home-hero { padding: 18px 16px; }
  .home-hero::after { display: none; }
  .category-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 520px) {
  .home-subtitle { margin: 6px 0 12px; font-size: 13px; }
  .home-search { gap: 6px; padding: 4px 4px 4px 10px; }
  .home-search .home-search-icon, .home-search .home-search-arrow, .home-search .home-search-verb { display: none; }
  .home-search input { padding-left: 4px; font-size: 13px; }
  .home-search button { gap: 4px; padding: 8px 14px; font-size: 13px; }
  .random-button { width: 100%; }
  .home-section-heading h2 { font-size: 18px; }
  .category-card { min-height: auto; padding: 12px; }
  .category-icon { width: 30px; height: 30px; border-radius: 9px; }
  .category-card h3 { font-size: 15px; }
  .category-card p { font-size: 12px; }
  .recent-item { gap: 10px; padding: 10px 12px; }
  .recent-item-icon { flex-basis: 28px; height: 28px; }
  .recent-title { font-size: 13px; }
  .recent-category { font-size: 11px; }
}
@media (max-width: 340px) {
  .category-grid { grid-template-columns: 1fr; }
}
@media (prefers-reduced-motion: reduce) {
  .category-card, .recent-arrow, .category-arrow { transition: none; }
  .category-card:hover { transform: none; }
}
</style>