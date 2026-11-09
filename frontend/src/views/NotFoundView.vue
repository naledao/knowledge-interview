<script setup lang="ts">
/**
 * 页面用途：当访问的路由不存在时显示提示，并提供返回首页的入口。
 * 当前仅创建路由占位页面，后续再接入后端只读接口和业务组件。
 */
import { useRoute } from 'vue-router'

const route = useRoute()
const currentId = route.params.id
</script>

<template>
  <section class="page-placeholder">
    <h1>404 页面</h1>
    <p>当访问的路由不存在时显示提示，并提供返回首页的入口。</p>
    <p v-if="currentId" class="page-id">当前路由 ID：{{ currentId }}</p>
    <p class="page-note">当前为页面占位内容，业务功能尚未开发。</p>
    <RouterLink to="/" class="back-link">返回首页</RouterLink>
  </section>
</template>