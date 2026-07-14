# MyBlog V2

[简体中文](README.md) | **English** | [日本語](README.ja.md)

MyBlog V2 is a modular-monolith personal blogging system composed of a Spring Boot API, a public blog, and an independently built admin console. V2 is the active codebase and is live at the [public blog](https://tong-yibin.com) and [admin console](https://admin.tong-yibin.com); V1 source has been removed from the main branch and preserved in the read-only `archive/v1-master-2026-06-26` branch.

## Current capabilities

- Trilingual public blog with home, articles, categories, tags, archives, search, about, a guestbook, friend links, and article comments.
- Home-page curation with one pinned article, up to two featured articles, and a regular feed.
- Admin workflows for articles, taxonomy, comments, friend links, attachments, site settings, author profile, and traffic dashboards.
- ADMIN/DEMO roles, JWT access tokens, database-backed refresh-token rotation, logout, and password-change revocation.
- Markdown content, sanitized comment HTML, Flyway V1–V4, 14 tables, soft deletion, and audit fields.
- Local/test/prod profiles, LOCAL/S3 storage, a health endpoint, and GitHub Actions CI/CD with GHCR and Docker Compose deployment.

Password-protected article unlocking is not implemented. Publishing to `main` builds commit-SHA-tagged GHCR images and automatically deploys them to AWS EC2; public HTTPS health endpoints are smoke-tested after deployment. See [open issues](docs/handbook/start-here/open-issues.md) for remaining work.

## Architecture

The backend root package is `com.tyb.myblog.v2`. Five business modules—identity, content, comment, system, and stats—use web/application/domain/infrastructure layers. Cross-module calls go through application contracts, and ArchUnit tests enforce the dependency direction. The common package provides shared security, storage, mail, time, web, and persistence infrastructure.

| Area | Technology |
| --- | --- |
| Backend | Java 17, Spring Boot 3.5, Spring Security, MyBatis-Plus, Flyway |
| Data and content | MySQL 8, CommonMark, OWASP HTML Sanitizer, Caffeine |
| Storage and mail | Local files / AWS S3, optional Resend |
| Blog | Vue 3, Pinia, Vue Router, vue-i18n, markdown-it, Vite, Vitest |
| Admin | Vue 3, Pinia, Element Plus, ECharts, Vite, Vitest |
| Delivery and deployment | Docker Compose, Caddy, GitHub Actions, GHCR, AWS EC2 / Route 53 / S3 |
| Testing | JUnit 5, Spring Boot Test, ArchUnit, H2, Testcontainers |

V2 has no runtime dependency on Redis, RabbitMQ, Elasticsearch, or Quartz. Its Caffeine rate limits assume a single backend instance.

## Repository layout

```text
MyBlog-springboot-v2/   V2 backend
frontend/apps/blog/     V2 public blog
frontend/apps/admin/    V2 admin console
docs/                   Current documentation and governance
```

Use the read-only `archive/v1-master-2026-06-26` branch when historical V1 implementation details are needed. Do not restore the V1 directories on the active main line.

## Local development

Requirements: JDK 17, Maven 3.9.x, MySQL 8, Node 24+, pnpm 9, and an `Asia/Tokyo` JVM timezone.

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

The backend requires database, JWT, and statistics secrets. Follow the [local setup](docs/handbook/ops/local-development.md), [environment variable](docs/handbook/ops/environment.md), and [MySQL guide](docs/handbook/ops/local-mysql-development.md).

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
- [Production deployment direction](docs/handbook/ops/deployment-direction.md)

## License

This repository uses the [MIT License](LICENSE). The blog is derived from [Hexo Theme Aurora](https://github.com/auroral-ui/hexo-theme-aurora), and the admin app is based on [pure-admin-thin](https://github.com/pure-admin/pure-admin-thin). Their upstream licenses and provenance records are preserved in the application directories.
