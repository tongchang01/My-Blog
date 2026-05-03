# MyBlog

一个包含前台博客、后台管理端和 Spring Boot 后端的完整博客项目。

## 项目结构

```text
E:\My-Blog
├─ MyBlog-vue
│  ├─ MyBlog-blog        # 前台博客（Vue 3 + TypeScript）
│  └─ MyBlog-admin       # 后台管理端（Vue 2 + Element UI）
└─ MyBlog-springboot     # 后端服务（Spring Boot + MyBatis-Plus）
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
- 说说 / 相册 / 友链管理
- 用户、角色、资源、菜单权限管理
- 定时任务与任务日志
- 网站配置管理
- 音乐播放器配置与曲库管理

## 环境要求

- Node.js 16+ 推荐
- npm 8+ 推荐
- Java 8
- Maven 3.6+
- MySQL 8.x 推荐
- Redis
- RabbitMQ

## 目录说明

### 1. 前台博客

目录：`E:\My-Blog\MyBlog-vue\MyBlog-blog`

常用命令：

```bash
npm install
npm run serve -- --port 8081
npm run build
```

说明：

- 开发代理在 [vue.config.js](E:/My-Blog/MyBlog-vue/MyBlog-blog/vue.config.js) 中配置，`/api` 默认代理到 `http://localhost:8080`
- 这是前台站点，负责博客首页、文章详情、归档、相册、说说、评论等页面
- 当前音乐播放器逻辑为“默认悬浮停靠”，不再支持“右下角固定”切换

### 2. 后台管理端

目录：`E:\My-Blog\MyBlog-vue\MyBlog-admin`

常用命令：

```bash
npm install
npm run serve -- --port 8082
npm run build
```

说明：

- 后台的 `publicPath` 为 `/admin/`
- 开发代理在 [vue.config.js](E:/My-Blog/MyBlog-vue/MyBlog-admin/vue.config.js) 中配置，`/api` 默认代理到 `http://localhost:8080`
- 登录页路由为 `/login`
- 后台网站配置页位于 [Website.vue](E:/My-Blog/MyBlog-vue/MyBlog-admin/src/views/website/Website.vue)

### 3. 后端服务

目录：`E:\My-Blog\MyBlog-springboot`

常用命令：

```bash
mvn clean package
mvn spring-boot:run
```

打包后运行：

```bash
java -jar target/*.jar
```

说明：

- 启动类：`com.aurora.AuroraSpringbootApplication`
- 默认端口：`8080`
- 默认激活环境见 [application.yml](E:/My-Blog/MyBlog-springboot/src/main/resources/application.yml)
- 当前默认激活的是 `dev`

## 数据库初始化

SQL 目录：`E:\My-Blog\MyBlog-springboot\sql`

可用脚本：

- [aurora.sql](E:/My-Blog/MyBlog-springboot/sql/aurora.sql)：基础表结构和初始化数据
- [music-player.sql](E:/My-Blog/MyBlog-springboot/sql/music-player.sql)：音乐播放器相关表和菜单/资源补充脚本

推荐初始化顺序：

1. 创建数据库
2. 导入 `aurora.sql`
3. 如果你的库里还没有音乐播放器相关表和菜单，再补充导入 `music-player.sql`

注意：

- `music-player.sql` 更像是增量脚本，不建议在不确认当前库状态的情况下重复手工修改
- `application-dev.yml` 和 `application-local.yml` 中数据库名并不完全一致，启动前请先核对

## 后端配置项

配置文件位于：`E:\My-Blog\MyBlog-springboot\src\main\resources`

- [application.yml](E:/My-Blog/MyBlog-springboot/src/main/resources/application.yml)
- [application-dev.yml](E:/My-Blog/MyBlog-springboot/src/main/resources/application-dev.yml)
- [application-local.yml](E:/My-Blog/MyBlog-springboot/src/main/resources/application-local.yml)

### 需要重点确认的配置

