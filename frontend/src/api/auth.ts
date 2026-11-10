import axios from 'axios'

const STORAGE_KEY = 'knowledge-interview-api-key'

export function getApiKey(): string {
  return sessionStorage.getItem(STORAGE_KEY) || ''
}

export function saveApiKey(key: string): void {
  sessionStorage.setItem(STORAGE_KEY, key.trim())
}

export function clearApiKey(): void {
  sessionStorage.removeItem(STORAGE_KEY)
}

/** 独立验证，不触发业务接口的 401 自动跳转拦截器。 */
export async function verifyApiKey(key: string): Promise<void> {
  await axios.get('/api/categories', {
    headers: { Authorization: `Bearer ${key}` },
    timeout: 10000,
  })
}