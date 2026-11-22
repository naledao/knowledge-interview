<script setup lang="ts">
import { Search, FolderOpen, Layers3, CircleHelp, SearchX, AlertCircle, ArrowUpRight } from '@lucide/vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter, type LocationQueryRaw } from 'vue-router'
import { useCachedRoute } from '../router/useCachedRoute'
import { rememberedSearchMode, rememberSearchMode, SEARCH_MODE_OPTIONS } from '../utils/searchModeMemory'
import SearchModeSelect from '../components/SearchModeSelect.vue'
import {
  errorDetail, fetchHotKeywords, searchKnowledge,
  type SearchHit, type SearchKind, type SearchMode, type SearchResponse, type SearchType,
} from '../api/knowledge'

/**
 * 搜索页：匹配、相关度排序和分页均由后端 /api/search 完成，页面只请求当前页数据。
 * URL（q / mode / type / page）是搜索状态的唯一来源，支持前进后退、刷新和链接分享。
 */
const PAGE_SIZE = 8
const MAX_KEYWORD_LENGTH = 100
const MODES: SearchMode[] = ['keyword', 'exact']
const TYPES: SearchType[] = ['all', 'category', 'topic', 'question']
const typeLabels: Record<SearchType, string> = { all: '全部', category: '分类', topic: '主题', question: '问题' }
const kindLabels: Record<SearchKind, string> = { category: '知识分类', topic: '学习主题', question: '面试问题' }
const kindPaths: Record<SearchKind, string> = { category: '/categories', topic: '/topics', question: '/questions' }
const HOT_KEYWORD_LIMIT = 5
/** 热门搜索词由后端按搜索次数返回；未加载、加载失败或暂无数据时为空，此时不显示推荐区。 */
const suggestions = ref<string[]>([])

const route = useCachedRoute()
const router = useRouter()

function queryString(value: unknown): string {
  return typeof value === 'string' ? value : Array.isArray(value) && typeof value[0] === 'string' ? value[0] : ''
}
const keyword = computed(() => queryString(route.query.q).trim())
/** URL 中显式携带的搜索模式；未携带时为 null。 */
const urlMode = computed<SearchMode | null>(() => {
  const value = queryString(route.query.mode) as SearchMode
  return MODES.includes(value) ? value : null
})
const activeMode = computed<SearchMode>(() => urlMode.value ?? 'keyword')
/** 搜索模式下拉：选择后只记住状态 A，URL 由下方 watch 同步，顶部搜索框的下拉走同一条路径。 */
const modeModel = computed<SearchMode>({
  get: () => activeMode.value,
  set: (value) => rememberSearchMode(value),
})
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
      { q: keyword.value, mode: activeMode.value, type: activeType.value, page: page.value, size: PAGE_SIZE },
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

