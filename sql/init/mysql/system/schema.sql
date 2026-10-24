-- =====================================================================
--  system/schema.sql
-- =====================================================================

SET NAMES utf8mb4;
USE `designer_mindmap`;

-- ---------------------------------------------------------------------
-- 系统管理
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL                COMMENT '登录账号',
    `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt）',
    `nickname`    VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '昵称',
    `avatar`      VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '头像地址',
    `email`       VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '邮箱',
    `phone`       VARCHAR(20)  NOT NULL DEFAULT ''     COMMENT '手机号',
    `dept_id`     BIGINT       DEFAULT NULL            COMMENT '部门ID',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0停用',
    `remark`      VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`, (IF(`deleted` = 0, 0, NULL))),
    KEY `idx_sys_user_dept` (`dept_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户';

DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父部门ID，0为顶级',
    `ancestors`   VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '祖级列表，如 0,100,101',
    `dept_name`   VARCHAR(50)  NOT NULL                COMMENT '部门名称',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0停用',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_dept_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门';

DROP TABLE IF EXISTS `sys_user_post`;
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `post_name`   VARCHAR(50)  NOT NULL                COMMENT '岗位名称，如系长/科长/部长',
    `post_code`   VARCHAR(50)  NOT NULL                COMMENT '岗位编码，全局唯一，如 KE_ZHANG',
    `post_level`  INT          NOT NULL DEFAULT 0      COMMENT '职级，数值越大越高',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0停用',
    `remark`      VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_post_code` (`post_code`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '岗位（全局字典，部门通过用户关联）';

CREATE TABLE `sys_user_post` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `post_id` BIGINT NOT NULL COMMENT '岗位ID',
    PRIMARY KEY (`user_id`, `post_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户岗位关联';

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '参数ID',
    `config_name`  VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT '参数名称',
    `config_key`   VARCHAR(100)  NOT NULL                COMMENT '参数键名',
    `config_value` VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '参数键值',
    `config_type`  CHAR(1)       NOT NULL DEFAULT 'N'    COMMENT '系统内置：Y是 N否',
    `remark`       VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`    VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`  DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`    VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`  DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`      TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_config_key` (`config_key`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '参数配置';

DROP TABLE IF EXISTS `sys_setting`;
CREATE TABLE `sys_setting` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键（单行固定 id=1）',
    `system_name`           VARCHAR(50)  NOT NULL DEFAULT 'Designer Admin' COMMENT '系统名称（登录页/侧边栏/顶栏/浏览器标题）',
    `login_subtitle`        VARCHAR(100) NOT NULL DEFAULT '统一管理后台，让协作更高效' COMMENT '登录页副标题，留空不显示',
    `logo_url`              VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '系统 Logo 地址，留空用内置 Logo',
    `logo_radius`           INT          NOT NULL DEFAULT 6      COMMENT 'Logo 圆角（px，0 为直角）',
    `default_layout_mode`   VARCHAR(32)  NOT NULL DEFAULT 'mixed-vertical' COMMENT '默认布局模式',
    `default_primary_color` VARCHAR(16)  NOT NULL DEFAULT '#16A34A' COMMENT '默认主题色（十六进制）',
    `create_by`             VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`           DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`             VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`           DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`               TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统设置（单行：系统名称/副标题/Logo/默认偏好）';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`   VARCHAR(50)  NOT NULL                COMMENT '角色名称',
    `role_key`    VARCHAR(50)  NOT NULL                COMMENT '角色标识',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0停用',
    `is_super`    TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否超级管理员：1是 0否（取代 role_key=admin 字符串判断）',
    `role_type`   TINYINT      NOT NULL DEFAULT 2      COMMENT '角色类型：1=系统内置（不可修改/删除） 2=自定义',
    `data_scope`  TINYINT      NOT NULL DEFAULT 1      COMMENT '数据权限：1全部 2自定义部门 3本部门 4本部门及以下 5仅本人',
    `remark`      VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_key` (`role_key`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统角色';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父菜单ID，0为顶级',
    `menu_name`   VARCHAR(50)  NOT NULL                COMMENT '菜单名称',
    `menu_type`   CHAR(1)      NOT NULL                COMMENT '类型：M目录 C菜单 F按钮',
    `path`        VARCHAR(200) NOT NULL DEFAULT ''     COMMENT '路由地址',
    `component`   VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '组件路径',
    `perms`       VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '权限标识',
    `icon`        VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '菜单图标',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
    `visible`     TINYINT      NOT NULL DEFAULT 1      COMMENT '是否显示：1显示 0隐藏',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0停用',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_menu_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统菜单';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联';

DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `dept_id` BIGINT NOT NULL COMMENT '部门ID（data_scope=2 自定义部门时有效）',
    PRIMARY KEY (`role_id`, `dept_id`),
    KEY `idx_sys_role_dept_dept` (`dept_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色数据权限（自定义部门）关联';

DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
    `dict_name`   VARCHAR(50)  NOT NULL                COMMENT '字典名称',
    `dict_type`   VARCHAR(50)  NOT NULL                COMMENT '字典类型标识',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0停用',
    `remark`      VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_dict_type` (`dict_type`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典类型';

DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
    `dict_type`   VARCHAR(50)  NOT NULL                COMMENT '字典类型标识',
    `dict_label`  VARCHAR(50)  NOT NULL                COMMENT '字典标签',
    `dict_value`  VARCHAR(50)  NOT NULL                COMMENT '字典键值',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
    `is_default`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否默认：1是 0否',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0停用',
    `remark`      VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_dict_data_type` (`dict_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典数据';

DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `title`          VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '操作模块',
    `business_type`  VARCHAR(20)   NOT NULL DEFAULT ''     COMMENT '业务类型',
    `method`         VARCHAR(255)  NOT NULL DEFAULT ''     COMMENT '方法名称',
    `request_method` VARCHAR(10)   NOT NULL DEFAULT ''     COMMENT '请求方式',
    `oper_name`      VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '操作人',
    `oper_url`       VARCHAR(255)  NOT NULL DEFAULT ''     COMMENT '请求URL',
    `oper_ip`        VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '操作IP',
    `oper_param`     TEXT          DEFAULT NULL            COMMENT '请求参数',
    `json_result`    TEXT          DEFAULT NULL            COMMENT '返回结果',
    `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1成功 0失败',
    `error_msg`      TEXT          DEFAULT NULL            COMMENT '错误信息',
    `cost_time`      BIGINT        NOT NULL DEFAULT 0      COMMENT '耗时（毫秒）',
    `oper_time`      DATETIME      DEFAULT NULL            COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_sys_oper_log_time` (`oper_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志';

DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `username`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '登录账号',
    `ip`         VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '登录IP',
    `browser`    VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '浏览器',
    `os`         VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '操作系统',
    `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1成功 0失败',
    `msg`        VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '提示信息',
    `login_time` DATETIME     DEFAULT NULL            COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_sys_login_log_time` (`login_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志';

DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `original_name` VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '原始文件名',
    `file_name`     VARCHAR(128) NOT NULL DEFAULT ''     COMMENT '存储文件名',
    `file_path`     VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '存储相对路径/对象Key',
    `url`           VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '访问URL',
    `file_size`     BIGINT       NOT NULL DEFAULT 0      COMMENT '文件大小（字节）',
    `content_type`  VARCHAR(128) NOT NULL DEFAULT ''     COMMENT 'Content-Type',
    `extension`     VARCHAR(32)  NOT NULL DEFAULT ''     COMMENT '扩展名',
    `biz_dir`       VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '业务子目录',
    `storage_type`  VARCHAR(16)  NOT NULL DEFAULT 'local' COMMENT '存储类型：local/s3/...',
    `config_id`     BIGINT       DEFAULT NULL            COMMENT '存储配置ID',
    `create_by`     VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`   DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`     VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_file_biz` (`biz_dir`),
    KEY `idx_sys_file_path` (`file_path`),
    KEY `idx_sys_file_name` (`original_name`),
    KEY `idx_sys_file_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统文件';

DROP TABLE IF EXISTS `sys_storage_config`;
CREATE TABLE `sys_storage_config` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_name`        VARCHAR(50)   NOT NULL                COMMENT '配置名称',
    `storage_type`       VARCHAR(32)   NOT NULL                COMMENT '存储类型：local/s3/...',
    `is_default`         TINYINT       NOT NULL DEFAULT 0      COMMENT '是否默认：1是 0否',
    `status`             TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
    `is_public`          TINYINT       NOT NULL DEFAULT 0      COMMENT '是否公开访问：1公开 0私有',
    `url_prefix`         VARCHAR(255)  NOT NULL DEFAULT '/admin-api/system/file/download/' COMMENT '代理下载前缀（按访问类型自动生成）',
    `allowed_extensions` VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '允许扩展名，逗号分隔',
    `max_size`           BIGINT        NOT NULL DEFAULT 5242880 COMMENT '单文件大小上限（字节）',
    `compress_enabled`   TINYINT       NOT NULL DEFAULT 0      COMMENT '上传图片是否压缩：1是 0否',
    `compress_quality`   TINYINT       NOT NULL DEFAULT 80     COMMENT 'JPEG 压缩质量：10-100，越小体积越小；PNG 为无损压缩、忽略该值',
    `config_json`        TEXT          DEFAULT NULL            COMMENT '类型专属配置JSON',
    `remark`             VARCHAR(255)  NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`          VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`        DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`          VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`        DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`            TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_storage_type` (`storage_type`),
    KEY `idx_sys_storage_default` (`is_default`),
    KEY `idx_sys_storage_public` (`is_public`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文件存储配置';

DROP TABLE IF EXISTS `sys_message_config`;
CREATE TABLE `sys_message_config` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_name`  VARCHAR(50)   NOT NULL                COMMENT '配置名称',
    `channel_type` VARCHAR(32)   NOT NULL                COMMENT '通道类型：email/sms/dingtalk/wechat/feishu',
    `is_default`   TINYINT       NOT NULL DEFAULT 0      COMMENT '同通道类型下是否默认：1是 0否',
    `status`       TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
    `config_json`  TEXT          DEFAULT NULL            COMMENT '通道专属配置JSON',
    `remark`       VARCHAR(255)  NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`    VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`  DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`    VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`  DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`      TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_message_type` (`channel_type`),
    KEY `idx_sys_message_default` (`channel_type`, `is_default`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息通道配置';

DROP TABLE IF EXISTS `sys_message_record`;
CREATE TABLE `sys_message_record` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `config_id`    BIGINT        DEFAULT NULL            COMMENT '通道配置ID',
    `config_name`  VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '配置名称快照',
    `channel_type` VARCHAR(32)   NOT NULL DEFAULT ''     COMMENT '通道类型',
    `receivers`    VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '收件人',
    `title`        VARCHAR(200)  NOT NULL DEFAULT ''     COMMENT '标题',
    `content`      VARCHAR(2000) NOT NULL DEFAULT ''     COMMENT '内容摘要',
    `html`         TINYINT       NOT NULL DEFAULT 0      COMMENT '是否HTML：1是 0否',
    `status`       TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1成功 0失败',
    `error_msg`    VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '错误信息',
    `cost_time`    BIGINT        NOT NULL DEFAULT 0      COMMENT '耗时（毫秒）',
    `send_by`      VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '发送人',
    `send_time`    DATETIME      DEFAULT NULL            COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_sys_message_record_type` (`channel_type`),
    KEY `idx_sys_message_record_time` (`send_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息发送记录';

DROP TABLE IF EXISTS `sys_notice_read`;
DROP TABLE IF EXISTS `sys_notice_receiver`;
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `title`        VARCHAR(100)  NOT NULL                COMMENT '标题',
    `content`      TEXT          DEFAULT NULL            COMMENT '内容',
    `status`       TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1正常 0关闭',
    `receiver_all` TINYINT       NOT NULL DEFAULT 0      COMMENT '是否全体用户：1是 0否（可见性以本列 + sys_notice_receiver 为准）',
    `receivers`    VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '接收人原文（账号或ID，逗号分隔）；仅展示/审计，不参与查询',
    `remark`       VARCHAR(255)  NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`    VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`  DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`    VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`  DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`      TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_notice_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内消息（inbox 通道）';

-- 定向消息的接收人：按 (user_id, notice_id) 索引，收件箱与未读数可在数据库分页/计数。
-- 全体消息（receiver_all = 1）不落行，避免用户数 × 消息数膨胀。
CREATE TABLE `sys_notice_receiver` (
    `notice_id` BIGINT NOT NULL COMMENT '消息ID',
    `user_id`   BIGINT NOT NULL COMMENT '接收人用户ID',
    PRIMARY KEY (`notice_id`, `user_id`),
    KEY `idx_sys_notice_receiver_user` (`user_id`, `notice_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内消息接收人（定向消息）';

CREATE TABLE `sys_notice_read` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `notice_id`    BIGINT        NOT NULL                COMMENT '消息ID',
    `user_id`      BIGINT        NOT NULL                COMMENT '用户ID',
    `read_time`    DATETIME      DEFAULT NULL            COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_notice_read` (`notice_id`, `user_id`),
    KEY `idx_sys_notice_read_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内消息已读记录';

DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `job_name`        VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '任务名称',
    `job_group`       VARCHAR(64)   NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
    `invoke_target`   VARCHAR(500)  NOT NULL                COMMENT '调用目标：bean.method / bean.method(args)',
    `cron_expression` VARCHAR(255)  NOT NULL DEFAULT ''     COMMENT 'cron 表达式',
    `misfire_policy`  TINYINT       NOT NULL DEFAULT 1      COMMENT 'misfire：1默认 2立即执行 3放弃',
    `concurrent`      TINYINT       NOT NULL DEFAULT 1      COMMENT '是否允许并发：1允许 0禁止',
    `status`          TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1正常 0暂停',
    `remark`          VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`       VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`     DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`       VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`     DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_job_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务';

DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `job_id`          BIGINT        DEFAULT NULL            COMMENT '任务ID',
    `job_name`        VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '任务名称',
    `job_group`       VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '任务组名',
    `invoke_target`   VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '调用目标',
    `job_message`     VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '日志信息',
    `status`          TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1成功 0失败',
    `exception_info`  VARCHAR(2000) NOT NULL DEFAULT ''     COMMENT '异常信息',
    `cost_time`       BIGINT        NOT NULL DEFAULT 0      COMMENT '耗时（毫秒）',
    `create_time`     DATETIME      DEFAULT NULL            COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sys_job_log_job` (`job_id`),
    KEY `idx_sys_job_log_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务调度日志';

DROP TABLE IF EXISTS `sys_sso_client`;
CREATE TABLE `sys_sso_client` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '客户端ID',
    `client_name` VARCHAR(100) NOT NULL                COMMENT '客户端名称',
    `client_key`  VARCHAR(64)  NOT NULL                COMMENT '客户端标识，如 app1',
    `origin`      VARCHAR(255) NOT NULL                COMMENT '允许回跳的 Origin，如 http://localhost:5176',
    `open_url`    VARCHAR(500) NOT NULL                COMMENT '打开地址（一般与 Origin 同主机）',
    `entry_path`  VARCHAR(100) NOT NULL DEFAULT '/sso' COMMENT 'SSO 接入路径',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0停用',
    `remark`      VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_sso_client_key` (`client_key`, (IF(`deleted` = 0, 0, NULL))),
    UNIQUE KEY `uk_sys_sso_client_origin` (`origin`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SSO 客户端（跨应用单点登录白名单）';

DROP TABLE IF EXISTS `sys_oauth_client_scope`;
DROP TABLE IF EXISTS `sys_oauth_client`;
DROP TABLE IF EXISTS `sys_oauth_scope`;
CREATE TABLE `sys_oauth_scope` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Scope ID',
    `scope_code`   VARCHAR(100) NOT NULL                COMMENT 'Scope 标识，如 open.ping',
    `scope_name`   VARCHAR(100) NOT NULL                COMMENT '显示名称',
    `path_pattern` VARCHAR(255) NOT NULL                COMMENT '路径模式（* 单层 / ** 多层 / {var} 变量），如 /open-api/system/ping',
    `sort`         INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
    `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0停用',
    `remark`       VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`    VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`  DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`    VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`  DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_oauth_scope_code` (`scope_code`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'OAuth2 开放接口 Scope 目录';

CREATE TABLE `sys_oauth_client` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `client_id`         VARCHAR(64)  NOT NULL                COMMENT 'OAuth2 client_id',
    `client_secret`     VARCHAR(100) NOT NULL                COMMENT '客户端密钥（BCrypt）',
    `client_name`       VARCHAR(100) NOT NULL                COMMENT '客户端名称',
    `access_token_ttl`  INT          NOT NULL DEFAULT 7200   COMMENT 'access_token 有效期（秒）',
    `status`            TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0停用',
    `remark`            VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`         VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`       DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`         VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`       DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`           TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_oauth_client_id` (`client_id`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'OAuth2 客户端（Client Credentials）';

CREATE TABLE `sys_oauth_client_scope` (
    `client_id` BIGINT NOT NULL COMMENT 'sys_oauth_client.id',
    `scope_id`  BIGINT NOT NULL COMMENT 'sys_oauth_scope.id',
    PRIMARY KEY (`client_id`, `scope_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'OAuth2 客户端授权 Scope';

-- ---------------------------------------------------------------------
-- Quartz JDBC JobStore
--
-- 由 Quartz 自己读写，业务代码不要直接改这里的行。任务定义仍以 sys_job 为准，
-- 两张表由 SysJobServiceImpl 与 ScheduleManager 负责同步。
-- 列名/索引与 Quartz 2.5 官方 tables_mysql_innodb.sql 一致（仅去掉 BIGINT 显示宽度），
-- 升级 Quartz 版本时必须回来核对，否则会出现「调度器启动时报 SQL 语法错误」
-- 或「任务不触发」这类难以定位的问题。
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
DROP TABLE IF EXISTS `QRTZ_LOCKS`;
DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
DROP TABLE IF EXISTS `QRTZ_CALENDARS`;

CREATE TABLE `QRTZ_JOB_DETAILS` (
    `SCHED_NAME`        VARCHAR(120) NOT NULL COMMENT '调度器名（instanceName）',
    `JOB_NAME`          VARCHAR(190) NOT NULL COMMENT '任务名',
    `JOB_GROUP`         VARCHAR(190) NOT NULL COMMENT '任务组',
    `DESCRIPTION`       VARCHAR(250) NULL     COMMENT '描述',
    `JOB_CLASS_NAME`    VARCHAR(250) NOT NULL COMMENT '执行类全名',
    `IS_DURABLE`        VARCHAR(1)   NOT NULL COMMENT '是否持久',
    `IS_NONCONCURRENT`  VARCHAR(1)   NOT NULL COMMENT '是否禁止并发',
    `IS_UPDATE_DATA`    VARCHAR(1)   NOT NULL COMMENT '是否更新 JobData',
    `REQUESTS_RECOVERY` VARCHAR(1)   NOT NULL COMMENT '故障后是否恢复',
    `JOB_DATA`          BLOB         NULL     COMMENT 'JobDataMap 的 Java 序列化结果',
    PRIMARY KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz 任务明细';

CREATE TABLE `QRTZ_TRIGGERS` (
    `SCHED_NAME`     VARCHAR(120) NOT NULL COMMENT '调度器名',
    `TRIGGER_NAME`   VARCHAR(190) NOT NULL COMMENT '触发器名',
    `TRIGGER_GROUP`  VARCHAR(190) NOT NULL COMMENT '触发器组',
    `JOB_NAME`       VARCHAR(190) NOT NULL COMMENT '任务名',
    `JOB_GROUP`      VARCHAR(190) NOT NULL COMMENT '任务组',
    `DESCRIPTION`    VARCHAR(250) NULL     COMMENT '描述',
    `NEXT_FIRE_TIME` BIGINT       NULL     COMMENT '下次触发时间（毫秒时间戳）',
    `PREV_FIRE_TIME` BIGINT       NULL     COMMENT '上次触发时间（毫秒时间戳）',
    `PRIORITY`       INT          NULL     COMMENT '优先级',
    `TRIGGER_STATE`  VARCHAR(16)  NOT NULL COMMENT '触发器状态',
    `TRIGGER_TYPE`   VARCHAR(8)   NOT NULL COMMENT '触发器类型',
    `START_TIME`     BIGINT       NOT NULL COMMENT '开始时间',
    `END_TIME`       BIGINT       NULL     COMMENT '结束时间',
    `CALENDAR_NAME`  VARCHAR(190) NULL     COMMENT '日历名',
    `MISFIRE_INSTR`  SMALLINT     NULL     COMMENT 'misfire 策略',
    `JOB_DATA`       BLOB         NULL     COMMENT 'JobDataMap 的 Java 序列化结果',
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    KEY `IDX_QRTZ_T_J` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`),
    KEY `IDX_QRTZ_T_JG` (`SCHED_NAME`, `JOB_GROUP`),
    KEY `IDX_QRTZ_T_C` (`SCHED_NAME`, `CALENDAR_NAME`),
    KEY `IDX_QRTZ_T_G` (`SCHED_NAME`, `TRIGGER_GROUP`),
    KEY `IDX_QRTZ_T_STATE` (`SCHED_NAME`, `TRIGGER_STATE`),
    KEY `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`, `TRIGGER_STATE`),
    KEY `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`, `TRIGGER_GROUP`, `TRIGGER_STATE`),
    KEY `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`, `NEXT_FIRE_TIME`),
    KEY `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`, `TRIGGER_STATE`, `NEXT_FIRE_TIME`),
    KEY `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`, `MISFIRE_INSTR`, `NEXT_FIRE_TIME`),
    KEY `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`, `MISFIRE_INSTR`, `NEXT_FIRE_TIME`, `TRIGGER_STATE`),
    KEY `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`, `MISFIRE_INSTR`, `NEXT_FIRE_TIME`, `TRIGGER_GROUP`, `TRIGGER_STATE`),
    CONSTRAINT `QRTZ_TRIGGERS_JOB_FK` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
        REFERENCES `QRTZ_JOB_DETAILS` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz 触发器';

CREATE TABLE `QRTZ_SIMPLE_TRIGGERS` (
    `SCHED_NAME`      VARCHAR(120) NOT NULL COMMENT '调度器名',
    `TRIGGER_NAME`    VARCHAR(190) NOT NULL COMMENT '触发器名',
    `TRIGGER_GROUP`   VARCHAR(190) NOT NULL COMMENT '触发器组',
    `REPEAT_COUNT`    BIGINT       NOT NULL COMMENT '重复次数',
    `REPEAT_INTERVAL` BIGINT       NOT NULL COMMENT '重复间隔（毫秒）',
    `TIMES_TRIGGERED` BIGINT       NOT NULL COMMENT '已触发次数',
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_FK` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
        REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz 简单触发器';

CREATE TABLE `QRTZ_CRON_TRIGGERS` (
    `SCHED_NAME`      VARCHAR(120) NOT NULL COMMENT '调度器名',
    `TRIGGER_NAME`    VARCHAR(190) NOT NULL COMMENT '触发器名',
    `TRIGGER_GROUP`   VARCHAR(190) NOT NULL COMMENT '触发器组',
    `CRON_EXPRESSION` VARCHAR(120) NOT NULL COMMENT 'cron 表达式',
    `TIME_ZONE_ID`    VARCHAR(80)  NULL     COMMENT '时区',
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_CRON_TRIGGERS_FK` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
        REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz cron 触发器';

CREATE TABLE `QRTZ_SIMPROP_TRIGGERS` (
    `SCHED_NAME`    VARCHAR(120)  NOT NULL COMMENT '调度器名',
    `TRIGGER_NAME`  VARCHAR(190)  NOT NULL COMMENT '触发器名',
    `TRIGGER_GROUP` VARCHAR(190)  NOT NULL COMMENT '触发器组',
    `STR_PROP_1`    VARCHAR(512)  NULL     COMMENT '字符串属性 1',
    `STR_PROP_2`    VARCHAR(512)  NULL     COMMENT '字符串属性 2',
    `STR_PROP_3`    VARCHAR(512)  NULL     COMMENT '字符串属性 3',
    `INT_PROP_1`    INT           NULL     COMMENT '整型属性 1',
    `INT_PROP_2`    INT           NULL     COMMENT '整型属性 2',
    `LONG_PROP_1`   BIGINT        NULL     COMMENT '长整型属性 1',
    `LONG_PROP_2`   BIGINT        NULL     COMMENT '长整型属性 2',
    `DEC_PROP_1`    NUMERIC(13,4) NULL     COMMENT '小数属性 1',
    `DEC_PROP_2`    NUMERIC(13,4) NULL     COMMENT '小数属性 2',
    `BOOL_PROP_1`   VARCHAR(1)    NULL     COMMENT '布尔属性 1',
    `BOOL_PROP_2`   VARCHAR(1)    NULL     COMMENT '布尔属性 2',
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_FK` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
        REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz 自定义属性触发器';

CREATE TABLE `QRTZ_BLOB_TRIGGERS` (
    `SCHED_NAME`    VARCHAR(120) NOT NULL COMMENT '调度器名',
    `TRIGGER_NAME`  VARCHAR(190) NOT NULL COMMENT '触发器名',
    `TRIGGER_GROUP` VARCHAR(190) NOT NULL COMMENT '触发器组',
    `BLOB_DATA`     BLOB         NULL     COMMENT '序列化后的触发器',
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    KEY `IDX_QRTZ_BT_T` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_BLOB_TRIGGERS_FK` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
        REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz Blob 触发器';

CREATE TABLE `QRTZ_CALENDARS` (
    `SCHED_NAME`    VARCHAR(120) NOT NULL COMMENT '调度器名',
    `CALENDAR_NAME` VARCHAR(190) NOT NULL COMMENT '日历名',
    `CALENDAR`      BLOB         NOT NULL COMMENT '序列化后的日历',
    PRIMARY KEY (`SCHED_NAME`, `CALENDAR_NAME`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz 日历';

CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS` (
    `SCHED_NAME`    VARCHAR(120) NOT NULL COMMENT '调度器名',
    `TRIGGER_GROUP` VARCHAR(190) NOT NULL COMMENT '被暂停的触发器组',
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_GROUP`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz 暂停的触发器组';

CREATE TABLE `QRTZ_FIRED_TRIGGERS` (
    `SCHED_NAME`        VARCHAR(120) NOT NULL COMMENT '调度器名',
    `ENTRY_ID`          VARCHAR(95)  NOT NULL COMMENT '触发记录 ID',
    `TRIGGER_NAME`      VARCHAR(190) NOT NULL COMMENT '触发器名',
    `TRIGGER_GROUP`     VARCHAR(190) NOT NULL COMMENT '触发器组',
    `INSTANCE_NAME`     VARCHAR(190) NOT NULL COMMENT '执行实例',
    `FIRED_TIME`        BIGINT       NOT NULL COMMENT '触发时间',
    `SCHED_TIME`        BIGINT       NOT NULL COMMENT '计划时间',
    `PRIORITY`          INT          NOT NULL COMMENT '优先级',
    `STATE`             VARCHAR(16)  NOT NULL COMMENT '状态',
    `JOB_NAME`          VARCHAR(190) NULL     COMMENT '任务名',
    `JOB_GROUP`         VARCHAR(190) NULL     COMMENT '任务组',
    `IS_NONCONCURRENT`  VARCHAR(1)   NULL     COMMENT '是否禁止并发',
    `REQUESTS_RECOVERY` VARCHAR(1)   NULL     COMMENT '是否要求恢复',
    PRIMARY KEY (`SCHED_NAME`, `ENTRY_ID`),
    KEY `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`, `INSTANCE_NAME`),
    KEY `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`, `INSTANCE_NAME`, `REQUESTS_RECOVERY`),
    KEY `IDX_QRTZ_FT_J_G` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`),
    KEY `IDX_QRTZ_FT_JG` (`SCHED_NAME`, `JOB_GROUP`),
    KEY `IDX_QRTZ_FT_T_G` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    KEY `IDX_QRTZ_FT_TG` (`SCHED_NAME`, `TRIGGER_GROUP`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz 已触发记录';

CREATE TABLE `QRTZ_SCHEDULER_STATE` (
    `SCHED_NAME`        VARCHAR(120) NOT NULL COMMENT '调度器名',
    `INSTANCE_NAME`     VARCHAR(190) NOT NULL COMMENT '实例名（节点心跳）',
    `LAST_CHECKIN_TIME` BIGINT       NOT NULL COMMENT '最近一次心跳时间',
    `CHECKIN_INTERVAL`  BIGINT       NOT NULL COMMENT '心跳间隔',
    PRIMARY KEY (`SCHED_NAME`, `INSTANCE_NAME`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz 调度器实例状态';

CREATE TABLE `QRTZ_LOCKS` (
    `SCHED_NAME` VARCHAR(120) NOT NULL COMMENT '调度器名',
    `LOCK_NAME`  VARCHAR(40)  NOT NULL COMMENT '锁名（TRIGGER_ACCESS / STATE_ACCESS）',
    PRIMARY KEY (`SCHED_NAME`, `LOCK_NAME`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Quartz 集群行锁';

-- 集群互斥依赖这两行：Quartz 启动时会 SELECT ... FOR UPDATE 它们，
-- 记录缺失会导致获取锁失败、调度器无法启动
INSERT INTO `QRTZ_LOCKS` (`SCHED_NAME`, `LOCK_NAME`) VALUES
('designerAdminScheduler', 'TRIGGER_ACCESS'),
('designerAdminScheduler', 'STATE_ACCESS');
