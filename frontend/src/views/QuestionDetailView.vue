<script setup lang='ts'>
import { Home, ChevronRight, MessageCircleQuestion, BookOpenCheck, Eye, EyeOff, ArrowLeft, ArrowRight, ArrowUp, ArrowUpRight, CircleHelp, AlertCircle, Shuffle } from '@lucide/vue'
import axios from 'axios'
import { computed, nextTick, onActivated, onDeactivated, onMounted, onUnmounted, ref, watch } from 'vue'
import { useCachedRoute } from '../router/useCachedRoute'
import {
  getCategory, getTopic, getQuestion, getQuestionNeighbors,
  type KnowledgeCategory, type KnowledgeTopic, type KnowledgeQuestion, type QuestionNeighbors,
} from '../api/knowledge'
import MarkdownContent from '../components/MarkdownContent.vue'
import HtmlAnswerRenderer from '../components/HtmlAnswerRenderer.vue'
import { isHtmlAnswer } from '../utils/answerFormat'
import { useRandomQuestion } from '../composables/useRandomQuestion'

/** 问题详情通过主键获取完整答案，前后题由轻量查询提供，避免加载全部问题。 */
const route = useCachedRoute()
const question = ref<KnowledgeQuestion | null>(null)
const topic = ref<KnowledgeTopic | null>(null)
const category = ref<KnowledgeCategory | null>(null)
const neighbors = ref<QuestionNeighbors>({ previous: null, next: null })
const loading = ref(true)
const errorMessage = ref('')
const notFound = ref(false)
const showAnswer = ref(true)
const readProgress = ref(0)
const showBackToTop = ref(false)
const isActiveView = ref(true)
let scrollRafId = 0

function syncReadingMetrics() {
  scrollRafId = 0
  if (!isActiveView.value) return
  const doc = document.documentElement
  const maxScroll = Math.max(0, doc.scrollHeight - window.innerHeight)
  const y = window.scrollY
  readProgress.value = maxScroll > 40 ? Math.min(1, Math.max(0, y / maxScroll)) : 0
  showBackToTop.value = y > 360
}

function scheduleReadingMetrics() {
  if (scrollRafId || !isActiveView.value) return
  scrollRafId = window.requestAnimationFrame(syncReadingMetrics)
}

function scrollToTop() {
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' })
}
const { loading: randomLoading, errorMessage: randomError, goRandom } = useRandomQuestion()
let requestVersion = 0

const previousQuestion = computed(() => neighbors.value.previous)
const nextQuestion = computed(() => neighbors.value.next)
const answerText = computed(() => question.value?.answerText?.trim() || '')
const answerIsHtml = computed(() => isHtmlAnswer(answerText.value))

