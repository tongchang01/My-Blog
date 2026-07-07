-- Local-only demo data supplement. Safe to rerun against myblog_v2_dev.
START TRANSACTION;

INSERT INTO t_attachment (
    id, storage_type, bucket, object_key, public_url, content_type, file_size,
    width, height, original_filename, hash_sha256,
    created_at, created_by, updated_at, updated_by, deleted
) VALUES
    (9007199254741501, 'LOCAL', 'local', 'demo/covers/backend-notes.jpg',
     'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=1200&q=80',
     'image/jpeg', 120000, 1200, 800, 'backend-notes.jpg',
     '1111111111111111111111111111111111111111111111111111111111111111',
     '2026-07-06 18:00:00', 9007199254741001,
     '2026-07-06 18:00:00', 9007199254741001, 0),
    (9007199254741502, 'LOCAL', 'local', 'demo/covers/vue-integration.jpg',
     'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=1200&q=80',
     'image/jpeg', 118000, 1200, 800, 'vue-integration.jpg',
     '2222222222222222222222222222222222222222222222222222222222222222',
     '2026-07-06 18:01:00', 9007199254741001,
     '2026-07-06 18:01:00', 9007199254741001, 0),
    (9007199254741503, 'LOCAL', 'local', 'demo/covers/mysql-observe.jpg',
     'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?auto=format&fit=crop&w=1200&q=80',
     'image/jpeg', 116000, 1200, 800, 'mysql-observe.jpg',
     '3333333333333333333333333333333333333333333333333333333333333333',
     '2026-07-06 18:02:00', 9007199254741001,
     '2026-07-06 18:02:00', 9007199254741001, 0)
ON DUPLICATE KEY UPDATE
    public_url = VALUES(public_url),
    hash_sha256 = VALUES(hash_sha256),
    updated_at = VALUES(updated_at),
    updated_by = VALUES(updated_by),
    deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL;

INSERT INTO t_article (
    id, title_zh, title_ja, title_en,
    summary_zh, summary_ja, summary_en, body,
    category_id, author_id, slug, status, access_password,
    publish_at, homepage_slot, cover_attachment_id, comment_count,
    created_at, created_by, updated_at, updated_by, deleted
) VALUES
    (9007199254741306, '前台评论迁移复盘', 'フロントコメント移行メモ', 'Frontend Comment Migration Notes',
     '记录从第三方评论插件迁移到 V2 自研评论 API 的关键取舍。',
     'サードパーティコメントから V2 API へ移行した判断を整理します。',
     'Notes from moving article comments from plugins to the V2 API.',
     '# 前台评论迁移复盘\n\n这篇文章用于查看首页推荐、文章详情和新评论区。\n\n- V2 API\n- 嵌套回复\n- 待审核提示',
     9007199254741102, 9007199254741001, 'frontend-comment-migration', 2, NULL,
     '2026-07-05 10:00:00', 'PINNED', 9007199254741502, 4,
     '2026-07-05 09:30:00', 9007199254741001,
     '2026-07-06 18:11:00', 9007199254741001, 0),
    (9007199254741307, 'MySQL 联调观察清单', 'MySQL 連携チェックリスト', 'MySQL Integration Checklist',
     '用于验证分类、标签、搜索、归档和统计数据展示。',
     'カテゴリ、タグ、検索、アーカイブ、統計表示の確認用です。',
     'A compact checklist for checking category, tag, search, archive and stats views.',
     '# MySQL 联调观察清单\n\n当前数据可以用来检查：\n\n1. 首页槽位\n2. 分类和标签计数\n3. 评论列表和回复\n4. 访问统计摘要',
     9007199254741101, 9007199254741001, 'mysql-integration-checklist', 2, NULL,
     '2026-07-04 11:00:00', 'FEATURED', 9007199254741503, 1,
     '2026-07-04 10:30:00', 9007199254741001,
     '2026-07-06 18:12:00', 9007199254741001, 0),
    (9007199254741308, '后台内容管理体验', '管理画面コンテンツ体験', 'Admin Content Workflow',
     '给后台列表、编辑和状态筛选准备的本地演示文章。',
     '管理画面の一覧、編集、状態フィルター確認用の記事です。',
     'Demo content for admin list, edit and status filtering.',
     '# 后台内容管理体验\n\n这篇文章用于后台和前台同时查看。',
     9007199254741101, 9007199254741001, 'admin-content-workflow', 2, NULL,
     '2026-07-03 12:00:00', 'NONE', NULL, 0,
     '2026-07-03 11:30:00', 9007199254741001,
     '2026-07-06 18:13:00', 9007199254741001, 0),
    (9007199254741309, '搜索和归档样例', '検索とアーカイブの例', 'Search and Archive Example',
     '标题和摘要里包含 MySQL、Vue、Spring Boot，方便验证搜索。',
     'MySQL、Vue、Spring Boot を含む検索確認用の概要です。',
     'Mentions MySQL, Vue and Spring Boot so keyword search has useful hits.',
     '# 搜索和归档样例\n\nMySQL、Vue、Spring Boot 都在这里出现，方便测试搜索。',
     9007199254741102, 9007199254741001, 'search-and-archive-example', 2, NULL,
     '2026-06-28 08:00:00', 'NONE', NULL, 2,
     '2026-06-28 07:30:00', 9007199254741001,
     '2026-07-06 18:14:00', 9007199254741001, 0)
