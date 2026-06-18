# Backend V2 Release Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce an evidence-backed, read-only release review of the current V2 backend without assuming either its design documents or its implementation are correct.

**Architecture:** Build one review report incrementally from a requirement-to-code-to-verification evidence matrix. Audit document validity first, then architecture, contracts, security, persistence, comments, and tests; only manually confirmed findings enter the final severity-ranked report.

**Tech Stack:** Java 17, Spring Boot 3.5, Maven, JUnit 5, ArchUnit, MyBatis-Plus, Flyway, H2, MySQL 8, PowerShell, ripgrep

---

## File structure

**Read-only inputs:**

- `docs/project-handbook/**/*.md` — current product, API, architecture, rules, ADR, status, and workflow claims.
- `docs/superpowers/specs/**/*.md` — accepted implementation-era designs that may still affect current behavior.
- `docs/superpowers/reviews/**/*.md` — historical findings; evidence only, never assumed current.
- `MyBlog-springboot-v2/pom.xml` — dependencies, build plugins, and test setup.
- `MyBlog-springboot-v2/src/main/**` — production implementation, configuration, SQL, and Flyway migrations.
- `MyBlog-springboot-v2/src/test/**` — unit, integration, architecture, H2, and Testcontainers tests.

**Created output:**

- `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md` — the only review result; contains scope, evidence, findings, verified boundaries, limitations, and remediation order.

No reviewed source file is modified during execution.

### Task 1: Freeze the review baseline and create the evidence ledger

**Files:**

- Read: `docs/superpowers/specs/2026-06-18-backend-v2-release-review-design.md`
- Read: `MyBlog-springboot-v2/pom.xml`
- Create: `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **Step 1: Record immutable Git and toolchain baseline**

Run from the worktree root:

```powershell
git status --short
git branch --show-current
git rev-parse HEAD
git log -5 --oneline
java -version
mvn -version
& 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe' --version
```

Expected: named branch `backend-v2-refactor`, clean worktree before the report is created, and explicit Java/Maven/MySQL versions. If the worktree contains unrelated changes, stop and report them before continuing.

- [ ] **Step 2: Inventory the review surface**

Run:

```powershell
(Get-ChildItem -Recurse -File docs\project-handbook).Count
(Get-ChildItem -Recurse -File MyBlog-springboot-v2\src\main).Count
(Get-ChildItem -Recurse -File MyBlog-springboot-v2\src\test).Count
rg --files MyBlog-springboot-v2/src/main/resources/mapper
rg --files MyBlog-springboot-v2/src/main/resources/db/migration
rg --files MyBlog-springboot-v2/src/test | rg 'Test\.java$'
```

Expected: a complete count and path inventory. Record counts as scope evidence, not as quality claims.

- [ ] **Step 3: Create the report skeleton**

Create the report with these fixed sections:

```markdown
# 后端 V2 第一版发布前 Review

## 1. 总体结论
## 2. 范围、基线与限制
## 3. 设计文档有效性与冲突
## 4. Critical
## 5. Important
## 6. Minor
## 7. 已验证的关键边界
## 8. 未覆盖风险
## 9. 修复批次与顺序
## 10. 前端联调与发布门槛
```

Under section 2, record the exact commit SHA, branch, tool versions, database used, Docker absence, and the rule that reviewed files remain untouched.

- [ ] **Step 4: Establish the finding template**

Every finding must use this shape:

```markdown
### [Severity-N] 简短标题

- 类型：设计缺陷 / 实现缺陷 / 测试缺口 / 文档漂移
- 阻塞：前端联调 / 发布 / 不阻塞
- 要求证据：文件与行号，并说明该要求是否有效
- 实现证据：文件与行号
- 验证证据：测试、命令或缺失证据
- 影响：可复现的实际后果
- 建议：最小修复方向，不在本轮实施
```

Expected: no finding is accepted without a concrete effect and file-level evidence.

### Task 2: Audit document authority, currency, and reasonableness

**Files:**

- Read: `docs/project-handbook/CLAUDE.md`
- Read: `docs/project-handbook/INDEX.md`
- Read: `docs/project-handbook/status.md`
- Read: `docs/project-handbook/roadmap.md`
- Read: `docs/project-handbook/m3-preflight-review.md`
- Read: `docs/project-handbook/pitfalls.md`
- Read: `docs/project-handbook/product/**/*.md`
- Read: `docs/project-handbook/api-contract/**/*.md`
- Read: `docs/project-handbook/arch/**/*.md`
- Read: `docs/project-handbook/rules/**/*.md`
- Read: `docs/project-handbook/decisions/**/*.md`
- Modify: `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **Step 1: Build the document status table**

