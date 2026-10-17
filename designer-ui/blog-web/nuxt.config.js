import { fileURLToPath } from 'node:url'
import { syncBrandMark } from './scripts/syncBrandMark.js'
import { syncThemeTokens } from './scripts/syncThemeTokens.js'

// 构建期产物（tokens.css / favicon.svg）必须早于 Vite、Nitro 落盘。
// nuxt.config 是启动链路上最早求值的一环，放这里最稳；否则 index.css 的
// @import 可能解析到一个还不存在的文件。
syncThemeTokens()
syncBrandMark()

/**
 * 与 admin-web 共用的滑块验证码源码目录（见 designer-ui/shared/captcha）。
 *
 * <p>共享走源码 + alias，而不是建 npm workspace 包：两个前端各自
 * `pnpm install --frozen-lockfile` 独立构建（见 deploy 下的 Dockerfile），
 * 引入 workspace 会改变两者的 node_modules 布局与锁文件，收益却只是省下这一行 alias。</p>
 */
const sharedCaptchaDir = fileURLToPath(new URL('../shared/captcha', import.meta.url))

/**
 * 后端的内部地址：SSR 取数直连它，dev 下的 `/app-api` 代理也转发到它。
 *
 * <p>只在这一处读环境变量，然后同时喂给 `runtimeConfig` 与 `nitro.devProxy` ——
 * 否则「SSR 取数」和「浏览器取数」可能指向两个不同的后端，
 * 症状是页面能打开但评论/验证码静默失败，很难查。</p>
 */
const apiInternalBase = (process.env.NUXT_API_INTERNAL_BASE || 'http://localhost:8080').replace(
  /\/+$/,
  ''
)

