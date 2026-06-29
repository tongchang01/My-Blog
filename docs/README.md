# MyBlog 文档入口

> 状态：迁移中
> 适用范围：全项目文档
> 最后校准：2026-06-29
> 权威程度：入口索引

## 本文档回答什么问题

本文档说明 MyBlog 仓库里的文档应该从哪里开始读、哪些目录是当前有效依据、哪些目录只保留历史追溯价值。

## 当前主线

MyBlog 当前主线是 V2 开发：

- V1：历史版本，只作业务参考，不再修改。
- V2 后端：主体模块已完成，后续以联调、修正和上线准备为主。
- V2 前台：刚进入读者端页面和公开接口接入阶段。
- V2 后台：基础登录和部分业务页已完成，仍需补齐内容生产和管理闭环。

## 文档目录分工

| 目录 | 用途 | 是否作为当前开发依据 |
|------|------|----------------------|
| `docs/handbook/` | 当前有效开发手册，后续权威源 | 是 |
| `docs/working/` | 临时计划、盘点、review、调研 | 否，完成后需提炼或归档 |
| `docs/archive/` | 历史资料和旧迁移过程 | 否 |
| `docs/governance/` | 分支策略、仓库治理、发布协作规则 | 视主题而定 |
| `docs/showcase/` | 项目展示文档，可多语言 | 否 |
| `docs/project-handbook/` | 旧权威手册来源，迁移中 | 暂时参考，最终由 `handbook/` 接管 |
| `docs/superpowers/` | 旧阶段计划、设计和 review | 否，待归档 |

## 推荐阅读顺序

1. 当前入口：`docs/handbook/README.md`
2. 当前进度：`docs/handbook/start-here/current-status.md`
3. 未完成和争议项：`docs/handbook/start-here/open-issues.md`
4. 术语表：`docs/handbook/start-here/glossary.md`
5. 文档维护规则：`docs/handbook/rules/documentation.md`

## 迁移说明

当前文档正在从旧结构迁移到新结构。迁移期间：

- 新增权威文档优先放入 `docs/handbook/`。
- 旧 `docs/project-handbook/` 中的有效内容会逐步迁移并校准。
- 旧 `docs/superpowers/` 中的过程材料会先提炼，再归档。
- 未完成和有争议的事项统一登记到 `open-issues.md`。
