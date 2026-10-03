// @vitest-environment happy-dom
import { beforeAll, describe, expect, it } from 'vitest'
import { resolve } from 'node:path'
import { compileStyle } from 'vue/compiler-sfc'
import postcss from 'postcss'
import tailwindcss from 'tailwindcss'

import controlsSource from './Controls.vue?raw'
import appSource from '@/App.vue?raw'

describe('language switching', () => {
  it('changes every page through its localized route', () => {
    expect(controlsSource).toContain('params: { ...route.params, lang: name }')
    expect(controlsSource).not.toContain('appStore.changeLocale(name)')
    expect(controlsSource).not.toContain('await appStore.fetchConfig()')
    expect(controlsSource).not.toContain(
      "await router.push({ name: 'home', params: { lang: name } })"
    )
    expect(appSource).toContain(':key="pageKey"')
  })
})

describe('header control colors', () => {
  let css: string
  beforeAll(async () => {
    const compiled = compileStyle({
      source: controlsSource
        .split('<style lang="scss" scoped>')[1]
        .split('</style>')[0],
      filename: 'Controls.vue',
      id: 'data-v-controls-test',
      scoped: true,
      preprocessLang: 'scss'
    })
    expect(compiled.errors).toEqual([])
    const result = await postcss([
      tailwindcss(resolve('tailwind.config.js'))
    ]).process(`@tailwind utilities;\n${compiled.code}`, { from: undefined })
    css = result.css
  })

  it.each([
    { sticky: false, bright: '#000000', expected: 'rgb(255 255 255 / 1)' },
    { sticky: true, bright: '#000000', expected: '#000000' },
    { sticky: false, bright: '#ffffff', expected: 'rgb(255 255 255 / 1)' },
    { sticky: true, bright: '#ffffff', expected: '#ffffff' }
  ])(
    'inherits the control color with sticky=$sticky and theme text=$bright',
    ({ sticky, bright, expected }) => {
      const style = document.createElement('style')
      style.textContent = css
      const header = document.createElement('div')
      header.className = sticky ? 'header-active' : ''
      header.style.setProperty('--text-bright', bright)
      header.innerHTML = controlsSource
        .split('<template>')[1]
        .split('</template>')[0]
      header
        .querySelectorAll('*')
        .forEach(element => element.setAttribute('data-v-controls-test', ''))
      document.head.append(style)
      document.body.append(header)
      try {
        const controls = header.querySelector('.header-controls')!
        expect(getComputedStyle(controls).color).toBe(expected)
        for (const name of ['language', 'search', 'menu']) {
          const button = header.querySelector(`[data-dia="${name}"]`)!
          // happy-dom 保留显式 inherit；实际颜色由上面的共用容器提供。
          expect(getComputedStyle(button).color, name).toBe('inherit')
        }
      } finally {
        header.remove()
        style.remove()
      }
    }
  )
})
