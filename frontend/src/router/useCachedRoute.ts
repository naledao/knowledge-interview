import { useRoute, useRouter } from 'vue-router'

/**
 * Android 保活时固定页面创建时的路由快照。
 * 避免其他页面导航触发休眠实例的路由监听，清空数据、分页和展开状态。
 * 普通浏览器仍使用响应式路由。
 */
export function useCachedRoute() {
  const route = useRoute()
  const router = useRouter()
  return navigator.userAgent.includes('KnowledgeInterviewAndroid/')
    ? router.currentRoute.value
    : route
}