function updateQuery(patch: { q?: string; mode?: SearchMode; type?: SearchType; page?: number }, replace = false) {
  const q = patch.q ?? keyword.value
  const mode = patch.mode ?? activeMode.value
  const type = patch.type ?? activeType.value
  const nextPage = patch.page ?? page.value
  const query: LocationQueryRaw = {}
  if (q) query.q = q
  if (mode !== 'keyword') query.mode = mode
  if (q && mode === 'keyword' && type !== 'all') query.type = type
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

function setMode(value: SearchMode) {
  if (value === activeMode.value) return
  // 先记住用户选择的模式，顶部搜索框和刷新后都以此为准。
  rememberSearchMode(value)
  const trimmed = searchInput.value.trim().slice(0, MAX_KEYWORD_LENGTH)
  const canCarryQuery = value === 'keyword'
    ? trimmed.length > 0
    : /^[1-9]\d*$/.test(trimmed)
  if (!canCarryQuery) searchInput.value = ''
  void updateQuery({
    q: canCarryQuery ? trimmed : '',
    mode: value,
    type: 'all',
    page: 1,
  })
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
/** 首次加载或切换关键词/模式时显示整块加载态；翻页、切换筛选时保留旧列表并淡化，减少闪烁。 */
const showSkeleton = computed(() => loading.value && (!result.value || result.value.keyword !== keyword.value || result.value.mode !== activeMode.value))
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
/**
 * 同步搜索模式（状态 A）：
 * - URL 显式带模式或带关键词时，以 URL 为准并记住；
 * - 空白搜索页（如点击导航进入）且 URL 无模式时，恢复上次记住的精确搜索模式。
 */
watch([keyword, urlMode], () => {
  if (urlMode.value || keyword.value) {
    rememberSearchMode(activeMode.value)
    return
  }
  const saved = rememberedSearchMode.value
  if (saved !== 'keyword') void updateQuery({ mode: saved }, true)
}, { immediate: true })
/** URL 修正（如页码越界被后端改为末页）后，若已有结果与新状态一致则不重复请求。 */
function resultMatchesRoute() {
  const data = result.value
  return !!data && !errorMessage.value && data.keyword === keyword.value.replace(/\s+/g, ' ')
    && data.mode === activeMode.value && data.type === activeType.value && data.page === page.value
}
watch([keyword, activeMode, activeType, page], () => { if (!resultMatchesRoute()) void runSearch() }, { immediate: true })
/**
 * 顶部搜索框或本页下拉切换了搜索模式（状态 A）时，同步搜索页 URL。
 * 仅当前激活的搜索页实例处理，避免安卓保活的休眠实例误触发跳转。
 */
watch(rememberedSearchMode, (value) => {
  if (router.currentRoute.value.fullPath !== route.fullPath) return
  if (value !== activeMode.value) setMode(value)
})
/** 加载热门搜索词；失败时只记录日志，推荐区保持隐藏，不打扰用户。 */
async function loadHotKeywords() {
  try {
    suggestions.value = await fetchHotKeywords(HOT_KEYWORD_LIMIT)
  } catch (error) {
    console.warn('加载热门搜索词失败，不显示推荐词', error)
  }
}
onMounted(() => { void loadHotKeywords() })
onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <div class="search-page">
    <section class="search-hero" aria-labelledby="search-title">
      <div class="search-hero-header">
        <h1 id="search-title"><Search :size="29" aria-hidden="true" />搜索题库</h1>
      </div>
      <p>
        <template v-if="activeMode === 'exact'">精确搜索目前仅支持按问题 ID 查找，输入正整数编号直接定位对应面试题。</template>
        <template v-else>支持分类、主题、问题和答案模糊搜索；多个关键词用空格分隔。</template>
      </p>
      <form class="search-form" role="search" @submit.prevent="submitSearch">
        <Search class="search-symbol" :size="24" aria-hidden="true" />
        <SearchModeSelect v-model="modeModel" :options="SEARCH_MODE_OPTIONS" variant="form" />
        <input
          v-model="searchInput"
          type="search"
          :inputmode="activeMode === 'exact' ? 'numeric' : 'search'"
          :aria-label="activeMode === 'exact' ? '输入精确搜索的问题 ID' : '搜索知识内容'"
          :placeholder="activeMode === 'exact' ? '输入问题 ID（如 42）' : '输入知识点、面试题关键词'"
          autocomplete="off"
          :maxlength="MAX_KEYWORD_LENGTH"
        />
        <button type="submit" :disabled="loading && searchInput.trim() === keyword">
          {{ activeMode === 'exact' ? '精确查找' : '搜索' }}
        </button>
      </form>
      <div v-if="activeMode === 'keyword' && suggestions.length" class="suggestions">
        <span>试试</span>
        <button v-for="suggestion in suggestions" :key="suggestion" type="button" @click="searchSuggestion(suggestion)">
          {{ suggestion }}
        </button>
      </div>
    </section>

    <section v-if="keyword" class="search-content" aria-labelledby="results-title">
      <div class="result-heading">
        <div>
          <h2 id="results-title"><Layers3 :size="22" aria-hidden="true" />搜索结果</h2>
          <p aria-live="polite">
            <template v-if="showSkeleton">正在检索知识库...</template>
            <template v-else-if="errorMessage">暂时无法获取结果</template>
            <template v-else-if="result">
              <template v-if="activeMode === 'exact'">问题 ID #{{ keyword }} · <strong>{{ result.counts.all }}</strong> 条</template>
              <template v-else>“{{ keyword }}” · <strong>{{ result.counts.all }}</strong> 条</template>
              <span v-if="result.truncated" class="truncated-note">（结果较多，建议缩小范围）</span>
            </template>
          </p>
        </div>
      </div>

      <div v-if="showSkeleton" class="message-panel" role="status">
        <div class="loading-spinner" aria-hidden="true"></div>
        <h3>搜索中…</h3>
      </div>

      <div v-else-if="errorMessage" class="message-panel" role="alert">
        <div class="message-icon error-icon" aria-hidden="true"><AlertCircle :size="30" /></div>
        <h3>搜索失败</h3>
        <p>{{ errorMessage }}</p>
        <button class="outline-button" type="button" @click="runSearch">重新搜索</button>
      </div>

      <template v-else-if="result">
        <div v-if="activeMode === 'keyword'" class="filter-bar" role="group" aria-label="筛选搜索结果">
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
          <div class="message-icon" aria-hidden="true"><SearchX :size="30" /></div>
          <template v-if="activeMode === 'exact'">
            <h3>未找到对应问题</h3>
            <p>请确认问题 ID 是否存在，或切换回关键词搜索。</p>
          </template>
          <template v-else>
            <h3>{{ result.counts.all ? '该类型下暂无匹配内容' : '没有找到相关内容' }}</h3>
            <p>试试其他关键词。</p>
            <button v-if="result.counts.all" class="outline-button" type="button" @click="setType('all')">查看全部结果</button>
          </template>
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
                <span :class="['kind-tag', `kind-${item.kind}`]"><FolderOpen v-if="item.kind === 'category'" :size="13" /><Layers3 v-else-if="item.kind === 'topic'" :size="13" /><CircleHelp v-else :size="13" />{{ kindLabels[item.kind] }}</span>
                <span v-if="item.kind === 'question'" class="question-id-tag">#{{ item.id }}</span>
                <span class="result-breadcrumb">{{ item.breadcrumb }}</span><ArrowUpRight class="result-open-icon" :size="17" aria-hidden="true" />
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
.search-hero-header { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.search-hero h1 { position: relative; z-index: 1; margin: 0; font-size: clamp(27px, 4vw, 41px); line-height: 1.3; letter-spacing: -.03em; }
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
.question-id-tag { padding: 3px 8px; border-radius: 6px; background: #f1f5f9; color: #475569; font-size: 11px; font-weight: 700; font-variant-numeric: tabular-nums; }
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

/* 次要修饰让位于搜索框与结果。 */
.search-hero { padding: 30px 34px 28px; border: 1px solid #e0e7f1; border-radius: 16px; background: #f5f8fe; }
.search-hero::after { display: none; }
.search-hero h1 { margin: 0; font-size: 28px; }
.search-hero > p { margin: 0 0 18px; font-size: 13px; line-height: 1.6; }
.search-form { min-height: 54px; max-width: 700px; box-shadow: none; }
.search-form button { background: #2563eb; }
.search-form button:hover { background: #1d4ed8; }
.suggestions { margin-top: 14px; }
.search-content { padding-top: 28px; }
.result-heading { margin-bottom: 18px; }
.result-heading h2 { margin: 0 0 6px; font-size: 22px; }
.filter-bar { margin-bottom: 12px; }
.filter-button { padding: 8px 13px; }
.filter-button.active { background: #2563eb; border-color: #2563eb; }
.result-list { gap: 10px; }
.result-card { padding: 18px 22px; border-radius: 12px; }
.result-topline { margin-bottom: 8px; }
.result-card h3 { margin-bottom: 7px; font-size: 16px; }
.result-description { -webkit-line-clamp: 2; margin-bottom: 0; line-height: 1.65; }
.message-panel { padding: 36px 20px; margin-top: 20px; }
.message-icon { width: 42px; height: 42px; font-size: 24px; }
.message-panel h3 { font-size: 17px; }
@media (max-width: 640px) { .search-hero { padding: 24px 18px; } .search-hero h1 { font-size: 24px; } .search-form { min-height: 51px; } .search-content { padding-top: 24px; } .result-card { padding: 16px; } }

/* 搜索页：统一图标语言与结果卡片的交互层次。 */
.search-hero h1, .result-heading h2 { display: flex; align-items: center; gap: 12px; }
.search-hero h1 > svg, .result-heading h2 > svg { color: #4f68d7; flex-shrink: 0; }
.search-symbol { flex-shrink: 0; color: #7386a6; }
.kind-tag { display: inline-flex; align-items: center; gap: 5px; }
.result-open-icon { margin-left: auto; flex-shrink: 0; color: #91a5c0; transition: transform .2s, color .2s; }
.result-card:hover .result-open-icon { transform: translate(2px, -2px); color: #315ed2; }
.message-icon svg { stroke-width: 1.8; }
.result-card { box-shadow: 0 2px 9px rgba(27, 54, 110, .025); }
@media (prefers-reduced-motion: reduce) { .result-open-icon { transition: none; } }
/* 搜索卡片：滚动结果列表时固定在顶部栏下方；top 需与全局顶部栏高度一致（桌面 74px，宽度 ≤760px 时 62px）。 */
.search-hero { position: sticky; top: 74px; z-index: 20; box-shadow: 0 8px 22px rgba(30, 41, 59, .08); }
@media (max-width: 760px) { .search-hero { top: 62px; } }
@media (max-width: 640px) {
  .search-form .search-symbol { display: none; }
  .search-form { gap: 6px; padding-left: 12px; }
  .search-form input { padding-left: 4px; }
  .search-form button[type="submit"] { padding: 11px 18px; }
}
</style>