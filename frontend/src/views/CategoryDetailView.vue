<script setup lang="ts">
/**
 * 页面用途：根据分类 ID 展示分类信息及其下属知识主题，供用户选择具体学习方向。
 * 当前仅创建路由占位页面，后续再接入后端只读接口和业务组件。
 */
import { useRoute } from 'vue-router'

const route = useRoute()
const currentId = route.params.id
</script>

<template>
  <section class="page-placeholder">
    <h1>分类详情</h1>
    <p>根据分类 ID 展示分类信息及其下属知识主题，供用户选择具体学习方向。</p>
    <p v-if="currentId" class="page-id">当前路由 ID：{{ currentId }}</p>
    <p class="page-note">当前为页面占位内容，业务功能尚未开发。</p>
    <RouterLink to="/" class="back-link">返回首页</RouterLink>
  </section>
</template>