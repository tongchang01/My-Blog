# MyBlog V2

[简体中文](README.md) | **English** | [日本語](README.ja.md)

MyBlog V2 is a modular-monolith personal blogging system composed of a Spring Boot API, a public blog, and an independently built admin console. V2 is the active codebase; the V1 directories are retained only as historical reference.

## Current capabilities

- Trilingual public blog with home, articles, categories, tags, archives, search, about, friend links, and article comments.
- Home-page curation with one pinned article, up to two featured articles, and a regular feed.
- Admin workflows for articles, taxonomy, comments, friend links, attachments, site settings, author profile, and traffic dashboards.
- ADMIN/DEMO roles, JWT access tokens, database-backed refresh-token rotation, logout, and password-change revocation.
- Markdown content, sanitized comment HTML, Flyway V1–V4, 14 tables, soft deletion, and audit fields.
- Local/test/prod profiles, LOCAL/S3 storage, a health endpoint, and GitHub Actions CI.

Password-protected article unlocking and a public guestbook page are not implemented. The production server, proxy, backup, and rollback topology is also not yet confirmed. See [current status](docs/handbook/start-here/current-status.md) and [open issues](docs/handbook/start-here/open-issues.md).

## Architecture

The backend root package is `com.tyb.myblog.v2`. Five business modules—identity, content, comment, system, and stats—use web/application/domain/infrastructure layers. Cross-module calls go through application contracts, and ArchUnit tests enforce the dependency direction. The common package provides shared security, storage, mail, time, web, and persistence infrastructure.

| Area | Technology |
| --- | --- |
| Backend | Java 17, Spring Boot 3.5, Spring Security, MyBatis-Plus, Flyway |
| Data and content | MySQL 8, CommonMark, OWASP HTML Sanitizer, Caffeine |
| Storage and mail | Local files / AWS S3, optional Resend |
| Blog | Vue 3, Pinia, Vue Router, vue-i18n, markdown-it, Vite, Vitest |
| Admin | Vue 3, Pinia, Element Plus, ECharts, Vite, Vitest |
| Testing | JUnit 5, Spring Boot Test, ArchUnit, H2, Testcontainers |

V2 has no runtime dependency on Redis, RabbitMQ, Elasticsearch, or Quartz. Its Caffeine rate limits assume a single backend instance.

## Repository layout

```text
MyBlog-springboot-v2/   V2 backend
frontend/apps/blog/     V2 public blog
frontend/apps/admin/    V2 admin console
docs/                   Current documentation and governance
MyBlog-springboot/      V1 backend reference
MyBlog-vue/             V1 frontend reference
```

## Local development

Requirements: JDK 17, Maven 3.9.x, MySQL 8, Node 20.19+ or a compatible Node 22+ release, pnpm 9, and an `Asia/Tokyo` JVM timezone.

```powershell
# Backend
cd MyBlog-springboot-v2
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Blog: http://localhost:5173
cd frontend/apps/blog
corepack pnpm install --frozen-lockfile
corepack pnpm dev

# Admin: http://localhost:8848
cd frontend/apps/admin
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

The backend requires database, JWT, and statistics secrets. Follow the [local setup](docs/handbook/ops/local-development.md) and [environment variable](docs/handbook/ops/environment.md) guides. The local MySQL helper scripts currently have a PowerShell-version compatibility issue; read the [MySQL guide](docs/handbook/ops/local-mysql-development.md) before using them.

## Verification

```powershell
cd MyBlog-springboot-v2
mvn clean test

corepack pnpm --dir frontend/apps/blog test
corepack pnpm --dir frontend/apps/blog typecheck
corepack pnpm --dir frontend/apps/blog build

corepack pnpm --dir frontend/apps/admin test
corepack pnpm --dir frontend/apps/admin typecheck
corepack pnpm --dir frontend/apps/admin build
```

## Documentation

- [Documentation index](docs/README.md)
- [Engineering handbook](docs/handbook/README.md)
- [API contracts](docs/handbook/api/README.md)
- [Product specification](docs/handbook/product/README.md)
- [Operations and release](docs/handbook/ops/README.md)

## License

This repository uses the [MIT License](LICENSE). The blog is derived from [Hexo Theme Aurora](https://github.com/auroral-ui/hexo-theme-aurora), and the admin app is based on [pure-admin-thin](https://github.com/pure-admin/pure-admin-thin). Their upstream licenses and provenance records are preserved in the application directories.
