-- Local-only deterministic development data. Never use these accounts in production.
START TRANSACTION;

INSERT INTO t_user_auth (
    id, username, password_hash, type, token_version,
    created_at, created_by, updated_at, updated_by, deleted
) VALUES
    (9007199254741001, 'admin',
     '$2a$10$Mqkpf6/Bcf5ZD2UfqZDxJOjrzLMDi.r6zHSEVX0FzHk1ZdXcZD6ky',
     1, 0, '2026-06-21 09:00:00', 9007199254741001,
     '2026-06-21 09:00:00', 9007199254741001, 0),
    (9007199254741002, 'demo',
     '$2a$10$Mqkpf6/Bcf5ZD2UfqZDxJOjrzLMDi.r6zHSEVX0FzHk1ZdXcZD6ky',
     2, 0, '2026-06-21 09:05:00', 9007199254741001,
     '2026-06-21 09:05:00', 9007199254741001, 0);

INSERT INTO t_user_info (
    user_id, nickname, bio_zh, bio_ja, bio_en, location,
    created_at, created_by, updated_at, updated_by, deleted
) VALUES
    (9007199254741001, '本地管理员', 'V2 本地开发管理员',
     'V2 ローカル開発管理者', 'V2 local development administrator', 'Tokyo',
     '2026-06-21 09:00:00', 9007199254741001,
     '2026-06-21 09:00:00', 9007199254741001, 0),
    (9007199254741002, '演示账号', '只读演示账号',
     '読み取り専用デモアカウント', 'Read-only demo account', 'Tokyo',
     '2026-06-21 09:05:00', 9007199254741001,
     '2026-06-21 09:05:00', 9007199254741001, 0);

INSERT INTO t_category (
    id, name_zh, name_ja, name_en, slug, sort_order,
    created_at, created_by, updated_at, updated_by, deleted
) VALUES
    (9007199254741101, '后端开发', 'バックエンド開発', 'Backend Development',
     'backend-development', 10, '2026-06-21 09:10:00', 9007199254741001,
     '2026-06-21 09:10:00', 9007199254741001, 0),
    (9007199254741102, '前端开发', 'フロントエンド開発', 'Frontend Development',
     'frontend-development', 20, '2026-06-21 09:11:00', 9007199254741001,
     '2026-06-21 09:11:00', 9007199254741001, 0);

INSERT INTO t_tag (
    id, name_zh, name_ja, name_en, slug,
    created_at, created_by, updated_at, updated_by, deleted
) VALUES
    (9007199254741201, 'Spring Boot', 'Spring Boot', 'Spring Boot', 'spring-boot',
     '2026-06-21 09:12:00', 9007199254741001,
     '2026-06-21 09:12:00', 9007199254741001, 0),
    (9007199254741202, 'Vue', 'Vue', 'Vue', 'vue',
     '2026-06-21 09:13:00', 9007199254741001,
     '2026-06-21 09:13:00', 9007199254741001, 0),
    (9007199254741203, '数据库', 'データベース', 'Database', 'database',
     '2026-06-21 09:14:00', 9007199254741001,
     '2026-06-21 09:14:00', 9007199254741001, 0);

