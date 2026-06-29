# 站点配置与作者资料管理设计

## 目标

在后台工程补齐博客基础信息维护能力，让站点配置和当前用户资料可以从后台页面查看与编辑。现阶段只复用 V2 后端已经存在的接口，不新增后端表，也不引入主题颜色、布局开关、导航菜单等尚未建模的配置。

## 范围

本次交付两个后台页面：

- `/settings/site-config`：站点配置管理，ADMIN 可编辑，DEMO 只读。
- `/settings/profile`：当前用户资料管理，ADMIN 可编辑本人资料，DEMO 只读。

后台菜单新增“系统管理”，放置这两个页面。页面遵循现有后台模式：独立卡片、加载/失败/重试/空状态、保存时禁用与错误提示、三语文案。

## 后端契约

站点配置复用：

- `GET /api/admin/site-config`
- `PUT /api/admin/site-config`

字段为：

- `siteTitleZh/siteTitleJa/siteTitleEn`
- `siteSubtitleZh/siteSubtitleJa/siteSubtitleEn`
- `aboutMdZh/aboutMdJa/aboutMdEn`
- `logoUrl`
- `faviconUrl`
- `icpNo`
- `spotifyPlaylistId`
- `updatedAt`
- `updatedBy`

注意：后端 `PUT` 要求提交全部字段，前端保存时必须从当前表单生成完整 payload，不能只提交脏字段。

当前用户资料复用：

- `GET /api/auth/me`
- `PATCH /api/auth/me/profile`

资料字段为：

- `nickname`
- `avatarUrl`
- `bioZh/bioJa/bioEn`
- `location`
- `website`
- `emailPublic`
- `githubUrl`
- `twitterUrl`
- `linkedinUrl`
- `zhihuUrl`
- `qiitaUrl`
- `juejinUrl`

资料更新接口是 PATCH，但页面会提交完整资料字段，保持实现简单、可预测。

## 页面设计

### 站点配置页

页面由两个卡片组成：

1. 基础配置卡片：三语标题、三语副标题、Logo、Favicon、ICP、Spotify playlist。
2. 关于内容卡片：三语 About Markdown 文本域。

顶部显示最后更新时间和更新人。ADMIN 显示保存按钮；DEMO 显示只读提示且所有输入禁用。

### 作者资料页

页面由两个卡片组成：

1. 账号概览卡片：账号 ID、用户名、角色、头像预览。
2. 资料编辑卡片：昵称、头像、三语简介、位置、网站、邮箱和社交链接。

ADMIN 可保存；DEMO 只读。保存成功后刷新当前用户资料并同步 Pinia 用户状态，避免导航栏仍显示旧昵称。

## 错误处理

- 初次加载失败：显示错误 Alert 和重试按钮。
- 保存失败：保留表单内容，显示不可关闭错误 Alert。
- 403：页面正常不渲染 DEMO 写入口，后端仍作为最终权限边界。
- 校验：前端只做基础校验，空昵称、超长文本和 URL 格式由后端继续兜底。

## 测试策略

- API 测试：断言站点配置 GET/PUT、资料 PATCH 请求路径和 payload。
- 状态测试：断言加载、保存、保存失败、资料保存后同步当前用户。
- 页面测试：断言 ADMIN 可编辑保存，DEMO 只读，加载失败可重试。
- 路由测试：断言系统管理菜单包含两个页面，ADMIN/DEMO 均可访问。

