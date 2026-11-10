<script setup lang='ts'>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  getCategories, getTopics, getQuestions,
  type KnowledgeCategory, type KnowledgeTopic, type KnowledgeQuestion,
} from '../api/knowledge'

/** 问题详情页：展示问题和答案，提供所属主题导航及上一题、下一题切换。 */
const route = useRoute()
const question = ref<KnowledgeQuestion | null>(null)
const topic = ref<KnowledgeTopic | null>(null)
const category = ref<KnowledgeCategory | null>(null)
const questions = ref<KnowledgeQuestion[]>([])
const loading = ref(true)
const errorMessage = ref('')
const notFound = ref(false)
const showAnswer = ref(true)
let requestVersion = 0

/** 仅在同一主题内切换问题，并按业务排序值及 ID 排序。 */
const siblingQuestions = computed(() => {
  if (!question.value) return []
  return questions.value
    .filter((item) => item.topicId === question.value?.topicId)
    .sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
})
const currentIndex = computed(() => siblingQuestions.value.findIndex((item) => item.id === question.value?.id))
const previousQuestion = computed(() => currentIndex.value > 0 ? siblingQuestions.value[currentIndex.value - 1] ?? null : null)
const nextQuestion = computed(() => currentIndex.value >= 0 ? siblingQuestions.value[currentIndex.value + 1] ?? null : null)
const answerText = computed(() => question.value?.answerText?.trim() || '')

/** 校验路由参数，避免无效 ID 和超出安全整数范围的 ID。 */
function parseQuestionId(value: unknown): number | null {
  if (typeof value !== 'string' || !/^[1-9][0-9]*$/.test(value)) return null
  const id = Number(value)
  return Number.isSafeInteger(id) ? id : null
}