ON DUPLICATE KEY UPDATE
    title_zh = VALUES(title_zh),
    title_ja = VALUES(title_ja),
    title_en = VALUES(title_en),
    summary_zh = VALUES(summary_zh),
    summary_ja = VALUES(summary_ja),
    summary_en = VALUES(summary_en),
    body = VALUES(body),
    category_id = VALUES(category_id),
    slug = VALUES(slug),
    status = VALUES(status),
    publish_at = VALUES(publish_at),
    homepage_slot = VALUES(homepage_slot),
    cover_attachment_id = VALUES(cover_attachment_id),
    comment_count = VALUES(comment_count),
    updated_at = VALUES(updated_at),
    updated_by = VALUES(updated_by),
    deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL;

INSERT IGNORE INTO t_article_tag (article_id, tag_id) VALUES
    (9007199254741306, 9007199254741202),
    (9007199254741306, 9007199254741201),
    (9007199254741307, 9007199254741203),
    (9007199254741307, 9007199254741201),
    (9007199254741308, 9007199254741201),
    (9007199254741309, 9007199254741201),
    (9007199254741309, 9007199254741202),
    (9007199254741309, 9007199254741203);

INSERT INTO t_comment (
    id, target_type, target_id, parent_id, reply_to_comment_id,
    reply_to_nickname, author_nickname, author_email, author_site,
    content_md, content_html, audit_status,
    created_at, created_by, updated_at, updated_by, deleted
) VALUES
    (9007199254741403, 1, 9007199254741306, 9007199254741406, 9007199254741406,
     '评论观察员', '路过的前端', 'frontend@example.invalid', 'https://example.com',
     '回复第一条评论。', '<p>回复第一条评论。</p>', 1,
     '2026-07-06 18:20:00', NULL, '2026-07-06 18:20:00', NULL, 0),
    (9007199254741404, 1, 9007199254741306, 9007199254741406, 9007199254741403,
     '路过的前端', '本地管理员', 'admin@example.invalid', NULL,
     '嵌套回复也能展示。', '<p>嵌套回复也能展示。</p>', 1,
     '2026-07-06 18:21:00', 9007199254741001,
     '2026-07-06 18:21:00', 9007199254741001, 0),
    (9007199254741405, 1, 9007199254741306, NULL, NULL,
     NULL, '待审核读者', 'pending@example.invalid', NULL,
     '这条评论用于后台审核列表。', '<p>这条评论用于后台审核列表。</p>', 2,
     '2026-07-06 18:22:00', NULL, '2026-07-06 18:22:00', NULL, 0),
    (9007199254741406, 1, 9007199254741306, NULL, NULL,
     NULL, '评论观察员', 'observer@example.invalid', NULL,
     '新评论区看起来更统一。', '<p>新评论区看起来更统一。</p>', 1,
     '2026-07-06 18:23:00', NULL, '2026-07-06 18:23:00', NULL, 0),
    (9007199254741407, 1, 9007199254741306, 9007199254741406, 9007199254741406,
     '评论观察员', 'TYB', 'tyb@example.invalid', NULL,
     '这条是作者回复。', '<p>这条是作者回复。</p>', 1,
     '2026-07-06 18:24:00', 9007199254741001,
     '2026-07-06 18:24:00', 9007199254741001, 0),
    (9007199254741408, 1, 9007199254741306, NULL, NULL,
     NULL, '等待审核', 'wait@example.invalid', NULL,
     '用于查看 PENDING 状态。', '<p>用于查看 PENDING 状态。</p>', 2,
     '2026-07-06 18:25:00', NULL, '2026-07-06 18:25:00', NULL, 0),
    (9007199254741409, 1, 9007199254741307, NULL, NULL,
     NULL, '数据库读者', 'db@example.invalid', NULL,
     '统计数据也需要一点样例。', '<p>统计数据也需要一点样例。</p>', 1,
     '2026-07-06 18:26:00', NULL, '2026-07-06 18:26:00', NULL, 0),
    (9007199254741410, 1, 9007199254741309, NULL, NULL,
     NULL, '搜索用户', 'search@example.invalid', NULL,
     '搜索页可以搜到这篇。', '<p>搜索页可以搜到这篇。</p>', 1,
     '2026-07-06 18:27:00', NULL, '2026-07-06 18:27:00', NULL, 0),
    (9007199254741411, 1, 9007199254741309, 9007199254741410, 9007199254741410,
     '搜索用户', '本地管理员', 'admin@example.invalid', NULL,
     '回复搜索用户。', '<p>回复搜索用户。</p>', 1,
     '2026-07-06 18:28:00', 9007199254741001,
     '2026-07-06 18:28:00', 9007199254741001, 0),
    (9007199254741412, 2, 0, NULL, NULL,
     NULL, '留言板读者', 'guestbook@example.invalid', 'https://example.com',
     '留言板第二批接入前，先准备一点数据。', '<p>留言板第二批接入前，先准备一点数据。</p>', 1,
     '2026-07-06 18:29:00', NULL, '2026-07-06 18:29:00', NULL, 0),
    (9007199254741413, 2, 0, 9007199254741412, 9007199254741412,
     '留言板读者', '本地管理员', 'admin@example.invalid', NULL,
     '管理员留言回复样例。', '<p>管理员留言回复样例。</p>', 1,
     '2026-07-06 18:30:00', 9007199254741001,
     '2026-07-06 18:30:00', 9007199254741001, 0)
