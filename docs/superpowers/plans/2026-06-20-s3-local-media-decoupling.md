# S3 与本地媒体映射解耦 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** S3 模式默认不装配本地 `/media/**` 映射，同时允许显式开启历史 LOCAL 附件兼容。

**Architecture:** 使用独立 Spring `Condition` 读取 `myblog.storage.local.web-enabled`；未显式配置时根据 `myblog.storage.type` 推导。`LocalStorageWebConfiguration` 仅在条件成立时注册，现有资源映射实现保持不变。

**Tech Stack:** Java 17、Spring Boot Condition/Binder、ApplicationContextRunner、JUnit 5、AssertJ。

---

### Task 1: 为本地媒体映射增加条件装配

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/web/LocalStorageWebCondition.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/web/LocalStorageWebConfiguration.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/config/StorageProperties.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/storage/web/LocalStorageWebConfigurationTest.java`

- [ ] **Step 1: 写 S3 默认不装配的失败测试**

创建测试上下文并先加入以下测试：

```java
package com.tyb.myblog.v2.common.storage.web;

import com.tyb.myblog.v2.common.storage.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageWebConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean(StorageProperties.class,
                            StorageProperties::new)
                    .withUserConfiguration(
                            LocalStorageWebConfiguration.class);

    @Test
    void doesNotRegisterLocalMediaMappingByDefaultForS3() {
        contextRunner
                .withPropertyValues("myblog.storage.type=S3")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(
                                LocalStorageWebConfiguration.class));
    }
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
mvn '-Dtest=LocalStorageWebConfigurationTest' test
```

Expected: FAIL，S3 上下文仍包含 `LocalStorageWebConfiguration`。

- [ ] **Step 3: 增加最小条件实现**

新增：

```java
package com.tyb.myblog.v2.common.storage.web;

import com.tyb.myblog.v2.common.storage.StorageType;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

final class LocalStorageWebCondition implements Condition {

    private static final String ENABLED_PROPERTY =
            "myblog.storage.local.web-enabled";
    private static final String TYPE_PROPERTY = "myblog.storage.type";

    @Override
    public boolean matches(
            ConditionContext context,
            AnnotatedTypeMetadata metadata) {
        Binder binder = Binder.get(context.getEnvironment());
        BindResult<Boolean> explicit = binder.bind(
                ENABLED_PROPERTY, Boolean.class);
        if (explicit.isBound()) {
            return explicit.get();
        }
        StorageType type = binder.bind(TYPE_PROPERTY, StorageType.class)
                .orElse(StorageType.LOCAL);
        return type == StorageType.LOCAL;
    }
}
```

把配置类的 `@ConditionalOnBean(StorageProperties.class)` 替换为：

```java
@Conditional(LocalStorageWebCondition.class)
```

并在 `StorageProperties.Local` 增加可空配置字段，便于配置元数据和维护者识别：

```java
private Boolean webEnabled;
```

- [ ] **Step 4: 运行测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=LocalStorageWebConfigurationTest' test
```

Expected: 1 test，0 failures，0 errors。

- [ ] **Step 5: 补齐四种装配矩阵测试**

追加：

```java
@Test
void registersLocalMediaMappingByDefaultForLocal() {
    contextRunner
            .withPropertyValues("myblog.storage.type=LOCAL")
            .run(context -> assertThat(context)
                    .hasSingleBean(LocalStorageWebConfiguration.class));
}

@Test
void registersLocalMediaMappingWhenExplicitlyEnabledForS3() {
    contextRunner
            .withPropertyValues(
                    "myblog.storage.type=S3",
                    "myblog.storage.local.web-enabled=true")
            .run(context -> assertThat(context)
                    .hasSingleBean(LocalStorageWebConfiguration.class));
}

@Test
void doesNotRegisterLocalMediaMappingWhenExplicitlyDisabledForLocal() {
    contextRunner
            .withPropertyValues(
                    "myblog.storage.type=LOCAL",
                    "myblog.storage.local.web-enabled=false")
            .run(context -> assertThat(context)
                    .doesNotHaveBean(LocalStorageWebConfiguration.class));
}
```

- [ ] **Step 6: 运行局部回归**

Run:

```powershell
mvn '-Dtest=LocalStorageWebConfigurationTest,StoragePropertiesTest,LocalStorageServiceTest,S3StorageServiceTest,StorageServiceRegistryTest,ArchitectureRulesTest' test
```

Expected: 全部通过，0 failures，0 errors。

- [ ] **Step 7: 提交条件装配**

```powershell
git diff --check
git diff --stat
git status --short
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/storage'
git commit -m '按存储模式装配本地媒体映射'
```

### Task 2: 同步附件与发布文档

**Files:**
- Modify: `docs/project-handbook/api-contract/attachment.md`
- Modify: `docs/project-handbook/workflows/release-checklist.md`

- [ ] **Step 1: 更新附件配置契约**

在存储配置部分明确：

```markdown
- `/media/**` 只在 LOCAL 模式默认注册；S3 模式默认不注册。
- S3 迁移期如需继续读取历史 LOCAL 附件，可显式设置
  `MYBLOG_STORAGE_LOCAL_WEB_ENABLED=true`；历史附件迁移完成后必须关闭。
```

- [ ] **Step 2: 更新发布检查**

在通用或存储检查中加入：

```markdown
- [ ] S3 模式确认 `/media/**` 未注册；若显式开启历史 LOCAL 兼容，确认目录存在并记录关闭时间点。
```

- [ ] **Step 3: 文档和阶段验证**

Run:

```powershell
rg -n 'web-enabled|/media/|历史 LOCAL' 'docs/project-handbook'
git diff --check
mvn clean test
```

Expected: 文档语义可定位；全量测试 0 failures、0 errors，Docker 条件测试如环境不可用需单独记录。

- [ ] **Step 4: 提交文档**

```powershell
git diff --stat
git status --short
git add -- 'docs/project-handbook/api-contract/attachment.md' 'docs/project-handbook/workflows/release-checklist.md'
git commit -m '说明本地媒体映射启用边界'
```

### Task 3: 最终范围检查

**Files:**
- No code changes expected.

- [ ] **Step 1: 检查提交和工作树**

Run:

```powershell
git log --oneline -4
git status --short
```

Expected: 设计、条件装配、文档分别为单目的中文提交；工作树干净。