/** 校验 URL 中的数字主键。 */
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
  neighbors.value = { previous: null, next: null }
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
    const current = await getQuestion(questionId)
    if (version !== requestVersion) return
    const [currentTopic, adjacent] = await Promise.all([
      getTopic(current.topicId), getQuestionNeighbors(questionId),
    ])
    if (version !== requestVersion) return
    const currentCategory = await getCategory(currentTopic.categoryId)
    if (version !== requestVersion) return
    question.value = current
    topic.value = currentTopic
    category.value = currentCategory
    neighbors.value = adjacent
  } catch (error) {
    if (version !== requestVersion) return
    if (axios.isAxiosError(error) && error.response?.status === 404) notFound.value = true
    else {
      errorMessage.value = '问题数据加载失败，请检查网络连接后重试。'
      console.error('问题详情加载失败', error)
    }
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

watch(() => route.params.id, () => { void loadQuestion() }, { immediate: true })
watch([loading, showAnswer], () => { void nextTick(scheduleReadingMetrics) })

onMounted(() => {
  window.addEventListener('scroll', scheduleReadingMetrics, { passive: true })
  window.addEventListener('resize', scheduleReadingMetrics, { passive: true })
  scheduleReadingMetrics()
})
onActivated(() => {
  isActiveView.value = true
  scheduleReadingMetrics()
})
onDeactivated(() => {
  isActiveView.value = false
  if (scrollRafId) { window.cancelAnimationFrame(scrollRafId); scrollRafId = 0 }
  showBackToTop.value = false
})
onUnmounted(() => {
  window.removeEventListener('scroll', scheduleReadingMetrics)
  window.removeEventListener('resize', scheduleReadingMetrics)
  if (scrollRafId) { window.cancelAnimationFrame(scrollRafId); scrollRafId = 0 }
})
</script>

<template>
  <div class='question-page'>
    <div v-if='isActiveView && question && !loading' class='reading-progress-track' aria-hidden='true'>
      <div class='reading-progress-bar' :style="{ transform: `scaleX(${readProgress})` }"></div>
    </div>
    <nav class='breadcrumbs' aria-label='面包屑导航'>
      <RouterLink to='/'><Home :size='15' aria-hidden='true' />首页</RouterLink>
      <ChevronRight :size='15' aria-hidden='true' />
      <RouterLink v-if='category' :to='`/categories/${category.id}`'>{{ category.name }}</RouterLink>
      <span v-else>知识分类</span>
      <ChevronRight :size='15' aria-hidden='true' />
      <RouterLink v-if='topic' :to='`/topics/${topic.id}`'>{{ topic.title }}</RouterLink>
      <span v-else>知识主题</span>
      <ChevronRight :size='15' aria-hidden='true' />
      <span aria-current='page'>问题详情</span>
    </nav>

    <div v-if='loading' class='skeleton-page' role='status' aria-label='加载问题详情中'>
      <div class='question-hero' aria-hidden='true'>
        <div class='hero-title-row'>
          <span class='hero-icon skeleton-shimmer' style='border: 0; box-shadow: none'></span>
          <div style='flex: 1'>
            <div class='skeleton-line skeleton-line-lg skeleton-shimmer' style='width: 82%'></div>
            <div class='skeleton-line skeleton-line-lg skeleton-shimmer' style='width: 56%; margin-top: 8px'></div>
          </div>
        </div>
      </div>
      <div class='answer-section' aria-hidden='true'>
        <div class='answer-card'>
          <div class='skeleton-line skeleton-shimmer' style='width: 94%'></div>
          <div class='skeleton-line skeleton-shimmer' style='width: 88%'></div>
          <div class='skeleton-line skeleton-shimmer' style='width: 76%'></div>
          <div class='skeleton-line skeleton-shimmer' style='width: 84%; margin-top: 16px'></div>
          <div class='skeleton-line skeleton-shimmer' style='width: 62%'></div>
        </div>
      </div>
    </div>

    <section v-else-if='errorMessage' class='state-panel' role='alert'>
      <div class='state-symbol' aria-hidden='true'><AlertCircle :size='30' /></div>
      <h1>加载失败</h1>
      <p>{{ errorMessage }}</p>
      <button type='button' class='primary-action' @click='loadQuestion'>重新加载</button>
    </section>

    <section v-else-if='notFound' class='state-panel'>
      <div class='state-symbol' aria-hidden='true'><CircleHelp :size='30' /></div>
      <h1>问题不存在</h1>
      <p>请返回主题查看其他问题。</p>
      <RouterLink to='/' class='primary-action'>返回首页</RouterLink>
    </section>

    <template v-else-if='question'>
      <header class='question-hero'>
        <div class='hero-title-row'>
          <span class='hero-icon' aria-hidden='true'><MessageCircleQuestion :size='20' /></span>
          <h1>{{ question.questionText }}</h1>
        </div>
        <div class='hero-footer'>
          <span class='topic-label'>{{ topic?.title || '知识问题' }}</span>
          <RouterLink :to='`/topics/${question.topicId}`' class='topic-link'>返回主题 <ArrowUpRight :size='17' aria-hidden='true' /></RouterLink>
        </div>
      </header>

      <section class='answer-section' aria-labelledby='answer-heading'>
        <div class='answer-heading'>
          <div>
            <h2 id='answer-heading'><BookOpenCheck :size='23' aria-hidden='true' />参考答案</h2>
          </div>
          <button v-if='answerText' class='answer-toggle' type='button' :aria-expanded='showAnswer' aria-controls='answer-content' @click='showAnswer = !showAnswer'>
            <EyeOff v-if='showAnswer' :size='17' aria-hidden='true' /><Eye v-else :size='17' aria-hidden='true' />{{ showAnswer ? '收起答案' : '展开答案' }}
          </button>
        </div>
        <div id='answer-content' class='answer-card' :class="{ 'answer-card-html': answerIsHtml && showAnswer, 'is-collapsed': !showAnswer }">
          <p v-if='!answerText' class='empty-answer'>该问题尚未添加参考答案。</p>
          <template v-else>
            <div class='answer-collapse' :class="{ 'is-collapsed': !showAnswer }">
              <div class='answer-collapse-inner'>
                <HtmlAnswerRenderer v-if='answerIsHtml' :source='answerText' />
                <MarkdownContent v-else class='answer-text' :source='answerText' />
              </div>
            </div>
            <p v-if='!showAnswer' class='hidden-answer'>答案已收起</p>
          </template>
        </div>
      </section>

      <nav v-if='previousQuestion || nextQuestion' class='question-navigation' aria-label='题目切换'>
        <RouterLink v-if='previousQuestion' class='nav-card' :to='`/questions/${previousQuestion.id}`'>
          <span class='nav-direction'><ArrowLeft :size='16' aria-hidden='true' />上一题</span>
          <strong>{{ previousQuestion.questionText }}</strong>
        </RouterLink>
        <RouterLink v-if='nextQuestion' class='nav-card nav-next' :to='`/questions/${nextQuestion.id}`'>
          <span class='nav-direction'>下一题<ArrowRight :size='16' aria-hidden='true' /></span>
          <strong>{{ nextQuestion.questionText }}</strong>
        </RouterLink>
      </nav>

      <div class='random-next'>
        <button type='button' class='random-next-button' :disabled='randomLoading' @click='goRandom(question.id)'>
          <Shuffle :size='17' aria-hidden='true' />{{ randomLoading ? '抽取中…' : '再来一题' }}
        </button>
        <span v-if='randomError' class='random-next-error' role='alert'>{{ randomError }}</span>
      </div>
    </template>
    <Transition name='fab-pop'>
      <button
        v-if='isActiveView && question && !loading && showBackToTop'
        type='button'
        class='back-to-top-fab'
        aria-label='回到顶部'
        title='回到顶部'
        @click='scrollToTop'
      >
        <ArrowUp :size='19' aria-hidden='true' />
      </button>
    </Transition>
  </div>
</template>

<style scoped>
.question-page { padding: 0; color: #0f172a; }
.breadcrumbs { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: var(--breadcrumb-gap); color: #64748b; font-size: 13px; }
.breadcrumbs a { display: inline-flex; align-items: center; gap: 5px; color: #475569; text-decoration: none; }
.breadcrumbs a:hover { color: #2563eb; }
.breadcrumbs > svg { flex-shrink: 0; color: #94a3b8; }
.question-hero { padding: 18px 22px; border: 1px solid #bfdbfe; border-radius: var(--hero-radius); background: #f5f8fe; }
.hero-title-row { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 12px; }
.hero-icon { display: grid; flex: 0 0 36px; width: 36px; height: 36px; place-items: center; border: 1px solid #d2e2ff; border-radius: 10px; background: #fff; color: #3767cd; box-shadow: 0 4px 12px #315ed214; }
.question-hero h1 { margin: 0; font-size: clamp(20px, 2.6vw, 26px); line-height: 1.4; white-space: pre-wrap; overflow-wrap: anywhere; }
.hero-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; padding-top: 12px; border-top: 1px solid #dbeafe; }
.topic-label { color: #475569; font-size: 13px; }
.topic-link { display: inline-flex; align-items: center; gap: 5px; color: #1d4ed8; font-size: 13px; font-weight: 600; }
.answer-section { margin-top: 18px; }
.answer-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.answer-heading h2 { display: inline-flex; align-items: center; gap: 8px; margin: 0; font-size: 19px; }
.answer-heading h2 > svg { width: 19px; height: 19px; color: #4774d4; flex-shrink: 0; }
.answer-toggle { display: inline-flex; align-items: center; justify-content: center; gap: 6px; min-height: 36px; padding: 7px 12px; border: 1px solid #bfdbfe; border-radius: 9px; background: #fff; color: #1d4ed8; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }
.answer-toggle { transition: background .16s, border-color .16s, transform var(--motion-micro, 140ms) var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)); }
.answer-toggle:hover { background: #eff6ff; }
.answer-toggle:active { transform: scale(0.95); }
.reading-progress-track { position: fixed; top: var(--header-height); left: 0; right: 0; z-index: 45; height: 2px; background: transparent; pointer-events: none; }
.reading-progress-bar { width: 100%; height: 100%; background: linear-gradient(90deg, #3b82f6, #6366f1); transform-origin: left center; will-change: transform; }
.answer-collapse { display: grid; grid-template-rows: 1fr; opacity: 1; transition: grid-template-rows 240ms var(--ease-enter, cubic-bezier(0.22, 1, 0.36, 1)), opacity 200ms var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)); }
.answer-collapse.is-collapsed { grid-template-rows: 0fr; opacity: 0; }
.answer-collapse-inner { overflow: hidden; min-height: 0; }
.answer-card.is-collapsed { min-height: 0; padding: 14px 18px; }
.back-to-top-fab { position: fixed; right: 16px; bottom: 22px; z-index: 35; display: grid; place-items: center; width: 42px; height: 42px; padding: 0; border: 1px solid #dbeafe; border-radius: 50%; background: rgba(255, 255, 255, .94); color: #2563eb; box-shadow: 0 8px 22px rgba(30, 64, 175, .16); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); cursor: pointer; transition: background .16s, transform var(--motion-micro, 140ms) var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)), box-shadow .16s; }
.back-to-top-fab:hover { background: #eff6ff; box-shadow: 0 10px 26px rgba(30, 64, 175, .22); }
.back-to-top-fab:active { transform: scale(0.92); }
.fab-pop-enter-active { transition: opacity 220ms var(--ease-enter, cubic-bezier(0.22, 1, 0.36, 1)), transform 220ms var(--ease-enter, cubic-bezier(0.22, 1, 0.36, 1)); }
.fab-pop-leave-active { transition: opacity 150ms var(--ease-exit, cubic-bezier(0.4, 0, 1, 1)), transform 150ms var(--ease-exit, cubic-bezier(0.4, 0, 1, 1)); }
.fab-pop-enter-from, .fab-pop-leave-to { opacity: 0; transform: translateY(10px) scale(0.9); }
.answer-card { min-height: 100px; padding: 20px 24px; border: 1px solid #e2e8f0; border-radius: 14px; background: #fff; box-shadow: 0 6px 20px #0f172a07; }
.answer-card.answer-card-html { min-height: 0; padding: 0; border: 0; border-radius: 0; background: transparent; box-shadow: none; overflow: visible; }
.answer-text { line-height: 1.78; }
.empty-answer, .hidden-answer { margin: 0; color: #64748b; font-size: 14px; line-height: 1.75; }
.question-navigation { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 20px; }
.nav-card { display: flex; flex-direction: column; gap: 6px; padding: 14px 16px; border: 1px solid #e2e8f0; border-radius: var(--card-radius); background: #fff; color: #0f172a; text-decoration: none; transition: border-color .18s, transform .18s, box-shadow .18s; }
.nav-card:hover { border-color: #93c5fd; transform: translateY(-2px); box-shadow: 0 10px 22px #1d4ed812; }
.nav-card:active { transform: scale(0.982); background: #f5f9ff; border-color: #93c5fd; transition-duration: 100ms; }
.nav-direction { display: inline-flex; align-items: center; gap: 6px; color: #2563eb; font-size: 12px; font-weight: 700; }
.nav-card strong { overflow-wrap: anywhere; line-height: 1.45; font-size: 14px; }
.nav-next { text-align: right; }
.nav-next .nav-direction { justify-content: flex-end; }
.random-next { display: flex; flex-wrap: wrap; align-items: center; justify-content: center; gap: 10px; margin-top: 14px; }
.random-next-button { display: inline-flex; align-items: center; gap: 7px; min-height: 38px; padding: 9px 18px; border: 1px solid #c7d7f5; border-radius: 10px; background: #fff; color: #2563eb; font: inherit; font-size: 14px; font-weight: 700; cursor: pointer; transition: border-color .18s, background .18s; }
.random-next-button { transition: border-color .18s, background .18s, transform var(--motion-micro, 140ms) var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)); }
.random-next-button:hover:not(:disabled) { border-color: #93b4ee; background: #f4f8ff; }
.random-next-button:active:not(:disabled) { transform: scale(0.97); background: #eff6ff; }
.random-next-button:disabled { cursor: progress; opacity: .7; }
.random-next-error { color: #b91c1c; font-size: 13px; }
.state-panel { padding: 36px 20px; border: 1px solid #e2e8f0; border-radius: 14px; background: #fff; text-align: center; }
.state-panel h1 { margin: 0 0 8px; font-size: 19px; }
.state-panel p { margin: 0 auto 16px; color: #64748b; font-size: 14px; line-height: 1.65; }
.state-symbol { display: grid; place-items: center; width: 48px; height: 48px; margin: 0 auto 12px; border-radius: 14px; background: #eff6ff; color: #2563eb; }
.primary-action { display: inline-block; padding: 9px 18px; border: 0; border-radius: 9px; background: #2563eb; color: #fff; font: inherit; font-size: 14px; font-weight: 600; text-decoration: none; cursor: pointer; }
.loading-spinner { display: block; width: 34px; height: 34px; margin: 0 auto 14px; border: 3px solid #dbeafe; border-top-color: #2563eb; border-radius: 50%; animation: spin .9s linear infinite; }
.question-page a:focus-visible, .question-page button:focus-visible { outline: 3px solid #2563eb; outline-offset: 3px; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 720px) {
  .question-hero { padding: 14px 16px; }
  .hero-title-row { gap: 10px; }
  .hero-icon { flex-basis: 32px; height: 32px; }
  .answer-heading h2 { font-size: 18px; }
  .answer-card { padding: 16px; }
  .question-navigation { grid-template-columns: 1fr; gap: 10px; }
  .nav-next { text-align: left; }
  .nav-next .nav-direction { justify-content: flex-start; }
  .state-panel { padding: 28px 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .nav-card { transition: none; }
  .nav-card:hover, .nav-card:active, .answer-toggle:active, .random-next-button:active, .back-to-top-fab:active { transform: none; }
  .answer-collapse, .fab-pop-enter-active, .fab-pop-leave-active { transition: none; }
  .loading-spinner { animation: none; }
}
</style>