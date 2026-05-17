# MyBlog

语言版本：

- [中文](./README.zh-CN.md)
- [English](./README.en.md)
- [日本語](./README.ja.md)

## 项目简介

MyBlog 是一个包含前台博客、后台管理端和 Spring Boot 后端服务的完整博客系统。

当前仓库包含三个工程：

- `MyBlog-vue/MyBlog-blog`：博客前台
- `MyBlog-vue/MyBlog-admin`：后台管理端
- `MyBlog-springboot`：后端服务

## 当前技术栈

### 前台 `MyBlog-blog`

- Vue 3
- TypeScript
- Pinia
- Vue Router 4
- Element Plus
- Tailwind CSS

### 后台 `MyBlog-admin`

- Vue 2
- Vuex
- Vue Router 3
- Element UI
- mavon-editor
- ECharts

### 后端 `MyBlog-springboot`

- Java 8
- Spring Boot 2.3.7
- Spring Security
- MyBatis-Plus
- MySQL / Redis / RabbitMQ / Quartz

## 当前项目现状

项目已经具备完整的博客、评论、说说、相册、友链、权限、定时任务和网站配置能力，但三端都存在不同程度的技术债：

- 前台已经升级到 Vue 3，但仍有较多重复逻辑、类型边界薄弱、事件耦合较重的问题
- 后台仍停留在 Vue 2 + Vuex + Element UI 体系，后续维护和升级成本会继续上升
- 后端基于 Spring Boot 2.3.7 和 Java 8，功能完整，但依赖版本、模块边界和测试体系仍有较大的优化空间

## 文档导航

- [中文完整说明](./README.zh-CN.md)
- [项目重构执行计划（中文）](./docs/refactor-plan.zh-CN.md)
- [项目展示文章（中文）](./docs/MyBlog-项目展示.md)

## 下一阶段方向

- 梳理三端现状与重构优先级
- 优先修复高频维护痛点，而不是一次性大迁移
- 前台逐步收敛类型、接口层和公共逻辑
- 后台以 Vue 3 化为长期目标，但不建议只为“语法糖”单独迁移
- 后端逐步补齐架构边界、配置治理和测试能力

详细方案见：

- [项目重构执行计划（中文）](./docs/refactor-plan.zh-CN.md)
