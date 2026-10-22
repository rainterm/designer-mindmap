import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { syncBrandMark } from './plugins/syncBrandMark.js'
import { compressAssets, minifyHtml } from './plugins/compressAssets.js'

/**
 * 滑块验证码的共享源码目录（见 designer-ui/shared/captcha）。
 *
 * <p>共享走源码 + alias，而不是建 npm workspace 包：前端
 * `pnpm install --frozen-lockfile` 独立构建（见 deploy 下的 Dockerfile），
 * 引入 workspace 会改变 node_modules 布局与锁文件，收益却只是省下这一行 alias。</p>
 */
const sharedCaptchaDir = fileURLToPath(new URL('../shared/captcha', import.meta.url))

export default defineConfig({
  plugins: [vue(), syncBrandMark(), minifyHtml(), compressAssets()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@captcha': sharedCaptchaDir
    },
    /**
     * 共享目录在项目根之外，从那里解析不到本项目的 node_modules
     * （`../shared/` 往上找只有 designer-ui/ 与仓库根，都没有 node_modules）。
     * dedupe 会把这些包的解析基准换成项目根（实测 Vite 内部是 `basedir = root`），
     * 于是共享源码能拿到本项目的 vue / crypto-js —— 顺带也保证了只有一份 Vue 实例，
     * 这比手写 alias 更不容易写错（alias 还得处理 `crypto-js/aes.js` 这类子路径）。
     */
    dedupe: ['vue', 'crypto-js']
  },
  optimizeDeps: {
    /**
     * crypto-js 没有 `exports` 字段，只有子模块；共享的 sliderCaptcha.js 引的是
     * `crypto-js/aes.js` 这类子路径。Vite 要运行到才会发现它们并重新预打包，
     * 表现为「第一次拖验证码时页面整页刷新一次」。预先声明掉。
     */
    include: [
      'crypto-js/aes.js',
      'crypto-js/enc-utf8.js',
      'crypto-js/mode-ecb.js',
      'crypto-js/pad-pkcs7.js'
    ]
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    fs: {
      /**
       * Vite 默认只放行 workspace root（这里就是项目根），而共享目录是它的**兄弟**目录，
       * dev 下会被 fs 检查拦成 403。显式加入即可；生产构建走 Rollup，不经过这道检查。
       */
      allow: [fileURLToPath(new URL('.', import.meta.url)), sharedCaptchaDir]
    },
    proxy: {
      // 后端接口（含文件下载、SSE）
      '/admin-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 0,
        proxyTimeout: 0
      },
      // 公开前台接口（文件公开访问、滑块验证码等）
      '/app-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 0,
        proxyTimeout: 0
      },
      // 第三方开放接口（含令牌端点 /open-api/oauth2/token），与生产 nginx 路径一致
      '/open-api': {
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
    /**
     * 生产产物必须压缩：Vite 默认用 Oxc 压 JS、用 Lightning CSS 压 CSS
     * （去掉换行与多余空格）。这里显式写出来，避免以后有人改成 false ——
     * 关掉后产物体积会直接翻倍。
     *
     * <p>`.gz` 由 `plugins/compressAssets.js` 在构建末尾生成，
     * 生产由 nginx 的 `gzip_static` 直接下发（见 deploy/nginx/default.conf）。</p>
     */
    minify: 'oxc',
    cssMinify: true,
    chunkSizeWarningLimit: 1600
  }
})
