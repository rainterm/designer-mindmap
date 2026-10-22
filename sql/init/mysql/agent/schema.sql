-- =====================================================================
--  agent/schema.sql  智能体模块（表结构）
-- =====================================================================

SET NAMES utf8mb4;
USE `designer_agent`;

DROP TABLE IF EXISTS `agent_message`;
DROP TABLE IF EXISTS `agent_session`;
DROP TABLE IF EXISTS `agent_agent_version`;
DROP TABLE IF EXISTS `agent_agent`;
DROP TABLE IF EXISTS `agent_knowledge_doc`;
DROP TABLE IF EXISTS `agent_knowledge`;
DROP TABLE IF EXISTS `agent_workflow_version`;
DROP TABLE IF EXISTS `agent_workflow`;
DROP TABLE IF EXISTS `agent_skill`;
DROP TABLE IF EXISTS `agent_mcp`;
DROP TABLE IF EXISTS `agent_tool`;
DROP TABLE IF EXISTS `agent_provider`;

CREATE TABLE `agent_provider` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
    `name`           VARCHAR(100)  NOT NULL                COMMENT '名称',
    `type`           VARCHAR(50)   NOT NULL DEFAULT 'openai_compat' COMMENT '类型：openai_compat 等',
    `base_url`       VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT 'API Base URL',
    `api_key`        VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT 'API Key（密文存储）',
    `api_key_masked` VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT '脱敏展示',
    `models_json`    LONGTEXT                              COMMENT '模型列表 JSON',
    `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '1启用 0停用',
    `remark`         VARCHAR(255)  NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `create_time`    DATETIME      DEFAULT NULL,
    `update_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `update_time`    DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_agent_provider_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '模型供应商';

