<script setup lang="ts">
/**
 * 页面用途：根据用户输入的关键词搜索知识分类、主题、问题及答案，后续接入搜索接口。
 * 当前仅创建路由占位页面，后续再接入后端只读接口和业务组件。
 */
import { useRoute } from 'vue-router'

const route = useRoute()
const currentId = route.params.id
</script>

<template>
  <section class="page-placeholder">
    <h1>搜索结果</h1>
    <p>根据用户输入的关键词搜索知识分类、主题、问题及答案，后续接入搜索接口。</p>
    <p v-if="currentId" class="page-id">当前路由 ID：{{ currentId }}</p>
    <p class="page-note">当前为页面占位内容，业务功能尚未开发。</p>
    <RouterLink to="/" class="back-link">返回首页</RouterLink>
  </section>
</template>