import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  define: {
    __VUE_OPTIONS_API__: 'true',
    __VUE_PROD_DEVTOOLS__: 'false'
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    target: 'es2020',
    cssCodeSplit: true,
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replace(/\\/g, '/')
          if (!normalizedId.includes('node_modules')) return
          if (
            normalizedId.includes('/vue/') ||
            normalizedId.includes('/@vue/') ||
            normalizedId.includes('/vue-router/') ||
            normalizedId.includes('/pinia/')
          ) {
            return 'vue-runtime'
          }
          if (normalizedId.includes('/element-plus/')) {
            return normalizedId.includes('/@element-plus/icons-vue/') ? 'element-icons' : 'element-plus'
          }
          if (normalizedId.includes('/axios/')) {
            return 'net-runtime'
          }
          return 'vendor'
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
        // 去掉 /ws 前缀，转发为 /api/v1/ws
        rewrite: (path) => path.replace(/^\/ws/, '')
      },
      // 兼容直接访问 /api/v1/ws（部分场景可能未走 /ws 前缀）
      '/api/v1/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
