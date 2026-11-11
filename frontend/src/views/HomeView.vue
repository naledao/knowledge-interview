<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCategories, getTopics, type KnowledgeCategory, type KnowledgeTopic } from '../api/knowledge'
import { markdownToPlainText } from '../utils/plainText'
import ListPagination from '../components/ListPagination.vue'

const router = useRouter()
const keyword = ref('')
const hotKeywords = ['Vue', 'JavaScript', 'MySQL', 'Redis', '算法']
const loading = ref(true)
const error = ref('')

interface HomeCategory extends KnowledgeCategory { icon: string; tone: string; tags: string[] }
interface HomeRecent { id: number; category: string; title: string; description: string; icon: string; tone: string }
const categories = ref<HomeCategory[]>([])
const recentItems = ref<HomeRecent[]>([])
const categoryPage = ref(1)
const categoryPages = ref(0)
const palette = [
  { icon: '💻', tone: 'blue' }, { icon: '⚙️', tone: 'purple' },
  { icon: '🗄️', tone: 'orange' }, { icon: '🧩', tone: 'green' },
  { icon: '🧠', tone: 'pink' }, { icon: '🏗️', tone: 'cyan' },
  { icon: '🛠️', tone: 'amber' }, { icon: '✨', tone: 'indigo' },
]
const recentTones = ['yellow', 'blue', 'purple']
const firstCategoryPath = computed(() => categories.value.length ? `/categories/${categories.value[0]!.id}` : '/search')
const firstTopicPath = computed(() => recentItems.value.length ? `/topics/${recentItems.value[0]!.id}` : '/search')
const learningSteps = computed(() => [
  { number: '01', title: '选择知识方向', description: '根据兴趣或目标选择知识分类。', to: firstCategoryPath.value },
  { number: '02', title: '梳理核心主题', description: '围绕主题建立系统的知识框架。', to: firstTopicPath.value },
  { number: '03', title: '针对性查找问题', description: '通过搜索定位需要复习的知识点。', to: '/search' },
])

function timestamp(value: string | null): number {
  const parsed = value ? Date.parse(value) : NaN
  return Number.isNaN(parsed) ? 0 : parsed
}

