# ADR-0008: 引入 Hutool 但按需引入子模块

- 状态：accepted
- 日期：2026-04
- 决策者：项目负责人

## 背景

V1 部分代码使用 Hutool 工具集。Hutool `hutool-all` 体积大，引入大量未使用工具。

## 决定

V2 允许使用 Hutool，但：
- 🔴 禁止 `hutool-all`
- 仅按需引入 `hutool-core`、`hutool-crypto` 等具体子模块
- 优先使用 JDK 标准库（Java 17 提供大量工具方法）
- 同等功能下，Spring 已提供的工具（如 `StringUtils`）优先于 Hutool

## 理由

- 减小打包体积
- 避免引入未审计的工具方法
- 明确依赖边界，便于安全审计

## 后果

正面：依赖清晰，体积小
负面：需要在 review 时检查 import，避免误引 `hutool-all`

## 相关

- 相关 rules：`rules/package-layout.md`（依赖原则）
