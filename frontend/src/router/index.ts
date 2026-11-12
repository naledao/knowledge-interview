import { createRouter, createWebHistory } from 'vue-router'
/** Android WebView 中按完整路由地址记录页面滚动位置。 */
const isNativeAndroid = typeof navigator !== 'undefined' && navigator.userAgent.includes('KnowledgeInterviewAndroid/')
const scrollPositions = new Map<string, { left: number; top: number }>()

/**
 * PC 和移动端共用同一套路由，由页面组件负责响应式布局。
 * 分类、主题、问题使用 ID 参数定位具体内容。
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: isNativeAndroid
    ? (to, _from, savedPosition) => scrollPositions.get(to.fullPath) ?? savedPosition ?? { left: 0, top: 0 }
    : undefined,
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '首页' } },
    { path: '/categories/:id', name: 'category-detail', component: () => import('../views/CategoryDetailView.vue'), meta: { title: '分类详情' } },
    { path: '/topics/:id', name: 'topic-detail', component: () => import('../views/TopicDetailView.vue'), meta: { title: '主题详情' } },
    { path: '/questions/:id', name: 'question-detail', component: () => import('../views/QuestionDetailView.vue'), meta: { title: '问题详情' } },
    { path: '/search', name: 'search', component: () => import('../views/SearchView.vue'), meta: { title: '搜索结果' } },
    { path: '/api-key', name: 'api-key', component: () => import('../views/ApiKeyView.vue'), meta: { title: 'API Key 验证' } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFoundView.vue'), meta: { title: '页面不存在' } },
  ],
})

if (isNativeAndroid) {
  router.beforeEach((_to, from) => {
    // 离开页面前保存位置，返回已缓存页面时恢复阅读进度。
    if (from.name != null) {
      scrollPositions.set(from.fullPath, { left: window.scrollX, top: window.scrollY })
    }
  })
}

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '知识题库')} - 知识题库`
})

export default router