Run:

```powershell
rg -n "状态：|当前阶段|下一步|已完成|待完成|tests|测试|JdbcTemplate|MyBatis|Redis|Docker|Flyway|V1__init" docs/project-handbook
```

For each controlling document, classify relevant claims as `有效`, `被替代`, `冲突`, `过时`, `不合理`, or `证据不足`. Include last-known dates and the newer document or code that challenges a claim.

- [ ] **Step 2: Validate rule references and filenames**

Run:

```powershell
rg -n "decisions/|rules/|arch/|product/|api-contract/" docs/project-handbook
rg --files docs/project-handbook/decisions docs/project-handbook/rules docs/project-handbook/arch docs/project-handbook/product docs/project-handbook/api-contract
```

Expected: identify broken links, renamed ADR references, stale module names, and rules pointing to removed classes. Confirm each suspected broken reference against the filesystem before reporting it.

- [ ] **Step 3: Challenge the comment rules**

Read `rules/comment-style.md`, ADR-0011, and the current code-comment specification. Evaluate separately:

1. whether Chinese-only comments remain the current language decision;
2. whether mandatory class/field/method coverage is proportionate;
3. whether the rule wrongly rewards redundant comments;
4. whether OpenAPI descriptions and Javadoc are being conflated;
5. whether references to V1 compatibility are obsolete.

Expected: the report distinguishes an unreasonable rule from code non-compliance. Do not count missing comments until the rule itself is classified.

- [ ] **Step 4: Challenge the SQL placement rules**

Read `rules/sql-placement.md`, ADR-0010, persistence strategy, and the SQL placement specification. Evaluate:

1. BaseMapper versus wrapper versus annotation versus XML boundaries;
2. whether `projection => XML` and `SQL > 10 lines => XML` remain useful hard rules;
3. stale V1 compatibility requirements;
4. whether XML comment requirements explain current business semantics rather than removed legacy tables;
5. whether Flyway and runtime SQL are clearly separated.

- [ ] **Step 5: Write only confirmed documentation findings**

Add the document status table and confirmed conflicts to sections 3–6. Historical Review statements must be reverified against current files before inclusion.

### Task 3: Audit architecture and dependency boundaries

**Files:**

