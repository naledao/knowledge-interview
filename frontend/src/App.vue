<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'

/**
 * 全站公共布局：提供基础导航及路由内容出口。
 * 当前仅做响应式占位，后续可扩展桌面侧边栏和移动端导航。
 */
</script>

<template>
  <div class="app-shell">
    <header class="site-header">
      <RouterLink to="/" class="brand">知识题库</RouterLink>
      <nav class="site-nav" aria-label="主导航">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/search">搜索</RouterLink>
      </nav>
    </header>
    <main class="site-main">
      <RouterView />
    </main>
  </div>
</template>