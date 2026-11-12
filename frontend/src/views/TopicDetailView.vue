<script setup lang="ts">
import axios from 'axios'
import { computed, ref, watch } from 'vue'
import { useCachedRoute } from '../router/useCachedRoute'
import { getCategory, getTopic, getQuestions, type KnowledgeCategory, type KnowledgeTopic, type KnowledgeQuestion } from '../api/knowledge'
import MarkdownContent from '../components/MarkdownContent.vue'
import ListPagination from '../components/ListPagination.vue'

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

/** 只加载当前问题页，并忽略切换路由或翻页后过期的请求。 */
async function loadTopic(resetPage = false) {
  const version = ++requestVersion
  if (resetPage) questionPage.value = 1
  topic.value = null
  category.value = null
  questions.value = []
  loading.value = true
  errorMessage.value = ''
  notFound.value = false
  const topicId = parseTopicId(route.params.id)
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
    questions.value = result.items
    questionPages.value = result.pages
    questionTotal.value = result.total
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
  questionPage.value = page
  void loadTopic()
}

watch(() => route.params.id, () => { void loadTopic(true) }, { immediate: true })
</script>

<template>
  <div class="topic-page">
    <nav class="breadcrumbs" aria-label="面包屑导航">
      <RouterLink to="/">首页</RouterLink>
      <span aria-hidden="true">/</span>
      <RouterLink v-if="topic" :to="`/categories/${topic.categoryId}`">{{ category?.name || '所属分类' }}</RouterLink>
      <span v-else>所属分类</span>
      <span aria-hidden="true">/</span>
      <span class="current-crumb" aria-current="page">{{ topic?.title || '主题详情' }}</span>
    </nav>

    <section v-if="loading" class="state-panel" role="status">
      <span class="loading-spinner" aria-hidden="true"></span>
      <h1>正在加载主题...</h1>
      <p>正在获取主题介绍及相关问题。</p>
    </section>

    <section v-else-if="errorMessage" class="state-panel" role="alert">
      <div class="state-symbol" aria-hidden="true">!</div>
      <h1>加载失败</h1>
      <p>{{ errorMessage }}</p>
      <button class="primary-action" type="button" @click="loadTopic()">重新加载</button>
    </section>

    <section v-else-if="notFound" class="state-panel">
      <div class="state-symbol" aria-hidden="true">?</div>
      <h1>主题不存在</h1>
      <p>主题可能已被删除，或当前链接的主题 ID 无效。</p>
      <RouterLink class="primary-action" to="/">返回首页</RouterLink>
    </section>

    <template v-else-if="topic">
      <header class="topic-hero">
        <div class="hero-main">
          <p class="eyebrow">KNOWLEDGE TOPIC</p>
          <h1>{{ topic.title }}</h1>
          <MarkdownContent class="hero-description" size="compact" :source="topic.description || '围绕本主题系统学习相关问题，逐步巩固知识。'" />
          <div class="hero-meta">
            <span class="meta-pill"><strong>{{ questionTotal }}</strong> 个相关问题</span>
            <RouterLink class="category-link" :to="`/categories/${topic.categoryId}`">返回分类 <span aria-hidden="true">↗</span></RouterLink>
          </div>
        </div>
        <div class="hero-decoration" aria-hidden="true"><span>Q<span class="hero-amp">&</span>A</span></div>
      </header>

      <section class="questions-section" aria-labelledby="questions-heading">
        <div class="section-heading">
          <div>
            <p class="eyebrow">QUESTION LIBRARY</p>
            <h2 id="questions-heading">知识问题</h2>
            <p class="section-description">选择问题，查看对应的知识内容与答案。</p>
          </div>
          <span class="question-count">共 {{ questionTotal }} 题</span>
        </div>

        <ol v-if="sortedQuestions.length" class="question-list">
          <li v-for="(question, index) in sortedQuestions" :key="question.id">
            <RouterLink class="question-card" :to="`/questions/${question.id}`">
              <span class="question-number">{{ String((questionPage - 1) * 6 + index + 1).padStart(2, '0') }}</span>
              <span class="question-main">
                <strong>{{ question.questionText }}</strong>
                <span class="question-hint">查看问题详情</span>
              </span>
              <span class="question-arrow" aria-hidden="true">↗</span>
            </RouterLink>
          </li>
        </ol>

        <div v-else class="empty-state">
          <div class="empty-symbol" aria-hidden="true">◇</div>
          <h3>暂无问题</h3>
          <p>当前主题尚未收录问题，可以返回分类浏览其他主题。</p>
          <RouterLink class="return-link" :to="`/categories/${topic.categoryId}`">浏览其他主题 →</RouterLink>
        </div>
        <ListPagination :page-num='questionPage' :pages='questionPages' :loading='loading' @change='changeQuestionPage' />
      </section>
    </template>
  </div>
</template>

