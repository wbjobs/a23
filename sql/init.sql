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
    `assign_time` DATETIME COMMENT '分配时间',
    `anonymous` TINYINT(1) DEFAULT 0 COMMENT '评审人是否匿名',
    `reviewer_alias` VARCHAR(64) COMMENT '匿名评审人显示名(评审专家1/2/3...)',
    `author_info_desensitized` TINYINT(1) DEFAULT 1 COMMENT '作者信息是否脱敏',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_paper_id` (`paper_id`),
    INDEX `idx_reviewer_id` (`reviewer_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评审记录表';

-- PDF解析结果表（如不存在则创建，已存在则跳过）
CREATE TABLE IF NOT EXISTS `pdf_parse_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `paper_id` VARCHAR(64) COMMENT '论文ID',
    `pdf_file_name` VARCHAR(255),
    `parse_status` VARCHAR(32) DEFAULT 'PENDING',
    `grobid_result` JSON,
    `pdfplumber_result` JSON,
    `mathpix_formulas` JSON,
    `formula_anchors` JSON,
    `topics` JSON,
    `citation_network` JSON,
    `error_message` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_paper_id` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PDF解析结果表';

-- 学术不端检测报告表
CREATE TABLE IF NOT EXISTS `duplicate_check_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `paper_id` BIGINT COMMENT '论文ID',
    `paper_title` VARCHAR(512),
    `check_time` DATETIME,
    `overall_risk_level` VARCHAR(32) COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    `overall_similarity` DOUBLE COMMENT '整体最高相似度 0~1',
    `similar_papers_json` JSON COMMENT '相似论文列表',
    `sausage_detection_json` JSON COMMENT '切香肠检测结果',
    `evidence_details_json` JSON COMMENT '证据详情',
    `checker` VARCHAR(64) COMMENT '检测人',
    `check_note` VARCHAR(1024) COMMENT '人工复核备注',
    `report_status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/DISMISSED',
    `suspected_sausage_count` INT DEFAULT 0 COMMENT '疑似切香肠论文数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_paper_id` (`paper_id`),
    INDEX `idx_status` (`report_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学术不端检测报告表';

-- 作者回复表
CREATE TABLE IF NOT EXISTS `review_reply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `review_record_id` BIGINT NOT NULL COMMENT '关联评审记录ID',
    `paper_id` VARCHAR(64) COMMENT '论文ID',
    `author_id` BIGINT NOT NULL COMMENT '回复人(作者)ID',
    `author_name` VARCHAR(64) COMMENT '作者姓名冗余',
    `thread_number` INT DEFAULT 1 COMMENT '第几条评审意见(1,2,3...)',
    `original_comment_segment` TEXT COMMENT '被回复的评审意见片段',
    `parent_reply_id` BIGINT COMMENT '父回复ID(对话线程)',
    `content` TEXT COMMENT '回复内容',
    `suggested_references_json` JSON COMMENT '系统推荐的参考文献',
    `cited_literature_json` JSON COMMENT '作者引用的文献',
    `status` VARCHAR(32) DEFAULT 'SUBMITTED' COMMENT 'DRAFT/SUBMITTED/RESOLVED',
    `resolved` TINYINT(1) DEFAULT 0 COMMENT '是否已解决',
    `reviewer_response` TEXT COMMENT '评审人回应',
    `reviewer_response_time` DATETIME COMMENT '评审人回应时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_review_record_id` (`review_record_id`),
    INDEX `idx_thread` (`review_record_id`, `thread_number`),
    INDEX `idx_author` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作者评审回复表';

-- 评审质量指标快照表
CREATE TABLE IF NOT EXISTS `review_quality_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `reviewer_id` BIGINT NOT NULL,
    `reviewer_name` VARCHAR(64),
    `period` INT DEFAULT 1 COMMENT '统计周期: 1月/3月/12月',
    `calculate_time` DATETIME COMMENT '计算时间',
    `review_count` INT DEFAULT 0,
    `average_overall_score` DOUBLE,
    `average_innovation_score` DOUBLE,
    `average_method_score` DOUBLE,
    `average_writing_score` DOUBLE,
    `average_review_duration_hours` DOUBLE,
    `median_review_duration_hours` DOUBLE,
    `score_standard_deviation` DOUBLE COMMENT '评分偏离度(标准差)',
    `score_coefficient_of_variation` DOUBLE COMMENT '变异系数',
    `timely_completed_count` INT DEFAULT 0,
    `over_due_count` INT DEFAULT 0,
    `timely_completion_rate` DOUBLE COMMENT '及时完成率 %',
    `rejected_by_author_count` INT DEFAULT 0,
    `accepted_by_author_count` INT DEFAULT 0,
    `comment_word_count_average` DOUBLE,
    `comment_detail_score` DOUBLE COMMENT '评审意见详细度评分',
    `overall_quality_score` DOUBLE COMMENT '综合质量分 0-100',
    `quality_level` VARCHAR(32) COMMENT 'EXCELLENT/GOOD/FAIR/POOR',
    INDEX `idx_reviewer` (`reviewer_id`),
    INDEX `idx_calc_time` (`calculate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评审质量指标快照表';