ON DUPLICATE KEY UPDATE
    target_type = VALUES(target_type),
    target_id = VALUES(target_id),
    parent_id = VALUES(parent_id),
    reply_to_comment_id = VALUES(reply_to_comment_id),
    reply_to_nickname = VALUES(reply_to_nickname),
    author_nickname = VALUES(author_nickname),
    author_email = VALUES(author_email),
    author_site = VALUES(author_site),
    content_md = VALUES(content_md),
    content_html = VALUES(content_html),
    audit_status = VALUES(audit_status),
    updated_at = VALUES(updated_at),
    updated_by = VALUES(updated_by),
    deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL;

INSERT INTO t_friend_link (
    id, name, url, avatar_url, description, sort_order, status,
    created_at, created_by, updated_at, updated_by, deleted
) VALUES
    (9007199254741601, 'Spring Boot', 'https://spring.io/projects/spring-boot',
     'https://spring.io/img/projects/spring-boot.svg',
     '后端框架参考入口', 10, 1,
     '2026-07-06 18:40:00', 9007199254741001,
     '2026-07-06 18:40:00', 9007199254741001, 0),
    (9007199254741602, 'Vue', 'https://vuejs.org/',
     'https://vuejs.org/logo.svg',
     '前台页面和交互参考入口', 20, 1,
     '2026-07-06 18:41:00', 9007199254741001,
     '2026-07-06 18:41:00', 9007199254741001, 0),
    (9007199254741603, '隐藏友链样例', 'https://example.com/hidden',
     NULL, '后台可见，公开端不显示', 30, 2,
     '2026-07-06 18:42:00', 9007199254741001,
     '2026-07-06 18:42:00', 9007199254741001, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    url = VALUES(url),
    avatar_url = VALUES(avatar_url),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    updated_at = VALUES(updated_at),
    updated_by = VALUES(updated_by),
    deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL;

INSERT INTO t_page_view_daily (
    article_id, lang, stat_date, pv, uv, created_at
) VALUES
    (0, 'zh', CURRENT_DATE, 38, 12, CURRENT_TIMESTAMP),
    (0, 'en', CURRENT_DATE, 12, 5, CURRENT_TIMESTAMP),
    (0, 'ja', CURRENT_DATE, 8, 3, CURRENT_TIMESTAMP),
    (9007199254741302, 'zh', CURRENT_DATE, 26, 9, CURRENT_TIMESTAMP),
    (9007199254741306, 'zh', CURRENT_DATE, 18, 7, CURRENT_TIMESTAMP),
    (9007199254741307, 'zh', CURRENT_DATE, 14, 6, CURRENT_TIMESTAMP),
    (9007199254741309, 'zh', CURRENT_DATE, 10, 4, CURRENT_TIMESTAMP),
    (0, 'zh', CURRENT_DATE - INTERVAL 1 DAY, 31, 10, CURRENT_TIMESTAMP),
    (9007199254741302, 'zh', CURRENT_DATE - INTERVAL 1 DAY, 20, 8, CURRENT_TIMESTAMP),
    (9007199254741306, 'zh', CURRENT_DATE - INTERVAL 1 DAY, 15, 6, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    pv = VALUES(pv),
    uv = VALUES(uv),
    created_at = VALUES(created_at);

UPDATE t_site_config
SET site_title_zh = 'MyBlog V2 本地演示',
    site_title_ja = 'MyBlog V2 ローカルデモ',
    site_title_en = 'MyBlog V2 Local Demo',
    site_subtitle_zh = '文章、评论、统计和后台管理联调',
    site_subtitle_ja = '記事、コメント、統計、管理画面の連携確認',
    site_subtitle_en = 'Articles, comments, stats and admin workflow',
    about_md_zh = '# 关于本站\n\n这是 V2 本地演示数据，用于查看首页、文章、评论、统计和后台管理效果。\n\n## 当前可看\n\n- 首页置顶和推荐文章\n- 文章详情评论区\n- 分类、标签、归档和搜索\n- 页脚访问统计和建站天数',
    about_md_ja = '# このサイトについて\n\nV2 ローカルデモデータです。記事、コメント、統計、管理画面の確認に使います。',
    about_md_en = '# About\n\nLocal V2 demo data for checking articles, comments, stats and admin screens.',
    started_date = '2024-01-02',
    updated_at = '2026-07-06 18:50:00',
    updated_by = 9007199254741001,
    deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL
WHERE id = 1;

UPDATE t_article a
SET comment_count = (
    SELECT COUNT(*)
    FROM t_comment c
    WHERE c.target_type = 1
      AND c.target_id = a.id
      AND c.audit_status = 1
      AND c.deleted = 0
)
WHERE a.id IN (
    9007199254741306,
    9007199254741307,
    9007199254741308,
    9007199254741309
);

COMMIT;
