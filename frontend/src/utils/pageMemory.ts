/**
 * 记住列表页当前页码，供整页刷新后恢复。
 * 内存缓存保证同一页面实例内即时生效，sessionStorage 保证刷新后仍可读取。
 */
const memory = new Map<string, number>()

function storage(): Storage | null {
  try {
    return window.sessionStorage
  } catch {
    return null
  }
}

function storageKey(key: string): string {
  return `page:${key}`
}

export function readRememberedPage(key: string): number {
  const cached = memory.get(key)
  if (cached != null) return cached
  try {
    const raw = storage()?.getItem(storageKey(key))
    const page = raw ? Number.parseInt(raw, 10) : 1
    return Number.isSafeInteger(page) && page > 0 ? page : 1
  } catch {
    return 1
  }
}

export function rememberPage(key: string, page: number): void {
  const safe = Number.isSafeInteger(page) && page > 0 ? page : 1
  memory.set(key, safe)
  try {
    if (safe <= 1) storage()?.removeItem(storageKey(key))
    else storage()?.setItem(storageKey(key), String(safe))
  } catch {
    // 存储不可用时忽略，刷新后回退到第一页。
  }
}