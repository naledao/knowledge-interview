<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter, type LocationQueryRaw } from 'vue-router'
import { useCachedRoute } from '../router/useCachedRoute'
import {
  errorDetail, searchKnowledge,
  type SearchHit, type SearchKind, type SearchResponse, type SearchType,
} from '../api/knowledge'

/**
 * 搜索页：匹配、相关度排序和分页均由后端 /api/search 完成，页面只请求当前页数据。
 * URL（q / type / page）是搜索状态的唯一来源，支持前进后退、刷新和链接分享。
 */
const PAGE_SIZE = 8
const MAX_KEYWORD_LENGTH = 100
const TYPES: SearchType[] = ['all', 'category', 'topic', 'question']
const typeLabels: Record<SearchType, string> = { all: '全部', category: '分类', topic: '主题', question: '问题' }
const kindLabels: Record<SearchKind, string> = { category: '知识分类', topic: '学习主题', question: '面试问题' }
const kindActions: Record<SearchKind, string> = { category: '探索分类主题', topic: '浏览主题问题', question: '查看问题与答案' }
const kindPaths: Record<SearchKind, string> = { category: '/categories', topic: '/topics', question: '/questions' }
const suggestions = ['Vue', 'JavaScript', 'MySQL', 'Redis', '算法']

const route = useCachedRoute()
const router = useRouter()

function queryString(value: unknown): string {
  return typeof value === 'string' ? value : Array.isArray(value) && typeof value[0] === 'string' ? value[0] : ''
}
const keyword = computed(() => queryString(route.query.q).trim())
const activeType = computed<SearchType>(() => {
  const value = queryString(route.query.type) as SearchType
  return TYPES.includes(value) ? value : 'all'
})
const page = computed(() => {
  const value = Number.parseInt(queryString(route.query.page), 10)
  return Number.isFinite(value) && value > 0 ? value : 1
})

const searchInput = ref('')
const loading = ref(false)
const errorMessage = ref('')
const result = ref<SearchResponse | null>(null)
let controller: AbortController | null = null

/** 新请求会取消上一次未完成的请求，避免快速切换时旧结果覆盖新结果。 */
async function runSearch() {
  controller?.abort()
  if (!keyword.value) {
    result.value = null
    errorMessage.value = ''
    loading.value = false
    return
  }
  const current = new AbortController()
  controller = current
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await searchKnowledge(
      { q: keyword.value, type: activeType.value, page: page.value, size: PAGE_SIZE },
      current.signal,
    )
    if (current.signal.aborted) return
    result.value = data
    // 页码越界时后端返回末页，同步修正 URL，保持地址栏与内容一致。
    if (data.page !== page.value) void updateQuery({ page: data.page }, true)
  } catch (error) {
    if (current.signal.aborted) return
    result.value = null
    errorMessage.value = errorDetail(error) ?? '搜索服务暂时不可用，请检查网络连接后重试。'
    console.error('搜索失败', error)
  } finally {
    if (controller === current) {
      loading.value = false
      controller = null
    }
  }
}

function updateQuery(patch: { q?: string; type?: SearchType; page?: number }, replace = false) {
  const q = patch.q ?? keyword.value
  const type = patch.type ?? activeType.value
  const nextPage = patch.page ?? page.value
  const query: LocationQueryRaw = {}
  if (q) query.q = q
  if (q && type !== 'all') query.type = type
  if (q && nextPage > 1) query.page = String(nextPage)
  const location = { name: 'search' as const, query }
  return replace ? router.replace(location) : router.push(location)
}

function submitSearch() {
  const q = searchInput.value.trim().slice(0, MAX_KEYWORD_LENGTH)
  // 新关键词从全部类型第一页开始；同一关键词重复提交时强制刷新结果。
  if (q === keyword.value && activeType.value === 'all' && page.value === 1) void runSearch()
  else void updateQuery({ q, type: 'all', page: 1 })
}

function searchSuggestion(value: string) {
  searchInput.value = value
  submitSearch()
}

function setType(value: SearchType) {
  if (value !== activeType.value) void updateQuery({ type: value, page: 1 })
}

