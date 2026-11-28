<script setup lang="ts">
import { LockKeyhole, Eye, EyeOff, ShieldCheck, ArrowRight, Info, AlertCircle, LoaderCircle } from '@lucide/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCachedRoute } from '../router/useCachedRoute'
import axios from 'axios'
import { saveApiKey, verifyApiKey } from '../api/auth'

const route = useCachedRoute()
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
      <div class="key-icon" aria-hidden="true"><LockKeyhole :size="29" :stroke-width="1.8" /></div>
      <h1 id="api-key-title">设置 API Key</h1>
      <p class="key-description">访问知识分类和主题需要验证 API Key。请输入服务端配置的密钥，验证成功后将返回原页面。</p>
      <form @submit.prevent="submitKey">
        <label for="api-key-input">API Key</label>
        <div class="key-input-row">
          <input id="api-key-input" v-model="apiKey" :type="showKey ? 'text' : 'password'" placeholder="请输入 API Key" autocomplete="off" autocapitalize="off" spellcheck="false" required />
          <button type="button" class="visibility-button" :aria-label="showKey ? '隐藏密钥' : '显示密钥'" :aria-pressed="showKey" @click="showKey = !showKey"><EyeOff v-if="showKey" :size="17" aria-hidden="true" /><Eye v-else :size="17" aria-hidden="true" />{{ showKey ? '隐藏' : '显示' }}</button>
        </div>
        <p v-if="errorMessage" class="key-error" role="alert"><AlertCircle :size="17" aria-hidden="true" />{{ errorMessage }}</p>
        <button class="submit-button" type="submit" :disabled="checking || !apiKey.trim()"><LoaderCircle v-if="checking" :size="18" class="key-spinner" aria-hidden="true" /><ShieldCheck v-else :size="18" aria-hidden="true" />{{ checking ? '正在验证…' : '验证并继续' }}<ArrowRight v-if="!checking" :size="17" aria-hidden="true" /></button>
      </form>
      <p class="key-note"><Info :size="17" aria-hidden="true" /><span>密钥仅保存在当前浏览器标签页的会话存储中，不会写入网址或前端构建文件；关闭标签页后需重新输入。请勿在公共设备上使用私人密钥。</span></p>
    </div>
  </section>
</template>

<style scoped>
.api-key-page { min-height: min(62vh, 520px); display: grid; place-items: center; padding: 12px 0; background: radial-gradient(circle at 50% 15%, #eaf1ff 0%, transparent 58%); }
.key-card { width: min(100%, 440px); padding: 24px 28px; border: 1px solid #dce7f5; border-radius: 16px; background: #fff; box-shadow: 0 16px 44px #1e40af0c, 0 2px 8px #1e293b08; }
.key-icon { display: grid; width: 44px; height: 44px; place-items: center; margin-bottom: 12px; border: 1px solid #cfddff; border-radius: 12px; background: linear-gradient(135deg, #eaf2ff, #f0edff); color: #365fcd; }
.key-icon svg { width: 22px; height: 22px; }
h1 { font-size: 22px; margin: 0 0 8px; color: #0f172a; }
.key-description { color: #475569; font-size: 14px; line-height: 1.65; margin: 0 0 18px; }
label { display: block; font-size: 14px; font-weight: 600; color: #334155; margin-bottom: 6px; }
.key-input-row { display: flex; align-items: center; border: 1px solid #cbd5e1; border-radius: 10px; overflow: hidden; background: #fafdff; transition: border-color .2s, box-shadow .2s; }
.key-input-row:focus-within { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, .1); }
.key-input-row input { min-width: 0; flex: 1; border: 0; outline: 0; padding: 10px 12px; background: transparent; font: inherit; font-size: 14px; }
.visibility-button { display: inline-flex; align-items: center; gap: 5px; min-height: 36px; border: 0; background: transparent; color: #2563eb; cursor: pointer; padding: 8px 12px; font: inherit; font-size: 13px; white-space: nowrap; }
.visibility-button:hover { background: #eef4ff; }
.submit-button { display: inline-flex; align-items: center; justify-content: center; gap: 8px; width: 100%; min-height: 44px; margin-top: 14px; padding: 10px 14px; background: #2563eb; color: #fff; border: none; border-radius: 10px; font: inherit; font-size: 14px; font-weight: 600; cursor: pointer; transition: background .18s, box-shadow .18s; }
.submit-button:not(:disabled):hover { background: #1d4ed8; box-shadow: 0 6px 18px #2563eb2c; }
.submit-button:disabled { background: #94a3b8; cursor: not-allowed; }
.key-error { display: flex; align-items: flex-start; gap: 7px; color: #b91c1c; margin: 10px 0 0; font-size: 13px; line-height: 1.55; }
.key-error svg { flex-shrink: 0; margin-top: 2px; }
.key-note { display: flex; gap: 8px; margin: 14px 0 0; padding: 10px 12px; border-radius: 10px; background: #f4f7fc; color: #64748b; font-size: 12px; line-height: 1.65; }
.key-note svg { flex-shrink: 0; margin-top: 2px; color: #647da2; }
.key-spinner { animation: key-spin 1s linear infinite; }
@keyframes key-spin { to { transform: rotate(360deg); } }
@media (max-width: 640px) {
  .api-key-page { min-height: auto; padding: 6px 0; }
  .key-card { padding: 18px 16px; }
  h1 { font-size: 20px; }
}
@media (prefers-reduced-motion: reduce) { .key-spinner { animation: none; } }
</style>