async function loadQuestion() {
  const version = ++requestVersion
  question.value = null
  topic.value = null
  category.value = null
  questions.value = []
  loading.value = true
  errorMessage.value = ''
  notFound.value = false
  showAnswer.value = true

  const questionId = parseQuestionId(route.params.id)
  if (questionId === null) {
    notFound.value = true
    loading.value = false
    return
  }

  try {
    // 复用现有只读接口：问题列表用于定位当前问题以及计算相邻问题。
    const [questionList, topicList, categoryList] = await Promise.all([
      getQuestions(), getTopics(), getCategories(),
    ])
    // 切换上一题或下一题时，忽略过期的异步响应。
    if (version !== requestVersion) return

    const selected = questionList.find((item) => item.id === questionId)
    if (!selected) {
      notFound.value = true
      return
    }

    question.value = selected
    questions.value = questionList
    topic.value = topicList.find((item) => item.id === selected.topicId) ?? null
    category.value = categoryList.find((item) => item.id === topic.value?.categoryId) ?? null
  } catch (error) {
    if (version !== requestVersion) return
    errorMessage.value = '问题数据加载失败，请检查网络连接后重试。'
    console.error('问题详情加载失败', error)
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

/** 同组件内路由参数改变时重新读取数据。 */
watch(() => route.params.id, () => { void loadQuestion() }, { immediate: true })
</script>

<template>
  <div class='question-page'>
    <nav class='breadcrumbs' aria-label='面包屑导航'>
      <RouterLink to='/'>首页</RouterLink>
      <span aria-hidden='true'>/</span>
      <RouterLink v-if='category' :to='`/categories/${category.id}`'>{{ category.name }}</RouterLink>
      <span v-else>知识分类</span>
      <span aria-hidden='true'>/</span>
      <RouterLink v-if='topic' :to='`/topics/${topic.id}`'>{{ topic.title }}</RouterLink>
      <span v-else>知识主题</span>
      <span aria-hidden='true'>/</span>
      <span aria-current='page'>问题详情</span>
    </nav>

    <section v-if='loading' class='state-panel' role='status'>
      <span class='loading-spinner' aria-hidden='true'></span>
      <h1>正在加载问题...</h1>
      <p>正在获取问题内容及对应答案。</p>
    </section>

    <section v-else-if='errorMessage' class='state-panel' role='alert'>
      <div class='state-symbol' aria-hidden='true'>!</div>
      <h1>加载失败</h1>
      <p>{{ errorMessage }}</p>
      <button type='button' class='primary-action' @click='loadQuestion'>重新加载</button>
    </section>

    <section v-else-if='notFound' class='state-panel'>
      <div class='state-symbol' aria-hidden='true'>?</div>
      <h1>问题不存在</h1>
      <p>该问题可能已被删除，或访问链接中的问题 ID 无效。</p>
      <RouterLink to='/' class='primary-action'>返回首页</RouterLink>
    </section>

    <template v-else-if='question'>
      <header class='question-hero'>
        <div class='hero-header'>
          <span class='eyebrow'>KNOWLEDGE QUESTION</span>
          <span v-if='currentIndex >= 0' class='progress-pill'>第 {{ currentIndex + 1 }} / {{ siblingQuestions.length }} 题</span>
        </div>
        <h1>{{ question.questionText }}</h1>
        <div class='hero-footer'>
          <span class='topic-label'>{{ topic?.title || '知识问题' }}</span>
          <RouterLink :to='`/topics/${question.topicId}`' class='topic-link'>返回主题 <span aria-hidden='true'>↗</span></RouterLink>
        </div>
      </header>

      <section class='answer-section' aria-labelledby='answer-heading'>
        <div class='answer-heading'>
          <div>
            <span class='eyebrow'>REFERENCE ANSWER</span>
            <h2 id='answer-heading'>参考答案</h2>
          </div>
          <button v-if='answerText' class='answer-toggle' type='button' :aria-expanded='showAnswer' aria-controls='answer-content' @click='showAnswer = !showAnswer'>
            {{ showAnswer ? '收起答案' : '展开答案' }}
          </button>
        </div>
        <div id='answer-content' class='answer-card'>
          <p v-if='!answerText' class='empty-answer'>该问题尚未添加参考答案。</p>
          <div v-else-if='showAnswer' class='answer-text'>{{ answerText }}</div>
          <p v-else class='hidden-answer'>答案已收起，点击“展开答案”继续阅读。</p>
        </div>
      </section>

      <nav class='question-navigation' aria-label='题目切换'>
        <RouterLink v-if='previousQuestion' class='nav-card' :to='`/questions/${previousQuestion.id}`'>
          <span class='nav-direction'>← 上一题</span>
          <strong>{{ previousQuestion.questionText }}</strong>
        </RouterLink>
        <div v-else class='nav-card nav-disabled' aria-disabled='true'>
          <span class='nav-direction'>← 上一题</span>
          <strong>已经是本主题的第一题</strong>
        </div>
        <RouterLink v-if='nextQuestion' class='nav-card nav-next' :to='`/questions/${nextQuestion.id}`'>
          <span class='nav-direction'>下一题 →</span>
          <strong>{{ nextQuestion.questionText }}</strong>
        </RouterLink>
        <div v-else class='nav-card nav-next nav-disabled' aria-disabled='true'>
          <span class='nav-direction'>下一题 →</span>
          <strong>已经是本主题的最后一题</strong>
        </div>
      </nav>

      <div class='back-row'>
        <RouterLink :to='`/topics/${question.topicId}`'>← 返回问题列表</RouterLink>
      </div>
    </template>
  </div>
</template>

<style scoped>
.question-page { max-width: 1180px; margin: 0 auto; padding: 32px 24px 88px; color: #0f172a; }
.breadcrumbs { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; margin-bottom: 28px; color: #64748b; font-size: 14px; }
.breadcrumbs a { color: #475569; text-decoration: none; }
.breadcrumbs a:hover { color: #2563eb; }
.question-hero { padding: 45px 50px; border: 1px solid #bfdbfe; border-radius: 24px; background: linear-gradient(120deg, #eff6ff, #f8fbff 65%, #eef2ff); }
.hero-header, .hero-footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.eyebrow { color: #2563eb; font-size: 12px; letter-spacing: .14em; font-weight: 800; }
.progress-pill { padding: 8px 15px; border: 1px solid #bfdbfe; border-radius: 99px; background: #fff; color: #1d4ed8; font-size: 13px; font-weight: 600; }
.question-hero h1 { margin: 28px 0 30px; font-size: clamp(24px, 3.8vw, 36px); line-height: 1.55; white-space: pre-wrap; overflow-wrap: anywhere; }
.hero-footer { padding-top: 20px; border-top: 1px solid #dbeafe; }
.topic-label { color: #475569; font-size: 14px; }
.topic-link { color: #1d4ed8; font-size: 14px; font-weight: 600; }
.answer-section { margin-top: 48px; }
.answer-heading { display: flex; align-items: end; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.answer-heading h2 { margin: 10px 0 0; font-size: 27px; }
.answer-toggle { padding: 10px 17px; border: 1px solid #bfdbfe; border-radius: 10px; background: #fff; color: #1d4ed8; font: inherit; font-size: 14px; font-weight: 600; cursor: pointer; }
.answer-toggle:hover { background: #eff6ff; }
.answer-card { min-height: 170px; padding: 34px 38px; border: 1px solid #e2e8f0; border-radius: 20px; background: #fff; box-shadow: 0 10px 28px #0f172a08; }
.answer-text { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.95; color: #334155; font-size: 16px; }
.empty-answer, .hidden-answer { margin: 0; color: #64748b; line-height: 1.9; }
.question-navigation { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; margin-top: 38px; }
.nav-card { display: flex; flex-direction: column; gap: 13px; padding: 24px 27px; min-height: 117px; border: 1px solid #e2e8f0; border-radius: 16px; background: #fff; color: #0f172a; text-decoration: none; transition: border-color .2s, transform .2s, box-shadow .2s; }
.nav-card:not(.nav-disabled):hover { border-color: #93c5fd; transform: translateY(-2px); box-shadow: 0 10px 24px #0f172a0d; }
.nav-direction { color: #2563eb; font-size: 13px; font-weight: 700; }
.nav-card strong { overflow-wrap: anywhere; line-height: 1.5; font-size: 15px; }
.nav-next { text-align: right; }
.nav-disabled { background: #f8fafc; color: #94a3b8; }
.nav-disabled .nav-direction { color: #94a3b8; }
.back-row { margin-top: 32px; text-align: center; }
.back-row a { color: #2563eb; font-weight: 600; }
.state-panel { padding: 75px 24px; border: 1px solid #e2e8f0; border-radius: 20px; background: #fff; text-align: center; }
.state-panel h1 { margin: 0 0 10px; font-size: 23px; }
.state-panel p { margin: 0 auto 24px; color: #64748b; line-height: 1.8; }
.state-symbol { display: grid; place-items: center; width: 64px; height: 64px; margin: 0 auto 22px; border-radius: 20px; background: #eff6ff; color: #2563eb; font-size: 36px; }
.primary-action { display: inline-block; padding: 11px 22px; border: 0; border-radius: 10px; background: #2563eb; color: #fff; font: inherit; font-weight: 600; text-decoration: none; cursor: pointer; }
.loading-spinner { display: block; width: 40px; height: 40px; margin: 0 auto 22px; border: 4px solid #dbeafe; border-top-color: #2563eb; border-radius: 50%; animation: spin .9s linear infinite; }
.question-page a:focus-visible, .question-page button:focus-visible { outline: 3px solid #2563eb; outline-offset: 3px; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 720px) {
  .question-page { padding: 22px 16px 60px; }
  .question-hero { padding: 28px 22px; border-radius: 18px; }
  .question-hero h1 { margin: 22px 0; font-size: 24px; }
  .answer-section { margin-top: 35px; }
  .answer-heading h2 { font-size: 24px; }
  .answer-card { padding: 24px 22px; border-radius: 16px; }
  .answer-text { font-size: 15px; }
  .question-navigation { grid-template-columns: 1fr; gap: 14px; }
  .nav-card { min-height: 95px; padding: 20px 22px; }
  .state-panel { padding: 56px 20px; }
}
@media (prefers-reduced-motion: reduce) {
  .nav-card { transition: none; }
  .nav-card:not(.nav-disabled):hover { transform: none; }
  .loading-spinner { animation: none; }
}
</style>