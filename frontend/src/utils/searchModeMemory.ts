import { ref } from 'vue'
import type { SearchMode } from '../api/knowledge'

/**
 * 记住用户当前选择的搜索模式（状态 A），供顶部搜索框和搜索页共用。
 * 响应式变量保证两处即时同步，sessionStorage 保证刷新后仍可恢复。
 */
const STORAGE_KEY = 'search:mode'
const MODES: SearchMode[] = ['keyword', 'exact']

function storage(): Storage | null {
  try {
    return window.sessionStorage
  } catch {
    return null
  }
}

function readStored(): SearchMode {
  try {
    const raw = storage()?.getItem(STORAGE_KEY)
    return MODES.includes(raw as SearchMode) ? (raw as SearchMode) : 'keyword'
  } catch {
    return 'keyword'
  }
}

/** 当前记住的搜索模式；默认关键词搜索。 */
export const rememberedSearchMode = ref<SearchMode>(readStored())

/** 记住搜索模式；关键词为默认值，直接清除存储即可。 */
export function rememberSearchMode(mode: SearchMode): void {
  rememberedSearchMode.value = mode
  try {
    if (mode === 'keyword') storage()?.removeItem(STORAGE_KEY)
    else storage()?.setItem(STORAGE_KEY, mode)
  } catch {
    // 存储不可用时忽略，刷新后回退到关键词搜索。
  }
}
