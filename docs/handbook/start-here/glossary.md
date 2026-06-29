# 术语表

> 状态：当前有效
> 适用范围：MyBlog V2 文档与开发沟通
> 最后校准：2026-06-29
> 权威程度：术语权威源

## 本文档回答什么问题

本文档统一 MyBlog V2 开发文档中的术语，避免同一概念在不同文档中使用不同叫法。

## 版本与端

| 术语 | 定义 |
|------|------|
| V1 | 旧版博客系统，包括 `MyBlog-springboot/` 与旧 `MyBlog-vue/`，当前只作历史和业务参考 |
| V2 | 当前重构主线，包括 `MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/` |
| 前台 | 博客访客端，也称 blog、访客端、读者端，代码位于 `frontend/apps/blog/` |
| 后台 | 博客管理端，也称 admin、管理端，代码位于 `frontend/apps/admin/` |
| 后端 V2 | Spring Boot 3 后端，代码位于 `MyBlog-springboot-v2/` |

## 角色

| 术语 | 定义 |
|------|------|
| ADMIN | 管理员账号，可以访问后台读写功能 |
| DEMO | 后台演示账号，只读，不允许写操作；敏感字段需要裁剪 |
| GUEST | 游客或匿名访问者，只能访问公开接口 |

## 模块

| 术语 | 定义 |
|------|------|
| identity | 用户、登录、JWT、refresh token、当前用户信息模块 |
| content | 文章、分类、标签模块 |
| comment | 文章评论、留言板、审核模块 |
| system | 站点配置、附件、友链模块 |
| stats | 访问统计模块 |
| common-infra | 后端公共基础设施层，实际 Java 包为 `com.tyb.myblog.v2.common` |

## 认证与安全

| 术语 | 定义 |
|------|------|
| access token | 登录访问令牌，JWT，短期有效，用于访问受保护接口 |
| refresh token | 刷新令牌，随机字符串，数据库只保存 hash，用于换取新的 access token |
| token_version | 用户表中的 token 版本号，用于让旧 access token 失效 |
| PASSWORD 文章 | 需要访问密码才能查看正文的文章状态，不等于后台登录密码 |
| Article Access Token | PASSWORD 文章解锁后的文章访问令牌，和后台登录 access token 互不通用 |

## 文档治理

| 术语 | 定义 |
|------|------|
| 权威源 | 当前开发必须以其为准的文档 |
| 过程材料 | 计划、阶段 review、调研等临时文档，完成后应提炼或归档 |
| 历史归档 | 保留追溯价值但不再作为当前实现依据的文档 |
| 待校准 | 文档可能仍有价值，但尚未对照当前代码确认 |
| open issue | 未完成、存在争议或需要后续裁决的事项 |
