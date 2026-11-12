<script setup lang='ts'>
import axios from 'axios'
import { computed, ref, watch } from 'vue'
import { useCachedRoute } from '../router/useCachedRoute'
import { getCategory, getTopics, type KnowledgeCategory, type KnowledgeTopic } from '../api/knowledge'
import MarkdownContent from '../components/MarkdownContent.vue'
import ListPagination from '../components/ListPagination.vue'
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

/** 路由变更重置页码；翻页时只加载目标页，旧请求结果不覆盖新页面。 */
async function loadCategory(resetPage = false) {
  const version = ++requestVersion
  if (resetPage) topicPage.value = 1
  category.value = null
  topics.value = []
  loading.value = true
  errorMessage.value = ''
  notFound.value = false
  const categoryId = parseCategoryId(route.params.id)
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
    topics.value = result.items
    topicPages.value = result.pages
    topicTotal.value = result.total
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
  topicPage.value = page
  void loadCategory()
}

watch(() => route.params.id, () => { void loadCategory(true) }, { immediate: true })
</script>

<template>
  <div class='category-page'>
    <nav class='breadcrumbs' aria-label='面包屑导航'>
      <RouterLink to='/'>首页</RouterLink>
      <span aria-hidden='true'>/</span>
      <span aria-current='page'>{{ category?.name || '分类详情' }}</span>
    </nav>

    <section v-if='loading' class='state-panel' role='status'>
      <span class='loading-spinner' aria-hidden='true'></span>
      <h1>正在加载分类...</h1>
      <p>正在获取分类信息与学习主题。</p>
    </section>

    <section v-else-if='errorMessage' class='state-panel' role='alert'>
      <div class='state-symbol' aria-hidden='true'>!</div>
      <h1>加载失败</h1>
      <p>{{ errorMessage }}</p>
      <button type='button' class='primary-action' @click='loadCategory()'>重新加载</button>
    </section>

    <section v-else-if='notFound' class='state-panel'>
      <div class='state-symbol' aria-hidden='true'>?</div>
      <h1>分类不存在</h1>
      <p>该分类可能已被删除，或访问链接中的分类 ID 无效。</p>
      <RouterLink to='/' class='primary-action'>返回首页</RouterLink>
    </section>

    <template v-else-if='category'>
      <header class='category-hero'>
        <div class='hero-content'>
          <p class='eyebrow'>KNOWLEDGE CATEGORY</p>
          <h1>{{ category.name }}</h1>
          <MarkdownContent class='category-description' size='compact' :source="category.description || '浏览该分类下的知识主题，循序渐进地展开学习。'" />
          <div class='hero-summary'>
            <strong>{{ topicTotal }}</strong>
            <span>个学习主题</span>
          </div>
        </div>
        <div class='hero-art' aria-hidden='true'>
          <span class='art-circle art-circle-back'></span>
          <span class='art-circle art-circle-front'>◎</span>
        </div>
      </header>

      <section class='topics-section' aria-labelledby='topics-heading'>
        <div class='section-heading'>
          <div>
            <p class='eyebrow section-eyebrow'>EXPLORE TOPICS</p>
            <h2 id='topics-heading'>学习主题</h2>
            <p class='section-description'>选择一个主题，进一步探索相关知识与问题。</p>
          </div>
          <span class='topic-count'>共 {{ topicTotal }} 项</span>
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
              <span class='card-arrow' aria-hidden='true'>↗</span>
            </div>
            <h3>{{ topic.title }}</h3>
            <p class='topic-description'>{{ markdownToPlainText(topic.description) || '查看这个主题下的知识内容与相关问题。' }}</p>
            <div class='topic-card-footer'>
              <span>查看主题</span>
              <span aria-hidden='true'>→</span>
            </div>
          </RouterLink>
        </div>

        <div v-else class='empty-state'>
          <div class='empty-symbol' aria-hidden='true'>◇</div>
          <h3>暂无学习主题</h3>
          <p>当前分类还没有添加主题，可以先浏览其他知识分类。</p>
          <RouterLink to='/' class='home-link'>浏览其他分类 →</RouterLink>
        </div>
        <ListPagination :page-num='topicPage' :pages='topicPages' :loading='loading' @change='changeTopicPage' />
      </section>
    </template>
  </div>
</template>

