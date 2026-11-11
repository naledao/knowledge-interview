<script setup lang="ts">
/** 统一的分页导航组件，由服务端返回页数并驱动翻页。 */
defineProps<{ pageNum: number; pages: number; loading?: boolean }>()
defineEmits<{ change: [page: number] }>()
</script>

<template>
  <nav v-if="pages > 1" class="list-pagination" aria-label="列表分页">
    <button type="button" :disabled="loading || pageNum <= 1" @click="$emit('change', pageNum - 1)">← 上一页</button>
    <span>第 {{ pageNum }} / {{ pages }} 页</span>
    <button type="button" :disabled="loading || pageNum >= pages" @click="$emit('change', pageNum + 1)">下一页 →</button>
  </nav>
</template>

<style scoped>
.list-pagination { display: flex; justify-content: center; align-items: center; flex-wrap: wrap; gap: 16px; margin: 28px 0 6px; font-size: 14px; color: #475569; }
.list-pagination button { padding: 9px 16px; border: 1px solid #dbe3ee; border-radius: 9px; background: #fff; color: #1d4ed8; font: inherit; cursor: pointer; }
.list-pagination button:hover:not(:disabled) { background: #eff6ff; border-color: #93c5fd; }
.list-pagination button:disabled { opacity: .45; cursor: not-allowed; }
</style>