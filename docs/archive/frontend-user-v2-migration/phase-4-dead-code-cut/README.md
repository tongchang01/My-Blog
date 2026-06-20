# Phase 4 · 死代码一刀切

> **目标**：物理删除盘点确认"绝对死"的代码，给后续 Phase 5（`<script setup>` 迁移）减负——少改 1768 行注定要删的代码。
>
> **耗时**：半小时
>
> **沙盒验证**：✅ 通过（main bundle 476 → 452.92 KB，**-23 KB**）

---

## 删除范围

| 项 | 路径 | 理由 |
|---|---|---|
| Dia 看板娘组件 | `src/components/Dia.vue` | v2 不要二次元元素 |
| Dia store | `src/stores/dia.ts` | 仅 Dia.vue 引用 |
| Dia 数据/工具 | `src/utils/aurora-dia/`（整目录 4 文件） | 仅 stores/dia.ts 引用 |
| Navigator 弹窗组件 | `src/components/Navigator.vue` | App.vue 已注释，0 实际使用 |
| LinkBoxTitle | `src/components/Link/LinkBoxTitle.vue` | 全仓 0 引用 |
| routers store | `src/stores/routers.ts` | 0 引用，TS 报 5 个错误 |
| `aurora_bot` 配置项 | `src/models/ThemeConfig.class.ts` | Dia 的配置容器 |

## 不在本 phase 范围

| 项 | 原因 | 去哪做 |
|---|---|---|
| `src/utils/comments/` 6 文件（github/gravatar/leancloud/twikoo/valine/waline） | 物理删会炸 7 个消费者文件（useCommentPlugin + Comment.vue + 5 个组件） | **Phase 7g** 重写 Comment.vue 时一起清 |
| `src/hooks/useCommentPlugin.ts` | 同上 | Phase 7g |
| `Comment.vue` | 同上 | Phase 7g |
| `stores/navigator` 完整删除 | 实际是"滚动进度+移动菜单+Navigator 弹窗"三合一，移动菜单 + Sticky 滚动进度都还要用 | 本 phase **只切 Navigator 弹窗那一半**，保留另两半 |

---

## 文档导航

| 文件 | 内容 |
|---|---|
| [01-pre-flight.md](./01-pre-flight.md) | 入口条件 + tag + 0 引用确认 |
| [02-cut.md](./02-cut.md) | 删除矩阵 + App.vue / ThemeConfig / navigator store / MobileMenu 编辑步骤 |
| [03-verify.md](./03-verify.md) | 5 项出口验收 |
| [04-troubleshooting.md](./04-troubleshooting.md) | 沙盒踩到的 2 个坑（navigator 误判 + commitlint 格式）|

---

## 出口验收速览

| 项 | 期望 |
|---|---|
| `tsc --noEmit` | 仅剩 2 个 pre-existing LoadingSkeleton errors（routers.ts 的 5 个错误消失） |
| `vite build` | ✓ 通过 |
| main bundle | 与 Phase 3b 末（沙盒 476 KB）相比有可见下降（沙盒 -23 KB / -4.8%） |
| `grep -ri "aurora-dia\|<Dia\|useDiaStore"` | 0 |
| `grep -r "LinkBoxTitle\|stores/routers\|aurora_bot"` | 0 |

---

## 不做事项

- ❌ **不动 `utils/comments/`**——留到 Phase 7g
- ❌ **不动 `Comment.vue`**——留到 Phase 7g
- ❌ **不删整个 navigator store**——只切 Navigator 弹窗那 3 个 state/action
- ❌ **不做 `<script setup>` 迁移**——Phase 5 集中做
- ❌ **不做接口改造**——Phase 7 集中做
