SELECT 't_user_auth active' AS check_name,
       2 AS expected_count,
       COUNT(*) AS actual_count,
       COUNT(*) = 2 AS passed
FROM t_user_auth
WHERE deleted = 0
UNION ALL
SELECT 't_user_info active', 2, COUNT(*), COUNT(*) = 2
FROM t_user_info
WHERE deleted = 0
UNION ALL
SELECT 't_category active', 2, COUNT(*), COUNT(*) >= 2
FROM t_category
WHERE deleted = 0
UNION ALL
SELECT 't_tag active', 3, COUNT(*), COUNT(*) >= 3
FROM t_tag
WHERE deleted = 0
UNION ALL
SELECT 't_article active', 5, COUNT(*), COUNT(*) >= 5
FROM t_article
WHERE deleted = 0
UNION ALL
SELECT 't_article_tag', 5, COUNT(*), COUNT(*) >= 5
FROM t_article_tag
UNION ALL
SELECT 't_comment active', 2, COUNT(*), COUNT(*) >= 2
FROM t_comment
WHERE deleted = 0
UNION ALL
SELECT 't_site_config active', 1, COUNT(*), COUNT(*) = 1
FROM t_site_config
WHERE deleted = 0;

SELECT 'article_comment_count' AS check_name,
       a.comment_count AS expected_count,
       COUNT(c.id) AS actual_count,
       a.comment_count = COUNT(c.id) AS passed
FROM t_article a
LEFT JOIN t_comment c
       ON c.target_type = 1
      AND c.target_id = a.id
      AND c.audit_status = 1
      AND c.deleted = 0
WHERE a.id = 9007199254741302
GROUP BY a.id, a.comment_count;
