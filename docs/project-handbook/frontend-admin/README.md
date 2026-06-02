# frontend-admin/ — 后台管理端规格

> 本目录回答："后台 Vue 应用做什么？管理员管理哪些资源？页面长什么样？"
> 对应代码目录：`MyBlog-vue/MyBlog-admin/`（V1 现状，Vue 2）/ 重构后的 V2 后台目录
> 当前状态：骨架待填，等 `product/feature-inventory.md` 标注完后再写。

## 范围

只覆盖**管理员**视角的页面：仪表盘、文章管理、评论审核、用户管理、网站配置等。
访客面向的页面在 `frontend-user/`。

## 关键待定项（待决策后回填）

| 议题 | 选项 | 状态 |
|------|------|------|
| Vue 版本 | 跟前台一起升 Vue 3 + Element Plus / 维持 Vue 2 + Element UI | ⏳ |
| 菜单方式 | 后端动态下发 / 前端静态路由 | ⏳（与 RBAC 决策绑定） |
| 角色模型 | 复杂 RBAC / 简化 USER+ADMIN | ⏳ |
| 编辑器 | 沿用 mavon-editor / 换 Vditor / 换 Bytemd | ⏳ |
| 仪表盘 | echarts 全家桶 / 简化数字卡 | ⏳ |

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
