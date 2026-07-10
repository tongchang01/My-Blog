# 新增后端业务模块

> 状态：当前有效
> 适用范围：V2 顶层业务模块
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
> 权威程度：标准流程

新增顶层模块属于架构变更，开始实现前必须确认现有五个模块无法合理承载该职责，并记录 ADR。

1. 定义模块职责、拥有的数据、公开 application 能力和允许的跨模块依赖。
2. 建立 `web / application / domain / infrastructure` 四层包。
3. 先定义领域模型、规则和端口，再实现 infrastructure 适配、application 用例和 web 接口。
4. 把模块名加入 `ArchitectureRulesTest`，补跨模块限制、循环依赖和故意违规 fixture。
5. 更新 `../architecture/module-map.md`、`../architecture/request-flow.md`、产品规格和 ADR 导航。
6. 编写各层局部测试并运行 `ArchitectureRulesTest`。
7. 阶段结束运行后端全量测试。

禁止通过 common 承载新模块的专属业务对象，也禁止以直接访问其他模块 Mapper 的方式完成集成。
