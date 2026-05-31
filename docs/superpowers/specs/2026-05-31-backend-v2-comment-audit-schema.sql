-- 后端 V2 评论审计字段迁移脚本
-- 执行前置条件：
-- 1. 确认目标库为本地或预发 aurora，不直接在生产库无备份执行。
-- 2. 执行前先备份 t_comment。
-- 3. 执行后使用本文末尾 verification SQL 验证字段存在。

alter table t_comment
    add column create_ip varchar(45) null comment '评论提交 IP',
    add column user_agent varchar(255) null comment '评论提交 User-Agent',
    add column reviewed_by int null comment '最后审核人用户 ID',
    add column review_time timestamp null comment '最后审核时间',
    add column deleted_by int null comment '最后删除人用户 ID',
    add column delete_time timestamp null comment '最后删除时间',
    add column restored_by int null comment '最后恢复人用户 ID',
    add column restore_time timestamp null comment '最后恢复时间';

create index idx_comment_review_delete_time
    on t_comment (is_review, is_delete, create_time);

create index idx_comment_parent_delete_review
    on t_comment (parent_id, is_delete, is_review);

-- verification
select column_name, data_type, is_nullable
from information_schema.columns
where table_schema = database()
  and table_name = 't_comment'
  and column_name in (
      'create_ip', 'user_agent',
      'reviewed_by', 'review_time',
      'deleted_by', 'delete_time',
      'restored_by', 'restore_time'
  )
order by ordinal_position;
