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
