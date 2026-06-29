# Article Publish Time Precision Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent newly published articles from briefly disappearing on MySQL because `DATETIME(0)` rounds fractional seconds.

**Architecture:** Normalize article publication timestamps to whole-second precision in `ArticleCreateService`, matching the existing MySQL schema. Keep the change local to article creation and preserve the frozen V1 migration.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, AssertJ, Mockito, Maven, MySQL 8

---

### Task 1: Lock and fix publication timestamp precision

**Files:**
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/ArticleWriteServiceTest.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/ArticleCreateService.java`

- [x] **Step 1: Write the failing test**

Use a fixed clock containing fractional seconds, create a published article without an explicit `publishAt`, and assert that the persisted `NewArticle.publishAt()` has zero nanoseconds.

- [x] **Step 2: Verify RED**

Run: `mvn -Dtest=ArticleWriteServiceTest#createsPublishedArticleWithDatabasePrecision test`

Expected: FAIL because the current service preserves the clock nanoseconds.

- [x] **Step 3: Implement the minimal fix**

Normalize the resolved publication timestamp with `withNano(0)` before constructing `NewArticle`.

- [x] **Step 4: Verify GREEN and regressions**

Run the focused unit test, full H2 suite, and broad local MySQL suite. The MySQL run must include `ArticleIntegrationTest`; only the known H2-specific fixture classes may be excluded.

- [ ] **Step 5: Commit**

Inspect `git diff --stat` and `git status --short`, then create one Chinese commit containing the regression test, minimal implementation, and this plan.