- Read: `docs/project-handbook/arch/module-map.md`
- Read: `docs/project-handbook/rules/package-layout.md`
- Read: `docs/project-handbook/decisions/0001-modular-monolith.md`
- Read: `docs/project-handbook/decisions/0003-four-layer-architecture.md`
- Read: `docs/project-handbook/decisions/0004-six-business-modules.md`
- Read: `docs/project-handbook/decisions/0012-archunit-guards.md`
- Read: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
- Read: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/*.java`
- Modify: `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **Step 1: Compare documented and actual modules**

Run:

```powershell
Get-ChildItem MyBlog-springboot-v2\src\main\java\com\tyb\myblog\v2 -Directory | Select-Object -ExpandProperty Name
rg -n "identity|content|comment|system|stats|common|infrastructure|common-infra" docs/project-handbook/arch docs/project-handbook/decisions docs/project-handbook/rules
```

Expected: one matrix of documented modules, actual packages, and ArchUnit coverage.

- [ ] **Step 2: Run executable architecture guards**

Run from `MyBlog-springboot-v2/`:

```powershell
mvn -Dtest=ArchitectureRulesTest test
```

Expected: record exact test count and failures. A passing suite proves only encoded rules, not that the encoded rules are complete.

- [ ] **Step 3: Search for unguarded dependency violations**

Run targeted searches:

```powershell
rg -n "^import com\.tyb\.myblog\.v2\.(identity|content|comment|system|stats)\.(domain|infrastructure|web)" MyBlog-springboot-v2/src/main/java
rg -n "BaseMapper|Mapper<|Lambda(Query|Update)Wrapper|JdbcTemplate" MyBlog-springboot-v2/src/main/java/**/application MyBlog-springboot-v2/src/main/java/**/web
rg -n "org\.springframework|com\.baomidou|jakarta\.servlet" MyBlog-springboot-v2/src/main/java/**/domain
```

If PowerShell glob expansion prevents `**`, rerun against `src/main/java` and manually filter paths. Inspect every match; imports alone are leads, not findings.

- [ ] **Step 4: Trace each cross-module gateway**

For each actual cross-module import, verify the call enters through the target module's application-facing contract and does not expose its persistence entity, repository, mapper, web DTO, or framework type.

- [ ] **Step 5: Record architecture findings and verified boundaries**

Add violations to severity sections. Add passing, manually checked boundaries to section 7 so the report does not consist only of defects.

### Task 4: Audit API contracts, domain rules, and authorization

**Files:**

- Read: `docs/project-handbook/api-contract/*.md`
- Read: `docs/project-handbook/product/business-rules.md`
- Read: `docs/project-handbook/product/use-cases.md`
- Read: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/web/**/*.java`
- Read: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/application/**/*.java`
- Read: matching controller, OpenAPI, application, and integration tests
- Modify: `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **Step 1: Inventory implemented endpoints**

Run:

```powershell
rg -n "@(RequestMapping|GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)|@PreAuthorize" MyBlog-springboot-v2/src/main/java
```

Build a matrix containing method, path, controller, required role, request type, response type, documented contract, and test evidence.

- [ ] **Step 2: Verify response and error contracts**

Trace representative public, auth, ADMIN, and DEMO flows through controller, application service, exception handler, and tests. Check status codes, `ApiResponse`, pagination fields, validation errors, conflict errors, and internal-error sanitization.

- [ ] **Step 3: Verify role and visibility boundaries**

Inspect and cross-check tests for:

- anonymous public access;
- ADMIN writes;
- DEMO allowed reads and forbidden writes;
- ADMIN-only sensitive reads;
- DRAFT, PRIVATE, PASSWORD, PUBLISHED, and SCHEDULED article visibility;
- comment audit fields and moderation actions.

Expected: every claimed boundary has both implementation and test evidence, or is reported as a test gap.

- [ ] **Step 4: Verify deterministic contract details**

Check pagination defaults and maxima, stable secondary sorting, multilingual fallback, title backfill, 30-day zero-fill, average daily UV denominator, `dailyUvSum`, and TOP tie ordering against contract, code, and tests.

- [ ] **Step 5: Record contract findings**

Only report a mismatch after confirming the controlling contract is current. Separate “contract is stale” from “implementation violates contract.”

### Task 5: Audit security, secrets, and environment configuration

**Files:**

- Read: `docs/project-handbook/rules/security-baseline.md`
- Read: `docs/project-handbook/arch/auth-flow.md`
- Read: `MyBlog-springboot-v2/src/main/resources/application*.yml`
- Read: security, auth, rate-limit, upload, proxy, and startup-validation production code and tests
- Modify: `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **Step 1: Scan for secret and unsafe-default patterns**

Run:

```powershell
rg -n -i "password\s*[:=]|secret\s*[:=]|api[-_]?key\s*[:=]|token\s*[:=]|root|allow-all|permitAll|allowed-origins|\*" MyBlog-springboot-v2/src/main MyBlog-springboot-v2/pom.xml
```

Inspect every match in context. Test fixtures and environment-variable placeholders are not secrets.

- [ ] **Step 2: Trace authentication lifecycle**

Follow login → access token verification → refresh → logout → password change. Verify token type isolation, issuer/expiry/version checks, refresh hash storage, revocation, transaction boundaries, and audit updates.

- [ ] **Step 3: Review public endpoint and method matching**

Compare configured public endpoints, Spring Security matchers, controller paths, HTTP methods, and security tests. Confirm no path-only matcher accidentally permits another method.

- [ ] **Step 4: Review request-origin security**

Inspect CORS, trusted proxy handling, forwarded headers, client IP resolution, login/unlock/comment rate limits, and logging. Check that user-controlled headers do not silently become trusted identity or audit data.

- [ ] **Step 5: Review attachment and untrusted-content handling**

Trace attachment upload validation, storage path construction, MIME/size checks, public access, deletion/reference checks, comment Markdown sanitization, and article body rendering assumptions.

- [ ] **Step 6: Record security findings**

Critical requires a concrete reachable path or direct secret exposure. Unproven concerns are placed in section 8 as limitations, not promoted to defects.

### Task 6: Audit persistence, SQL style, Flyway, and MySQL behavior

**Files:**

