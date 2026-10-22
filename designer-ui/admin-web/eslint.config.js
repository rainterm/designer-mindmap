import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'

/**
 * admin-web（Vue 3 + Vite，纯 JS）的 ESLint 扁平配置。
 *
 * <p><b>只查正确性，不管格式。</b>本项目不接 Prettier，也不希望保存时被自动重排代码，
 * 所以用的是 `flat/essential` 而不是 `flat/recommended`：
 * 后者含 `vue/html-indent`、`vue/max-attributes-per-line`、`vue/html-self-closing`
 * 等上百条纯格式规则，会把缩进和换行方式变成 lint 错误 —— 那就等于用 ESLint
 * 重新造一遍格式化器，正是要避免的。</p>
 *
 * <p>换行、缩进、引号、分号一律由编写者自己决定。下面对齐了仓库现有风格
 * （2 空格、单引号、无分号），但不再由工具强制。</p>
 */
export default [
  {
    // 构建产物与第三方内容不参与检查
    ignores: ['dist/**', 'node_modules/**', 'public/**']
  },

  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],

  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser
      }
    },
    rules: {
      /* ---------- 通用 JS ---------- */
      // 'smart' 允许 `x == null`（同时判 null + undefined 的惯用写法，全仓 34 处），
      // 其余比较仍强制 ===。用 'always' 会把这 34 处全报成错，且它们都不是 bug。
      eqeqeq: ['error', 'smart'],
      'prefer-const': 'error',
      'no-var': 'error',
      'no-unused-vars': [
        'error',
        {
          // 约定：下划线开头的参数/变量是刻意保留的占位
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          // `catch (e) {}` 是本项目的既定写法：错误提示已由 axios 拦截器统一弹出，
          // 这里刻意不引用异常对象。强制改名 _e 只是噪音。
          caughtErrors: 'none'
        }
      ],
      // 生产代码里不该有 console.log；warn/error 是刻意的诊断信息（如 router 的视图缺失提示）
      'no-console': ['warn', { allow: ['warn', 'error'] }],

      /* ---------- Vue ---------- */
      // essential 未覆盖，但这两条能抓到真实缺陷，值得单开
      'vue/no-unused-refs': 'error',
      'vue/component-api-style': ['error', ['script-setup']],
      // 项目里大量单文件名视图（index.vue / 403.vue / sso.vue）
      'vue/multi-word-component-names': 'off'
    }
  },

  {
    // main.js 里全局注册了 `Dialog`，与 HTML 的 <dialog> 同名。
    // 模板里 `<Dialog>` 按 PascalCase 解析成组件，不会被当原生标签，实际无冲突；
    // 但该组件已被 28 处模板使用，改名收益低于风险，故仅在注册处放行。
    files: ['src/main.js'],
    rules: {
      'vue/no-reserved-component-names': 'off'
    }
  },

  {
    // 构建脚本跑在 Node 里
    files: ['vite.config.js', 'plugins/**/*.js'],
    languageOptions: {
      globals: {
        ...globals.node
      }
    }
  }
]
