import axios from 'axios'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRandomQuestion } from '../api/knowledge'

/**
 * 随机抽题：向后端请求一道题并跳转到详情页。
 * 首页与问题详情页共用，统一处理加载中、失败和题库为空的提示。
 */
export function useRandomQuestion() {
  const router = useRouter()
  const loading = ref(false)
  const errorMessage = ref('')

  /** excludeId 为当前正在看的题，传入后不会连续抽到同一道。 */
  async function goRandom(excludeId?: number) {
    if (loading.value) return
    loading.value = true
    errorMessage.value = ''
    try {
      const { id } = await getRandomQuestion(excludeId)
      await router.push(`/questions/${id}`)
      window.scrollTo({ top: 0 })
    } catch (error) {
      const empty = axios.isAxiosError(error) && error.response?.status === 404
      errorMessage.value = empty ? '暂无题目' : '随机抽题失败，请重试'
      if (!empty) console.error('随机抽题失败', error)
    } finally {
      loading.value = false
    }
  }

  return { loading, errorMessage, goRandom }
}