INSERT INTO t_article (
    id, title_zh, title_ja, title_en,
    summary_zh, summary_ja, summary_en, body,
    category_id, author_id, slug, status, access_password,
    publish_at, comment_count,
    created_at, created_by, updated_at, updated_by, deleted
) VALUES
    (9007199254741301, '草稿示例', '下書きの例', 'Draft Example',
     '用于验证草稿筛选。', '下書きフィルター確認用。', 'Used to verify draft filtering.',
     '# 草稿示例\n\n这是一篇本地开发草稿。',
     NULL, 9007199254741001, 'draft-example', 1, NULL, NULL, 0,
     '2026-06-21 09:20:00', 9007199254741001,
     '2026-06-21 09:20:00', 9007199254741001, 0),
    (9007199254741302, '已发布示例', '公開済みの例', 'Published Example',
     '公开文章和评论联调入口。', '公開記事とコメント連携用。',
     'Public article and comment integration entry.',
     '# 已发布示例\n\n用于验证公开文章列表、详情和评论。',
     9007199254741101, 9007199254741001, 'published-example', 2, NULL,
     '2026-06-20 10:00:00', 2,
     '2026-06-20 09:30:00', 9007199254741001,
     '2026-06-21 09:21:00', 9007199254741001, 0),
    (9007199254741303, '私密示例', '非公開の例', 'Private Example',
     '用于验证私密文章筛选。', '非公開記事フィルター確認用。',
     'Used to verify private article filtering.',
     '# 私密示例\n\n仅后台可见。',
     9007199254741101, 9007199254741001, 'private-example', 3, NULL, NULL, 0,
     '2026-06-21 09:22:00', 9007199254741001,
     '2026-06-21 09:22:00', 9007199254741001, 0),
    (9007199254741304, '密码文章示例', 'パスワード記事の例', 'Password Article Example',
     '用于验证密码文章状态。', 'パスワード記事状態確認用。',
     'Used to verify password-protected status.',
     '# 密码文章示例\n\n仅用于本地开发。',
     9007199254741102, 9007199254741001, 'password-example', 4,
     '$2a$10$Mqkpf6/Bcf5ZD2UfqZDxJOjrzLMDi.r6zHSEVX0FzHk1ZdXcZD6ky',
     '2026-06-20 11:00:00', 0,
     '2026-06-20 10:30:00', 9007199254741001,
     '2026-06-21 09:23:00', 9007199254741001, 0),
    (9007199254741305, '定时发布示例', '予約公開の例', 'Scheduled Example',
     '用于验证定时发布筛选。', '予約公開フィルター確認用。',
     'Used to verify scheduled article filtering.',
     '# 定时发布示例\n\n将在未来时间发布。',
     9007199254741102, 9007199254741001, 'scheduled-example', 5, NULL,
     '2099-06-21 10:00:00', 0,
     '2026-06-21 09:24:00', 9007199254741001,
     '2026-06-21 09:24:00', 9007199254741001, 0);

INSERT INTO t_article_tag (article_id, tag_id) VALUES
    (9007199254741301, 9007199254741201),
    (9007199254741302, 9007199254741201),
    (9007199254741302, 9007199254741203),
    (9007199254741303, 9007199254741203),
    (9007199254741304, 9007199254741202),
    (9007199254741305, 9007199254741202);

INSERT INTO t_comment (
    id, target_type, target_id, author_nickname, author_email,
    content_md, content_html, audit_status,
    created_at, created_by, updated_at, updated_by, deleted
) VALUES
    (9007199254741401, 1, 9007199254741302, '本地读者', 'reader@example.invalid',
     '第一条本地评论。', '<p>第一条本地评论。</p>', 1,
     '2026-06-20 12:00:00', NULL, '2026-06-20 12:00:00', NULL, 0),
    (9007199254741402, 1, 9007199254741302, '本地管理员', 'admin@example.invalid',
     '欢迎使用 V2 本地环境。', '<p>欢迎使用 V2 本地环境。</p>', 1,
     '2026-06-20 12:10:00', 9007199254741001,
     '2026-06-20 12:10:00', 9007199254741001, 0);

UPDATE t_site_config
SET site_title_zh = 'MyBlog V2 本地开发',
    site_title_ja = 'MyBlog V2 ローカル開発',
    site_title_en = 'MyBlog V2 Local Development',
    site_subtitle_zh = '真实 MySQL 联调环境',
    site_subtitle_ja = '実 MySQL 連携環境',
    site_subtitle_en = 'Real MySQL integration environment',
    about_md_zh = '# 关于本站\n\n这是 V2 本地开发数据。',
    about_md_ja = '# このサイトについて\n\nV2 ローカル開発データです。',
    about_md_en = '# About\n\nThis is V2 local development data.',
    updated_at = '2026-06-21 09:30:00',
    updated_by = 9007199254741001,
    deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL
WHERE id = 1;

COMMIT;
