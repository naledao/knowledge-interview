<script setup lang="ts">
import { Home, ChevronRight, BookOpenText, ArrowUpRight, CircleHelp, AlertCircle } from '@lucide/vue'
import axios from 'axios'
import { computed, ref, watch } from 'vue'
import { useCachedRoute } from '../router/useCachedRoute'
import { getCategory, getTopic, getQuestions, type KnowledgeCategory, type KnowledgeTopic, type KnowledgeQuestion } from '../api/knowledge'
import MarkdownContent from '../components/MarkdownContent.vue'
import ListPagination from '../components/ListPagination.vue'
import { readRememberedPage, rememberPage } from '../utils/pageMemory'

/** 主题详情按 ID 查询，问题列表通过数据库分页获取。 */
const route = useCachedRoute()
const topic = ref<KnowledgeTopic | null>(null)
const category = ref<KnowledgeCategory | null>(null)
const questions = ref<KnowledgeQuestion[]>([])
const questionPage = ref(1)
const questionPages = ref(0)
const questionTotal = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const notFound = ref(false)
let requestVersion = 0

const sortedQuestions = computed(() =>
  [...questions.value].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id),
)

function parseTopicId(value: unknown): number | null {
  if (typeof value !== 'string' || !/^[1-9][0-9]*$/.test(value)) return null
  const id = Number(value)
  return Number.isSafeInteger(id) ? id : null
}

/** 页码按主题维度记忆，整页刷新后可回到原分页。 */
function topicPageKey(id: number): string {
  return `topic:${id}`
}

