import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

const nativeFormViolations = [
  {
    selector: ':is(JSXOpeningElement, JSXSelfClosingElement)[name.name="form"]',
    message: 'Dùng Form từ @/components/ui/form để lỗi nhập liệu xuất hiện trong giao diện.',
  },
  {
    selector: 'JSXAttribute[name.name="formNoValidate"], JSXAttribute[name.name="noValidate"]',
    message: 'Xử lý kiểm tra theo nghiệp vụ bằng Form validation="manual" và hiển thị lỗi cạnh trường hoặc thao tác.',
  },
  {
    selector: 'MemberExpression[property.name=/^(reportValidity|validationMessage)$/], MemberExpression[property.value=/^(reportValidity|validationMessage)$/]',
    message: 'Dùng Form để hiển thị lỗi trong giao diện thay cho tooltip của trình duyệt.',
  },
]

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    files: ['src/**/*.{ts,tsx,js,jsx}'],
    ignores: ['src/components/ui/form.tsx'],
    rules: {
      'no-restricted-syntax': ['error', ...nativeFormViolations],
      'no-restricted-imports': ['error', {
        paths: [
          { name: '@base-ui/react/form', message: 'Dùng Form từ @/components/ui/form.' },
          { name: '@base-ui/react', importNames: ['Form'], message: 'Dùng Form từ @/components/ui/form.' },
        ],
      }],
    },
  },
])
