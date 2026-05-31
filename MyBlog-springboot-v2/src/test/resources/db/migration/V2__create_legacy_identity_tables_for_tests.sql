create table t_user_info (
    id int auto_increment primary key,
    email varchar(50),
    nickname varchar(50) not null,
    avatar varchar(1024) not null default '',
    intro varchar(255),
    website varchar(255),
    is_subscribe tinyint,
    is_disable tinyint not null default 0,
    create_time timestamp not null,
    update_time timestamp
);

create table t_user_auth (
    id int auto_increment primary key,
    user_info_id int not null,
    username varchar(50) not null unique,
    password varchar(100) not null,
    login_type tinyint not null,
    ip_address varchar(50),
    ip_source varchar(50),
    create_time timestamp not null,
    update_time timestamp,
    last_login_time timestamp
);

create table t_role (
    id int auto_increment primary key,
    role_name varchar(20) not null,
    is_disable tinyint not null default 0,
    create_time timestamp not null,
    update_time timestamp
);

create table t_user_role (
    id int auto_increment primary key,
    user_id int,
    role_id int
);

create table t_menu (
    id int auto_increment primary key,
    name varchar(50) not null,
    path varchar(100) not null,
    component varchar(100) not null,
    icon varchar(50),
    order_num int not null default 0,
    parent_id int,
    is_hidden tinyint not null default 0,
    create_time timestamp not null,
    update_time timestamp
);

create table t_role_menu (
    id int auto_increment primary key,
    role_id int not null,
    menu_id int not null
);

insert into t_user_info (id, email, nickname, avatar, intro, website, is_subscribe, is_disable, create_time)
values
    (1, 'admin@163.com', '管理员', '', null, null, 0, 0, current_timestamp),
    (2, 'user@163.com', '普通用户', '', null, null, 0, 0, current_timestamp),
    (3, 'disabled@163.com', '禁用用户', '', null, null, 0, 1, current_timestamp);

insert into t_user_auth (id, user_info_id, username, password, login_type, create_time)
values
    (1, 1, 'admin@163.com', '$2a$10$djjHOm86X5nKsIY0Zv0lO.iLoVuEO5J6mkBGdf1G.i/8qHL7IaOxy', 1, current_timestamp),
    (2, 2, 'user@163.com', '$2a$10$djjHOm86X5nKsIY0Zv0lO.iLoVuEO5J6mkBGdf1G.i/8qHL7IaOxy', 1, current_timestamp),
    (3, 3, 'disabled@163.com', '$2a$10$djjHOm86X5nKsIY0Zv0lO.iLoVuEO5J6mkBGdf1G.i/8qHL7IaOxy', 1, current_timestamp);

insert into t_role (id, role_name, is_disable, create_time)
values
    (1, 'admin', 0, current_timestamp),
    (2, 'user', 0, current_timestamp),
    (14, 'test', 0, current_timestamp);

insert into t_user_role (id, user_id, role_id)
values
    (1, 1, 1),
    (2, 1, 14),
    (3, 2, 2),
    (4, 3, 1);

insert into t_menu (id, name, path, component, icon, order_num, parent_id, is_hidden, create_time)
values
    (1, '首页', '/', 'Layout', 'Home', 1, null, 0, current_timestamp),
    (2, '文章管理', '/article', 'Layout', 'Document', 2, null, 0, current_timestamp),
    (3, '文章列表', 'list', 'article/ArticleList', 'List', 1, 2, 0, current_timestamp),
    (4, '草稿箱', 'drafts', 'article/DraftList', 'Edit', 2, 2, 1, current_timestamp),
    (5, '评论管理', '/comments', 'comment/CommentList', 'Message', 3, null, 0, current_timestamp),
    (6, '个人中心', '/profile', 'user/Profile', 'User', 4, null, 0, current_timestamp);

insert into t_role_menu (id, role_id, menu_id)
values
    (1, 1, 1),
    (2, 1, 2),
    (3, 1, 3),
    (4, 1, 4),
    (5, 1, 5),
    (6, 1, 6),
    (7, 2, 6);

create table t_category (
    id int auto_increment primary key,
    category_name varchar(50) not null,
    create_time timestamp not null,
    update_time timestamp
);

create table t_tag (
    id int auto_increment primary key,
    tag_name varchar(50) not null,
    create_time timestamp not null,
    update_time timestamp
);

create table t_article (
    id int auto_increment primary key,
    user_id int not null,
    category_id int not null,
    article_cover varchar(1024),
    article_title varchar(100) not null,
    article_abstract varchar(255),
    article_content clob,
    is_top tinyint not null default 0,
    is_featured tinyint not null default 0,
    is_delete tinyint not null default 0,
    status tinyint not null,
    type tinyint,
    password varchar(255),
    original_url varchar(1024),
    create_time timestamp not null,
    update_time timestamp
);

