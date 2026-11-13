<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCategories, getTopics, type KnowledgeCategory, type KnowledgeTopic } from '../api/knowledge'
import { markdownToPlainText } from '../utils/plainText'
import ListPagination from '../components/ListPagination.vue'

const router = useRouter()
const keyword = ref('')
const loading = ref(true)
const error = ref('')
const categories = ref<KnowledgeCategory[]>([])
const recentItems = ref<{ id: number; title: string; category: string }[]>([])
const categoryPage = ref(1)
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
  categoryPage.value = page
  void loadHome()
}

function search() {
  const q = keyword.value.trim()
  void router.push({ name: 'search', query: q ? { q } : {} })
}

onMounted(loadHome)
</script>

<template>
  <div class="home-page">
    <section class="home-hero" aria-labelledby="home-title">
      <p class="home-kicker">知识题库</p>
      <h1 id="home-title">找到你要复习的知识点</h1>
      <p class="home-subtitle">直接搜索，或按分类浏览。</p>
      <form class="home-search" role="search" @submit.prevent="search">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m16 16 4 4"/></svg>
        <input v-model="keyword" type="search" aria-label="搜索知识点" placeholder="搜索知识点、面试题" autocomplete="off" />
        <button type="submit">搜索</button>
      </form>
    </section>

    <section class="home-section" aria-labelledby="category-heading">
      <div class="home-section-heading">
        <h2 id="category-heading">知识分类</h2>
      </div>
      <p v-if="loading" class="home-status" role="status">加载中…</p>
      <div v-else-if="error" class="home-status home-error" role="alert">{{ error }} <button type="button" @click="loadHome">重试</button></div>
      <p v-else-if="!categories.length" class="home-status">暂无分类</p>
      <div v-else class="category-grid">
        <RouterLink v-for="category in categories" :key="category.id" :to="`/categories/${category.id}`" class="category-card">
          <div class="category-card-top"><h3>{{ category.name }}</h3><span class="category-arrow" aria-hidden="true">→</span></div>
          <p v-if="category.description">{{ category.description }}</p>
        </RouterLink>
      </div>
      <ListPagination :page-num="categoryPage" :pages="categoryPages" :loading="loading" @change="changeCategoryPage" />
    </section>

    <section class="home-section recent-section" aria-labelledby="recent-heading">
      <div class="home-section-heading">
        <h2 id="recent-heading">最近更新</h2>
        <RouterLink to="/search" class="section-link">搜索更多 <span aria-hidden="true">→</span></RouterLink>
      </div>
      <p v-if="loading" class="home-status" role="status">加载中…</p>
      <p v-else-if="!recentItems.length" class="home-status">暂无内容</p>
      <div v-else class="recent-list">
        <RouterLink v-for="item in recentItems" :key="item.id" :to="`/topics/${item.id}`" class="recent-item">
          <span class="recent-title">{{ item.title }}</span>
          <span v-if="item.category" class="recent-category">{{ item.category }}</span>
          <span class="recent-arrow" aria-hidden="true">→</span>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page { max-width: 1120px; margin: 0 auto; padding: 16px 4px 76px; color: #172439; }
.home-page * { box-sizing: border-box; }
.home-page a { text-decoration: none; }
.home-hero { padding: 44px 48px 48px; border: 1px solid #e1e9f4; border-radius: 22px; background: #f4f8ff; }
.home-kicker { margin: 0 0 12px; color: #3367b5; font-size: 12px; font-weight: 700; }
.home-hero h1 { margin: 0; font-size: clamp(27px, 3.6vw, 39px); line-height: 1.35; letter-spacing: -.03em; font-weight: 800; }
.home-subtitle { margin: 12px 0 26px; color: #677992; font-size: 14px; }
.home-search { display: flex; align-items: center; gap: 14px; width: min(100%, 690px); padding: 7px 7px 7px 18px; border: 1px solid #cedaeb; border-radius: 12px; background: #fff; }
.home-search:focus-within { border-color: #417de2; box-shadow: 0 0 0 3px #2563eb18; }
.home-search svg { width: 20px; height: 20px; flex-shrink: 0; color: #8594aa; }
.home-search input { flex: 1; min-width: 0; border: 0; outline: 0; background: none; color: #172439; font: inherit; font-size: 14px; }
.home-search input::placeholder { color: #98a5b6; }
.home-search button { flex-shrink: 0; padding: 12px 24px; border: 0; border-radius: 8px; background: #2563eb; color: #fff; font: inherit; font-weight: 700; cursor: pointer; }
.home-search button:hover { background: #1d4ed8; }
.home-section { margin-top: 47px; }
.home-section-heading { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 20px; }
.home-section-heading h2 { margin: 0; font-size: 22px; line-height: 1.35; font-weight: 750; }
.section-link { color: #346ac2; font-size: 13px; white-space: nowrap; }
.section-link:hover { color: #1d4ed8; }
.category-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.category-card { display: block; min-width: 0; min-height: 110px; padding: 22px; border: 1px solid #e4eaf2; border-radius: 14px; background: #fff; color: inherit; transition: border-color .15s, box-shadow .15s; }
.category-card:hover { border-color: #9ab9ec; box-shadow: 0 6px 20px #28436c0b; }
.category-card-top { display: flex; align-items: start; justify-content: space-between; gap: 12px; }
.category-arrow { color: #9ba9bc; font-size: 18px; }
.category-card h3 { margin: 0 0 8px; font-size: 17px; line-height: 1.35; overflow-wrap: anywhere; }
.category-card p { display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; margin: 0; color: #728197; font-size: 13px; line-height: 1.65; overflow-wrap: anywhere; }
.home-status { padding: 28px 16px; border: 1px solid #e4eaf2; border-radius: 12px; background: #fff; color: #64748b; text-align: center; font-size: 13px; }
.home-error button { margin-left: 10px; padding: 5px 12px; border: 1px solid #c7d8f3; border-radius: 7px; background: #fff; color: #2563eb; cursor: pointer; }
.recent-section { margin-top: 48px; }
.recent-list { border: 1px solid #e4eaf2; border-radius: 14px; overflow: hidden; background: #fff; }
.recent-item { display: flex; align-items: center; gap: 16px; min-height: 65px; padding: 16px 22px; color: #26384e; }
.recent-item + .recent-item { border-top: 1px solid #eef1f6; }
.recent-item:hover { background: #f8fbff; }
.recent-title { flex: 1; min-width: 0; font-size: 14px; font-weight: 650; overflow-wrap: anywhere; }
.recent-category { max-width: 32%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #8190a6; font-size: 12px; }
.recent-arrow { color: #8ca0b9; font-size: 17px; }
@media (max-width: 780px) { .category-grid { grid-template-columns: repeat(2, minmax(0,1fr)); } .home-hero { padding: 34px; } }
@media (max-width: 520px) { .home-page { padding: 0 0 46px; } .home-hero { padding: 27px 20px; border-radius: 16px; } .home-hero h1 { font-size: 27px; } .home-subtitle { margin-bottom: 20px; font-size: 13px; } .home-search { gap: 8px; padding-left: 12px; } .home-search input { font-size: 13px; } .home-search button { padding: 11px 16px; } .home-section { margin-top: 32px; } .home-section-heading h2 { font-size: 20px; } .category-grid { gap: 10px; } .category-card { min-height: 112px; padding: 17px; } .category-card h3 { font-size: 15px; } .category-card p { font-size: 12px; } .recent-item { gap: 10px; padding: 15px 14px; } .recent-title { font-size: 13px; } .recent-category { font-size: 11px; } }
@media (max-width: 340px) { .category-grid { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) { .category-card { transition: none; } }
</style>