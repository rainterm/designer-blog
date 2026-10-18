import withNuxt from './.nuxt/eslint.config.mjs'

/**
 * blog-web（Nuxt 4，纯 JS）的 ESLint 配置。
 *
 * <p>`./.nuxt/eslint.config.mjs` 由 `@nuxt/eslint` 在 `nuxt prepare` 时生成，
 * 它带上了 Nuxt 的自动导入清单（`ref` / `useFetch` / `definePageMeta` / 各 composable……）。
 * 直接手写 `globals` 是行不通的：每加一个 `app/composables` 下的函数就得同步一次。</p>
 *
 * <p><b>只查正确性，不管格式。</b>本项目不接 Prettier，也不希望保存时被自动重排代码，
 * 所以不用 @nuxt/eslint 自带的风格规则集（`stylistic` 在 `nuxt.config.js` 里已关闭），
 * 并且不启用 `eslint-plugin-vue` 的 recommended 规则集 —— 那里面有上百条纯格式规则，
 * 会把缩进和换行方式变成 lint 错误，等于用 ESLint 重新造一遍格式化器。</p>
 */
export default withNuxt(
  {
    // 生成物与构建产物不参与检查（`.nuxt` 的 config 自带一部分，这里补齐项目特有的）
    ignores: ['.nuxt/**', '.output/**', '.data/**', 'dist/**', 'node_modules/**', 'public/**']
  },

  {
    files: ['**/*.{js,mjs,vue}'],
    rules: {
      /* ---------- 通用 JS ---------- */
      // 'smart' 允许 `x == null`（同时判 null + undefined），与 admin-web 保持一致
      eqeqeq: ['error', 'smart'],
      'prefer-const': 'error',
      'no-var': 'error',
      'no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          // `catch (e) {}` 是既定写法：提示已由 request.js 统一弹出，这里刻意不引用异常
          caughtErrors: 'none'
        }
      ],
      'no-console': ['warn', { allow: ['warn', 'error'] }],

      /* ---------- Vue ---------- */
      // @nuxt/eslint 额外带了几条纯格式规则（警告级），全部关掉：
      // 本项目不接 Prettier，格式由编写者决定，不该被 lint 提示。
      'vue/first-attribute-linebreak': 'off',
      'vue/html-self-closing': 'off',
      'vue/attributes-order': 'off',
      'vue/block-order': 'off',

      // 以下两条 essential 未覆盖，但能抓到真实缺陷
      'vue/no-unused-refs': 'error',
      'vue/component-api-style': ['error', ['script-setup']],
      // 项目里存在单文件名页面/组件
      'vue/multi-word-component-names': 'off'
    }
  }
)