- MySQL 连接地址、库名、账号、密码
- Redis 地址和密码
- RabbitMQ 地址和账号密码
- JWT 密钥
- 网站域名
- 邮件 SMTP 配置
- AWS S3 / SES 配置
- QQ 登录配置

### 当前使用到的环境变量

后端配置里已经引用了以下环境变量，启动前建议先设置：

- `DBPASSWORD`
- `JWT_SECRET`
- `AWS_SES_REGION`
- `AWS_SES_KEY`
- `AWS_SES_SECRET`
- `AWS_SES_FROMEMAIL`
- `AWS_S3_KEY`
- `AWS_S3_SECRET`
- `AWS_S3_REGION`
- `AWS_S3_BUCKET`
- `AWS_S3_URL`

## 本地启动顺序

推荐按下面顺序启动：

1. 启动 MySQL、Redis、RabbitMQ
2. 启动后端 `MyBlog-springboot`
3. 启动前台 `MyBlog-blog`
4. 启动后台 `MyBlog-admin`

推荐本地端口：

- 后端：`8080`
- 前台：`8081`
- 后台：`8082`

这样可以避免 Vue CLI 默认端口和后端 `8080` 冲突。

## 开发约定

### API 代理

前后台两个前端都通过 `/api` 转发到后端：

- 前台：`/api -> http://localhost:8080`
- 后台：`/api -> http://localhost:8080`

所以本地联调时，后端必须先在 `8080` 跑起来。

### 后台路径

后台构建后默认挂在 `/admin/` 路径下。

### 接口文档

项目中启用了 Knife4j 配置，相关类在 [Knife4jConfig.java](E:/My-Blog/MyBlog-springboot/src/main/java/com/aurora/config/Knife4jConfig.java)。

通常本地可尝试访问：

- `http://localhost:8080/doc.html`
- `http://localhost:8080/swagger-ui.html`

如果打不开，请检查安全配置、反向代理或当前环境是否关闭了文档入口。

## 你现在这份代码里的定制点

### 音乐播放器

这份项目已经做了播放器相关的扩展：

- 前台增加了 APlayer 播放器
- 后台增加了音乐播放器网站配置
- 后端增加了 `MusicController`
- 数据库补充了 `t_music` 和对应菜单、资源权限脚本

当前播放器行为：

- 只保留“悬浮停靠”
- 已移除“右下角固定”切换项
- 桌面端位置已上移，避免遮挡右下角区域

## 常见问题

### 1. 为什么前端启动后访问不到接口？

先确认：

- 后端是否运行在 `8080`
- 前端是否通过 `npm run serve -- --port 8081/8082` 启动
- `vue.config.js` 的代理目标是否还是本地 `8080`

### 2. 为什么两个前端不能都直接 `npm run serve`？

因为 Vue CLI 默认也可能占用 `8080`，而这个项目的代理目标和后端默认端口同样是 `8080`。本地建议手动把前台、后台分别跑在 `8081`、`8082`。

### 3. 后端为什么启动失败？

优先检查：

- MySQL 库是否已创建且已导入 SQL
- `DBPASSWORD`、`JWT_SECRET` 等环境变量是否已设置
- Redis / RabbitMQ 是否启动
- `application.yml` 当前激活的 profile 是否与你的本地配置一致

## 构建命令汇总

### 前台

```bash
cd E:\My-Blog\MyBlog-vue\MyBlog-blog
npm install
npm run build
```

### 后台

```bash
cd E:\My-Blog\MyBlog-vue\MyBlog-admin
npm install
npm run build
```

### 后端

```bash
cd E:\My-Blog\MyBlog-springboot
mvn clean package
```

## 后续建议

- 补一份 `.env.example` 或启动前配置说明，减少环境变量遗漏
- 为前台、后台分别补默认开发端口配置，避免每次手动传 `--port`
- 给后台 README 再补一份默认管理员账号说明
- 把音乐播放器的数据库脚本整合进主初始化流程，避免新环境遗漏
