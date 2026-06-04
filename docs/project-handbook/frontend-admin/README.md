# frontend-admin/ — 后台管理端规格

> 本目录回答："后台 Vue 应用做什么？管理员管理哪些资源？页面长什么样？"
> 对应代码目录：`MyBlog-vue/MyBlog-admin/`（V1 现状，Vue 2）/ 重构后的 V2 后台目录
> 当前状态：骨架待填，等 `product/feature-inventory.md` 标注完后再写。

## 范围

只覆盖**管理员**视角的页面：仪表盘、文章管理、评论审核、用户管理、网站配置等。
访客面向的页面在 `frontend-user/`。

## 关键决策项（已定，详见 `product/feature-inventory.md` + `product/decisions-draft.md`）

| 议题 | 决策 | 引用 |
|------|------|------|
| Vue 版本 | **Vue 3 + Element Plus + Pinia + TypeScript + vue-i18n**（前后台统一栈） | ⑳ / R7 D11 |
| 菜单方式 | **前端静态路由**，删 `t_menu` + `/admin/user/menus` 接口 | ⑩ |
| 角色模型 | **Role 三态枚举 ADMIN / DEMO / GUEST**，删 4 张 RBAC 表，`@PreAuthorize("hasRole('ADMIN')")` 控制 | ⑨ / R5 / R6 |
| 编辑器 | **Vditor**（候选 Vditor / Bytemd，前端实施前最终敲定） | ⑳ / R7 D11 |
| 仪表盘 | **简化为数字卡 + 最新评论 + 文章访问 TOP 10**；活动热图迁前台 about 页 | ⑯ |
| API 文档 | **Knife4j 4.x**（基于 springdoc-openapi） | R7 / ADR-0009 |

## 计划包含的文件

| 文件 | 内容 | 状态 |
|------|------|------|
| `tech-stack.md` | 后台技术选型 | ⏳ |
| `menu-structure.md` | 后台菜单层级 + 每项对应的功能模块 | ⏳ |
| `pages.md` | 页面清单：路径、用途、调用接口、权限要求 | ⏳ |
| `components.md` | 后台通用组件（DataTable / FormDialog / ImageUploader 等） | ⏳ |
| `permission-model.md` | 前端权限控制策略（路由守卫、按钮级权限） | ⏳ |

## 写作约定

- 每个页面写清：路径、所需角色、调用接口（引用 api-contract）、关键操作、危险操作（删除/批量）的二次确认要求
- 后台所有写操作必须有 loading + 成功反馈 + 失败错误码展示