/** 只加载当前问题页，并忽略切换路由或翻页后过期的请求。 */
async function loadTopic(resetPage = false) {
  const version = ++requestVersion
  const topicId = parseTopicId(route.params.id)
  if (resetPage) questionPage.value = topicId === null ? 1 : readRememberedPage(topicPageKey(topicId))
  topic.value = null
  category.value = null
  questions.value = []
  loading.value = true
  errorMessage.value = ''
  notFound.value = false
  if (topicId === null) {
    notFound.value = true
    loading.value = false
    return
  }
  try {
    const current = await getTopic(topicId)
    if (version !== requestVersion) return
    const [currentCategory, result] = await Promise.all([
      getCategory(current.categoryId), getQuestions(topicId, questionPage.value),
    ])
    if (version !== requestVersion) return
    topic.value = current
    category.value = currentCategory
    questionPages.value = result.pages
    questionTotal.value = result.total
    if (questionPage.value > Math.max(1, result.pages)) {
      questionPage.value = Math.max(1, result.pages)
      rememberPage(topicPageKey(topicId), Math.max(1, result.pages))
      void loadTopic()
      return
    }
    questions.value = result.items
  } catch (error) {
    if (version !== requestVersion) return
    if (axios.isAxiosError(error) && error.response?.status === 404) notFound.value = true
    else {
      errorMessage.value = '主题数据加载失败，请检查网络连接后重试。'
      console.error('主题详情加载失败', error)
    }
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

function changeQuestionPage(page: number) {
  if (loading.value || page < 1 || page > questionPages.value || page === questionPage.value) return
  const topicId = parseTopicId(route.params.id)
  if (topicId !== null) rememberPage(topicPageKey(topicId), page)
  questionPage.value = page
  void loadTopic()
}

watch(() => route.params.id, () => { void loadTopic(true) }, { immediate: true })
</script>

<template>
  <div class="topic-page">
    <nav class="breadcrumbs" aria-label="面包屑导航">
      <RouterLink to="/"><Home :size="15" aria-hidden="true" />首页</RouterLink>
      <ChevronRight :size="15" aria-hidden="true" />
      <RouterLink v-if="topic" :to="`/categories/${topic.categoryId}`">{{ category?.name || '所属分类' }}</RouterLink>
      <span v-else>所属分类</span>
      <ChevronRight :size="15" aria-hidden="true" />
      <span class="current-crumb" aria-current="page">{{ topic?.title || '主题详情' }}</span>
    </nav>

    <section v-if="loading" class="state-panel" role="status">
      <span class="loading-spinner" aria-hidden="true"></span>
      <h1>加载中…</h1>
    </section>

    <section v-else-if="errorMessage" class="state-panel" role="alert">
      <div class="state-symbol" aria-hidden="true"><AlertCircle :size="30" /></div>
      <h1>加载失败</h1>
      <p>{{ errorMessage }}</p>
      <button class="primary-action" type="button" @click="loadTopic()">重新加载</button>
    </section>

    <section v-else-if="notFound" class="state-panel">
      <div class="state-symbol" aria-hidden="true"><CircleHelp :size="30" /></div>
      <h1>主题不存在</h1>
      <p>请返回首页查找其他主题。</p>
      <RouterLink class="primary-action" to="/">返回首页</RouterLink>
    </section>

    <template v-else-if="topic">
      <header class="topic-hero">
        <div class="hero-main">
          <h1>{{ topic.title }}</h1>
          <MarkdownContent v-if="topic.description" class="hero-description" size="compact" :source="topic.description" />
          <div class="hero-meta">
            <span class="meta-pill"><strong>{{ questionTotal }}</strong> 个相关问题</span>
            <RouterLink class="category-link" :to="`/categories/${topic.categoryId}`">返回分类 <ArrowUpRight :size="17" aria-hidden="true" /></RouterLink>
          </div>
        </div>
      </header>

      <section class="questions-section" aria-labelledby="questions-heading">
        <div class="section-heading">
          <div>
            <h2 id="questions-heading"><BookOpenText :size="23" aria-hidden="true" />知识问题</h2>
          </div>
        </div>

        <ol v-if="sortedQuestions.length" class="question-list">
          <li v-for="(question, index) in sortedQuestions" :key="question.id">
            <RouterLink class="question-card" :to="`/questions/${question.id}`">
              <span class="question-number">{{ String((questionPage - 1) * 6 + index + 1).padStart(2, '0') }}</span>
              <span class="question-main">
                <strong>{{ question.questionText }}</strong>
              </span>
              <span class="question-arrow" aria-hidden="true"><ArrowUpRight :size="21" /></span>
            </RouterLink>
          </li>
        </ol>

        <div v-else class="empty-state">
          <div class="empty-symbol" aria-hidden="true"><BookOpenText :size="30" /></div>
          <h3>暂无问题</h3>
          <RouterLink class="return-link" :to="`/categories/${topic.categoryId}`">浏览其他主题 →</RouterLink>
        </div>
        <ListPagination :page-num='questionPage' :pages='questionPages' :loading='loading' @change='changeQuestionPage' />
      </section>
    </template>
  </div>
</template>

<style scoped>
.topic-page { padding: 0; color: #0f172a; }
.breadcrumbs { display: flex; align-items: center; gap: 8px; margin-bottom: var(--breadcrumb-gap); font-size: 13px; color: #64748b; }
.breadcrumbs a { display: inline-flex; align-items: center; gap: 5px; color: #475569; text-decoration: none; }
.breadcrumbs a:hover { color: #2563eb; }
.breadcrumbs > svg { flex-shrink: 0; color: #94a3b8; }
.current-crumb { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.topic-hero { display: flex; justify-content: space-between; align-items: center; gap: 20px; overflow: hidden; padding: var(--hero-padding-y) var(--hero-padding-x); border: 1px solid #ddd6fe; border-radius: var(--hero-radius); background: #f7f8fd; }
.hero-main { position: relative; z-index: 1; min-width: 0; }
.topic-hero h1 { margin: 0; font-size: clamp(22px, 2.8vw, 28px); line-height: 1.3; overflow-wrap: anywhere; }
.hero-description { max-width: 680px; margin: 8px 0 12px; color: #475569; font-size: 14px; }
.hero-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.meta-pill { display: inline-flex; gap: 6px; align-items: baseline; padding: 6px 12px; border: 1px solid #ddd6fe; border-radius: 99px; background: #fff; font-size: 13px; color: #475569; }
.meta-pill strong { color: #6d28d9; font-size: 16px; }
.category-link { display: inline-flex; align-items: center; gap: 5px; color: #6d28d9; font-size: 13px; font-weight: 600; }
.questions-section { padding-top: var(--section-gap); }
.section-heading { display: flex; justify-content: space-between; align-items: end; gap: 12px; margin-bottom: var(--section-heading-gap); }
.section-heading h2 { display: inline-flex; align-items: center; gap: 9px; margin: 0; font-size: 19px; }
.section-heading h2 > svg { width: 19px; height: 19px; color: #7955cb; flex-shrink: 0; }
.question-list { display: grid; grid-template-columns: 1fr; gap: 8px; margin: 0; padding: 0; list-style: none; }
.question-card { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border: 1px solid #e2e8f0; border-radius: var(--card-radius); background: #fff; color: inherit; text-decoration: none; transition: transform .18s, border-color .18s, box-shadow .18s; }
.question-card:hover { transform: translateY(-2px); border-color: #c4b5fd; box-shadow: 0 10px 24px #6d28d914; }
.question-card:focus-visible, .breadcrumbs a:focus-visible, .primary-action:focus-visible, .return-link:focus-visible { outline: 3px solid #7c3aed; outline-offset: 3px; }
.question-number { display: grid; place-items: center; flex: 0 0 36px; height: 36px; border-radius: 9px; background: #f5f3ff; color: #7c3aed; font-size: 13px; font-weight: 800; }
.question-main { display: flex; flex: 1; flex-direction: column; gap: 4px; min-width: 0; }
.question-main strong { line-height: 1.45; font-size: 15px; overflow-wrap: anywhere; white-space: pre-line; }
.question-arrow { display: grid; place-items: center; flex-shrink: 0; width: 28px; height: 28px; border-radius: 8px; background: #f2effd; color: #7c3aed; }
.question-arrow svg { width: 17px; height: 17px; }
.state-panel, .empty-state { padding: 36px 20px; border: 1px solid #e2e8f0; border-radius: 14px; background: #fff; text-align: center; }
.state-panel h1, .empty-state h3 { margin: 0 0 8px; font-size: 19px; }
.state-panel p, .empty-state p { margin: 0 auto 16px; color: #64748b; font-size: 14px; line-height: 1.65; }
.state-symbol, .empty-symbol { display: grid; place-items: center; width: 48px; height: 48px; margin: 0 auto 12px; border-radius: 14px; background: #f5f3ff; color: #7c3aed; }
.primary-action { display: inline-block; padding: 9px 18px; border: 0; border-radius: 9px; background: #7c3aed; color: #fff; font: inherit; font-size: 14px; font-weight: 600; text-decoration: none; cursor: pointer; }
.primary-action:hover { background: #6d28d9; }
.return-link { color: #6d28d9; font-size: 14px; font-weight: 600; }
.loading-spinner { display: block; width: 34px; height: 34px; margin: 0 auto 14px; border: 3px solid #ede9fe; border-top-color: #7c3aed; border-radius: 50%; animation: spin .9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 640px) {
  .section-heading h2 { font-size: 18px; }
  .question-card { padding: 11px 12px; gap: 10px; }
  .question-number { flex-basis: 32px; height: 32px; font-size: 12px; }
  .question-main strong { font-size: 14px; }
  .state-panel, .empty-state { padding: 28px 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .question-card { transition: none; }
  .question-card:hover { transform: none; }
  .loading-spinner { animation: none; }
}
</style>