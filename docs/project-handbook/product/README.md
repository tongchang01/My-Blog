# product/ — 业务规格

> 本目录回答："这个博客系统该做什么？哪些功能要保留 / 删除 / 新增？"
> 性质：业务层文档，**先于代码与表结构**确定。
> 当前状态：feature-inventory.md ⑳ 项已全部回填决策；use-cases / business-rules / data-model 已生成，等待评审后进入 Flyway `V1__init.sql`。

## 与代码层文档的区别

| 类型 | 关注 | 例 |
|------|------|----|
| `rules/` / `arch/` / `decisions/` | **怎么实现** | "Controller 返回 ApiResponse" |
| `product/` | **要实现什么** | "用户可以给文章点赞" |

`product/` 不写技术，只写业务。技术实现见 arch 和 rules。

## 计划包含的文件

| 文件 | 内容 | 状态 |
|------|------|------|
| `feature-inventory.md` | V1 全部功能清单 + 去/留/待讨论标注 | ✅ ⑳ 项已全部回填 R1-R7 决策 |
| `use-cases.md` | 用户能做什么（按角色：GUEST / DEMO / ADMIN / 系统任务） | ✅ 已生成 |
| `business-rules.md` | 关键业务规则（评论审核策略、文章可见性、权限模型等） | ✅ 已生成 |
| `data-model.md` | 业务实体与关系（领域模型，先于数据库表） | ✅ 已生成 |
| `er-diagram.md` | ER / 领域关系图（Mermaid） | ✅ 已生成 |
| `roles-and-permissions.md` | 角色权限模型（重设计后的，简化版） | ⏳ |

## 工作顺序

```
1. feature-inventory.md 标注（你逐条决定去/留/改/新增）
       ↓
2. use-cases.md（基于"留"+"新增"的功能写用例）
       ↓
3. business-rules.md（明确每个用例的业务规则）
       ↓
4. data-model.md（从业务规则提炼实体）
       ↓
5. arch/schema-design.md（把领域模型落到具体表设计）
       ↓
6. 后端代码 + 前端实现
```

## 写作约定

- 中文为主
- 用"用户能 / 不能做什么"的视角，不用"系统提供 / 不提供什么接口"
- 每条业务规则要可测试（能写出验收标准）
- 涉及取舍的写"为什么"，避免后人遗忘