create table t_article_tag (
    id int auto_increment primary key,
    article_id int not null,
    tag_id int not null
);

create table t_comment (
    id int auto_increment primary key,
    user_id int not null,
    reply_user_id int,
    topic_id int,
    comment_content varchar(1024) not null,
    parent_id int,
    type tinyint not null,
    is_delete tinyint not null default 0,
    is_review tinyint not null default 1,
    create_ip varchar(45),
    user_agent varchar(255),
    reviewed_by int,
    review_time timestamp,
    deleted_by int,
    delete_time timestamp,
    restored_by int,
    restore_time timestamp,
    create_time timestamp not null,
    update_time timestamp
);

insert into t_category (id, category_name, create_time)
values
    (1, 'Java', current_timestamp),
    (2, '生活', current_timestamp);

insert into t_tag (id, tag_name, create_time)
values
    (1, 'Spring', current_timestamp),
    (2, 'Vue', current_timestamp),
    (3, '重构', current_timestamp);

insert into t_article (
    id, user_id, category_id, article_cover, article_title, article_abstract,
    article_content, is_top, is_featured, is_delete, status, type, password, create_time, update_time
)
values
    (1, 1, 1, '/cover/java-1.png', '后端V2第一篇', '摘要一', '正文一', 1, 1, 0, 1, 1, null, timestamp '2026-05-28 10:00:00', timestamp '2026-05-28 10:00:00'),
    (2, 1, 2, '/cover/life-1.png', '生活记录第一篇', '摘要二', '正文二', 0, 1, 0, 1, 1, null, timestamp '2026-04-20 11:00:00', timestamp '2026-04-20 11:00:00'),
    (3, 1, 1, '/cover/protected.png', '密码文章', '不应出现在公开列表', '密码正文', 1, 1, 0, 2, 1, 'open-sesame', timestamp '2026-03-18 12:00:00', timestamp '2026-03-18 12:00:00'),
    (4, 1, 1, '/cover/draft.png', '草稿文章', '不应出现在第一阶段', '草稿正文', 1, 1, 0, 3, 1, null, timestamp '2026-02-16 13:00:00', timestamp '2026-02-16 13:00:00'),
    (5, 1, 2, '/cover/deleted.png', '已删除文章', '不应出现在第一阶段', '删除正文', 1, 1, 1, 1, 1, null, timestamp '2026-01-14 14:00:00', timestamp '2026-01-14 14:00:00');

insert into t_article_tag (id, article_id, tag_id)
values
    (1, 1, 1),
    (2, 1, 3),
    (3, 2, 2),
    (4, 2, 3),
    (5, 3, 1),
    (6, 4, 1),
    (7, 5, 2);

insert into t_comment (
    id, user_id, reply_user_id, topic_id, comment_content,
    parent_id, type, is_delete, is_review,
    create_ip, user_agent, reviewed_by, review_time,
    deleted_by, delete_time, restored_by, restore_time,
    create_time, update_time
)
values
    (1, 2, null, 1, '第一条文章评论', null, 1, 0, 1,
     '203.0.113.1', 'JUnit Browser', 1, timestamp '2026-05-29 10:01:00',
     null, null, null, null,
     timestamp '2026-05-29 10:00:00', timestamp '2026-05-29 10:00:00'),
    (2, 1, 2, 1, '管理员回复普通用户', 1, 1, 0, 1,
     '203.0.113.2', 'JUnit Browser', 1, timestamp '2026-05-29 10:06:00',
     null, null, null, null,
     timestamp '2026-05-29 10:05:00', timestamp '2026-05-29 10:05:00'),
    (3, 2, null, 1, '待审核评论', null, 1, 0, 0,
     '203.0.113.3', 'JUnit Browser', null, null,
     null, null, null, null,
     timestamp '2026-05-29 10:10:00', timestamp '2026-05-29 10:10:00'),
    (4, 2, null, 1, '已删除评论', null, 1, 1, 1,
     '203.0.113.4', 'JUnit Browser', 1, timestamp '2026-05-29 10:16:00',
     1, timestamp '2026-05-29 10:16:00', null, null,
     timestamp '2026-05-29 10:15:00', timestamp '2026-05-29 10:15:00'),
    (5, 2, null, null, '留言板第一条', null, 2, 0, 1,
     '203.0.113.5', 'JUnit Browser', 1, timestamp '2026-05-29 11:01:00',
     null, null, null, null,
     timestamp '2026-05-29 11:00:00', timestamp '2026-05-29 11:00:00'),
    (6, 1, 2, null, '留言板回复', 5, 2, 0, 1,
     '203.0.113.6', 'JUnit Browser', 1, timestamp '2026-05-29 11:06:00',
     null, null, null, null,
     timestamp '2026-05-29 11:05:00', timestamp '2026-05-29 11:05:00');

alter table t_comment alter column id restart with 7;
