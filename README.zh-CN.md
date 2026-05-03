# MyBlog

[English](./README.en.md) | [日本語](./README.ja.md) | [简体中文](./README.zh-CN.md)

一个完整的博客项目，包含：

- 前台博客：`MyBlog-blog`
- 后台管理端：`MyBlog-admin`
- 后端服务：`MyBlog-springboot`

## 项目结构

```text
E:\My-Blog
├─ MyBlog-vue
│  ├─ MyBlog-blog
│  └─ MyBlog-admin
└─ MyBlog-springboot
```

## 技术栈

### 前台 `MyBlog-blog`

- Vue 3
- TypeScript
- Pinia
- Vue Router 4
- Element Plus
- Tailwind CSS
- APlayer

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
- MySQL
- Redis
- RabbitMQ
- Quartz
- Knife4j / Swagger 2
- AWS S3 / SES

## 主要功能

- 博客前台展示
- 文章、分类、标签管理
- 评论管理与审核
- 说说、相册、友链管理
- 用户、角色、资源、菜单权限管理
- 定时任务与任务日志
- 网站配置管理
- 音乐播放器配置与曲库管理

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

前后台开发代理默认都转发到：

- `http://localhost:8080`

## 数据库初始化

SQL 目录：`E:\My-Blog\MyBlog-springboot\sql`

- [aurora.sql](./MyBlog-springboot/sql/aurora.sql)：基础结构与初始化数据
- [music-player.sql](./MyBlog-springboot/sql/music-player.sql)：音乐播放器相关增量脚本

推荐顺序：

1. 创建数据库
2. 导入 `aurora.sql`
3. 如需音乐播放器相关功能，再导入 `music-player.sql`

## 后端配置

配置文件目录：

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

## `.env` 变量说明

建议使用本地 `.env` 或服务器环境变量，不要把真实密钥提交到仓库。

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

变量用途：

- `AWS_S3_KEY` / `AWS_S3_SECRET`：S3 凭证
- `AWS_S3_URL` / `AWS_S3_BUCKET` / `AWS_S3_REGION`：S3 存储配置
- `AWS_SES_KEY` / `AWS_SES_SECRET`：SES 凭证
- `AWS_SES_FROMEMAIL` / `AWS_SES_REGION` / `AWS_SES_DOMAIN`：SES 发信配置
- `DBPASSWORD`：数据库与 Redis 密码变量
- `JWT_SECRET`：JWT 签名密钥

## 当前项目里的播放器定制

- 只保留“悬浮停靠”
- 已移除“右下角固定”切换项
- 桌面端位置已上移，避免遮挡右下角区域

## 博客展示文章

- [English Showcase](./docs/MyBlog-Project-Showcase.en.md)
- [日本語 紹介記事](./docs/MyBlog-プロジェクト紹介.ja.md)
- [中文项目展示](./docs/MyBlog-项目展示.md)
