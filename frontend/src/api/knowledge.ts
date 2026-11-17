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

/** 列表接口统一返回数据库分页结果，每页最多6条。 */
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  hasNext: boolean
  items: T[]
}

export async function getCategories(pageNum = 1): Promise<PageResult<KnowledgeCategory>> {
  const { data } = await client.get<PageResult<KnowledgeCategory>>('/categories', { params: { pageNum, pageSize: 6 } })
  return data
}

export async function getCategory(id: number): Promise<KnowledgeCategory> {
  const { data } = await client.get<KnowledgeCategory>(`/categories/${id}`)
  return data
}

export async function getTopics(categoryId?: number, pageNum = 1): Promise<PageResult<KnowledgeTopic>> {
  const { data } = await client.get<PageResult<KnowledgeTopic>>('/topics', { params: { categoryId, pageNum, pageSize: 6 } })
  return data
}

export async function getTopic(id: number): Promise<KnowledgeTopic> {
  const { data } = await client.get<KnowledgeTopic>(`/topics/${id}`)
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

/** 获取热门搜索词（按搜索次数排序）；没有数据时返回空数组。 */
export async function fetchHotKeywords(limit = 5): Promise<string[]> {
  const { data } = await client.get<string[]>('/search/hot-keywords', { params: { limit } })
  return data
}

/** 后端 400 响应为 ProblemDetail，detail 字段可直接展示。 */
export function errorDetail(error: unknown): string | undefined {
  if (!axios.isAxiosError(error)) return undefined
  const detail = (error.response?.data as { detail?: unknown } | undefined)?.detail
  return typeof detail === 'string' ? detail : undefined
}

/** 按主题分页获取问题列表，避免一次性传输所有参考答案。 */
export async function getQuestions(topicId?: number, pageNum = 1): Promise<PageResult<KnowledgeQuestion>> {
  const { data } = await client.get<PageResult<KnowledgeQuestion>>('/questions', {
    params: { topicId, pageNum, pageSize: 6 },
  })
  return data
}

export async function getQuestion(id: number): Promise<KnowledgeQuestion> {
  const { data } = await client.get<KnowledgeQuestion>(`/questions/${id}`)
  return data
}
/** 相邻题仅返回 ID 和标题，不加载完整答案。 */
export interface QuestionLink {
  id: number
  questionText: string
}

export interface QuestionNeighbors {
  previous: QuestionLink | null
  next: QuestionLink | null
}

export async function getQuestionNeighbors(id: number): Promise<QuestionNeighbors> {
  const { data } = await client.get<QuestionNeighbors>(`/questions/${id}/neighbors`)
  return data
}

/** 随机抽题结果：只含主键，前端据此跳转到问题详情页。 */
export interface RandomQuestion {
  id: number
}

/** 随机抽取一道题；excludeId 用于避免连续抽到同一道，题库为空时后端返回 404。 */
export async function getRandomQuestion(excludeId?: number): Promise<RandomQuestion> {
  const { data } = await client.get<RandomQuestion>('/questions/random', { params: excludeId ? { excludeId } : undefined })
  return data
}