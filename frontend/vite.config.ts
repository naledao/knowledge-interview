import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// 开发和生产环境都由浏览器输入 API Key；代理不再覆盖 Authorization 请求头。
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})