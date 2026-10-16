import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')

  return {
    plugins: [react(), tailwindcss()],
    build: {
      // The Three.js renderer is lazy-loaded only by the home-page scene.
      chunkSizeWarningLimit: 800,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules/react/') || id.includes('node_modules/react-dom/') || id.includes('node_modules/react-router')) return 'react-vendor'
            if (id.includes('node_modules/lucide-react/')) return 'ui-vendor'
            if (id.includes('node_modules/axios/')) return 'http-vendor'
            if (id.includes('node_modules/three/')) return 'three-core'
            if (id.includes('node_modules/@react-three/fiber/')) return 'three-fiber'
            if (id.includes('node_modules/@react-three/drei/')) return 'three-drei'
          },
        },
      },
    },
    server: {
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