function goToPage(value: number) {
  if (!result.value || value < 1 || value > result.value.totalPages || value === result.value.page) return
  void updateQuery({ page: value })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const filters = computed(() => TYPES.map((value) => ({
  value, label: typeLabels[value], count: result.value?.counts[value] ?? 0,
})))
/** 首次加载或切换关键词时显示整块加载态；翻页、切换筛选时保留旧列表并淡化，减少闪烁。 */
const showSkeleton = computed(() => loading.value && (!result.value || result.value.keyword !== keyword.value))
const terms = computed(() => result.value?.terms ?? [])

function hitPath(item: SearchHit) {
  return `${kindPaths[item.kind]}/${item.id}`
}

/** 使用文本插值而非 v-html 高亮，避免搜索内容引入 HTML 注入；高亮词以后端实际匹配的词为准。 */
function highlighted(value: string): { text: string; hit: boolean }[] {
  if (!terms.value.length || !value) return [{ text: value, hit: false }]
  const lower = value.toLocaleLowerCase()
  const pieces: { text: string; hit: boolean }[] = []
  let cursor = 0
  while (cursor < value.length) {
    let next = value.length
    let length = 0
    for (const term of terms.value) {
      const at = lower.indexOf(term, cursor)
      if (at >= 0 && (at < next || (at === next && term.length > length))) {
        next = at
        length = term.length
      }
    }
    if (!length) {
      pieces.push({ text: value.slice(cursor), hit: false })
      break
    }
    if (next > cursor) pieces.push({ text: value.slice(cursor, next), hit: false })
    pieces.push({ text: value.slice(next, next + length), hit: true })
    cursor = next + length
  }
  return pieces
}

watch(keyword, (value) => { searchInput.value = value }, { immediate: true })
/** URL 修正（如页码越界被后端改为末页）后，若已有结果与新状态一致则不重复请求。 */
function resultMatchesRoute() {
  const data = result.value
  return !!data && !errorMessage.value && data.keyword === keyword.value.replace(/\s+/g, ' ')
    && data.type === activeType.value && data.page === page.value
}
watch([keyword, activeType, page], () => { if (!resultMatchesRoute()) void runSearch() }, { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <div class="search-page">
    <nav class="breadcrumbs" aria-label="面包屑导航">
      <RouterLink to="/">首页</RouterLink>
      <span aria-hidden="true">/</span>
      <span>搜索结果</span>
    </nav>

    <section class="search-hero" aria-labelledby="search-title">
      <div class="hero-eyebrow">EXPLORE KNOWLEDGE · 知识检索</div>
      <h1 id="search-title">探索你的下一步<span>知识成长</span></h1>
      <p>搜索知识分类、学习主题、面试问题与答案；多个关键词用空格分隔，需同时命中。</p>
      <form class="search-form" role="search" @submit.prevent="submitSearch">
        <span class="search-symbol" aria-hidden="true">⌕</span>
        <input
          v-model="searchInput"
          type="search"
          aria-label="搜索知识内容"
          placeholder="输入关键词，例如 Vue、Redis 持久化..."
          autocomplete="off"
          :maxlength="MAX_KEYWORD_LENGTH"
        />
        <button type="submit" :disabled="loading && searchInput.trim() === keyword">搜索 <span aria-hidden="true">→</span></button>
      </form>
      <div class="suggestions">
        <span>热门搜索</span>
        <button v-for="suggestion in suggestions" :key="suggestion" type="button" @click="searchSuggestion(suggestion)">
          {{ suggestion }}
        </button>
      </div>
    </section>

    <section v-if="!keyword" class="message-panel" aria-labelledby="start-title">
      <div class="message-icon" aria-hidden="true">⌕</div>
      <h2 id="start-title">从一个关键词开始</h2>
      <p>试试搜索技术名称、知识点或面试题，开启你的知识探索。</p>
    </section>

    <section v-else class="search-content" aria-labelledby="results-title">
      <div class="result-heading">
        <div>
          <div class="section-eyebrow">SEARCH RESULTS</div>
          <h2 id="results-title">“{{ keyword }}” 的搜索结果</h2>
          <p aria-live="polite">
            <template v-if="showSkeleton">正在检索知识库...</template>
            <template v-else-if="errorMessage">暂时无法获取结果</template>
            <template v-else-if="result">
              共找到 <strong>{{ result.counts.all }}</strong> 条相关内容
              <span v-if="result.truncated" class="truncated-note">（结果较多，仅统计前若干条，建议补充关键词）</span>
            </template>
          </p>
        </div>
        <RouterLink to="/" class="home-link">返回首页 <span aria-hidden="true">↗</span></RouterLink>
      </div>

      <div v-if="showSkeleton" class="message-panel" role="status">
        <div class="loading-spinner" aria-hidden="true"></div>
        <h3>正在搜索知识库</h3>
        <p>正在查找分类、主题和问题，请稍候。</p>
      </div>

      <div v-else-if="errorMessage" class="message-panel" role="alert">
        <div class="message-icon error-icon" aria-hidden="true">!</div>
        <h3>搜索失败</h3>
        <p>{{ errorMessage }}</p>
        <button class="outline-button" type="button" @click="runSearch">重新搜索</button>
      </div>

      <template v-else-if="result">
        <div class="filter-bar" role="group" aria-label="筛选搜索结果">
          <button
            v-for="filter in filters"
            :key="filter.value"
            type="button"
            :class="['filter-button', { active: activeType === filter.value }]"
            :aria-pressed="activeType === filter.value"
            @click="setType(filter.value)"
          >
            {{ filter.label }} <span>{{ filter.count }}</span>
          </button>
        </div>

        <div v-if="!result.items.length" class="message-panel">
          <div class="message-icon" aria-hidden="true">◇</div>
          <h3>{{ result.counts.all ? '该类型下暂无匹配内容' : '没有找到相关内容' }}</h3>
          <p>尝试更简短的关键词，或换一个技术名称继续搜索。</p>
          <button v-if="result.counts.all" class="outline-button" type="button" @click="setType('all')">查看全部结果</button>
        </div>

        <template v-else>
          <div :class="['result-list', { refreshing: loading }]" :aria-busy="loading">
            <RouterLink
              v-for="item in result.items"
              :key="`${item.kind}-${item.id}`"
              :to="hitPath(item)"
              class="result-card"
            >
              <div class="result-topline">
                <span :class="['kind-tag', `kind-${item.kind}`]">{{ kindLabels[item.kind] }}</span>
                <span class="result-breadcrumb">{{ item.breadcrumb }}</span>
              </div>
              <h3>
                <template v-for="(segment, index) in highlighted(item.title)" :key="index">
                  <mark v-if="segment.hit">{{ segment.text }}</mark><span v-else>{{ segment.text }}</span>
                </template>
              </h3>
              <p v-if="item.excerpt" class="result-description">
                <span v-if="item.answerMatch" class="answer-label">答案 · </span>
                <template v-for="(segment, index) in highlighted(item.excerpt)" :key="index">
                  <mark v-if="segment.hit">{{ segment.text }}</mark><span v-else>{{ segment.text }}</span>
                </template>
              </p>
              <div class="result-bottom">
                <span>{{ kindActions[item.kind] }}</span>
                <span aria-hidden="true">→</span>
              </div>
            </RouterLink>
          </div>

          <nav v-if="result.totalPages > 1" class="pagination" aria-label="搜索结果分页">
            <button type="button" :disabled="loading || result.page === 1" @click="goToPage(result.page - 1)">← 上一页</button>
            <span>第 {{ result.page }} / {{ result.totalPages }} 页</span>
            <button type="button" :disabled="loading || result.page === result.totalPages" @click="goToPage(result.page + 1)">下一页 →</button>
          </nav>
        </template>
      </template>
    </section>
  </div>
</template>

<style scoped>
.search-page { max-width: 1160px; margin: 0 auto; padding: 12px 12px 90px; color: #0f172a; }
.breadcrumbs { display: flex; gap: 12px; align-items: center; margin: 0 0 27px; color: #94a3b8; font-size: 13px; }
.breadcrumbs a { color: #64748b; }
.search-hero { position: relative; overflow: hidden; padding: 52px 56px 46px; border: 1px solid #dbeafe; border-radius: 24px; background: radial-gradient(circle at 88% 8%, #dceafe 0, transparent 36%), linear-gradient(115deg, #eff6ff, #f5f3ff 78%, #f8fafc); }
.search-hero::after { content: ''; position: absolute; width: 270px; height: 270px; right: -110px; bottom: -200px; border: 42px solid #dbeafe; border-radius: 50%; pointer-events: none; }
.hero-eyebrow, .section-eyebrow { font-size: 11px; font-weight: 800; letter-spacing: .13em; color: #4f46e5; }
.search-hero h1 { position: relative; z-index: 1; margin: 15px 0 12px; font-size: clamp(27px, 4vw, 41px); line-height: 1.3; letter-spacing: -.03em; }
.search-hero h1 span { color: #4f46e5; }
.search-hero > p { margin: 0 0 28px; color: #64748b; font-size: 14px; line-height: 1.8; }
.search-form { position: relative; z-index: 1; display: flex; align-items: center; gap: 12px; max-width: 820px; min-height: 64px; padding: 7px 8px 7px 22px; background: white; border: 1px solid #dbe3f0; border-radius: 14px; box-shadow: 0 9px 26px #47556912; }
.search-form:focus-within { border-color: #818cf8; box-shadow: 0 0 0 3px #6366f119; }
.search-symbol { color: #64748b; font-size: 31px; line-height: 1; }
.search-form input { min-width: 0; flex: 1; border: 0; outline: 0; font: inherit; font-size: 15px; background: transparent; color: #0f172a; }
.search-form input::placeholder { color: #94a3b8; }
.search-form button { flex-shrink: 0; border: 0; border-radius: 10px; padding: 13px 22px; background: #4f46e5; color: #fff; font-size: 14px; font-weight: 700; cursor: pointer; }
.search-form button:hover { background: #4338ca; }
.search-form button span { margin-left: 12px; }
.suggestions { position: relative; z-index: 1; display: flex; flex-wrap: wrap; align-items: center; gap: 9px; margin-top: 20px; color: #64748b; font-size: 12px; }
.suggestions > span { margin-right: 6px; }
.suggestions button { padding: 6px 12px; border: 1px solid #dbe3f0; border-radius: 99px; color: #475569; background: #ffffffb8; font: inherit; cursor: pointer; }
.suggestions button:hover { border-color: #a5b4fc; color: #4338ca; background: #fff; }
.search-content { padding: 38px 4px 0; }
.result-heading { display: flex; justify-content: space-between; align-items: center; gap: 18px; margin-bottom: 28px; }
.result-heading h2 { margin: 9px 0 7px; overflow-wrap: anywhere; font-size: clamp(21px, 3vw, 27px); }
.result-heading p { margin: 0; color: #64748b; font-size: 13px; }
.result-heading p strong { color: #4f46e5; }
.home-link { flex-shrink: 0; color: #4f46e5; font-size: 13px; font-weight: 700; }
.filter-bar { display: flex; flex-wrap: wrap; gap: 10px; padding-bottom: 17px; margin-bottom: 14px; border-bottom: 1px solid #e2e8f0; }
.filter-button { display: inline-flex; align-items: center; gap: 9px; padding: 10px 18px; border: 1px solid #e2e8f0; border-radius: 10px; background: #fff; color: #64748b; font-size: 13px; font-weight: 650; cursor: pointer; }
.filter-button span { display: inline-flex; justify-content: center; align-items: center; min-width: 23px; padding: 2px 6px; border-radius: 6px; background: #f1f5f9; font-size: 11px; }
.filter-button:hover { border-color: #a5b4fc; }
.filter-button.active { color: #fff; background: #4f46e5; border-color: #4f46e5; }
.filter-button.active span { color: #fff; background: #ffffff30; }
.result-list { display: grid; gap: 14px; }
.result-card { display: block; padding: 24px 28px 20px; border: 1px solid #e2e8f0; border-radius: 15px; background: #fff; color: inherit; transition: border-color .2s, transform .2s, box-shadow .2s; }
.result-card:hover { border-color: #a5b4fc; transform: translateY(-2px); box-shadow: 0 12px 35px #47556910; text-decoration: none; }
.result-topline { display: flex; flex-wrap: wrap; align-items: center; gap: 11px; margin-bottom: 14px; }
.kind-tag { padding: 5px 10px; border-radius: 6px; font-size: 11px; font-weight: 800; }
.kind-category { color: #1d4ed8; background: #dbeafe; }
.kind-topic { color: #6d28d9; background: #ede9fe; }
.kind-question { color: #047857; background: #d1fae5; }
.result-breadcrumb { overflow: hidden; color: #94a3b8; font-size: 12px; text-overflow: ellipsis; }
.result-card h3 { margin: 0 0 10px; font-size: 18px; line-height: 1.6; font-weight: 750; overflow-wrap: anywhere; }
.result-description { display: -webkit-box; overflow: hidden; -webkit-line-clamp: 3; -webkit-box-orient: vertical; margin: 0 0 19px; color: #64748b; font-size: 13px; line-height: 1.9; overflow-wrap: anywhere; white-space: pre-line; }
.answer-label { color: #475569; font-weight: 700; }
.result-list.refreshing { opacity: .55; pointer-events: none; transition: opacity .15s; }
.truncated-note { color: #b45309; }
.search-form button:disabled { opacity: .6; cursor: progress; }
mark { padding: 0 2px; border-radius: 3px; color: #4338ca; background: #e0e7ff; }
.result-bottom { display: flex; justify-content: space-between; gap: 14px; align-items: center; margin-top: 15px; padding-top: 14px; border-top: 1px solid #f1f5f9; color: #4f46e5; font-size: 12px; font-weight: 700; }
.result-bottom span:last-child { font-size: 18px; }
.message-panel { display: flex; flex-direction: column; align-items: center; text-align: center; margin-top: 30px; padding: 74px 24px; border: 1px dashed #cbd5e1; border-radius: 18px; background: #fff; }
.message-icon { display: grid; place-items: center; width: 66px; height: 66px; margin-bottom: 12px; border-radius: 18px; background: #eef2ff; color: #6366f1; font-size: 32px; }
.error-icon { background: #fef2f2; color: #dc2626; font-weight: 800; }
.message-panel h2, .message-panel h3 { margin: 12px 0 6px; font-size: 20px; }
.message-panel p { max-width: 440px; color: #64748b; font-size: 14px; line-height: 1.8; }
.outline-button { margin-top: 16px; padding: 10px 22px; border: 1px solid #c7d2fe; border-radius: 9px; background: #fff; color: #4338ca; font-weight: 700; cursor: pointer; }
.loading-spinner { width: 40px; height: 40px; border: 4px solid #e0e7ff; border-top-color: #4f46e5; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.pagination { display: flex; justify-content: center; align-items: center; gap: 24px; margin-top: 30px; color: #64748b; font-size: 13px; }
.pagination button { padding: 10px 16px; border: 1px solid #e2e8f0; border-radius: 9px; background: #fff; color: #475569; font: inherit; cursor: pointer; }
.pagination button:hover:not(:disabled) { border-color: #a5b4fc; color: #4f46e5; }
.pagination button:disabled { opacity: .45; cursor: not-allowed; }
@media (max-width: 640px) {
  .search-page { padding: 0 0 55px; }
  .breadcrumbs { margin: 2px 4px 18px; }
  .search-hero { padding: 30px 20px; border-radius: 16px; }
  .search-hero h1 { font-size: 28px; }
  .search-hero > p { font-size: 13px; }
  .search-form { gap: 7px; min-height: 56px; padding: 6px 6px 6px 12px; }
  .search-form input { font-size: 13px; }
  .search-form button { padding: 11px 13px; font-size: 13px; }
  .search-form button span { margin-left: 3px; }
  .search-content { padding: 27px 0 0; }
  .result-heading { align-items: flex-start; }
  .home-link { margin-top: 12px; }
  .filter-bar { gap: 7px; }
  .filter-button { padding: 9px 11px; gap: 5px; }
  .result-card { padding: 20px 17px 17px; }
  .result-card h3 { font-size: 16px; }
  .message-panel { padding: 55px 20px; }
  .pagination { gap: 12px; }
}
@media (prefers-reduced-motion: reduce) {
  .result-card, .result-list.refreshing { transition: none; }
  .loading-spinner { animation: none; }
}
</style>
