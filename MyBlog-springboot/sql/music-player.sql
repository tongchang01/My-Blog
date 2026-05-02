SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_music
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_music`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `music_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '歌曲名称',
  `artist` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '歌手',
  `album` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专辑',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面地址',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '音频地址',
  `lrc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌词地址',
  `theme` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '#409EFF' COMMENT '主题色',
  `sort` int NOT NULL DEFAULT 1 COMMENT '排序',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0关闭 1启用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Seed records for t_music
-- ----------------------------
INSERT INTO `t_music` (`id`, `music_name`, `artist`, `album`, `cover`, `url`, `lrc`, `theme`, `sort`, `status`, `remark`, `create_time`, `update_time`)
SELECT 1, 'Midsummer Night', 'Aurora', 'Default Playlist', 'https://example.com/covers/midsummer-night.jpg', 'https://example.com/audio/midsummer-night.mp3', NULL, '#409EFF', 1, 1, '示例数据，请替换为正式音源', '2026-05-02 14:00:00', '2026-05-02 14:00:00'
WHERE NOT EXISTS (SELECT 1 FROM `t_music` WHERE `id` = 1);

INSERT INTO `t_music` (`id`, `music_name`, `artist`, `album`, `cover`, `url`, `lrc`, `theme`, `sort`, `status`, `remark`, `create_time`, `update_time`)
SELECT 2, 'Starlight', 'Aurora', 'Default Playlist', 'https://example.com/covers/starlight.jpg', 'https://example.com/audio/starlight.mp3', NULL, '#67C23A', 2, 1, '示例数据，请替换为正式音源', '2026-05-02 14:00:00', '2026-05-02 14:00:00'
WHERE NOT EXISTS (SELECT 1 FROM `t_music` WHERE `id` = 2);

-- ----------------------------
-- Resources for music APIs
-- ----------------------------
INSERT INTO `t_resource` (`id`, `resource_name`, `url`, `request_method`, `parent_id`, `is_anonymous`, `create_time`, `update_time`)
SELECT 1189, '音乐模块', NULL, NULL, NULL, 0, '2026-05-02 14:00:00', NULL
WHERE NOT EXISTS (SELECT 1 FROM `t_resource` WHERE `id` = 1189);

INSERT INTO `t_resource` (`id`, `resource_name`, `url`, `request_method`, `parent_id`, `is_anonymous`, `create_time`, `update_time`)
SELECT 1190, '获取音乐列表', '/musics', 'GET', 1189, 1, '2026-05-02 14:00:00', NULL
WHERE NOT EXISTS (SELECT 1 FROM `t_resource` WHERE `id` = 1190);

INSERT INTO `t_resource` (`id`, `resource_name`, `url`, `request_method`, `parent_id`, `is_anonymous`, `create_time`, `update_time`)
SELECT 1191, '查看后台音乐列表', '/admin/musics', 'GET', 1189, 0, '2026-05-02 14:00:00', NULL
WHERE NOT EXISTS (SELECT 1 FROM `t_resource` WHERE `id` = 1191);

INSERT INTO `t_resource` (`id`, `resource_name`, `url`, `request_method`, `parent_id`, `is_anonymous`, `create_time`, `update_time`)
SELECT 1192, '根据id查看后台音乐', '/admin/musics/*', 'GET', 1189, 0, '2026-05-02 14:00:00', NULL
WHERE NOT EXISTS (SELECT 1 FROM `t_resource` WHERE `id` = 1192);

INSERT INTO `t_resource` (`id`, `resource_name`, `url`, `request_method`, `parent_id`, `is_anonymous`, `create_time`, `update_time`)
SELECT 1193, '保存或修改音乐', '/admin/musics', 'POST', 1189, 0, '2026-05-02 14:00:00', NULL
WHERE NOT EXISTS (SELECT 1 FROM `t_resource` WHERE `id` = 1193);

INSERT INTO `t_resource` (`id`, `resource_name`, `url`, `request_method`, `parent_id`, `is_anonymous`, `create_time`, `update_time`)
SELECT 1194, '删除音乐', '/admin/musics', 'DELETE', 1189, 0, '2026-05-02 14:00:00', NULL
WHERE NOT EXISTS (SELECT 1 FROM `t_resource` WHERE `id` = 1194);

-- ----------------------------
-- Role permissions for music APIs
-- ----------------------------
INSERT INTO `t_role_resource` (`id`, `role_id`, `resource_id`)
SELECT 5547, 1, 1189
WHERE NOT EXISTS (SELECT 1 FROM `t_role_resource` WHERE `id` = 5547);

INSERT INTO `t_role_resource` (`id`, `role_id`, `resource_id`)
SELECT 5548, 1, 1191
WHERE NOT EXISTS (SELECT 1 FROM `t_role_resource` WHERE `id` = 5548);

INSERT INTO `t_role_resource` (`id`, `role_id`, `resource_id`)
SELECT 5549, 1, 1192
WHERE NOT EXISTS (SELECT 1 FROM `t_role_resource` WHERE `id` = 5549);

INSERT INTO `t_role_resource` (`id`, `role_id`, `resource_id`)
SELECT 5550, 1, 1193
WHERE NOT EXISTS (SELECT 1 FROM `t_role_resource` WHERE `id` = 5550);

INSERT INTO `t_role_resource` (`id`, `role_id`, `resource_id`)
SELECT 5551, 1, 1194
WHERE NOT EXISTS (SELECT 1 FROM `t_role_resource` WHERE `id` = 5551);

INSERT INTO `t_role_resource` (`id`, `role_id`, `resource_id`)
SELECT 5552, 14, 1191
WHERE NOT EXISTS (SELECT 1 FROM `t_role_resource` WHERE `id` = 5552);

INSERT INTO `t_role_resource` (`id`, `role_id`, `resource_id`)
SELECT 5553, 14, 1192
WHERE NOT EXISTS (SELECT 1 FROM `t_role_resource` WHERE `id` = 5553);

-- ----------------------------
-- Extend website config JSON with player config
-- ----------------------------
UPDATE `t_website_config`
SET `config` = JSON_SET(
  COALESCE(`config`, '{}'),
  '$.musicPlayerEnable', 0,
  '$.musicPlayerAutoPlay', 0,
  '$.musicPlayerFixed', 1,
  '$.musicPlayerTheme', '#409EFF',
  '$.musicPlayerLoop', 'all',
  '$.musicPlayerOrder', 'list'
)
WHERE `id` = 1;

SET FOREIGN_KEY_CHECKS = 1;
