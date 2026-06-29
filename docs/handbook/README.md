# V2 开发手册

> 状态：迁移中
> 适用范围：MyBlog V2 开发
> 最后校准：2026-06-29
> 权威程度：权威手册入口

## 本文档回答什么问题

本文档是 MyBlog V2 的当前开发手册入口。后续所有仍然有效的架构、规则、接口、前后台规格和运维说明，都应收口到本目录。

## 目录结构

```text
handbook/
├── start-here/       项目概览、当前状态、路线图、未完成项、术语和踩坑
├── architecture/     当前架构现状
├── rules/            编码、测试、安全、文档等强约束
├── adr/              架构决策记录
├── api/              前后端接口契约
├── product/          业务规格
├── frontend/         前台和后台规格
├── ops/              本地开发、环境变量、构建测试、发布部署
└── workflows/        可重复执行的开发 SOP
```

## 当前迁移状态

本目录正在建立中，旧权威来源仍位于 `docs/project-handbook/`。迁移原则：

1. 只把当前仍有效的结论迁入 `handbook/`。
2. 与代码不一致的内容必须先校准再迁入。
3. 阶段计划和历史 review 不直接作为权威文档，只提炼结论。
4. 未完成和争议事项统一进入 `start-here/open-issues.md`。
5. 迁移完成后，`docs/project-handbook/` 应降级为归档或跳转入口。

## 阅读顺序

1. `start-here/current-status.md`：当前 V2 进度。
2. `start-here/open-issues.md`：未完成和有争议的事项。
3. `start-here/glossary.md`：统一术语。
4. `start-here/pitfalls.md`：红线和历史踩坑。
5. `rules/documentation.md`：文档维护规则。
5. 按任务读取对应的 architecture、api、frontend、ops 或 workflows 文档。

## 冲突处理

迁移完成后，如其他目录内容与 `docs/handbook/` 冲突，以 `docs/handbook/` 为准。迁移期间发现冲突时，先登记到 `start-here/open-issues.md` 或 `docs/working/reviews/`，再在对应批次校准。
