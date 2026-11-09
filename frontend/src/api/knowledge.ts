import axios from 'axios'

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

const client = axios.create({ baseURL: '/api', timeout: 10000 })

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