<style scoped>
.topic-page { max-width: 1180px; margin: 0 auto; padding: 32px 24px 88px; color: #0f172a; }
.breadcrumbs { display: flex; align-items: center; gap: 12px; margin-bottom: 28px; font-size: 14px; color: #64748b; }
.breadcrumbs a { color: #475569; text-decoration: none; }
.breadcrumbs a:hover { color: #2563eb; }
.current-crumb { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.topic-hero { display: flex; justify-content: space-between; align-items: center; gap: 36px; overflow: hidden; padding: 50px 52px; border: 1px solid #ddd6fe; border-radius: 24px; background: linear-gradient(115deg, #f5f3ff, #fafaff 65%, #eef2ff); }
.hero-main { position: relative; z-index: 1; min-width: 0; }
.eyebrow { margin: 0 0 14px; font-size: 12px; font-weight: 800; color: #6d28d9; letter-spacing: .14em; }
.topic-hero h1 { margin: 0; font-size: clamp(29px, 4vw, 43px); line-height: 1.3; overflow-wrap: anywhere; }
.hero-description { max-width: 680px; margin: 21px 0 28px; color: #475569; }
.hero-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 18px; }
.meta-pill { display: inline-flex; gap: 7px; align-items: baseline; padding: 9px 16px; border: 1px solid #ddd6fe; border-radius: 99px; background: white; font-size: 13px; color: #475569; }
.meta-pill strong { color: #6d28d9; font-size: 20px; }
.category-link { color: #6d28d9; font-size: 14px; font-weight: 600; }
.hero-decoration { flex: 0 0 176px; display: grid; place-items: center; width: 176px; height: 176px; border: 20px solid #ede9fe; border-radius: 50%; color: #7c3aed; font-size: 49px; font-weight: 900; transform: rotate(-8deg); }
.hero-amp { color: #a78bfa; }
.questions-section { padding-top: 54px; }
.section-heading { display: flex; justify-content: space-between; align-items: end; gap: 18px; margin-bottom: 26px; }
.section-heading h2 { margin: 0; font-size: clamp(24px, 3vw, 31px); }
.section-description { margin: 12px 0 0; color: #64748b; line-height: 1.7; }
.question-count { flex-shrink: 0; padding: 8px 13px; border: 1px solid #e2e8f0; border-radius: 99px; color: #475569; font-size: 13px; }
.question-list { display: grid; grid-template-columns: 1fr; gap: 14px; margin: 0; padding: 0; list-style: none; }
.question-card { display: flex; align-items: center; gap: 21px; min-height: 106px; padding: 22px 26px; border: 1px solid #e2e8f0; border-radius: 17px; background: #fff; color: inherit; text-decoration: none; transition: transform .2s, border-color .2s, box-shadow .2s; }
.question-card:hover { transform: translateY(-2px); border-color: #c4b5fd; box-shadow: 0 12px 27px #0f172a0d; }
.question-card:focus-visible, .breadcrumbs a:focus-visible, .primary-action:focus-visible, .return-link:focus-visible { outline: 3px solid #7c3aed; outline-offset: 3px; }
.question-number { display: grid; place-items: center; flex: 0 0 52px; height: 52px; border-radius: 13px; background: #f5f3ff; color: #7c3aed; font-size: 16px; font-weight: 800; }
.question-main { display: flex; flex: 1; flex-direction: column; gap: 10px; min-width: 0; }
.question-main strong { line-height: 1.55; font-size: 17px; overflow-wrap: anywhere; white-space: pre-line; }
.question-hint { color: #64748b; font-size: 13px; }
.question-arrow { color: #7c3aed; font-size: 22px; }
.state-panel, .empty-state { padding: 75px 24px; border: 1px solid #e2e8f0; border-radius: 20px; background: #fff; text-align: center; }
.state-panel h1, .empty-state h3 { margin: 0 0 10px; font-size: 23px; }
.state-panel p, .empty-state p { margin: 0 auto 24px; color: #64748b; line-height: 1.8; }
.state-symbol, .empty-symbol { display: grid; place-items: center; width: 64px; height: 64px; margin: 0 auto 22px; border-radius: 20px; background: #f5f3ff; color: #7c3aed; font-size: 36px; }
.primary-action { display: inline-block; padding: 11px 22px; border: 0; border-radius: 10px; background: #7c3aed; color: #fff; font: inherit; font-weight: 600; text-decoration: none; cursor: pointer; }
.primary-action:hover { background: #6d28d9; }
.return-link { color: #6d28d9; font-weight: 600; }
.loading-spinner { display: block; width: 40px; height: 40px; margin: 0 auto 22px; border: 4px solid #ede9fe; border-top-color: #7c3aed; border-radius: 50%; animation: spin .9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 720px) {
  .topic-page { padding: 22px 16px 60px; }
  .topic-hero { padding: 30px 24px; border-radius: 18px; }
  .hero-description { margin: 16px 0 23px; }
  .hero-decoration { display: none; }
  .questions-section { padding-top: 38px; }
  .section-heading { align-items: start; }
  .section-description { font-size: 14px; }
  .question-count { margin-top: 25px; }
  .question-card { padding: 18px; gap: 14px; }
  .question-number { flex-basis: 42px; height: 42px; font-size: 14px; }
  .question-main strong { font-size: 15px; }
  .state-panel, .empty-state { padding: 56px 20px; }
}
@media (prefers-reduced-motion: reduce) {
  .question-card { transition: none; }
  .question-card:hover { transform: none; }
  .loading-spinner { animation: none; }
}
</style>