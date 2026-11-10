<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { saveApiKey, verifyApiKey } from '../api/auth'

const route = useRoute()
const router = useRouter()
const apiKey = ref('')
const showKey = ref(false)
const checking = ref(false)
const errorMessage = ref('')

const returnPath = computed(() => {
  const target = route.query.redirect
  if (typeof target !== 'string' || !target.startsWith('/') || target.startsWith('//') || target.split('?')[0] === '/api-key') {
    return '/'
  }
  return target
})

async function submitKey() {
  const key = apiKey.value.trim()
  if (!key || checking.value) return
  checking.value = true
  errorMessage.value = ''
  try {
    await verifyApiKey(key)
    saveApiKey(key)
    apiKey.value = ''
    await router.replace(returnPath.value)
  } catch (error) {
    if (axios.isAxiosError(error)) {
      errorMessage.value = error.response?.status === 401 || error.response?.status === 403
        ? 'API Key 无效或没有访问权限，请检查后重试。'
        : '无法验证 API Key，请确认后端服务和网络连接正常。'
    } else {
      errorMessage.value = '操作失败，请稍后重试。'
    }
  } finally {
    checking.value = false
  }
}
</script>

<template>
  <section class="api-key-page" aria-labelledby="api-key-title">
    <div class="key-card">
      <div class="key-icon" aria-hidden="true">🔐</div>
      <h1 id="api-key-title">设置 API Key</h1>
      <p class="key-description">访问知识分类和主题需要验证 API Key。请输入服务端配置的密钥，验证成功后将返回原页面。</p>
      <form @submit.prevent="submitKey">
        <label for="api-key-input">API Key</label>
        <div class="key-input-row">
          <input id="api-key-input" v-model="apiKey" :type="showKey ? 'text' : 'password'" placeholder="请输入 API Key" autocomplete="off" autocapitalize="off" spellcheck="false" required />
          <button type="button" class="visibility-button" @click="showKey = !showKey">{{ showKey ? '隐藏' : '显示' }}</button>
        </div>
        <p v-if="errorMessage" class="key-error" role="alert">{{ errorMessage }}</p>
        <button class="submit-button" type="submit" :disabled="checking || !apiKey.trim()">{{ checking ? '正在验证…' : '验证并继续' }}</button>
      </form>
      <p class="key-note">密钥仅保存在当前浏览器标签页的会话存储中，不会写入网址或前端构建文件；关闭标签页后需重新输入。请勿在公共设备上使用私人密钥。</p>
    </div>
  </section>
</template>

<style scoped>
.api-key-page { min-height: min(70vh, 650px); display: grid; place-items: center; padding: 24px 0; }
.key-card { width: min(100%, 460px); padding: 36px; border: 1px solid #e2e8f0; border-radius: 20px; background: #fff; box-shadow: 0 16px 48px rgba(15, 23, 42, .06); }
.key-icon { font-size: 32px; margin-bottom: 14px; }
h1 { font-size: 27px; margin: 0 0 12px; color: #0f172a; }
.key-description { color: #475569; line-height: 1.8; margin: 0 0 28px; }
label { display: block; font-weight: 600; color: #334155; margin-bottom: 10px; }
.key-input-row { display: flex; align-items: center; border: 1px solid #cbd5e1; border-radius: 10px; overflow: hidden; }
.key-input-row:focus-within { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, .1); }
input { min-width: 0; flex: 1; border: 0; outline: 0; padding: 13px 14px; font: inherit; }
.visibility-button { border: 0; background: transparent; color: #2563eb; cursor: pointer; padding: 12px; font: inherit; }
.submit-button { width: 100%; margin-top: 20px; padding: 14px; background: #2563eb; color: #fff; border: none; border-radius: 10px; font: inherit; font-weight: 600; cursor: pointer; }
.submit-button:disabled { background: #94a3b8; cursor: not-allowed; }
.key-error { color: #b91c1c; margin: 12px 0 0; line-height: 1.6; }
.key-note { margin: 22px 0 0; color: #64748b; font-size: 13px; line-height: 1.8; }
@media (max-width: 640px) { .api-key-page { min-height: 60vh; padding: 12px 0; } .key-card { padding: 24px 20px; } h1 { font-size: 24px; } }
</style>