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

      <div v-if="showSkeleton" class="result-list" role="status" aria-label="正在搜索">
        <div v-for="n in 4" :key="n" class="skeleton-card" aria-hidden="true">
          <div class="skeleton-line-pill skeleton-shimmer" style="width: 72px; height: 20px; margin-bottom: 10px"></div>
          <div class="skeleton-line skeleton-line-lg skeleton-shimmer" style="width: 66%"></div>
          <div class="skeleton-line skeleton-shimmer" style="width: 92%"></div>
        </div>
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
              v-for="(item, index) in result.items"
              :key="`${item.kind}-${item.id}`"
              :to="hitPath(item)"
              class="result-card stagger-enter"
              :style="{ '--stagger-index': index }"
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
.search-page { padding: 0; color: #0f172a; }
.search-hero { position: sticky; top: var(--header-height); z-index: 20; padding: 16px 20px; border: 1px solid #e0e7f1; border-radius: 14px; background: #f5f8fe; box-shadow: 0 8px 20px rgba(30, 41, 59, .07); }
.search-hero-header { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 4px; }
.search-hero h1 { display: flex; align-items: center; gap: 9px; margin: 0; font-size: 21px; line-height: 1.3; letter-spacing: -.02em; }
.search-hero h1 > svg, .result-heading h2 > svg { width: 20px; height: 20px; color: #4f68d7; flex-shrink: 0; }
.search-hero > p { margin: 0 0 12px; color: #64748b; font-size: 13px; line-height: 1.5; }
.search-form { display: flex; align-items: center; gap: 8px; max-width: 720px; min-height: 44px; padding: 4px 5px 4px 12px; background: #fff; border: 1px solid #dbe3f0; border-radius: 11px; }
.search-form:focus-within { border-color: #818cf8; box-shadow: 0 0 0 3px #6366f119; }
.search-symbol { width: 18px; height: 18px; flex-shrink: 0; color: #7386a6; }
.search-form input { min-width: 0; flex: 1; border: 0; outline: 0; font: inherit; font-size: 14px; background: transparent; color: #0f172a; }
.search-form input::placeholder { color: #94a3b8; }
.search-form button[type="submit"] { flex-shrink: 0; min-height: 36px; border: 0; border-radius: 8px; padding: 8px 16px; background: #2563eb; color: #fff; font-size: 13px; font-weight: 700; cursor: pointer; }
.search-form button[type="submit"] { transition: background .18s, transform var(--motion-micro, 140ms) var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)); }
.search-form button[type="submit"]:hover { background: #1d4ed8; }
.search-form button[type="submit"]:active:not(:disabled) { transform: scale(0.96); }
.search-form button[type="submit"]:disabled { opacity: .6; cursor: progress; }
.suggestions { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 10px; color: #64748b; font-size: 12px; }
.suggestions > span { margin-right: 4px; }
.suggestions button { padding: 4px 10px; border: 1px solid #dbe3f0; border-radius: 99px; color: #475569; background: #ffffffc8; font: inherit; cursor: pointer; }
.suggestions button { transition: border-color .16s, color .16s, background .16s, transform var(--motion-micro, 140ms) var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)); }
.suggestions button:hover { border-color: #a5b4fc; color: #2563eb; background: #fff; }
.suggestions button:active { transform: scale(0.95); background: #eff6ff; }
.search-content { padding: 18px 0 0; }
.result-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 12px; }
.result-heading h2 { display: flex; align-items: center; gap: 9px; margin: 0 0 4px; overflow-wrap: anywhere; font-size: 18px; }
.result-heading p { margin: 0; color: #64748b; font-size: 13px; }
.result-heading p strong { color: #2563eb; }
.truncated-note { color: #b45309; }
.filter-bar { display: flex; flex-wrap: wrap; gap: 8px; padding-bottom: 10px; margin-bottom: 10px; border-bottom: 1px solid #e2e8f0; }
.filter-button { display: inline-flex; align-items: center; gap: 6px; min-height: 36px; padding: 6px 11px; border: 1px solid #e2e8f0; border-radius: 9px; background: #fff; color: #64748b; font-size: 13px; font-weight: 650; cursor: pointer; transition: background 180ms var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)), color 180ms var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)), border-color 180ms var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)), transform var(--motion-micro, 140ms) var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)); }
.filter-button:active { transform: scale(0.96); }
.filter-button span { display: inline-flex; justify-content: center; align-items: center; min-width: 21px; padding: 1px 6px; border-radius: 6px; background: #f1f5f9; font-size: 11px; transition: background 180ms var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)), color 180ms var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)); }
.filter-button:hover { border-color: #a5b4fc; }
.filter-button.active { color: #fff; background: #2563eb; border-color: #2563eb; }
.filter-button.active span { color: #fff; background: #ffffff30; }
.result-list { display: grid; gap: 8px; }
.result-list.refreshing { opacity: .55; pointer-events: none; transition: opacity .15s; }
.result-card { display: block; padding: 12px 16px; border: 1px solid #e2e8f0; border-radius: var(--card-radius); background: #fff; color: inherit; box-shadow: 0 2px 8px rgba(27, 54, 110, .025); transition: border-color .2s, transform .2s, box-shadow .2s; }
.result-card:hover { border-color: #a5b4fc; transform: translateY(-2px); box-shadow: 0 10px 24px #47556910; text-decoration: none; }
.result-card:active { transform: scale(0.982); background: #f5f9ff; border-color: #93c5fd; transition-duration: 100ms; }
.result-topline { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 4px; }
.kind-tag { display: inline-flex; align-items: center; gap: 4px; padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: 800; }
.kind-category { color: #1d4ed8; background: #dbeafe; }
.kind-topic { color: #6d28d9; background: #ede9fe; }
.kind-question { color: #047857; background: #d1fae5; }
.question-id-tag { padding: 2px 7px; border-radius: 6px; background: #f1f5f9; color: #475569; font-size: 11px; font-weight: 700; font-variant-numeric: tabular-nums; }
.result-breadcrumb { overflow: hidden; color: #94a3b8; font-size: 12px; text-overflow: ellipsis; }
.result-open-icon { margin-left: auto; flex-shrink: 0; color: #91a5c0; transition: transform .2s, color .2s; }
.result-card:hover .result-open-icon { transform: translate(2px, -2px); color: #315ed2; }
.result-card h3 { margin: 0 0 4px; font-size: 15px; line-height: 1.45; font-weight: 750; overflow-wrap: anywhere; }
.result-description { display: -webkit-box; overflow: hidden; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin: 0; color: #64748b; font-size: 13px; line-height: 1.55; overflow-wrap: anywhere; white-space: pre-line; }
.answer-label { color: #475569; font-weight: 700; }
mark { padding: 0 2px; border-radius: 3px; color: #4338ca; background: #e0e7ff; }
.message-panel { display: flex; flex-direction: column; align-items: center; text-align: center; margin-top: 16px; padding: 36px 20px; border: 1px dashed #cbd5e1; border-radius: 14px; background: #fff; }
.message-icon { display: grid; place-items: center; width: 42px; height: 42px; margin-bottom: 10px; border-radius: 12px; background: #eef2ff; color: #6366f1; }
.message-icon svg { width: 24px; height: 24px; stroke-width: 1.8; }
.error-icon { background: #fef2f2; color: #dc2626; }
.message-panel h3 { margin: 6px 0 4px; font-size: 17px; }
.message-panel p { max-width: 440px; margin: 0; color: #64748b; font-size: 13px; line-height: 1.65; }
.outline-button { margin-top: 12px; min-height: 36px; padding: 8px 16px; border: 1px solid #c7d2fe; border-radius: 9px; background: #fff; color: #2563eb; font-size: 13px; font-weight: 700; cursor: pointer; }
.loading-spinner { width: 34px; height: 34px; margin-bottom: 8px; border: 3px solid #e0e7ff; border-top-color: #2563eb; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.pagination { display: flex; justify-content: center; align-items: center; gap: 10px; margin-top: 16px; color: #64748b; font-size: 13px; }
.pagination button { min-height: 36px; padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; color: #475569; font: inherit; cursor: pointer; }
.pagination button { transition: border-color .16s, color .16s, transform var(--motion-micro, 140ms) var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)); }
.pagination button:hover:not(:disabled) { border-color: #a5b4fc; color: #2563eb; }
.pagination button:active:not(:disabled) { transform: scale(0.96); }
.pagination button:disabled { opacity: .45; cursor: not-allowed; }
@media (max-width: 640px) {
  .search-hero { position: static; padding: 12px 14px; box-shadow: none; }
  .search-hero h1 { font-size: 19px; }
  .search-hero > p { margin-bottom: 10px; font-size: 12px; }
  .search-form { gap: 6px; padding: 4px 4px 4px 10px; }
  .search-form .search-symbol { display: none; }
  .search-form input { padding-left: 4px; font-size: 13px; }
  .search-form button[type="submit"] { padding: 8px 14px; }
  .search-content { padding-top: 14px; }
  .filter-bar { gap: 6px; }
  .result-card { padding: 12px; }
  .message-panel { padding: 28px 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .result-card, .result-list.refreshing, .result-open-icon { transition: none; }
  .result-card:hover, .result-card:active, .filter-button:active, .suggestions button:active, .pagination button:active { transform: none; }
  .loading-spinner { animation: none; }
}
</style>