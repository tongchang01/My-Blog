# Site Config And Profile Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Pure Admin 后台新增站点配置和当前用户资料管理页面。

**Architecture:** 只接入现有 V2 后端接口，不新增后端契约。新增 API 文件和两个 feature 目录，页面复用现有卡片式布局、Pinia 当前用户状态和静态路由权限。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Vitest、Vue Test Utils、Axios mock adapter、Pure Admin 静态路由。

---

## 文件结构

- Create `frontend/apps/admin/src/api/site-config.ts`：站点配置 GET/PUT API。
- Modify `frontend/apps/admin/src/api/auth.ts`：增加 `updateCurrentUserProfile`。
- Create `frontend/apps/admin/src/features/site-config/model.ts`：站点配置类型。
- Create `frontend/apps/admin/src/features/site-config/form.ts`：站点配置表单映射与校验。
- Create `frontend/apps/admin/src/features/site-config/useSiteConfigManagement.ts`：站点配置加载和保存状态。
- Create `frontend/apps/admin/src/features/site-config/index.vue`：站点配置页面。
- Create `frontend/apps/admin/src/features/profile/form.ts`：当前用户资料表单映射。
- Create `frontend/apps/admin/src/features/profile/useProfileManagement.ts`：资料保存状态与用户状态同步。
- Create `frontend/apps/admin/src/features/profile/index.vue`：当前用户资料页面。
- Create `frontend/apps/admin/src/router/modules/settings.ts`：系统管理路由。
- Modify `frontend/apps/admin/locales/{zh-CN,en,ja}.yaml`：菜单和页面文案。
- Modify tests under `frontend/apps/admin/src/**/*.test.ts`：API、状态、页面和路由覆盖。

## 任务

### Task 1: API 与表单模型

- [ ] 先写失败测试：`src/api/site-config.test.ts` 覆盖 `getSiteConfig` 和 `updateSiteConfig`。
- [ ] 先写失败测试：`src/api/auth.test.ts` 覆盖 `updateCurrentUserProfile`。
- [ ] 先写失败测试：`src/features/site-config/form.test.ts` 覆盖表单默认值、详情映射、payload 生成。
- [ ] 先写失败测试：`src/features/profile/form.test.ts` 覆盖当前用户资料到表单和 payload。
- [ ] 实现 API、模型和表单函数。
- [ ] 运行：`npm test -- site-config auth form`。
- [ ] 提交：`实现站点配置与资料表单模型`。

### Task 2: 状态管理

- [ ] 先写失败测试：`src/features/site-config/useSiteConfigManagement.test.ts` 覆盖加载、保存、保存失败。
- [ ] 先写失败测试：`src/features/profile/useProfileManagement.test.ts` 覆盖加载当前用户、保存资料、保存后同步 user store。
- [ ] 实现两个 composable。
- [ ] 运行相关状态测试。
- [ ] 提交：`实现站点配置与资料管理状态`。

### Task 3: 页面实现

- [ ] 先写失败测试：站点配置页面 ADMIN 可编辑保存、DEMO 只读、加载失败重试。
- [ ] 先写失败测试：资料页面 ADMIN 可编辑保存、DEMO 只读。
- [ ] 实现两个 Vue 页面。
- [ ] 运行页面测试。
- [ ] 提交：`实现站点配置与资料管理页面`。

### Task 4: 路由和文案

- [ ] 先写失败测试：静态路由包含系统管理、站点配置、作者资料，ADMIN/DEMO 均可访问。
- [ ] 新增 `settings.ts` 路由模块并补齐三语菜单和页面文案。
- [ ] 运行路由测试和相关页面测试。
- [ ] 提交：`接入系统管理菜单与三语文案`。

### Task 5: 验证和文档收尾

- [ ] 更新后台 README，记录两个页面、权限边界和验证命令。
- [ ] 运行 `npm test`。
- [ ] 运行 `npm run typecheck`。
- [ ] 运行 `npm run build`。
- [ ] 检查 `git diff --stat` 和 `git status --short`。
- [ ] 提交：`记录站点配置与资料管理验收结果`。

