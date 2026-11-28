<script setup lang='ts'>
import { Home, ChevronRight, Layers3, BookOpen, ArrowUpRight, CircleHelp, AlertCircle } from '@lucide/vue'
import axios from 'axios'
import { computed, ref, watch } from 'vue'
import { useCachedRoute } from '../router/useCachedRoute'
import { getCategory, getTopics, type KnowledgeCategory, type KnowledgeTopic } from '../api/knowledge'
import MarkdownContent from '../components/MarkdownContent.vue'
import ListPagination from '../components/ListPagination.vue'
import { readRememberedPage, rememberPage } from '../utils/pageMemory'
import { markdownToPlainText } from '../utils/plainText'

/** 分类详情页通过 ID 查询分类信息，并按页加载所属主题。 */
const route = useCachedRoute()
const category = ref<KnowledgeCategory | null>(null)
const topics = ref<KnowledgeTopic[]>([])
const topicPage = ref(1)
const topicPages = ref(0)
const topicTotal = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const notFound = ref(false)
let requestVersion = 0

const sortedTopics = computed(() =>
  [...topics.value].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id),
)

function parseCategoryId(value: unknown): number | null {
  if (typeof value !== 'string' || !/^[1-9][0-9]*$/.test(value)) return null
  const id = Number(value)
  return Number.isSafeInteger(id) ? id : null
}

/** 页码按分类维度记忆，整页刷新后可回到原分页。 */
function categoryPageKey(id: number): string {
  return `category:${id}`
}

