import axios, { type AxiosError } from 'axios'
import router from '../router'
import { clearApiKey, getApiKey } from './auth'

export interface KnowledgeCategory {
  id: number
  name: string
  description: string | null
  sortOrder: number
  createdAt: string | null
  updatedAt: string | null
}

export interface KnowledgeTopic {
  id: number
  categoryId: number
  title: string
  description: string | null
  sortOrder: number
  createdAt: string | null
  updatedAt: string | null
}

/** 问题数据结构与后端 KnowledgeQuestion 领域模型保持一致。 */
export interface KnowledgeQuestion {
  id: number
  topicId: number
  questionText: string
  answerText: string | null
  sortOrder: number
  createdAt: string | null
  updatedAt: string | null
}

const client = axios.create({ baseURL: '/api', timeout: 10000 })

client.interceptors.request.use((config) => {
  const key = getApiKey()
  if (key) config.headers.set('Authorization', `Bearer ${key}`)
  return config
})

let redirectingToKey = false
client.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      clearApiKey()
      if (!redirectingToKey && router.currentRoute.value.name !== 'api-key') {
        redirectingToKey = true
        const redirect = router.currentRoute.value.fullPath
        void router.replace({ name: 'api-key', query: { redirect } }).finally(() => {
          redirectingToKey = false
        })
      }
    }
    return Promise.reject(error)
  },
)

export async function getCategories(): Promise<KnowledgeCategory[]> {
  const { data } = await client.get<KnowledgeCategory[]>('/categories')
  return data
}

export async function getTopics(categoryId?: number): Promise<KnowledgeTopic[]> {
  const { data } = await client.get<KnowledgeTopic[]>('/topics', {
    params: categoryId === undefined ? undefined : { categoryId },
  })
  return data
}
export type SearchKind = 'category' | 'topic' | 'question'
export type SearchType = SearchKind | 'all'

/** 与后端 KnowledgeSearchResult.Hit 保持一致；excerpt 已在服务端截断。 */
export interface SearchHit {
  kind: SearchKind
  id: number
  title: string
  excerpt: string
  breadcrumb: string
  answerMatch: boolean
  score: number
}

/** 与后端 KnowledgeSearchResult 保持一致；page 为服务端修正后的实际页码。 */
export interface SearchResponse {
  keyword: string
  terms: string[]
  type: SearchType
  page: number
  size: number
  total: number
  totalPages: number
  counts: Record<SearchType, number>
  truncated: boolean
  items: SearchHit[]
}

export interface SearchParams {
  q: string
  type?: SearchType
  page?: number
  size?: number
}

/** 服务端搜索：匹配、排序和分页均由后端完成；传入 signal 以便取消过期请求。 */
export async function searchKnowledge(params: SearchParams, signal?: AbortSignal): Promise<SearchResponse> {
  const { data } = await client.get<SearchResponse>('/search', { params, signal })
  return data
}

/** 后端 400 响应为 ProblemDetail，detail 字段可直接展示。 */
export function errorDetail(error: unknown): string | undefined {
  if (!axios.isAxiosError(error)) return undefined
  const detail = (error.response?.data as { detail?: unknown } | undefined)?.detail
  return typeof detail === 'string' ? detail : undefined
}

/** 按主题获取问题列表；未传主题 ID 时返回全部问题。 */
export async function getQuestions(topicId?: number): Promise<KnowledgeQuestion[]> {
  const { data } = await client.get<KnowledgeQuestion[]>('/questions', {
    params: topicId === undefined ? undefined : { topicId },
  })
  return data
}