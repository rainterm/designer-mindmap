import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5178,
    proxy: {
      // 代理到脚手架后端，保持同源（后端未开启 CORS，浏览器直连跨域会被拦）
      '/admin-api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
