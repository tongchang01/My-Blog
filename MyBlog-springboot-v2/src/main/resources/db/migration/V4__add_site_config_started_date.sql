ALTER TABLE `t_site_config`
  ADD COLUMN `started_date` DATE NULL DEFAULT NULL COMMENT '建站日期，用于前台计算运行天数'
  AFTER `spotify_playlist_id`;
