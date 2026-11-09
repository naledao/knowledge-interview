<script setup lang="ts">
/**
 * 页面用途：根据问题 ID 展示题目及答案，后续支持上一题、下一题和答案阅读。
 * 当前仅创建路由占位页面，后续再接入后端只读接口和业务组件。
 */
import { useRoute } from 'vue-router'

const route = useRoute()
const currentId = route.params.id
</script>

<template>
  <section class="page-placeholder">
    <h1>问题详情</h1>
    <p>根据问题 ID 展示题目及答案，后续支持上一题、下一题和答案阅读。</p>
    <p v-if="currentId" class="page-id">当前路由 ID：{{ currentId }}</p>
    <p class="page-note">当前为页面占位内容，业务功能尚未开发。</p>
    <RouterLink to="/" class="back-link">返回首页</RouterLink>
  </section>
</template>