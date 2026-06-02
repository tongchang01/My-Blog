# decisions/ — 架构决策记录（ADR）

> 本目录回答："为什么是这样定的？"
> 性质：永久保留。一个决策一份文件，被取代时不删除，而是标记 superseded。

## 什么是 ADR

ADR（Architecture Decision Record）是一种轻量级文档，记录一项重要决策的：
- **背景**：当时面临什么问题、有哪些选项
- **决定**：选了哪个
- **理由**：为什么这么选
- **后果**：带来的好处与代价
- **状态**：accepted / superseded / deprecated

## 文件命名

`NNNN-短主题.md`，NNNN 是 4 位递增编号，不空号、不复用。

例：
- `0001-use-mybatis-plus.md`
- `0002-package-base-com-tyb-myblog-v2.md`
- `0003-no-default-jwt-secret.md`

## 模板

```markdown
# ADR-NNNN: 短主题

- 状态：accepted | superseded by ADR-XXXX | deprecated
- 日期：YYYY-MM-DD
- 决策者：xxx

## 背景

当时遇到的问题，涉及的约束。

## 备选方案

- 方案 A：……
- 方案 B：……
- 方案 C：……

## 决定

选 X。

## 理由

为什么选 X，关键权衡。

## 后果

正面：……
负面：……
后续需关注：……

## 相关

- 受影响的 rules：……
- 取代的 ADR：……
```

## 与其它目录的关系

- `rules/` 写"怎么做"，本目录写"为什么这么做"
- `arch/` 描述的结构由本目录的 ADR 决定
- 决策被取代时，原 ADR 状态改为 `superseded by ADR-XXXX`，文件保留

## 已有 ADR

> 待回填：从旧 specs/ 与 reviews/ 中提炼历史决策