<style scoped>
.category-page { max-width: 1180px; margin: 0 auto; padding: 32px 24px 88px; color: #0f172a; }
.breadcrumbs { display: flex; align-items: center; gap: 12px; margin-bottom: 28px; font-size: 14px; color: #64748b; }
.breadcrumbs a { color: #475569; text-decoration: none; }
.breadcrumbs a:hover { color: #2563eb; }
.breadcrumbs span:last-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.category-hero { display: flex; justify-content: space-between; align-items: center; gap: 36px; padding: 52px 56px; overflow: hidden; border: 1px solid #dbeafe; border-radius: 24px; background: linear-gradient(115deg, #eff6ff 0%, #f8fbff 70%, #eef2ff 100%); }
.hero-content { position: relative; z-index: 1; min-width: 0; }
.eyebrow { margin: 0 0 15px; color: #2563eb; font-size: 12px; font-weight: 800; letter-spacing: .14em; }
.category-hero h1 { margin: 0; font-size: clamp(30px, 4vw, 46px); font-weight: 800; line-height: 1.25; overflow-wrap: anywhere; }
.category-description { max-width: 630px; margin: 22px 0 28px; color: #475569; }
.hero-summary { display: inline-flex; align-items: baseline; gap: 8px; padding: 10px 17px; border: 1px solid #bfdbfe; border-radius: 999px; background: #fff; color: #475569; font-size: 13px; }
.hero-summary strong { color: #1d4ed8; font-size: 19px; }
.hero-art { position: relative; width: 185px; height: 185px; flex: 0 0 185px; }
.art-circle { position: absolute; display: grid; place-items: center; border-radius: 50%; }
.art-circle-back { inset: 0; border: 22px solid #dbeafe; }
.art-circle-front { inset: 34px; background: #2563eb; color: #fff; font-size: 74px; box-shadow: 0 15px 30px #1d4ed826; }
.topics-section { padding-top: 56px; }
.section-heading { display: flex; justify-content: space-between; align-items: end; gap: 20px; margin-bottom: 26px; }
.section-eyebrow { margin-bottom: 9px; }
.section-heading h2 { margin: 0; font-size: clamp(24px, 3vw, 31px); }
.section-description { margin: 12px 0 0; color: #64748b; line-height: 1.7; }
.topic-count { flex-shrink: 0; padding: 8px 13px; border: 1px solid #e2e8f0; border-radius: 999px; color: #475569; font-size: 13px; }
.topic-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; }
.topic-card { display: flex; flex-direction: column; min-width: 0; min-height: 225px; padding: 25px 27px 21px; border: 1px solid #e2e8f0; border-radius: 18px; background: #fff; color: inherit; text-decoration: none; transition: border-color .2s, box-shadow .2s, transform .2s; }
.topic-card:hover { transform: translateY(-3px); border-color: #93c5fd; box-shadow: 0 16px 30px #0f172a0d; }
.topic-card:focus-visible, .breadcrumbs a:focus-visible, .home-link:focus-visible, .primary-action:focus-visible { outline: 3px solid #2563eb; outline-offset: 3px; }
.topic-card-top, .topic-card-footer { display: flex; justify-content: space-between; align-items: center; }
.topic-index { color: #2563eb; font-size: 13px; font-weight: 800; letter-spacing: .05em; }
.card-arrow { display: grid; place-items: center; width: 31px; height: 31px; border-radius: 9px; background: #f1f5f9; color: #64748b; font-size: 17px; }
.topic-card h3 { margin: 20px 0 9px; font-size: 20px; line-height: 1.4; overflow-wrap: anywhere; }
.topic-description { flex: 1; margin: 0 0 26px; color: #64748b; font-size: 14px; line-height: 1.75; overflow-wrap: anywhere; display: -webkit-box; overflow: hidden; -webkit-line-clamp: 4; -webkit-box-orient: vertical; }
.topic-card-footer { border-top: 1px solid #f1f5f9; padding-top: 15px; color: #2563eb; font-size: 13px; font-weight: 700; }
.empty-state, .state-panel { padding: 75px 24px; border: 1px solid #e2e8f0; border-radius: 20px; background: #fff; text-align: center; }
.empty-symbol, .state-symbol { display: grid; place-items: center; width: 64px; height: 64px; margin: 0 auto 22px; border-radius: 20px; background: #eff6ff; color: #2563eb; font-size: 38px; }
.empty-state h3, .state-panel h1 { margin: 0 0 10px; font-size: 23px; }
.empty-state p, .state-panel p { margin: 0 auto 24px; color: #64748b; line-height: 1.8; }
.home-link { color: #2563eb; font-weight: 600; text-decoration: none; }
.primary-action { display: inline-block; padding: 11px 22px; border: 0; border-radius: 10px; background: #2563eb; color: #fff; font: inherit; font-weight: 600; text-decoration: none; cursor: pointer; }
.primary-action:hover { background: #1d4ed8; }
.loading-spinner { display: block; width: 40px; height: 40px; margin: 0 auto 22px; border: 4px solid #dbeafe; border-top-color: #2563eb; border-radius: 50%; animation: spin .9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 720px) {
  .category-page { padding: 22px 16px 60px; }
  .category-hero { padding: 30px 24px; border-radius: 18px; }
  .hero-art { display: none; }
  .category-description { margin: 16px 0 22px; }
  .topics-section { padding-top: 38px; }
  .section-heading { align-items: start; }
  .section-description { font-size: 14px; }
  .topic-grid { grid-template-columns: 1fr; gap: 14px; }
  .topic-card { min-height: 190px; padding: 22px; }
  .topic-count { margin-top: 25px; }
  .state-panel, .empty-state { padding: 56px 20px; }
}
@media (prefers-reduced-motion: reduce) {
  .topic-card { transition: none; }
  .topic-card:hover { transform: none; }
  .loading-spinner { animation: none; }
}
</style>