# ADR-0002: 基础包名定为 com.tyb.myblog.v2

- 状态：accepted
- 日期：2026-04
- 决策者：项目负责人

## 背景

V1 使用 `com.aurora.myblog` 包（原作者命名）。重构时需要：

- 与 V1 完全隔离（避免误引用 V1 代码）
- 体现"v2"版本标识（V1 仍保留运行）
- 使用项目所有者标识

## 备选方案

- 方案 A：沿用 `com.aurora.myblog`，加 v2 子包
- 方案 B：新建 `com.tyb.myblog.v2`
- 方案 C：完全重命名为 `com.tyb.blog`

## 决定

选 B：所有 V2 代码统一在 `com.tyb.myblog.v2.*` 下。

## 理由

- `tyb` 是项目所有者命名空间，与原作者隔离
- `v2` 显式版本号，与 V1 共存期间清晰区分
- 与 V1 完全独立，IDE 自动补全不会混淆
- 未来若 V2 稳定可去掉 v2 后缀（需 ADR 取代本决定）

## 后果

正面：
- 完全杜绝 V1/V2 包混淆
- ArchUnit 可基于包前缀写规则

负面：
- 包路径较长
- V2 稳定后需要再做一次重命名（已接受）

## 相关

- 相关 rules：`rules/package-layout.md`
