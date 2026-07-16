CREATE TABLE `t_homepage_slot_guard` (
  `slot` VARCHAR(16) NOT NULL,
  PRIMARY KEY (`slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页槽位并发锁';

INSERT INTO `t_homepage_slot_guard` (`slot`)
VALUES ('PINNED'), ('FEATURED');
