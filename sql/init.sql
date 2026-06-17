-- 创建数据库
CREATE DATABASE IF NOT EXISTS review_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE review_system;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '登录用户名',
    `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    `email` VARCHAR(128) NOT NULL COMMENT '邮箱',
    `role` VARCHAR(32) NOT NULL DEFAULT 'ROLE_AUTHOR' COMMENT '角色: ROLE_AUTHOR, ROLE_REVIEWER, ROLE_ADMIN',
    `name` VARCHAR(64) COMMENT '真实姓名',
    `affiliation` VARCHAR(255) COMMENT '所属机构',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`),
    INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 评审记录表
CREATE TABLE IF NOT EXISTS `review_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `paper_id` VARCHAR(64) NOT NULL COMMENT '论文Neo4j节点ID',
    `reviewer_id` BIGINT NOT NULL COMMENT '评审人用户ID',
    `reviewer_name` VARCHAR(64) COMMENT '评审人姓名(冗余)',
    `innovation_score` INT DEFAULT 0 COMMENT '创新性评分0-100',
    `method_score` INT DEFAULT 0 COMMENT '方法学评分0-100',
    `writing_score` INT DEFAULT 0 COMMENT '写作质量评分0-100',
    `overall_score` INT DEFAULT 0 COMMENT '综合评分',
    `comments` TEXT COMMENT '评审意见',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING待评审, SUBMITTED已提交',
    `review_time` DATETIME COMMENT '评审完成时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_paper_id` (`paper_id`),
    INDEX `idx_reviewer_id` (`reviewer_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评审记录表';