/** 路由变更重置页码；翻页时只加载目标页，旧请求结果不覆盖新页面。 */
async function loadCategory(resetPage = false) {
  const version = ++requestVersion
  const categoryId = parseCategoryId(route.params.id)
  if (resetPage) topicPage.value = categoryId === null ? 1 : readRememberedPage(categoryPageKey(categoryId))
  category.value = null
  topics.value = []
  loading.value = true
  errorMessage.value = ''
  notFound.value = false
  if (categoryId === null) {
    notFound.value = true
    loading.value = false
    return
  }

  try {
    const [currentCategory, result] = await Promise.all([
      getCategory(categoryId), getTopics(categoryId, topicPage.value),
    ])
    if (version !== requestVersion) return
    category.value = currentCategory
    topicPages.value = result.pages
    topicTotal.value = result.total
    if (topicPage.value > Math.max(1, result.pages)) {
      topicPage.value = Math.max(1, result.pages)
      rememberPage(categoryPageKey(categoryId), Math.max(1, result.pages))
      void loadCategory()
      return
    }
    topics.value = result.items
  } catch (error) {
    if (version !== requestVersion) return
    if (axios.isAxiosError(error) && error.response?.status === 404) notFound.value = true
    else {
      errorMessage.value = '分类数据加载失败，请检查网络连接后重试。'
      console.error('分类详情加载失败', error)
    }
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

function changeTopicPage(page: number) {
  if (loading.value || page < 1 || page > topicPages.value || page === topicPage.value) return
  const categoryId = parseCategoryId(route.params.id)
  if (categoryId !== null) rememberPage(categoryPageKey(categoryId), page)
  topicPage.value = page
  void loadCategory()
}

watch(() => route.params.id, () => { void loadCategory(true) }, { immediate: true })
</script>

<template>
  <div class='category-page'>
    <nav class='breadcrumbs' aria-label='面包屑导航'>
      <RouterLink to='/'><Home :size='15' aria-hidden='true' />首页</RouterLink>
      <ChevronRight :size='15' aria-hidden='true' />
      <span aria-current='page'>{{ category?.name || '分类详情' }}</span>
    </nav>

    <section v-if='loading' class='state-panel' role='status'>
      <span class='loading-spinner' aria-hidden='true'></span>
      <h1>加载中…</h1>
    </section>

    <section v-else-if='errorMessage' class='state-panel' role='alert'>
      <div class='state-symbol' aria-hidden='true'><AlertCircle :size='30' /></div>
      <h1>加载失败</h1>
      <p>{{ errorMessage }}</p>
      <button type='button' class='primary-action' @click='loadCategory()'>重新加载</button>
    </section>

    <section v-else-if='notFound' class='state-panel'>
      <div class='state-symbol' aria-hidden='true'><CircleHelp :size='30' /></div>
      <h1>分类不存在</h1>
      <p>请返回首页查找其他分类。</p>
      <RouterLink to='/' class='primary-action'>返回首页</RouterLink>
    </section>

    <template v-else-if='category'>
      <header class='category-hero'>
        <div class='hero-content'>
          <div class='hero-title-row'>
            <span class='hero-icon' aria-hidden='true'><BookOpen :size='20' /></span>
            <h1>{{ category.name }}</h1>
          </div>
          <MarkdownContent v-if='category.description' class='category-description' size='compact' :source='category.description' />
          <div class='hero-summary'>
            <strong>{{ topicTotal }}</strong>
            <span>个学习主题</span>
          </div>
        </div>
      </header>

      <section class='topics-section' aria-labelledby='topics-heading'>
        <div class='section-heading'>
          <div>
            <h2 id='topics-heading'><Layers3 :size='23' aria-hidden='true' />学习主题</h2>
          </div>
        </div>

        <div v-if='topicTotal' class='topic-grid'>
          <RouterLink
            v-for='(topic, index) in sortedTopics'
            :key='topic.id'
            :to='`/topics/${topic.id}`'
            class='topic-card'
          >
            <div class='topic-card-top'>
              <span class='topic-index'>{{ String((topicPage - 1) * 6 + index + 1).padStart(2, '0') }}</span>
              <span class='card-arrow' aria-hidden='true'><ArrowUpRight :size='19' /></span>
            </div>
            <h3>{{ topic.title }}</h3>
            <p v-if='topic.description' class='topic-description'>{{ markdownToPlainText(topic.description) }}</p>
          </RouterLink>
        </div>

        <div v-else class='empty-state'>
          <div class='empty-symbol' aria-hidden='true'><BookOpen :size='30' /></div>
          <h3>暂无学习主题</h3>
          <RouterLink to='/' class='home-link'>浏览其他分类 →</RouterLink>
        </div>
        <ListPagination :page-num='topicPage' :pages='topicPages' :loading='loading' @change='changeTopicPage' />
      </section>
    </template>
  </div>
</template>

<style scoped>
.category-page { padding: 0; color: #0f172a; }
.breadcrumbs { display: flex; align-items: center; gap: 8px; margin-bottom: var(--breadcrumb-gap); font-size: 13px; color: #64748b; }
.breadcrumbs a { display: inline-flex; align-items: center; gap: 5px; color: #475569; text-decoration: none; }
.breadcrumbs a:hover { color: #2563eb; }
.breadcrumbs > svg { flex-shrink: 0; color: #94a3b8; }
.breadcrumbs span:last-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.category-hero { display: flex; justify-content: space-between; align-items: center; gap: 20px; padding: var(--hero-padding-y) var(--hero-padding-x); overflow: hidden; border: 1px solid #dbeafe; border-radius: var(--hero-radius); background: #f5f8fe; }
.hero-content { position: relative; z-index: 1; min-width: 0; }
.hero-title-row { display: flex; align-items: center; gap: 12px; }
.hero-icon { display: grid; flex: 0 0 36px; width: 36px; height: 36px; place-items: center; border: 1px solid #d2e2ff; border-radius: 10px; background: #fff; color: #3767cd; box-shadow: 0 4px 12px #315ed214; }
.category-hero h1 { margin: 0; font-size: clamp(22px, 2.8vw, 28px); font-weight: 800; line-height: 1.3; overflow-wrap: anywhere; }
.category-description { max-width: 680px; margin: 8px 0 14px; color: #475569; font-size: 14px; }
.hero-summary { display: inline-flex; align-items: baseline; gap: 6px; padding: 6px 12px; border: 1px solid #bfdbfe; border-radius: 999px; background: #fff; color: #475569; font-size: 13px; }
.hero-summary strong { color: #1d4ed8; font-size: 16px; }
.topics-section { padding-top: var(--section-gap); }
.section-heading { display: flex; justify-content: space-between; align-items: end; gap: 12px; margin-bottom: var(--section-heading-gap); }
.section-heading h2 { display: inline-flex; align-items: center; gap: 9px; margin: 0; font-size: 19px; }
.section-heading h2 > svg { width: 19px; height: 19px; color: #4774d4; flex-shrink: 0; }
.topic-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--grid-gap); }
.topic-card { display: flex; flex-direction: column; min-width: 0; padding: var(--card-padding-y) var(--card-padding-x); border: 1px solid #e2e8f0; border-radius: var(--card-radius); background: #fff; color: inherit; text-decoration: none; transition: border-color .18s, box-shadow .18s, transform .18s; }
.topic-card:hover { transform: translateY(-2px); border-color: #93c5fd; box-shadow: 0 10px 24px #1d4ed815; }
.topic-card:focus-visible, .breadcrumbs a:focus-visible, .home-link:focus-visible, .primary-action:focus-visible { outline: 3px solid #2563eb; outline-offset: 3px; }
.topic-card-top { display: flex; justify-content: space-between; align-items: center; }
.topic-index { color: #2563eb; font-size: 12px; font-weight: 800; letter-spacing: .05em; }
.card-arrow { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 8px; background: #f1f5f9; color: #64748b; transition: background .18s, color .18s; }
.topic-card:hover .card-arrow { background: #e7efff; color: #315ed2; }
.topic-card h3 { margin: 10px 0 6px; font-size: 16px; line-height: 1.4; overflow-wrap: anywhere; }
.topic-description { flex: 1; margin: 0; color: #64748b; font-size: 13px; line-height: 1.55; overflow-wrap: anywhere; display: -webkit-box; overflow: hidden; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.empty-state, .state-panel { padding: 36px 20px; border: 1px solid #e2e8f0; border-radius: 14px; background: #fff; text-align: center; }
.empty-symbol, .state-symbol { display: grid; place-items: center; width: 48px; height: 48px; margin: 0 auto 12px; border-radius: 14px; background: #eff6ff; color: #2563eb; }
.empty-state h3, .state-panel h1 { margin: 0 0 8px; font-size: 19px; }
.empty-state p, .state-panel p { margin: 0 auto 16px; color: #64748b; font-size: 14px; line-height: 1.65; }
.home-link { color: #2563eb; font-size: 14px; font-weight: 600; text-decoration: none; }
.primary-action { display: inline-block; padding: 9px 18px; border: 0; border-radius: 9px; background: #2563eb; color: #fff; font: inherit; font-size: 14px; font-weight: 600; text-decoration: none; cursor: pointer; }
.primary-action:hover { background: #1d4ed8; }
.loading-spinner { display: block; width: 34px; height: 34px; margin: 0 auto 14px; border: 3px solid #dbeafe; border-top-color: #2563eb; border-radius: 50%; animation: spin .9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 900px) {
  .topic-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 560px) {
  .section-heading h2 { font-size: 18px; }
  .topic-grid { grid-template-columns: 1fr; }
  .empty-state, .state-panel { padding: 28px 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .topic-card, .card-arrow { transition: none; }
  .topic-card:hover { transform: none; }
  .loading-spinner { animation: none; }
}
</style>