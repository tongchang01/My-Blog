import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import path from 'path'

// https://vitejs.dev/config/
export default ({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  process.env = { ...process.env, ...env }

  return defineConfig({
    css: {
      preprocessorOptions: {
        scss: {
          api: 'modern'
        }
      }
    },
    build: {
      assetsDir: 'static',
      // Mermaid 图表按需加载；其最重的单图表 chunk 压缩后约 155 kB。
      chunkSizeWarningLimit: 700,
      rollupOptions: {
        output: {
          assetFileNames: assetInfo => {
            let extType = assetInfo.name.split('.').at(1)
            if (/png|jpe?g|svg|gif|tiff|bmp|ico/i.test(extType)) {
              extType = 'img'
            }
            return `static/${extType}/[hash][extname]`
          },
          manualChunks: id => {
            if (
              id.includes('/node_modules/vue/') ||
              id.includes('/node_modules/@vue/') ||
              id.includes('/node_modules/pinia/') ||
              id.includes('/node_modules/vue-router/') ||
              id.includes('/node_modules/vue-i18n/')
            ) {
              return 'vendor-vue'
            }
            if (
              id.includes('/node_modules/markdown-it/') ||
              id.includes('/node_modules/@mdit/')
            ) {
              return 'vendor-markdown'
            }
          },
          chunkFileNames: 'static/js/[hash].js',
          entryFileNames: 'static/js/[hash].js'
        },
        plugins: []
      }
    },
    plugins: [
      createSvgIconsPlugin({
        iconDirs: [path.resolve(process.cwd(), 'src/icons')],
        symbolId: 'icon-[dir]-[name]',
        customDomId: '__svg__icons__dom__'
      }),
      vue()
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      },
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    server: {
      proxy: {
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  })
}
