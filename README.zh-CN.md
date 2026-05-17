# MyBlog

[中文](./README.zh-CN.md) | [English](./README.en.md) | [日本語](./README.ja.md)

## 项目简介

MyBlog 是一个完整的博客系统，包含：

- 博客前台：`MyBlog-blog`
- 后台管理端：`MyBlog-admin`
- 后端服务：`MyBlog-springboot`

当前仓库的目标不再只是“能运行”，而是逐步把三端整理成更容易维护、扩展和迭代的工程。

## 项目结构

```text
E:\My-Blog
├─ MyBlog-vue
│  ├─ MyBlog-blog
│  └─ MyBlog-admin
├─ MyBlog-springboot
└─ docs
```

## 当前技术栈

### 前台 `MyBlog-blog`

- Vue 3
- TypeScript
- Pinia
- Vue Router 4
- Element Plus
- Tailwind CSS
- markdown-it / Prism / tocbot

### 后台 `MyBlog-admin`

- Vue 2
- Vuex
- Vue Router 3
- Element UI
- mavon-editor
- vue-echarts

### 后端 `MyBlog-springboot`

- Java 8
- Spring Boot 2.3.7
- Spring Security
- MyBatis-Plus
- MySQL
- Redis
- RabbitMQ
- Quartz
- Knife4j / Swagger 2
- AWS S3 / SES

## 当前项目现状

### 业务能力

项目已经具备以下核心能力：

- 博客前台展示
- 文章、分类、标签管理
- 评论、留言、友链、说说、相册
- 用户、角色、菜单、资源权限管理
- 网站配置、音乐播放器配置
- 定时任务与日志管理

### 现阶段主要问题

#### 前台

- 已经使用 Vue 3，但仍保留较多旧写法和重复逻辑
- 类型约束不足，`any` 和弱边界较多
- 评论、文章列表、接口响应处理重复明显
- 样式、国际化、主题逻辑有一定耦合

#### 后台

- 仍基于 Vue 2 + Vuex + Element UI
- 技术栈相对老旧，后续插件兼容和升级成本会越来越高
- 如果未来要做复杂交互、权限细化和数据可视化增强，当前架构的扩展性一般

#### 后端

- 基础功能完整，但依赖版本偏旧
- 配置项较多，运行环境要求较高
- 测试、模块边界和可观测性仍有提升空间

## 文档导航

- [项目重构执行计划（中文）](./docs/refactor-plan.zh-CN.md)
- [项目展示文章（中文）](./docs/MyBlog-项目展示.md)
- [English README](./README.en.md)
- [日本語 README](./README.ja.md)

## 本地运行

### 前台

目录：`E:\My-Blog\MyBlog-vue\MyBlog-blog`

```bash
npm install
npm run serve -- --port 8081
npm run build
```

### 后台

目录：`E:\My-Blog\MyBlog-vue\MyBlog-admin`

```bash
npm install
npm run serve -- --port 8082
npm run build
```

### 后端

目录：`E:\My-Blog\MyBlog-springboot`

```bash
mvn clean package
mvn spring-boot:run
```

默认后端端口：`8080`

## 数据库初始化

SQL 目录：`E:\My-Blog\MyBlog-springboot\sql`

- [aurora.sql](./MyBlog-springboot/sql/aurora.sql)：基础表结构和初始化数据
- [music-player.sql](./MyBlog-springboot/sql/music-player.sql)：音乐播放器相关增量脚本

推荐顺序：

1. 创建数据库
2. 导入 `aurora.sql`
3. 如需音乐播放器能力，再导入 `music-player.sql`

## 后端配置

主要配置文件：

- [application.yml](./MyBlog-springboot/src/main/resources/application.yml)
- [application-dev.yml](./MyBlog-springboot/src/main/resources/application-dev.yml)
- [application-local.yml](./MyBlog-springboot/src/main/resources/application-local.yml)

需要重点确认：

- MySQL
- Redis
- RabbitMQ
- JWT
- AWS S3 / SES
- SMTP 邮件
- 网站域名
- QQ 登录

## 环境变量说明

建议使用本地 `.env` 或服务器环境变量，不要把真实密钥直接提交到仓库。

```env
AWS_S3_KEY=your_s3_access_key
AWS_S3_SECRET=your_s3_secret
AWS_S3_URL=https://your-bucket.s3.your-region.amazonaws.com/
AWS_S3_BUCKET=your_bucket_name
AWS_S3_REGION=your_region

AWS_SES_KEY=your_ses_access_key
AWS_SES_SECRET=your_ses_secret
AWS_SES_FROMEMAIL=your_sender_email
AWS_SES_REGION=your_ses_region
AWS_SES_DOMAIN=your_domain

DBPASSWORD=your_database_password
JWT_SECRET=your_jwt_secret
```

## 后续方向摘要

当前项目后续的核心方向不是“大规模推倒重来”，而是按优先级逐步治理：

1. 先解决三端的高频维护痛点
2. 再推进基础设施和技术栈升级
3. 最后再做体验层和功能层扩展

重点原则：

- 前台优先做类型、接口层、重复逻辑治理
- 后台如果升级到 Vue 3，应作为一次完整工程迁移，而不是只改成语法糖
- 后端优先补齐依赖升级、测试、配置治理和模块边界

详细方案见：

- [项目重构执行计划（中文）](./docs/refactor-plan.zh-CN.md)
