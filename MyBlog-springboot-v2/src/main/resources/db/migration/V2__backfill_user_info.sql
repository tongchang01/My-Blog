INSERT INTO t_user_info (
    user_id,
    nickname,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted
)
SELECT ua.id,
       ua.username,
       CURRENT_TIMESTAMP,
       ua.id,
       CURRENT_TIMESTAMP,
       ua.id,
       0
FROM t_user_auth ua
WHERE ua.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM t_user_info ui
      WHERE ui.user_id = ua.id
  );