- Read: `docs/project-handbook/rules/sql-placement.md`
- Read: `docs/project-handbook/arch/schema-design.md`
- Read: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/infrastructure/persistence/**/*.java`
- Read: `MyBlog-springboot-v2/src/main/resources/mapper/**/*.xml`
- Read: `MyBlog-springboot-v2/src/main/resources/db/migration/*.sql`
- Read: persistence and migration tests
- Modify: `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **Step 1: Inventory every runtime SQL mechanism**

Run:

```powershell
rg -n "@(Select|Insert|Update|Delete)\b|JdbcTemplate|NamedParameterJdbcTemplate|createNativeQuery|SELECT |INSERT |UPDATE |DELETE " MyBlog-springboot-v2/src/main/java
rg -n "<(select|insert|update|delete)\b|<foreach|<if|<where|<choose|JOIN|GROUP BY|ORDER BY|LIMIT" MyBlog-springboot-v2/src/main/resources/mapper
```

Classify each hit as BaseMapper/wrapper, annotation SQL, XML, Flyway, or prohibited Java SQL. Manually confirm joins, dynamic filters, aggregation, pagination, projections, and batch IN placement.

- [ ] **Step 2: Validate Mapper/XML linkage**

For each XML file verify:

- path is `resources/mapper/{module}/`;
- filename matches Mapper interface;
- namespace matches fully qualified interface;
- statement id matches a method;
- result type/map matches projection fields;
- complex business rules and non-obvious sorting have useful Chinese comments.

- [ ] **Step 3: Review SQL safety and determinism**

Inspect `${...}` substitutions, user-selectable sort fields, LIKE escaping, null ordering, tie-breaking order, pagination count queries, empty collections, batch sizes, and potential N+1 loops.

Run:

```powershell
rg -n '\$\{' MyBlog-springboot-v2/src/main/resources/mapper MyBlog-springboot-v2/src/main/java
rg -n "ORDER BY|LIMIT|OFFSET|LIKE|IN \(|FOR UPDATE" MyBlog-springboot-v2/src/main/resources/mapper MyBlog-springboot-v2/src/main/java
```

- [ ] **Step 4: Review Flyway history and schema invariants**

Run:

```powershell
rg -n -i "foreign key|timestamp|on update|auto_increment|datetime|unique|index|key " MyBlog-springboot-v2/src/main/resources/db/migration
git log --follow --oneline -- MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql
```

Verify migration immutability from Git history, V1/V2 order, no DB foreign keys, ID strategy, audit/soft-delete columns, index support for actual query predicates, and precision compatibility with Java time values.

- [ ] **Step 5: Use local MySQL for read-only schema confirmation**

Connect using environment-provided credentials; do not embed the password in the report or command history. Run only metadata reads:

```sql
SELECT VERSION();
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SHOW TABLES;
SHOW CREATE TABLE t_article;
SHOW CREATE TABLE t_refresh_token;
SHOW CREATE TABLE t_page_view_daily;
```

Expected: schema state is recorded without DDL/DML mutations.

- [ ] **Step 6: Record persistence findings**

For style findings, cite both the rule validity decision from Task 2 and the concrete SQL. For correctness findings, include query/test/MySQL evidence.

### Task 7: Audit comment quality and OpenAPI descriptions

**Files:**

- Read: effective comment requirements determined in Task 2
- Read: `MyBlog-springboot-v2/src/main/java/**/*.java`
- Read: `MyBlog-springboot-v2/src/main/resources/mapper/**/*.xml`
- Read: `MyBlog-springboot-v2/src/test/java/**/*.java`
- Modify: `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **Step 1: Inventory comment-sensitive types**

Run:

```powershell
rg -n "^(public )?(class|record|interface|enum) |@(RestController|Configuration|ConfigurationProperties|TableName|Schema)" MyBlog-springboot-v2/src/main/java
rg -n "@Schema\(description|/\*\*|//|<!--" MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test
```

Group results by Entity, DTO/Command/Query/Response, Controller, Application Service, Enum, Mapper/Repository, configuration, XML, and special business logic.

- [ ] **Step 2: Review representative samples from every module and layer**

Review all high-risk public contracts and persistence types, then a documented sample from remaining repetitive types. Check whether comments explain business meaning, constraints, permission, state, ordering, time, or production risk.

- [ ] **Step 3: Detect misleading and redundant comments**

Look for comments that:

- merely translate class/method/field names;
- describe removed V1 compatibility;
- conflict with current code;
- repeat OpenAPI text without adding maintenance value;
- claim behavior not protected by code or tests.

- [ ] **Step 4: Compare Javadoc and OpenAPI semantics**

For request/response fields exposed through OpenAPI, compare Javadoc, validation annotations, `@Schema`, JSON names, and actual API tests. Report semantic conflict before simple absence.

- [ ] **Step 5: Record comment findings proportionately**

Missing explanation for security, state transitions, audit, time, SQL, or production configuration may be Important. Mechanical comment coverage and isolated stale wording are normally Minor. Do not produce one finding per field; group by coherent rule and module.

### Task 8: Audit tests, runtime evidence, dependencies, and release gaps

**Files:**

