import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const apiProxyTarget = loadEnv(mode, '.', '').API_PROXY_TARGET || 'http://localhost:8080'
  const proxy = {
    '/api': {
      target: apiProxyTarget,
      changeOrigin: true,
    },
    '/oauth2': {
      target: apiProxyTarget,
      changeOrigin: true,
    },
    '/login/oauth2': {
      target: apiProxyTarget,
      changeOrigin: true,
    },
  }

  return {
    plugins: [react(), tailwindcss()],
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy,
    },
    preview: {
      host: '0.0.0.0',
      port: 5173,
      proxy,
    },
  }
})
