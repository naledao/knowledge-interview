import { resolve } from 'node:path'
import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  // API Key 仅在开发服务器读取，不传给浏览器或前端构建产物。
  const rootEnv = loadEnv(mode, resolve(__dirname, '..'), 'MCP_')
  const backendEnv = loadEnv(mode, resolve(__dirname, '../backend'), 'MCP_')
  const apiKey = process.env.MCP_API_KEY || backendEnv.MCP_API_KEY || rootEnv.MCP_API_KEY

  return {
    plugins: [vue()],
    server: {
      proxy: {
        '/api': {
          target: process.env.VITE_BACKEND_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: true,
          ...(apiKey ? { headers: { Authorization: `Bearer ${apiKey}` } } : {}),
        },
      },
    },
  }
})