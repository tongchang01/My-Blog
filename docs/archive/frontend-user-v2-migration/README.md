# Aurora V2 重构文档集

> 这里是博客前端 V2 重构的"决策档案 + 执行手册"。
>
> **目标**：把上游 Aurora Hexo 主题（`auroral-ui/hexo-theme-aurora`）剥成一个普通 Vite SPA，再对接自研 V2 后端，最后从 v1 迁回需要保留的自定义功能。
>
> **使用方式**：本文档集会在家里另一台电脑上被严格执行。每个 phase 都要做到"按文档走能复现"。

---

## 目录结构

```
aurora-v2-docs/
├── README.md                  ← 你在这里
├── phase-0-setup/             ← 从零搭本地预览环境
│   ├── README.md              （阶段总览 + 验收）
│   ├── 01-environment.md      （Node/pnpm/Git 基线）
│   ├── 02-clone-and-install.md（克隆 + 装依赖）
│   ├── 03-local-modifications.md（新建根 index.html + 注释 proxy）
│   ├── 04-mock-data.md        （mock 数据用法）
│   ├── 05-start-and-verify.md （启动 vite + 8 项验收）
│   ├── 06-troubleshooting.md  （症状→修法对照表）
│   └── mock-data/             （9 个 mock JSON 文件）
├── phase-1-decouple-hexo/     ← 把 Hexo 残留剥干净
│   ├── README.md              （阶段总览 + 验收）
│   ├── 01-delete-hexo-files.md（删 _config.yml/templates/layout/build/...）
│   ├── 02-clean-vite-config.md（去 createHtmlPlugin + outDir + proxy）
│   ├── 03-clean-package-json.md（rebrand + 删 env:* / postbuild）
│   ├── 04-verify.md           （grep + dev + build 出口验收）
│   └── 05-troubleshooting.md  （症状→修法对照表）
├── phase-2-prune-deps/        ← 卸掉 9 个死依赖 + 3 个无主文件
│   ├── README.md              （阶段总览 + 验收 + 删除清单）
│   ├── 01-confirm-dead-deps.md（grep 验证零业务引用）
│   ├── 02-remove-packages.md  （pnpm remove + 删配套 config / tests + tsconfig 清理）
│   ├── 03-verify.md           （5 项出口验收）
│   └── 04-troubleshooting.md  （症状→修法对照表）
├── phase-3a-minor-bump/       ← 同 major 小升级（7 个包）
│   ├── README.md              （阶段总览 + 不做事项）
│   ├── 01-pre-flight.md       （打 tag、基线快照）
│   ├── 02-upgrade.md          （一条 pnpm up 命令）
│   ├── 03-verify.md           （4 项出口验收，容忍历史 TS 错误）
│   └── 04-troubleshooting.md  （含 plugin-vue v6 ESM-only 坑）
├── phase-3b-major-bump/       ← 跨 major 升级（Vite 4→5、ESLint 8→9）
│   ├── README.md              （阶段总览 + 2 step 划分）
│   ├── 01-pre-flight.md       （打 pre-deps-major tag、版本快照）
│   ├── 02-vite-5.md           （含 vite.config.js → .mjs 改名）
│   ├── 03-eslint-9.md         （flat config 完整重写）
│   ├── 04-verify.md           （5 项总出口验收）
│   └── 05-troubleshooting.md  （含 CRLF 13822 个误报修复）
└── planning/                  ← 决策类文档（早期定下的方向，不会随 phase 推进）
    ├── tech-stack.md          （技术栈、依赖、版本基线）
    ├── feature-audit.md       （全功能清单 + 留/改/删 决议）
    ├── decisions-and-roadmap.md（架构决策 + 5 阶段路线图）
    ├── config-guide.md        （主题配置项速查）
    └── backend-integration.md （i18n + 配置 DB 化 + 后台对接契约）
```

---

## 阅读顺序

### 第一次接触本项目（新机器、新人）

1. 把 `planning/` 全部过一遍（了解大方向，**不动手**）
2. 进入 `phase-0-setup/`，按里面的 README 走完 → 拿到能跑的本地预览
3. 等待后续 phase 文档（Phase 1+ 还没写）

### 已经熟悉、要继续推进

直接进 `phase-N/` 工作目录，每个 phase 都有自己的 README + 子文档。

---

## 文档约定

- **planning/** 里的文档是"已定的方向"，不轻易动；如要改方向，更新对应 md + 在 git 记录原因
- **phase-N/** 里每个 phase 是独立单元，包含：
  - `README.md`（阶段目标 + 验收标准 + 子文档导航）
  - 数字前缀的步骤文档（01、02、03…）
  - 必要时配套 `mock-data/`、`patches/`、`screenshots/` 等子目录
- 所有跨阶段的"决策记录"沉淀到 `planning/decisions-and-roadmap.md`

---

## 现在进行到哪一步

| 状态 | 项目 |
|---|---|
| ✅ | Phase 0 文档完成（含反向验证：clone + 4 处偏差修正） |
| ✅ | planning/ 5 篇决策文档已就位 |
| ✅ | 主题色锁定 `#06b6d4 → #6366f1 → #8b5cf6` |
| ✅ | 前台技术栈定为 Aurora + Tailwind（不引入 Element Plus） |
| ✅ | 三语方案 = zh / ja / en + 路径前缀（详见 planning/backend-integration.md） |
| ✅ | 可配置项全部进 DB（由后台管理页编辑，前端只读） |
| ⏳ | 在家电脑上跑一遍 phase-0-setup 验证可复现 |
| ⏳ | 等后端按 planning/backend-integration.md §2.4 落地新增字段 / 表 |
| ✅ | Phase 1（脱钩 Hexo）文档完成 + 沙盒已验证 install/dev/build 通过 |
| ⏳ | 在家电脑上跑一遍 phase-1 验证可复现 |
| ✅ | Phase 2（删死依赖）文档完成 + 沙盒已验证（-9 直接包 / -488 传递依赖） |
| ⏳ | 在家电脑上跑一遍 phase-2 验证可复现 |
| ✅ | Phase 3a（小升级）文档完成 + 沙盒已验证（Vue 3.5、router 4.5、pinia 2.3、TS 5.6、axios 1.7、plugin-vue 5、types/node 25） |
| ⏳ | 在家电脑上跑一遍 phase-3a 验证可复现 |
| ✅ | Phase 3b（跨 major）文档完成 + 沙盒已验证（Vite 5.4、plugin-vue 6、ESLint 9 flat config） |
| ⏳ | 在家电脑上跑一遍 phase-3b 验证可复现 |
| ✅ | 代码现状盘点 + Phase 4-9 路线图重写（2026-06-09） |
| ⏳ | 写 Phase 4（死代码一刀切：Dia / Navigator / Comments 6 文件 / LinkBoxTitle / routers store）的执行文档 |
| ⏳ | Phase 5（`<script setup>` 迁移）执行文档——前置 Phase 4 |

---

## 关键路径速查

| 用途 | 路径 |
|---|---|
| 上游仓库 | https://github.com/auroral-ui/hexo-theme-aurora |
| 工程根目录（本地约定） | `c:\tyb\hexo-theme-aurora-main\` |
| 本地 mock 数据（拷到工程） | `public\api\*.json` |
| 启动命令 | `cd <工程根> && ./node_modules/.bin/vite` |
| 本地预览端口 | http://localhost:5173 |

## v1 工程参考

| 名称 | 路径 |
|---|---|
| v1 前端（魔改源） | `c:\tyb\My-Blog\MyBlog-vue\MyBlog-blog\` |
| V2 后端 | `c:\tyb\My-Blog\`（当前工作区） |