- Read: `docs/project-handbook/rules/testing-policy.md`
- Read: `MyBlog-springboot-v2/pom.xml`
- Read: `MyBlog-springboot-v2/src/test/**/*.java`
- Read: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Modify: `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **Step 1: Map tests to critical behaviors**

Run:

```powershell
rg -n "@Test|@ParameterizedTest|@SpringBootTest|@WebMvcTest|@EnabledIf|@Disabled|Testcontainers|MySQLContainer" MyBlog-springboot-v2/src/test
```

Map tests to auth lifecycle, ADMIN/DEMO boundaries, content visibility, comment transitions, attachment security, stats aggregation, complex SQL, Flyway, and configuration startup.

- [ ] **Step 2: Inspect test quality and isolation**

Review whether critical tests assert real outputs and state rather than mock invocation only. Check shared database cleanup, ordering assumptions, fixed clocks, parallel safety, conditional skips, and H2-only SQL/type assertions.

- [ ] **Step 3: Run the complete H2 suite**

Run from `MyBlog-springboot-v2/`:

```powershell
mvn clean test
```

Expected: record exact tests, failures, errors, skips, duration, and names/reasons of skipped tests. Any failure pauses the review conclusion until classified.

- [ ] **Step 4: Run the broad local MySQL suite**

Use only the user-authorized disposable `myblog_v2_dev` test database; these integration tests may insert, update, delete, or reset test data. Do not run them against any non-test schema. Provide datasource URL, username, password, MySQL driver, and Flyway flag through process environment variables without writing credentials to the report. Run:

```powershell
mvn '-Dtest=**/*Test,!FlywayMigrationTest,!RefreshSessionTransactionIntegrationTest,!DatabasePasswordAccountRepositoryTest,!DatabaseUserProfileRepositoryTest' test
```

Expected: record exact results and explicitly explain why the four H2-specific fixture classes are excluded. Confirm `ArticleIntegrationTest` and persistence/integration tests execute against MySQL.

- [ ] **Step 5: Inspect dependency and build policy**

Run:

```powershell
mvn dependency:tree
mvn help:effective-pom -Doutput=target/effective-pom-review.xml
```

Inspect direct dependencies, duplicate stacks, Flyway MySQL support, test-only scope, Maven Enforcer, compiler level, and dependency convergence. Delete `target/effective-pom-review.xml` after inspection because it is generated evidence, not a review deliverable.

- [ ] **Step 6: Record runtime limitations**

Document Docker/Testcontainers gaps, production-profile startup not exercised, external mail/storage dependencies, and any behavior only proven on local MySQL. Do not call an unexecuted check “passed.”

### Task 9: Reconcile findings and finalize the review report

**Files:**

- Read: `docs/superpowers/specs/2026-06-18-backend-v2-release-review-design.md`
- Read: all evidence collected in `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`
- Modify: `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **Step 1: Deduplicate and challenge every finding**

For each candidate, ask:

1. Is the cited requirement current and reasonable?
2. Is the implementation evidence complete and in context?
3. Does a test or runtime result disprove the concern?
4. Is the impact concrete rather than stylistic preference?
5. Is severity consistent with the design criteria?

Remove or downgrade findings that fail these checks.

- [ ] **Step 2: Verify evidence links and line numbers**

Reopen every cited file at the referenced line. Critical and Important findings require requirement, implementation, and verification evidence. Minor documentation drift may use two evidence types when runtime proof is irrelevant.

- [ ] **Step 3: Write the release verdict**

The conclusion must separately answer:

- Can frontend integration begin now?
- Is the backend a release candidate now?
- Which findings block frontend integration?
- Which findings block production release only?
- Which limitations remain because Docker or production infrastructure is unavailable?

- [ ] **Step 4: Build the remediation sequence**

Group fixes into independently committable batches ordered by:

1. Critical security/data correctness;
2. frontend contract blockers;
3. MySQL/Flyway/transaction correctness;
4. Important architecture and test gaps;
5. documentation, comments, and low-risk cleanup.

Each batch names its files, verification command, and whether it changes behavior. Do not implement any batch.

- [ ] **Step 5: Verify review scope and report quality**

Run:

```powershell
git diff --check
git status --short
git diff --stat
rg -n "TBD|TODO|待补|稍后确认|可能有问题" docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md
```

Expected: only the review report is uncommitted during execution; no placeholder conclusions; no reviewed source or existing design document changed.

- [ ] **Step 6: Commit the final report**

Run:

```powershell
git add -- docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md
git diff --cached --check
git diff --cached --stat
git commit -m "完成后端V2第一版发布前审查"
```

Expected: one report-only Chinese commit. Preserve the branch and worktree; do not merge or push.
