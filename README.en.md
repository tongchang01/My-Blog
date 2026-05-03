# MyBlog

[English](./README.en.md) | [日本語](./README.ja.md) | [简体中文](./README.zh-CN.md)

MyBlog is a full-stack blog system that includes:

- a public blog frontend: `MyBlog-blog`
- an admin dashboard: `MyBlog-admin`
- a Spring Boot backend: `MyBlog-springboot`

## Project Structure

```text
E:\My-Blog
├─ MyBlog-vue
│  ├─ MyBlog-blog
│  └─ MyBlog-admin
└─ MyBlog-springboot
```

## Tech Stack

### Frontend `MyBlog-blog`

- Vue 3
- TypeScript
- Pinia
- Vue Router 4
- Element Plus
- Tailwind CSS
- APlayer

### Admin `MyBlog-admin`

- Vue 2
- Vuex
- Vue Router 3
- Element UI
- mavon-editor
- ECharts

### Backend `MyBlog-springboot`

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

## Features

- public blog pages
- article/category/tag management
- comment moderation
- talk, album, and friend-link management
- user/role/resource/menu permissions
- scheduled jobs and logs
- website settings
- music player and playlist management

## Local Development

### Blog Frontend

Directory: `E:\My-Blog\MyBlog-vue\MyBlog-blog`

```bash
npm install
npm run serve -- --port 8081
npm run build
```

### Admin Frontend

Directory: `E:\My-Blog\MyBlog-vue\MyBlog-admin`

```bash
npm install
npm run serve -- --port 8082
npm run build
```

### Backend

Directory: `E:\My-Blog\MyBlog-springboot`

```bash
mvn clean package
mvn spring-boot:run
```

Default backend port: `8080`

Both frontend apps proxy `/api` to:

- `http://localhost:8080`

## Database Setup

SQL directory: `E:\My-Blog\MyBlog-springboot\sql`

- [aurora.sql](./MyBlog-springboot/sql/aurora.sql): base schema and seed data
- [music-player.sql](./MyBlog-springboot/sql/music-player.sql): incremental script for music player features

Recommended order:

1. Create the database
2. Import `aurora.sql`
3. Import `music-player.sql` if you need music-related features

## Backend Configuration

Config files:

- [application.yml](./MyBlog-springboot/src/main/resources/application.yml)
- [application-dev.yml](./MyBlog-springboot/src/main/resources/application-dev.yml)
- [application-local.yml](./MyBlog-springboot/src/main/resources/application-local.yml)

Please verify:

- MySQL
- Redis
- RabbitMQ
- JWT
- AWS S3 / SES
- SMTP mail settings
- website domain
- QQ login settings

## `.env` Variables

Use local `.env` files or server environment variables. Do not commit real secrets into the repository.

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

Purpose of each variable:

- `AWS_S3_KEY` / `AWS_S3_SECRET`: S3 credentials
- `AWS_S3_URL` / `AWS_S3_BUCKET` / `AWS_S3_REGION`: S3 storage settings
- `AWS_SES_KEY` / `AWS_SES_SECRET`: SES credentials
- `AWS_SES_FROMEMAIL` / `AWS_SES_REGION` / `AWS_SES_DOMAIN`: SES mail settings
- `DBPASSWORD`: password used by database and Redis config
- `JWT_SECRET`: JWT signing secret

## Music Player Customization

Current player behavior in this project:

- docked floating mode only
- fixed bottom-right mode removed
- desktop position moved upward to avoid blocking the lower-right UI area

## Showcase Articles

- [English Showcase](./docs/MyBlog-Project-Showcase.en.md)
- [日本語 紹介記事](./docs/MyBlog-プロジェクト紹介.ja.md)
- [中文项目展示](./docs/MyBlog-项目展示.md)
