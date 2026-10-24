-- =====================================================================
--  mindmap/schema.sql
-- =====================================================================

SET NAMES utf8mb4;
USE `designer_mindmap`;

DROP TABLE IF EXISTS `mm_project`;
DROP TABLE IF EXISTS `mm_category`;

CREATE TABLE `mm_category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父分类ID，0为顶级',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '分类名称',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序（越小越靠前）',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_mm_category_parent` (`parent_id`),
    KEY `idx_mm_category_sort` (`sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '思维导图分类';

CREATE TABLE `mm_project` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `category_id` BIGINT        DEFAULT NULL            COMMENT '分类ID，空为未分类',
    `name`        VARCHAR(100)  NOT NULL                COMMENT '项目名称',
    `layout`      VARCHAR(50)   NOT NULL DEFAULT 'logicalStructure' COMMENT '布局',
    `theme`       VARCHAR(50)   NOT NULL DEFAULT 'default' COMMENT '主题',
    `content`     MEDIUMTEXT    NOT NULL                COMMENT '导图节点树 JSON',
    `remark`      VARCHAR(255)  NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`   VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_mm_project_name` (`name`),
    KEY `idx_mm_project_category` (`category_id`),
    KEY `idx_mm_project_update` (`update_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '思维导图项目';
