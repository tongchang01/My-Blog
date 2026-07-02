ALTER TABLE `t_article`
  ADD COLUMN `homepage_slot` VARCHAR(16) NOT NULL DEFAULT 'NONE';

CREATE INDEX `idx_article_homepage_slot`
  ON `t_article` (`homepage_slot`, `status`, `deleted`, `publish_at`);