export default defineNuxtConfig({
  compatibilityDate: '2026-10-14',
  devtools: { enabled: false },

  runtimeConfig: {
    /**
     * 仅服务端可见：SSR 期间直连后端，绕过 nginx。
     * 浏览器侧始终用相对路径 `/app-api`（生产由 nginx 反代，dev 由下面的 devProxy 转发），
     * 所以公开配置里没有它。
     */
    apiInternalBase,
    public: {
      /** 站点兜底名称：后台「站点设置」未下发时用于浏览器标题 */
      appTitle: 'Designer Blog',
      /**
       * 站点对外地址，供 sitemap.xml 绝对链接使用。
       *
       * <p>留空即按请求的 Host / X-Forwarded-Proto 推导（nginx 已透传），
       * 正常部署不需要配置 —— 改域名只改 nginx 的 server_name 即可。
       * 只有在「站点前面又套了一层 CDN，且回源 Host 不是站点域名」时才需要显式指定。</p>
       */
      siteUrl: ''
    }
  },

  css: ['~/assets/styles/index.css'],

  /**
   * 共享模块在项目根之外，必须显式给 alias：Nuxt 默认只按 srcDir 解析 `~` / `@`。
   * 组件与协议工具都从这一个入口进，两个前端因此共用同一份实现。
   */
  alias: {
    '@captcha': sharedCaptchaDir
  },

  vite: {
    resolve: {
      /**
       * 共享目录在项目根之外，从那里解析不到本项目的 node_modules
       * （`../shared/` 往上只有 designer-ui/ 与仓库根，都没有 node_modules）。
       * dedupe 会把这些包的解析基准换成项目根（Vite 内部是 `basedir = root`）。
       *
       * 别因为「现在能构建」就删掉：构建期 importer 恰好不是绝对路径时会退化成
       * `basedir = root` 而侥幸命中，dev 下 importer 是绝对路径就会解析失败 ——
       * 这种「只在 dev 挂」的差异最难查。
       */
      dedupe: ['vue', 'crypto-js']
    },
    server: {
      fs: {
        /**
         * Vite 默认只放行 workspace root（这里就是项目根），而共享目录是它的**兄弟**目录，
         * dev 下会被 fs 检查拦成 403。显式加入即可；生产构建走 Rollup，不经过这道检查。
         */
        allow: [fileURLToPath(new URL('.', import.meta.url)), sharedCaptchaDir]
      }
    },
    optimizeDeps: {
      /**
       * 这些依赖只在运行时才被 Vite 发现（naive-ui 走 plugin、md-editor-v3 走文章页、
       * crypto-js 是子路径 CJS）。每发现一批就会重新预打包并强制整页 reload —— 如果
       * reload 撞上正在加载的动态模块，页面会直接报
       * "Failed to fetch dynamically imported module" 白屏。
       * 全部预先声明，让冷启动只做一轮预打包。
       *
       * 注意：crypto-js 只引 AES + UTF8 + ECB + PKCS7 四个子模块，不进整包
       * （见 shared/captcha/sliderCaptcha.js）。
       */
      include: [
        'crypto-js/aes.js',
        'crypto-js/enc-utf8.js',
        'crypto-js/mode-ecb.js',
        'crypto-js/pad-pkcs7.js',
        'naive-ui',
        'md-editor-v3'
      ]
    }
  },

  app: {
    head: {
      htmlAttrs: { lang: 'zh-CN' },
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1.0' }
      ],
      link: [
        // favicon 不写死在这里：它要跟随站点 Logo，由 app.vue / error.vue 的
        // useHead（见 composables/useFavicon.js）统一渲染。两处都写会让 head 里
        // 出现两个 rel="icon"，浏览器挑哪个不确定，刷新时图标就会时而对、时而错。
        // 字体走 Google Fonts：SSR 首屏同样要等它，所以放 head 里预连接
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        {
          rel: 'stylesheet',
          href: 'https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500&family=Manrope:wght@400;500;600;700&display=swap'
        }
      ]
    }
  },

  hooks: {
    /** theme.js 一改，tokens.css / favicon.svg 立即重建（dev 下不必重启） */
    'builder:watch'(_event, path) {
      if (String(path).replaceAll('\\', '/').endsWith('app/config/theme.js')) {
        syncThemeTokens()
        syncBrandMark()
      }
    }
  },

  nitro: {
    // 产出 .output/server/index.mjs，Docker 里由 node 直接拉起
    preset: 'node-server',

    /**
     * dev 下的 `/app-api` 代理。
     *
     * <p>生产环境由 nginx 反代 `/app-api`，dev 没有 nginx，所以必须自己转一层。
     * 少了它，浏览器侧的所有请求（客户端导航取数、评论、验证码、访问埋点）
     * 都会直接 404 —— 而 SSR 直连后端不受影响，于是症状是「直接打开页面正常、
     * 点链接进去就报错」，很容易被误判成前端 bug。</p>
     *
     * <p>与 admin-web 的 Vite `server.proxy` 是同一件事，只是 Nuxt 走 Nitro。
     * 只代理 `/app-api`：博客站点不暴露 `/admin-api`（nginx 里也是显式 404）。
     * 唯一的例外是下面的图片下载路径，见那处注释。</p>
     */
    devProxy: {
      '/app-api': {
        /**
         * target 必须带上 `/app-api` 这一段。
         *
         * <p>h3 在挂载路由时会**剥掉**匹配到的前缀（`/app-api/blog/...` 到代理手里
         * 只剩 `/blog/...`），而 httpxy 默认把 target 的 pathname 前置回来
         * （`prependPath` 默认为 true）。两件事叠加，target 里写全路径才是对的；
         * 只写到 host 的话，后端收到的是 `/blog/public/posts`，会当成未知路由 500。</p>
         */
        target: `${apiInternalBase}/app-api`,
        changeOrigin: true
      },
      /**
       * 滑块验证码：AJ-Captcha 插件的端点挂在根路径 `/captcha/**`（不在 `/app-api` 下），
       * 生产环境由 nginx 单独反代一次，dev 这里同样得补一条。
       *
       * <p>target 带 `/captcha` 的原因与上面 `/app-api` 完全相同（h3 剥前缀 +
       * httpxy 前置 target 的 pathname），漏了就会得到 `/get`、`/check` 这样的后端未知路由。</p>
       */
      '/captcha': {
        target: `${apiInternalBase}/captcha`,
        changeOrigin: true
      },
      /**
       * 文章封面 / 正文图片。
       *
       * <p>存储模块下发的 URL 是 `/admin-api/system/file/download/...`（见 app.yml 的
       * `designer.storage.default-url-prefix`），而博客域名刻意不暴露 `/admin-api`。
       * 于是封面在 dev 与生产都取不到 —— 图片请求落到 SSR 路由上被当成页面，返回 404。</p>
       *
       * <p>这里**只放行下载这一个子路径**，而不是把 `/admin-api` 整个代理进来：
       * `FileController#download` 本身就是 `@SaIgnore` 的公开接口（图片要能匿名访问），
       * 但没有理由顺带把后台的用户 / 角色 / 配置接口也暴露给博客域名。
       * 生产 nginx 是同一条规则（见 deploy/nginx/default.conf 的博客 server 块）。</p>
       */
      '/admin-api/system/file/download': {
        target: `${apiInternalBase}/admin-api/system/file/download`,
        changeOrigin: true
      }
    }
  }
})
