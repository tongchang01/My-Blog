# 04 · 总出口验收

> Step 3b.1 + 3b.2 都收口后跑这一份。全绿才能打 `phase-3b-done` tag。

---

## 验收 1 · 版本到位

```bash
node -e "
const p = require('./package.json');
const targets = {
  'vite': '5.', '@vitejs/plugin-vue': '6.', 'vite-plugin-pages': '0.3',
  'eslint': '9.', 'eslint-plugin-vue': '10.',
  '@typescript-eslint/eslint-plugin': '8.', '@typescript-eslint/parser': '8.',
  '@vue/eslint-config-typescript': '14.', '@vue/eslint-config-prettier': '10.',
};
let bad = 0;
for (const [k, prefix] of Object.entries(targets)) {
  const v = (p.dependencies?.[k] || p.devDependencies?.[k] || '').replace(/^[\^~]/, '');
  const ok = v.startsWith(prefix);
  console.log(ok ? '✅' : '❌', k.padEnd(36), v);
  if (!ok) bad++;
}
process.exit(bad);
"
```

**期望**：9 行 ✅，exit 0。

---

## 验收 2 · `vite.config.mjs` 在位，`.eslintrc.js` / `.eslintignore` 已删

```bash
ls vite.config.* eslint.config.* 2>/dev/null
# 期望：vite.config.mjs  eslint.config.mjs（没有 .js 后缀的）

ls .eslintrc* .eslintignore 2>/dev/null
# 期望：空输出
```

---

## 验收 3 · dev 启动 OK，版本号是 5.x

```bash
./node_modules/.bin/vite
```

**期望**：

- `VITE v5.4.x  ready in xxx ms`（不再是 4.5.x）
- 浏览器 `http://localhost:5173/` 首页和 Phase 3a 末视觉一致
- F12 Console 无新增报错

`Ctrl+C` 关掉。

---

## 验收 4 · production build OK

```bash
./node_modules/.bin/vite build 2>&1 | tail -5
```

**期望**：

- `✓ built in xxx ms`
- 主 bundle 体积与 Phase 3a 末 **±10% 内**（Vite 5 的 rollup 4 略微优化是正常的，沙盒 478 KB → 476 KB）
- 文件名 hash 算法变了（Vite 5 默认 `base64url`，所以文件名长得不一样）

---

## 验收 5 · `eslint .` 跑得起来

```bash
./node_modules/.bin/eslint . 2>&1 | tail -3
```

**期望**：

```
✖ XX problems (YY errors, ZZ warnings)
```

任何不超过 ~50 的"问题"都是 **业务历史**（Phase 4 一起清）；ESLint 9 + flat config 本身工作正常即可。

**异常判定**：

| 看到的 | 含义 | 修法 |
|---|---|---|
| `Could not find config file` | flat config 没生效 | 检查 `eslint.config.mjs` 是否存在；当前目录是工程根目录吗 |
| `Cannot use import statement outside a module` | mjs 加载失败 | Node 版本 < 18？升 Node |
| 几千个 `Delete ␍` | CRLF 在闹 | `.prettierrc` 加 `endOfLine: 'auto'` |

---

## 收口

5 项全过 → 已经在 [03-eslint-9.md](./03-eslint-9.md) Step 3.8 commit + 子 tag 完成。

最后再确认：

```bash
git tag | grep -E "phase-3b-(1|2|done)"
# 期望：3 行
#   phase-3b-1-done
#   phase-3b-2-done
#   phase-3b-done
```

---

## 下一步

→ Phase 4 · `<script setup>` 迁移（待撰写）

预告：把 61 个 `.vue` 文件从 `defineComponent({ setup() })` 迁到 `<script setup>`。分 3 子步：

- 4a：`src/pages/**` 10 个文件
- 4b：`src/components/**` ~45 个文件
- 4c：App shell（App.vue + Header + Footer + main.ts）~6 个文件

Phase 3b 留下的 ~22 个 lint 错误也在 Phase 4 顺手清掉。
