import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { syncBrandMark } from './plugins/syncBrandMark.js'
import { copyVditorAssetsPlugin } from './plugins/copyVditorAssets.js'

export default defineConfig({
  plugins: [vue(), syncBrandMark(), copyVditorAssetsPlugin()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      // 后端接口（含文件下载、SSE）
      '/admin-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 0,
        proxyTimeout: 0
      },
      // 公开前台接口（文件公开访问等）
      '/app-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 0,
        proxyTimeout: 0
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1600
  }
})
