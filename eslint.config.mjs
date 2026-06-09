import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'
import prettier from 'eslint-plugin-prettier/recommended'
import globals from 'globals'

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{js,mjs,cjs,ts,tsx,vue}']
  },
  {
    name: 'app/files-to-ignore',
    ignores: [
      'build/**',
      'src/assets/**',
      'public/**',
      'dist/**',
      '**/scripts/**'
    ]
  },
  {
    languageOptions: {
      ecmaVersion: 2020,
      globals: {
        ...globals.node,
        ...globals.browser
      }
    }
  },
  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  skipFormatting,
  prettier,
  {
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
      'prettier/prettier': ['error', { semi: false }],
      'no-console': 'warn',
      'no-debugger': 'warn'
    }
  }
)