CREATE TABLE `agent_tool` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '工具ID',
    `name`           VARCHAR(100)  NOT NULL                COMMENT '名称',
    `code`           VARCHAR(64)   NOT NULL                COMMENT '编码（映射 Harness 工具）',
    `type`           VARCHAR(32)   NOT NULL DEFAULT 'builtin' COMMENT 'builtin',
    `description`    VARCHAR(500)  NOT NULL DEFAULT '',
    `schema_json`    TEXT                                  COMMENT '参数 schema',
    `status`         TINYINT       NOT NULL DEFAULT 1,
    `create_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `create_time`    DATETIME      DEFAULT NULL,
    `update_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `update_time`    DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_tool_code` (`code`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台内置工具';

CREATE TABLE `agent_mcp` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(100)  NOT NULL,
    `code`           VARCHAR(64)   NOT NULL,
    `transport`      VARCHAR(20)   NOT NULL DEFAULT 'stdio' COMMENT 'stdio / sse',
    `command`        VARCHAR(255)  NOT NULL DEFAULT '',
    `args`           VARCHAR(1000) NOT NULL DEFAULT '',
    `url`            VARCHAR(500)  NOT NULL DEFAULT '',
    `env_json`       TEXT,
    `tools_json`     LONGTEXT                              COMMENT '探测到的工具列表',
    `status`         TINYINT       NOT NULL DEFAULT 1,
    `remark`         VARCHAR(255)  NOT NULL DEFAULT '',
    `create_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `create_time`    DATETIME      DEFAULT NULL,
    `update_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `update_time`    DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_mcp_code` (`code`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'MCP 服务';

CREATE TABLE `agent_skill` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(100)  NOT NULL,
    `code`           VARCHAR(64)   NOT NULL,
    `description`    VARCHAR(500)  NOT NULL DEFAULT '',
    `content`        LONGTEXT                              COMMENT 'SKILL.md 内容',
    `source_type`    VARCHAR(20)   NOT NULL DEFAULT 'manual' COMMENT 'manual / package',
    `package_name`   VARCHAR(255)  NOT NULL DEFAULT '',
    `package_size`   BIGINT        NOT NULL DEFAULT 0,
    `files_json`     LONGTEXT,
    `skill_path`     VARCHAR(500)  NOT NULL DEFAULT '',
    `status`         TINYINT       NOT NULL DEFAULT 1,
    `create_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `create_time`    DATETIME      DEFAULT NULL,
    `update_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `update_time`    DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_skill_code` (`code`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Skill';

CREATE TABLE `agent_workflow` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(100)  NOT NULL,
    `code`           VARCHAR(64)   NOT NULL,
    `description`    VARCHAR(500)  NOT NULL DEFAULT '',
    `nodes_json`     LONGTEXT,
    `edges_json`     LONGTEXT,
    `draft_nodes_json` LONGTEXT,
    `draft_edges_json` LONGTEXT,
    `version`        INT           NOT NULL DEFAULT 1 COMMENT '已发布版本号：仅发布时递增，运行/试跑以此为准',
    `draft_version`  INT           NOT NULL DEFAULT 1 COMMENT '草稿版本号：保存即递增，不影响运行',
    `status`         TINYINT       NOT NULL DEFAULT 1,
    `remark`         VARCHAR(255)  NOT NULL DEFAULT '',
    `create_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `create_time`    DATETIME      DEFAULT NULL,
    `update_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `update_time`    DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_workflow_code` (`code`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工作流定义';

CREATE TABLE `agent_workflow_version` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `workflow_id`    BIGINT        NOT NULL,
    `version`        INT           NOT NULL COMMENT '已发布版本号（与主表 version 对齐）',
    `name`           VARCHAR(100)  NOT NULL DEFAULT '',
    `code`           VARCHAR(64)   NOT NULL DEFAULT '',
    `description`    VARCHAR(500)  NOT NULL DEFAULT '',
    `nodes_json`     LONGTEXT,
    `edges_json`     LONGTEXT,
    `status`         TINYINT       NOT NULL DEFAULT 1,
    `remark`         VARCHAR(255)  NOT NULL DEFAULT '',
    `published_by`   VARCHAR(64)   NOT NULL DEFAULT '',
    `published_at`   DATETIME      DEFAULT NULL,
    `create_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `create_time`    DATETIME      DEFAULT NULL,
    `update_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `update_time`    DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_version` (`workflow_id`, `version`),
    KEY `idx_workflow_version_wf` (`workflow_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工作流已发布历史快照';

CREATE TABLE `agent_knowledge` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `name`             VARCHAR(100)  NOT NULL,
    `code`             VARCHAR(64)   NOT NULL,
    `description`      VARCHAR(500)  NOT NULL DEFAULT '',
    `tags_json`        TEXT,
    `embedding_provider_id` BIGINT   DEFAULT NULL COMMENT 'Embedding 模型供应商',
    `embedding_model`  VARCHAR(100)  NOT NULL DEFAULT '',
    `embedding_dim`    INT           NOT NULL DEFAULT 0 COMMENT '向量维度，首次索引后锁定',
    `chunk_strategy`   VARCHAR(32)   NOT NULL DEFAULT 'section_recursive' COMMENT 'fixed / section_recursive',
    `chunk_size`       INT           NOT NULL DEFAULT 500,
    `chunk_overlap`    INT           NOT NULL DEFAULT 50,
    `top_k`            INT           NOT NULL DEFAULT 5,
    `score_threshold`  DECIMAL(6,4)  NOT NULL DEFAULT 0.5000,
    `status`           TINYINT       NOT NULL DEFAULT 1,
    `create_by`        VARCHAR(50)   NOT NULL DEFAULT '',
    `create_time`      DATETIME      DEFAULT NULL,
    `update_by`        VARCHAR(50)   NOT NULL DEFAULT '',
    `update_time`      DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_knowledge_code` (`code`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库';

CREATE TABLE `agent_knowledge_doc` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `knowledge_id`   BIGINT        NOT NULL,
    `title`          VARCHAR(200)  NOT NULL DEFAULT '',
    `source_type`    VARCHAR(20)   NOT NULL DEFAULT 'text' COMMENT 'upload/text/url',
    `file_name`      VARCHAR(255)  NOT NULL DEFAULT '',
    `file_type`      VARCHAR(50)   NOT NULL DEFAULT '',
    `file_size`      BIGINT        NOT NULL DEFAULT 0,
    `content`        LONGTEXT,
    `url`            VARCHAR(1000) NOT NULL DEFAULT '',
    `status`         VARCHAR(20)   NOT NULL DEFAULT 'ready' COMMENT 'ready/indexing/pending/failed',
    `chunk_count`    INT           NOT NULL DEFAULT 0,
    `index_progress` INT           NOT NULL DEFAULT 0 COMMENT '索引进度 0-100',
    `char_count`     INT           NOT NULL DEFAULT 0,
    `error_msg`      VARCHAR(500)  NOT NULL DEFAULT '',
    `create_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `create_time`    DATETIME      DEFAULT NULL,
    `update_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `update_time`    DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_agent_kdoc_kid` (`knowledge_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库文档';

CREATE TABLE `agent_agent` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `name`             VARCHAR(100)  NOT NULL,
    `code`             VARCHAR(64)   NOT NULL,
    `avatar`           VARCHAR(500)  NOT NULL DEFAULT '',
    `system_prompt`    LONGTEXT,
    `provider_id`      BIGINT        DEFAULT NULL,
    `model_code`       VARCHAR(100)  NOT NULL DEFAULT '',
    `temperature`      DECIMAL(6,4)  NOT NULL DEFAULT 0.7000,
    `top_p`            DECIMAL(6,4)  NOT NULL DEFAULT 1.0000,
    `max_tokens`       INT           NOT NULL DEFAULT 0 COMMENT '0=不限制，使用模型默认最大输出',
    `memory_rounds`    INT           NOT NULL DEFAULT 10 COMMENT '对话记忆轮数（一轮=用户+助手）；0=不带历史',
    `tool_ids_json`    TEXT,
    `mcp_ids_json`     TEXT,
    `skill_ids_json`   TEXT,
    `workflow_ids_json` TEXT,
    `knowledge_ids_json` TEXT,
    `embedding_provider_id` BIGINT   DEFAULT NULL COMMENT 'RAG 查询 Embedding 供应商',
    `embedding_model`  VARCHAR(100)  NOT NULL DEFAULT '' COMMENT 'RAG 查询 Embedding 模型',
    `rerank_provider_id` BIGINT      DEFAULT NULL COMMENT 'RAG 重排序供应商',
    `rerank_model`     VARCHAR(100)  NOT NULL DEFAULT '' COMMENT 'RAG 重排序模型',
    `rag_top_k`        INT           NOT NULL DEFAULT 5 COMMENT 'RAG 最终 Top K（进入上下文条数）',
    `rag_recall_top_k` INT           NOT NULL DEFAULT 0 COMMENT '向量召回候选数；0=自动（开 Rerank 时约为 TopK×3）',
    `rag_score_threshold` DECIMAL(6,4) NOT NULL DEFAULT 0.3500 COMMENT 'RAG 相似度阈值（向量召回）',
    `file_enabled`     TINYINT       NOT NULL DEFAULT 0      COMMENT '是否支持聊天文件上传：1是 0否',
    `status`           TINYINT       NOT NULL DEFAULT 1,
    `remark`           VARCHAR(255)  NOT NULL DEFAULT '',
    `version`          INT           NOT NULL DEFAULT 0 COMMENT '已发布版本号；0=从未发布（草稿态），商店不展示',
    `draft_version`    INT           NOT NULL DEFAULT 1 COMMENT '草稿版本号；大于 version 表示有未发布改动',
    `draft_json`       LONGTEXT      COMMENT '草稿配置快照（完整 AgentSaveDTO JSON）；发布前编辑均写此列，运行读主表正式字段',
    `public_enabled`   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否发布公开访问链接：1是 0否',
    `public_token`     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '公开访问 token；空表示未发布',
    `create_by`        VARCHAR(50)   NOT NULL DEFAULT '',
    `create_time`      DATETIME      DEFAULT NULL,
    `update_by`        VARCHAR(50)   NOT NULL DEFAULT '',
    `update_time`      DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_agent_code` (`code`, (IF(`deleted` = 0, 0, NULL))),
    UNIQUE KEY `uk_agent_agent_public_token` (`public_token`, (IF(`deleted` = 0 AND `public_token` <> '', 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能体';

CREATE TABLE `agent_agent_version` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `agent_id`       BIGINT        NOT NULL,
    `version`        INT           NOT NULL,
    `config_json`    LONGTEXT      COMMENT '发布时完整配置快照（AgentSaveDTO JSON）',
    `name`           VARCHAR(100)  NOT NULL DEFAULT '',
    `code`           VARCHAR(64)   NOT NULL DEFAULT '',
    `status`         TINYINT       NOT NULL DEFAULT 1,
    `remark`         VARCHAR(255)  NOT NULL DEFAULT '',
    `published_by`   VARCHAR(64)   NOT NULL DEFAULT '',
    `published_at`   DATETIME      DEFAULT NULL,
    `create_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `create_time`    DATETIME      DEFAULT NULL,
    `update_by`      VARCHAR(50)   NOT NULL DEFAULT '',
    `update_time`    DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_agent_ver` (`agent_id`, `version`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能体已发布历史快照';

CREATE TABLE `agent_session` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `agent_id`         BIGINT        NOT NULL,
    `title`            VARCHAR(200)  NOT NULL DEFAULT '新会话',
    `message_count`    INT           NOT NULL DEFAULT 0,
    `last_message_at`  DATETIME      DEFAULT NULL,
    `status`           TINYINT       NOT NULL DEFAULT 1,
    `create_by`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人；公开访问为 p:{token}:{visitor}',
    `create_time`      DATETIME      DEFAULT NULL,
    `update_by`        VARCHAR(64)   NOT NULL DEFAULT '',
    `update_time`      DATETIME      DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_agent_session_agent` (`agent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '调试会话';

CREATE TABLE `agent_message` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `session_id`     BIGINT        NOT NULL,
    `role`           VARCHAR(20)   NOT NULL COMMENT 'user/assistant/system',
    `content`        LONGTEXT,
    `attachments_json` LONGTEXT                              COMMENT '附件列表 JSON',
    `tools_json`     LONGTEXT                              COMMENT '工具调用过程 JSON',
    `thinking`       LONGTEXT                              COMMENT '模型思考过程',
    `created_at`     DATETIME      DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_agent_message_sid` (`session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会话消息';

-- 内置工具种子（code 映射 Harness ToolPermission）
INSERT INTO `agent_tool` (`id`, `name`, `code`, `type`, `description`, `schema_json`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '网页搜索', 'web_search', 'builtin', '按关键词检索公开网页摘要（平台内置）',
 '{\n  "type": "object",\n  "properties": {\n    "query": { "type": "string" }\n  },\n  "required": ["query"]\n}',
 1, 'system', NOW(), 'system', NOW()),
(2, 'HTTP 请求', 'http_request', 'builtin', '向指定 URL 发起 HTTP 调用（平台内置）',
 '{\n  "type": "object",\n  "properties": {\n    "url": { "type": "string" },\n    "method": { "type": "string" }\n  },\n  "required": ["url"]\n}',
 1, 'system', NOW(), 'system', NOW()),
(3, '文件系统', 'filesystem', 'builtin', '读写本地工作区文件（平台内置）',
 '{\n  "type": "object",\n  "properties": {\n    "path": { "type": "string" }\n  }\n}',
 1, 'system', NOW(), 'system', NOW()),
(4, '网页抓取', 'browser', 'builtin', '抓取网页内容（降级为 webfetch）',
 '{\n  "type": "object",\n  "properties": {\n    "url": { "type": "string" }\n  },\n  "required": ["url"]\n}',
 1, 'system', NOW(), 'system', NOW());
