# MyBlog V2: A Maintainable Three-Application Blog

MyBlog V2 is a full-stack personal blogging system composed of a public blog, an admin console, and a Spring Boot API. Its engineering focus is clear business boundaries, executable architecture rules, complete content workflows, and verifiable security constraints—not middleware quantity.

## Product capabilities

The public blog supports Chinese, Japanese, and English interfaces, home-page curation, article detail, categories, tags, archives, search, about content, friend links, a guestbook, article comments, author profile, and traffic statistics. Article URLs use a stable numeric ID with an optional readable slug.

The admin console covers authentication sessions, a traffic dashboard, article publishing and scheduling, pinned and featured slots, taxonomy, comment moderation, friend links, attachments, site settings, and author profile. ADMIN can read and write; DEMO is read-only and receives server-side sensitive-field redaction.

## Engineering design

The backend uses Java 17, Spring Boot 3.5, Spring Security, MyBatis-Plus, Flyway, and MySQL 8. Five business modules—identity, content, comment, system, and stats—follow four layers. Cross-module collaboration goes through application contracts, while ArchUnit prevents dependency violations.

Authentication combines short-lived JWT access tokens, database refresh tokens, concurrency-safe rotation, and token-version revocation. Comment Markdown is rendered and sanitized on the server. Attachments support local and S3 storage, and traffic statistics use irreversible visitor hashes plus daily aggregation.

Both frontends use Vue 3, TypeScript, Pinia, Vite, and Vitest. The blog evolved from Aurora's visual foundation, while the admin console uses pure-admin-thin as its UI scaffold. Their data sources and business interactions now use the V2 REST API.

## Current boundaries

PASSWORD articles currently expose locked metadata but have no public unlock flow. Full SEO/feed support, Spotify Embed, and multi-instance coordination are demand-triggered extensions rather than current capabilities.

Production runs on AWS EC2: Docker Compose hosts MySQL, the API, and Caddy, while S3 stores attachments. GitHub Actions builds GHCR images and uses GitHub OIDC plus restricted SSH to deploy the same commit SHA. Public HTTPS health checks run after deployment; database recovery, the full S3 path, and rollback drills still require ongoing validation.

## What the project demonstrates

- Full-stack workflows designed from product state, permissions, and data models.
- Long-term consistency through module boundaries, transactions, tests, and migrations.
- Public experience, admin operations, security, auditability, and deployment readiness in one system.
- A strict distinction between implemented behavior, known risk, and future extensions.