async function loadHome() {
  loading.value = true
  error.value = ''
  try {
    const [categoryData, topicData] = await Promise.all([getCategories(categoryPage.value), getTopics()])
    categoryPages.value = categoryData.pages
    const names = new Map(categoryData.items.map((category) => [category.id, category.name]))
    categories.value = [...categoryData.items]
      .sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
      .map((category, index) => ({
        ...category,
        description: markdownToPlainText(category.description),
        ...palette[index % palette.length]!,
        tags: topicData.items.filter((topic) => topic.categoryId === category.id).slice(0, 3).map((topic) => topic.title),
      }))
    recentItems.value = [...topicData.items]
      .sort((a, b) => timestamp(b.updatedAt || b.createdAt) - timestamp(a.updatedAt || a.createdAt) || b.id - a.id)
      .slice(0, 5)
      .map((topic: KnowledgeTopic, index) => ({
        id: topic.id,
        category: names.get(topic.categoryId) || '未分类',
        title: topic.title,
        description: markdownToPlainText(topic.description) || '查看这个知识主题的详细内容',
        icon: 'TOP',
        tone: recentTones[index % recentTones.length]!,
      }))
  } catch (cause) {
    categories.value = []
    recentItems.value = []
    error.value = '知识内容加载失败，请检查网络或稍后重试。'
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

function search(value = keyword.value) {
  const q = value.trim()
  void router.push({ path: '/search', query: q ? { q } : {} })
}

onMounted(loadHome)
</script>

<template>
  <div class="home-page">
    <section class="hero" aria-labelledby="hero-title">
      <div class="hero-copy">
        <div class="hero-eyebrow"><span class="eyebrow-dot"></span> SYSTEMATIC LEARNING · 知识面试题库</div>
        <h1 id="hero-title">让每一个知识点，<br /><span>成为你的竞争力。</span></h1>
        <p class="hero-description">分类梳理核心知识，循序渐进探索学习主题。<br class="desktop-break" />从知识积累到面试准备，找到适合自己的学习方向。</p>
        <form class="hero-search" role="search" @submit.prevent="search()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="10.8" cy="10.8" r="6.8" /><path d="m16 16 4.5 4.5" /></svg>
          <input v-model="keyword" type="search" aria-label="搜索知识点" placeholder="搜索你感兴趣的知识点，例如 Vue、MySQL..." />
          <button type="submit">探索知识 <span aria-hidden="true">↗</span></button>
        </form>
        <div class="hot-searches"><span>热门探索</span><button v-for="tag in hotKeywords" :key="tag" type="button" @click="search(tag)">{{ tag }}</button></div>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <div class="visual-orbit orbit-one"></div><div class="visual-orbit orbit-two"></div>
        <div class="floating-label label-top"><span class="small-indicator"></span> 持续探索 · 不断进步</div>
        <div class="visual-panel"><div class="panel-top"><span class="panel-dots"><i></i><i></i><i></i></span><span>LEARNING MAP</span></div><div class="visual-title">你的知识成长路线</div><div class="visual-step"><span class="step-symbol">01</span><div><strong>知识分类</strong><small>找到感兴趣的领域</small></div><span class="step-check">✓</span></div><div class="visual-connector"></div><div class="visual-step"><span class="step-symbol second">02</span><div><strong>主题学习</strong><small>逐步建立知识体系</small></div><span class="step-arrow">→</span></div><div class="visual-connector"></div><div class="visual-step"><span class="step-symbol third">03</span><div><strong>巩固理解</strong><small>让知识真正为你所用</small></div><span class="step-arrow">→</span></div></div>
        <div class="floating-label label-bottom">✦ 每一步都算数</div>
      </div>
    </section>

    <div class="highlights" aria-label="平台特点">
      <div class="highlight"><span class="highlight-icon">◈</span><div><strong>结构化知识分类</strong><small>从基础到进阶，清晰探索</small></div></div>
      <div class="highlight"><span class="highlight-icon">◎</span><div><strong>聚焦核心知识点</strong><small>更高效地找到学习方向</small></div></div>
      <div class="highlight"><span class="highlight-icon">↗</span><div><strong>随时随地学习</strong><small>支持桌面与移动设备</small></div></div>
    </div>

    <section class="home-section categories-section" aria-labelledby="categories-title">
      <div class="section-header"><div><div class="section-kicker">EXPLORE TOPICS <span></span></div><h2 id="categories-title">探索知识领域</h2><p>从感兴趣的方向出发，一步步构建自己的知识地图。</p></div><RouterLink to="/search" class="section-more">查找知识点 <span aria-hidden="true">→</span></RouterLink></div>
      <div v-if="loading" class="home-status" role="status">正在加载知识分类...</div><div v-else-if="error" class="home-status home-error" role="alert">{{ error }} <button type="button" @click="loadHome">重试</button></div><div v-else-if="!categories.length" class="home-status">暂无知识分类</div><div v-else class="category-grid"><RouterLink v-for="category in categories" :key="category.id" :to="'/categories/' + category.id" class="category-card" :class="'tone-' + category.tone"><div class="category-top"><span class="category-icon" aria-hidden="true">{{ category.icon }}</span><span class="card-arrow" aria-hidden="true">↗</span></div><h3>{{ category.name }}</h3><p>{{ category.description }}</p><div v-if="category.tags.length" class="category-tags"><span v-for="tag in category.tags" :key="tag">{{ tag }}</span></div></RouterLink></div>
      <ListPagination :page-num="categoryPage" :pages="categoryPages" :loading="loading" @change="changeCategoryPage" />
    </section>

    <section class="home-section bottom-section" aria-label="近期内容与学习指南">
      <div class="recent-column"><div class="section-header"><div><div class="section-kicker">KEEP LEARNING <span></span></div><h2>近期内容</h2><p>发现值得深入了解的知识主题。</p></div><RouterLink to="/search" class="section-more">查看更多 <span aria-hidden="true">→</span></RouterLink></div><div v-if="loading" class="demo-note" role="status">正在加载近期内容...</div><div v-else-if="error" class="demo-note">数据加载失败</div><div v-else-if="!recentItems.length" class="demo-note">暂无近期内容</div><div v-if="!loading && !error && recentItems.length" class="recent-list"><RouterLink v-for="item in recentItems" :key="item.id" :to="'/topics/' + item.id" class="recent-card"><div class="recent-icon" :class="'recent-' + item.tone">{{ item.icon }}</div><div class="recent-content"><span class="recent-category">{{ item.category }}</span><h3>{{ item.title }}</h3><p>{{ item.description }}</p></div><span class="recent-arrow" aria-hidden="true">↗</span></RouterLink></div></div>
      <aside class="guide-card" aria-labelledby="guide-title"><div class="section-kicker">YOUR LEARNING PATH <span></span></div><h2 id="guide-title">从这里，开始学习。</h2><p class="guide-intro">不用急着掌握所有内容，先从一个方向开始。</p><div class="learning-steps"><RouterLink v-for="step in learningSteps" :key="step.number" :to="step.to" class="learning-step"><span class="learning-number">{{ step.number }}</span><div><h3>{{ step.title }}</h3><p>{{ step.description }}</p></div><span class="learning-arrow" aria-hidden="true">→</span></RouterLink></div><RouterLink class="guide-button"  :to="firstCategoryPath">开启知识探索 <span aria-hidden="true">↗</span></RouterLink></aside>
    </section>
    <div class="home-footer">持续积累 · 保持好奇 · 不断成长 <span aria-hidden="true">✦</span></div>
  </div>
</template>

<style scoped>
.home-page{color:#192940;font-size:14px;min-width:0}.home-page *{box-sizing:border-box}.home-page a{text-decoration:none}.hero{position:relative;display:grid;grid-template-columns:minmax(0,1.1fr) minmax(0,.9fr);min-height:400px;overflow:hidden;isolation:isolate;border-radius:24px;background:linear-gradient(115deg,#eef5ff 0%,#e8f1ff 46%,#dcecff 100%);padding:54px 48px 46px}.hero:before{content:'';position:absolute;right:-140px;top:-240px;width:580px;height:580px;border-radius:50%;background:radial-gradient(circle,#bbd6ff99 0%,#cfe2ff22 67%,transparent 70%);z-index:-1}.hero-copy{position:relative;z-index:2;min-width:0}.hero-eyebrow,.section-kicker{display:flex;align-items:center;gap:9px;color:#426fba;font-size:11px;font-weight:800;letter-spacing:.12em}.eyebrow-dot{width:8px;height:8px;background:#3674e9;border-radius:50%;box-shadow:0 0 0 4px #3674e91a}.hero h1{font-size:clamp(28px,3.2vw,42px);line-height:1.43;letter-spacing:-1.2px;margin:21px 0 14px;font-weight:800;color:#1a3359}.hero h1 span{color:#2563db}.hero-description{margin:0 0 27px;color:#64748b;line-height:1.85;font-size:14px}.hero-search{display:flex;align-items:center;gap:12px;max-width:555px;min-height:60px;padding:6px 6px 6px 18px;background:#fff;box-shadow:0 12px 34px #3763a51a;border:1px solid #e4ecf9;border-radius:13px}.hero-search svg{height:20px;width:20px;flex:none;color:#8294b0}.hero-search input{min-width:0;flex:1;border:0;outline:0;background:transparent;color:#192940;font:inherit;font-size:13px}.hero-search input::placeholder{color:#9caac0}.hero-search button,.guide-button{border:0;border-radius:9px;background:#2865df;color:#fff;font-weight:700;cursor:pointer;white-space:nowrap}.hero-search button{padding:14px 17px;font-size:13px}.hero-search button span{margin-left:8px}.hero-search button:hover,.guide-button:hover{background:#1754ca}.hot-searches{display:flex;align-items:center;flex-wrap:wrap;gap:9px;margin-top:17px}.hot-searches>span{color:#7789a2;font-size:12px;margin-right:2px}.hot-searches button{padding:6px 11px;color:#54709a;background:#ffffffb3;border:1px solid #dce8fa;border-radius:6px;cursor:pointer;font-size:12px}.hot-searches button:hover{color:#2563db;border-color:#a9c8fb}.hero-visual{position:relative;display:flex;align-items:center;justify-content:center;min-height:300px;padding-left:30px}.visual-orbit{position:absolute;border:1px solid #9ec3ff80;border-radius:50%;transform:rotate(-24deg)}.orbit-one{width:370px;height:370px}.orbit-two{width:470px;height:470px;opacity:.55}.visual-panel{position:relative;z-index:1;width:min(100%,310px);padding:20px 24px 24px;background:#ffffffeb;box-shadow:0 26px 70px #2a5d9d26;border:1px solid #fff;border-radius:17px;transform:rotate(3deg)}.panel-top{display:flex;align-items:center;justify-content:space-between;color:#99a8bd;font-size:9px;font-weight:800;letter-spacing:.13em}.panel-dots{display:flex;gap:4px}.panel-dots i{width:7px;height:7px;border-radius:50%;background:#d2ddec}.panel-dots i:first-child{background:#8eb7fa}.visual-title{font-size:17px;font-weight:800;margin:18px 0 21px;color:#244166}.visual-step{display:flex;align-items:center;gap:12px}.step-symbol{width:37px;height:37px;border-radius:10px;display:grid;place-items:center;color:#2865df;background:#e5efff;font-weight:800;font-size:12px}.step-symbol.second{background:#f2ebff;color:#8957cb}.step-symbol.third{background:#e5f7f2;color:#299c83}.visual-step>div{flex:1;min-width:0}.visual-step strong,.visual-step small{display:block}.visual-step strong{font-size:12px;color:#294360}.visual-step small{margin-top:3px;font-size:10px;color:#99a8bb}.step-check{display:grid;place-items:center;background:#e6f6eb;border-radius:50%;width:21px;height:21px;color:#39a66a;font-size:13px}.step-arrow{color:#b1c0d3}.visual-connector{width:1px;height:22px;background:#dce6f5;margin:4px 0 4px 18px}.floating-label{position:absolute;z-index:2;padding:12px 15px;border:1px solid #fff;border-radius:10px;background:#ffffffef;box-shadow:0 12px 30px #3763a520;color:#536d93;font-size:11px;font-weight:700;white-space:nowrap}.label-top{right:0;top:11%;transform:rotate(7deg)}.label-bottom{left:1%;bottom:12%;transform:rotate(-7deg)}.small-indicator{display:inline-block;width:7px;height:7px;background:#50c99b;border-radius:50%;margin-right:5px}.highlights{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px;padding:26px 12px;border-bottom:1px solid #e7edf5}.highlight{display:flex;gap:13px;align-items:center;justify-content:center}.highlight-icon{display:grid;place-items:center;width:38px;height:38px;flex:none;background:#e9f1ff;border-radius:12px;color:#3977df;font-size:23px}.highlight strong,.highlight small{display:block}.highlight strong{font-weight:750;font-size:13px}.highlight small{margin-top:5px;font-size:11px;color:#91a0b1}.home-section{margin-top:57px}.section-header{display:flex;align-items:flex-end;justify-content:space-between;gap:15px;margin-bottom:24px}.section-kicker span{height:1px;width:25px;background:#93b5ed}.section-header h2,.guide-card h2{font-size:25px;line-height:1.3;letter-spacing:-.6px;margin:11px 0 9px;color:#1e304b}.section-header p{margin:0;color:#8592a4;line-height:1.6;font-size:13px}.section-more{color:#3975d5;font-size:13px;font-weight:650;white-space:nowrap;padding-bottom:4px}.section-more span{margin-left:7px}.section-more:hover{color:#174fbd}.category-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:15px}.category-card{display:flex;flex-direction:column;min-width:0;min-height:220px;padding:22px 19px 19px;background:#fff;border:1px solid #e9edf5;border-radius:15px;box-shadow:0 5px 20px #213d6610;transition:transform .2s,box-shadow .2s,border-color .2s;color:inherit}.category-card:hover{transform:translateY(-4px);box-shadow:0 12px 34px #213d661a;border-color:#c4d9ff}.category-top{display:flex;justify-content:space-between;align-items:flex-start}.category-icon{display:grid;place-items:center;width:46px;height:46px;background:var(--tone-bg);border-radius:12px;font-size:24px}.card-arrow{font-size:19px;color:#b3c0d2}.category-card:hover .card-arrow{color:#2865df}.category-card h3{margin:16px 0 8px;font-size:16px;color:#20314b}.category-card p{margin:0;flex:1;color:#8390a3;font-size:12px;line-height:1.75;display:-webkit-box;overflow:hidden;-webkit-line-clamp:3;-webkit-box-orient:vertical}.category-tags{display:flex;flex-wrap:wrap;gap:6px;margin-top:19px}.category-tags span{background:#f4f7fb;border-radius:5px;padding:5px 8px;color:#788ba5;font-size:10px}.tone-blue{--tone-bg:#e7efff}.tone-purple{--tone-bg:#f0eaff}.tone-orange{--tone-bg:#fff0e5}.tone-green{--tone-bg:#e4f8eb}.tone-pink{--tone-bg:#ffeaf1}.tone-cyan{--tone-bg:#e5f7fa}.tone-amber{--tone-bg:#fff3db}.tone-indigo{--tone-bg:#eceeff}.bottom-section{display:grid;grid-template-columns:minmax(0,1.15fr) minmax(0,.85fr);gap:26px;align-items:start}.bottom-section .section-header{margin-bottom:16px}.demo-note{font-size:11px;color:#97a4b5;margin-bottom:14px}.recent-list{display:grid;gap:11px}.recent-card{display:flex;align-items:center;gap:17px;min-height:102px;padding:17px 18px;background:#fff;border:1px solid #e9edf5;border-radius:13px;transition:border-color .2s,transform .2s}.recent-card:hover{border-color:#b9d1fb;transform:translateX(3px)}.recent-icon{width:53px;height:53px;display:grid;place-items:center;flex:none;border-radius:12px;font-size:13px;font-weight:800}.recent-yellow{background:#fff2d8;color:#ad7600}.recent-blue{background:#e7efff;color:#3973d6}.recent-purple{background:#f0eaff;color:#8258c4}.recent-content{flex:1;min-width:0}.recent-category{font-size:10px;color:#4c7ecf;font-weight:700}.recent-content h3{margin:5px 0;font-size:14px;color:#233853}.recent-content p{font-size:11px;color:#97a4b2;margin:0;line-height:1.6}.recent-arrow{font-size:18px;color:#b6c3d3}.guide-card{padding:28px;background:linear-gradient(145deg,#eaf1ff 0%,#f3f6ff 100%);border:1px solid #e0eafa;border-radius:17px}.guide-card h2{font-size:23px;margin-top:15px}.guide-intro{color:#7c8da5;margin:0 0 25px;font-size:12px;line-height:1.75}.learning-steps{display:grid;gap:18px}.learning-step{display:flex;align-items:center;gap:13px;color:inherit}.learning-number{align-self:flex-start;display:grid;place-items:center;flex:none;width:35px;height:35px;border-radius:9px;background:#fff;color:#3972d4;font-size:11px;font-weight:800;box-shadow:0 3px 10px #437ac00b}.learning-step>div{flex:1}.learning-step h3{font-size:13px;margin:0 0 6px;color:#2b4160}.learning-step p{font-size:11px;margin:0;color:#899bb4;line-height:1.5}.learning-arrow{font-size:16px;color:#9aacc8}.learning-step:hover h3{color:#2563db}.guide-button{display:flex;align-items:center;justify-content:center;gap:13px;margin-top:27px;min-height:44px;font-size:13px}.home-footer{text-align:center;margin:64px 0 18px;color:#a8b2c1;font-size:12px;letter-spacing:.12em}.home-footer span{color:#6997e3;margin-left:8px}
@media(max-width:1000px){.hero{padding:40px 30px;grid-template-columns:1.15fr .85fr}.hero-visual{padding-left:10px}.visual-panel{transform:rotate(2deg);padding:18px}.floating-label{display:none}.category-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.category-card{min-height:205px}.bottom-section{gap:18px}.guide-card{padding:24px}}
@media(max-width:700px){.hero{display:block;min-height:0;padding:34px 24px 32px;border-radius:18px}.hero h1{font-size:clamp(27px,6vw,36px);line-height:1.4;letter-spacing:-.7px;margin-top:18px}.hero-description{font-size:13px;margin-bottom:23px}.hero-visual{display:none}.highlights{gap:10px;padding:21px 0}.highlight{gap:9px;justify-content:flex-start}.highlight-icon{width:30px;height:30px;font-size:17px;border-radius:9px}.highlight strong{font-size:11px}.highlight small{font-size:10px}.home-section{margin-top:42px}.bottom-section{grid-template-columns:1fr}.section-header h2{font-size:23px}.guide-card{margin-top:9px}.home-footer{margin:45px 0 15px}}
@media(max-width:480px){.hero{padding:27px 18px 24px}.hero-eyebrow{font-size:9px;letter-spacing:.06em}.hero h1{font-size:27px}.desktop-break{display:none}.hero-search{padding:5px 5px 5px 12px;gap:8px;min-height:54px}.hero-search svg{width:17px;height:17px}.hero-search input{font-size:12px}.hero-search button{padding:12px 10px;font-size:12px}.hero-search button span{display:none}.hot-searches{gap:7px}.hot-searches>span{flex-basis:100%}.hot-searches button{padding:5px 9px}.highlights{grid-template-columns:1fr;gap:13px;padding:19px 5px}.highlight{gap:12px}.highlight-icon{width:35px;height:35px}.highlight strong{font-size:12px}.highlight small{font-size:11px}.section-header{align-items:flex-end}.section-header h2{font-size:21px}.section-header p{font-size:12px}.section-more{font-size:11px}.category-grid{gap:10px}.category-card{padding:15px 12px;min-height:220px;border-radius:12px}.category-icon{width:40px;height:40px;font-size:21px}.category-card h3{font-size:14px;margin-top:14px}.category-card p{font-size:11px}.category-tags{gap:4px;margin-top:12px}.category-tags span{font-size:9px;padding:4px 5px}.recent-card{padding:13px 12px;gap:11px}.recent-icon{width:43px;height:43px}.recent-content h3{font-size:12px}.recent-content p{font-size:10px}.guide-card{padding:22px 18px}.home-footer{font-size:10px}}
@media(max-width:360px){.category-grid{grid-template-columns:1fr}.category-card{min-height:175px}.hero-search input::placeholder{font-size:10px}}
@media(prefers-reduced-motion:reduce){.category-card,.recent-card{transition:none}}
.home-status{padding:28px 20px;border:1px solid #e9edf5;border-radius:14px;background:#fff;color:#788ba5;text-align:center;font-size:13px}.home-error{color:#b45309}.home-error button{margin-left:12px;padding:7px 12px;border:1px solid #d8e5fa;border-radius:7px;background:#f5f9ff;color:#2865df;cursor:pointer}.category-tags span{overflow-wrap:anywhere}
</style>