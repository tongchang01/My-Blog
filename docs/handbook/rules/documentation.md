# 文档维护规则

> 状态：当前有效
> 适用范围：MyBlog V2 所有开发文档
> 最后校准：2026-06-29
> 权威程度：规则

## 本文档回答什么问题

本文档规定 MyBlog V2 后续如何新增、修改、迁移和归档文档，避免文档再次散落、重复和过期。

## 1. 目录规则

1. 当前有效开发文档放入 `docs/handbook/`。
2. 临时计划、review、调研放入 `docs/working/`。
3. 历史资料放入 `docs/archive/`。
4. 仓库治理文档放入 `docs/governance/`。
5. 项目展示文档放入 `docs/showcase/`。
6. 应用代码目录下不长期沉淀产品和架构文档；如需保留，只放指向 `docs/handbook/` 的 README。

## 2. 权威源规则

1. 当前进度只维护在 `handbook/start-here/current-status.md`。
2. 未完成和争议事项只维护在 `handbook/start-here/open-issues.md`。
3. 术语定义只维护在 `handbook/start-here/glossary.md`。
4. API 契约只维护在 `handbook/api/`。
5. 环境变量只维护在 `handbook/ops/environment.md`。
6. 编码、安全、测试、文档规则只维护在 `handbook/rules/`。
7. 其他文档需要这些信息时，只链接权威源，不复制大段内容。

## 3. 命名规则

1. 开发文档文件名使用英文小写和短横线，例如 `security-baseline.md`。
2. 不新增中文、日文或带空格的开发文档文件名。
3. 不新增 `.zh-CN.md` 后缀；开发文档默认中文。
4. 日期前缀只允许用于 `working/plans/`、`working/reviews/`、`working/research/` 和 `archive/`。
5. ADR 保留编号格式，例如 `0007-jwt-via-spring-security-jose.md`。
6. `README.md` 只作为目录入口，不承载大量业务细节。

## 4. 状态头规则

`docs/handbook/` 下的文档必须在标题后包含状态头：

```md
> 状态：当前有效 / 草案 / 待校准 / 已废弃
> 适用范围：V2 后端 / V2 前台 / V2 后台 / 运维 / 全项目
> 最后校准：YYYY-MM-DD
> 对应代码：`MyBlog-springboot-v2/...` 或 `frontend/apps/...`
> 权威程度：权威源 / 参考资料
```

没有对照当前代码的文档不得标为“当前有效”。

## 5. 新功能文档规则

新增或完成一个功能时，必须同步处理文档：

1. API 变更：更新 `handbook/api/`。
2. 架构或模块边界变化：更新 `handbook/architecture/`。
3. 编码规则变化：更新 `handbook/rules/`，必要时写 ADR。
4. 前台或后台页面变化：更新 `handbook/frontend/` 对应文档。
5. 环境变量或启动方式变化：更新 `handbook/ops/`。
6. 任务完成：更新 `current-status.md`。
7. 未完成或有争议：登记或更新 `open-issues.md`。

## 6. 计划和 review 规则

1. 计划文档只放在 `working/plans/`。
2. 阶段 review 只放在 `working/reviews/`。
3. 计划完成后，必须选择以下处理之一：
   - 有效结论提炼进 `handbook/`，原文归档。
   - 未完成事项登记到 `open-issues.md`，原文归档。
   - 仍在执行则保留在 `working/`。
4. 计划和 review 不直接作为长期开发依据。

## 7. 归档规则

1. 归档文档必须放入 `docs/archive/`。
2. 归档文档不再更新业务内容。
3. 归档文档如被引用，必须说明其为历史资料。
4. 从归档文档提炼出的当前结论必须写入 `handbook/`，不能只靠原文。

## 8. 冲突处理

发现文档冲突时，按以下优先级处理：

1. 当前代码事实。
2. `docs/handbook/` 当前有效文档。
3. ADR。
4. `docs/working/` 中近期计划或 review。
5. `docs/archive/` 历史资料。

如果无法判断，先标记为“待校准”，并登记到 `open-issues.md` 或 `working/reviews/`。

## 9. 禁止事项

1. 禁止把同一段 API 契约复制到多个文件长期维护。
2. 禁止在历史计划中继续追加当前开发结论。
3. 禁止让归档文档成为当前实现依据。
4. 禁止在开发文档中混用多语言版本。
5. 禁止只移动文件不校准内容。
6. 禁止把大批文档整理和业务代码变更混在同一提交。
