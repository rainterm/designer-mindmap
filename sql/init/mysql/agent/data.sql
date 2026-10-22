-- =====================================================================
--  agent/data.sql（菜单 ID 6000 段）
-- =====================================================================

SET NAMES utf8mb4;
USE `designer_agent`;

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(6000, 0,    '智能体模块', 'M', '/agent',                '',                       '',                      'hardware-chip', 60, 1, 1, 'system', NOW(), 'system', NOW()),
-- 使用者入口（浏览已启用智能体并对话）
(6090, 6000, '智能体商店', 'C', '/agent/store',          'agent/store/index',      'agent:store:list',      'storefront',    0, 1, 1, 'system', NOW(), 'system', NOW()),
(6001, 6000, '供应商管理', 'C', '/agent/provider',       'agent/provider/index',   'agent:provider:list',   'cloud',         1, 1, 1, 'system', NOW(), 'system', NOW()),
(6002, 6000, '智能体管理', 'C', '/agent/agent',          'agent/agent/index',      'agent:agent:list',      'extension',     2, 1, 1, 'system', NOW(), 'system', NOW()),
(6003, 6000, '工具管理',   'C', '/agent/tool',           'agent/tool/index',       'agent:tool:list',       'construct',     3, 1, 1, 'system', NOW(), 'system', NOW()),
(6060, 6000, 'MCP 管理',   'C', '/agent/mcp',            'agent/mcp/index',        'agent:mcp:list',        'git-network',   4, 1, 1, 'system', NOW(), 'system', NOW()),
(6070, 6000, 'Skill 管理', 'C', '/agent/skill',          'agent/skill/index',      'agent:skill:list',      'book',          5, 1, 1, 'system', NOW(), 'system', NOW()),
(6080, 6000, '工作流',     'C', '/agent/workflow',       'agent/workflow/index',   'agent:workflow:list',   'swap-horizontal', 6, 1, 1, 'system', NOW(), 'system', NOW()),
(6004, 6000, '知识库',     'C', '/agent/knowledge',      'agent/knowledge/index',  'agent:knowledge:list',  'library',       7, 1, 1, 'system', NOW(), 'system', NOW()),
(6005, 6000, '会话记录',   'C', '/agent/session',        'agent/session/index',    'agent:session:list',    'chatbubbles',   8, 1, 1, 'system', NOW(), 'system', NOW()),
-- 隐藏页（随主列表页，不进侧栏/不单独分配）
(6006, 6002, '智能体编辑', 'C', '/agent/agent/edit',     'agent/agent/edit',       'agent:agent:edit',      '',             97, 0, 1, 'system', NOW(), 'system', NOW()),
(6007, 6005, '对话',       'C', '/agent/session/chat',   'agent/session/chat',     'agent:session:chat',    '',             98, 0, 1, 'system', NOW(), 'system', NOW()),
(6008, 6004, '知识库详情', 'C', '/agent/knowledge/detail','agent/knowledge/detail','agent:knowledge:query', '',             99, 0, 1, 'system', NOW(), 'system', NOW()),
(6009, 6080, '工作流编辑', 'C', '/agent/workflow/edit',  'agent/workflow/edit',    'agent:workflow:edit',   '',             96, 0, 1, 'system', NOW(), 'system', NOW()),
-- 供应商按钮
(6010, 6001, '供应商查询', 'F', '', '', 'agent:provider:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(6011, 6001, '供应商新增', 'F', '', '', 'agent:provider:add',    '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(6012, 6001, '供应商编辑', 'F', '', '', 'agent:provider:edit',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(6013, 6001, '供应商删除', 'F', '', '', 'agent:provider:remove', '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
-- 智能体按钮
(6020, 6002, '智能体查询', 'F', '', '', 'agent:agent:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(6021, 6002, '智能体新增', 'F', '', '', 'agent:agent:add',    '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(6022, 6002, '智能体编辑', 'F', '', '', 'agent:agent:edit',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(6023, 6002, '智能体删除', 'F', '', '', 'agent:agent:remove', '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
(6024, 6002, '智能体发布', 'F', '', '', 'agent:agent:publish', '', 5, 1, 1, 'system', NOW(), 'system', NOW()),
(6025, 6002, '智能体历史版本', 'F', '', '', 'agent:agent:history', '', 6, 1, 1, 'system', NOW(), 'system', NOW()),
(6026, 6002, '智能体回滚', 'F', '', '', 'agent:agent:rollback', '', 7, 1, 1, 'system', NOW(), 'system', NOW()),
-- 工具按钮
(6030, 6003, '工具查询', 'F', '', '', 'agent:tool:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(6031, 6003, '工具新增', 'F', '', '', 'agent:tool:add',    '', 2, 1, 0, 'system', NOW(), 'system', NOW()),
(6032, 6003, '工具编辑', 'F', '', '', 'agent:tool:edit',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(6033, 6003, '工具删除', 'F', '', '', 'agent:tool:remove', '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
-- MCP 按钮
(6061, 6060, 'MCP 查询', 'F', '', '', 'agent:mcp:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(6062, 6060, 'MCP 新增', 'F', '', '', 'agent:mcp:add',    '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(6063, 6060, 'MCP 编辑', 'F', '', '', 'agent:mcp:edit',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(6064, 6060, 'MCP 删除', 'F', '', '', 'agent:mcp:remove', '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
-- Skill 按钮
(6071, 6070, 'Skill 查询', 'F', '', '', 'agent:skill:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(6072, 6070, 'Skill 新增', 'F', '', '', 'agent:skill:add',    '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(6073, 6070, 'Skill 编辑', 'F', '', '', 'agent:skill:edit',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(6074, 6070, 'Skill 删除', 'F', '', '', 'agent:skill:remove', '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
-- 工作流按钮
(6081, 6080, '工作流查询', 'F', '', '', 'agent:workflow:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(6082, 6080, '工作流新增', 'F', '', '', 'agent:workflow:add',    '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(6083, 6080, '工作流编辑', 'F', '', '', 'agent:workflow:edit',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(6084, 6080, '工作流删除', 'F', '', '', 'agent:workflow:remove', '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
(6085, 6080, '工作流发布', 'F', '', '', 'agent:workflow:publish','', 5, 1, 1, 'system', NOW(), 'system', NOW()),
(6086, 6080, '历史版本',   'F', '', '', 'agent:workflow:history','', 6, 1, 1, 'system', NOW(), 'system', NOW()),
(6087, 6080, '版本回滚',   'F', '', '', 'agent:workflow:rollback','', 7, 1, 1, 'system', NOW(), 'system', NOW()),
-- 知识库按钮
(6040, 6004, '知识库查询', 'F', '', '', 'agent:knowledge:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(6041, 6004, '知识库新增', 'F', '', '', 'agent:knowledge:add',    '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(6042, 6004, '知识库编辑', 'F', '', '', 'agent:knowledge:edit',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(6043, 6004, '知识库删除', 'F', '', '', 'agent:knowledge:remove', '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
-- 会话按钮
(6050, 6005, '会话查询', 'F', '', '', 'agent:session:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(6051, 6005, '会话删除', 'F', '', '', 'agent:session:remove', '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(6052, 6005, '对话',     'F', '', '', 'agent:session:chat',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
-- 智能体商店按钮（使用者）
(6091, 6090, '商店查询', 'F', '', '', 'agent:store:list',   '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(6092, 6090, '开始对话', 'F', '', '', 'agent:session:chat', '', 2, 1, 1, 'system', NOW(), 'system', NOW());

-- =====================================================================
--  演示案例数据（首次初始化可见；默认接入 DeepSeek，仅需填写 API Key 即可对话）
-- =====================================================================

-- ------------------------- 模型供应商 -------------------------
INSERT INTO `agent_provider` (`id`, `name`, `type`, `base_url`, `api_key`, `api_key_masked`, `models_json`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 'DeepSeek', 'openai', 'https://api.deepseek.com', '', '',
 '[{"modelCode":"deepseek-flash","displayName":"DeepSeek Flash","modelType":"multimodal","contextWindow":128000,"status":1}]',
 1, '官方 DeepSeek（OpenAI 兼容）；填写 API Key 后即可使用 deepseek-flash', 'system', NOW(), 'system', NOW()),
(2, '阿里云百炼', 'openai', 'https://dashscope.aliyuncs.com/compatible-mode/v1', '', '',
 '[{"modelCode":"text-embedding-v4","displayName":"Text Embedding V4","modelType":"embedding","contextWindow":8192,"status":1},{"modelCode":"qwen3-rerank","displayName":"Qwen3 Rerank","modelType":"rerank","contextWindow":null,"status":1}]',
 1, '百炼 OpenAI 兼容：Embedding / Rerank；填写 API Key 后可用于知识库索引与重排序', 'system', NOW(), 'system', NOW());

-- ------------------------- MCP（国内天气：中国天气网，无需 API Key；需本机 Node.js + npx）-------------------------
INSERT INTO `agent_mcp` (`id`, `name`, `code`, `transport`, `command`, `args`, `url`, `env_json`, `tools_json`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '天气查询', 'weather', 'stdio', 'npx', '-y weather-china-mcp', '',
 '{}',
 '[{"name":"weather_current","description":"实时天气（温度、湿度、空气质量等）"},{"name":"weather_forecast","description":"5 天天气预报"},{"name":"weather_summary","description":"天气摘要（实时 + 今天 + 明天）"}]',
 1, '国内天气 MCP（中国天气网 weather.com.cn，无需 Key）；首次运行会经 npx 拉取包', 'system', NOW(), 'system', NOW());

-- ------------------------- Skill -------------------------
INSERT INTO `agent_skill` (`id`, `name`, `code`, `description`, `content`, `source_type`, `package_name`, `package_size`, `files_json`, `skill_path`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '写作润色', 'writing-polish', '中文写作润色与改写：通顺、简洁、语气得体',
 '---\nname: writing-polish\ndescription: 中文写作润色与改写\n---\n\n# 写作润色\n\n当用户需要润色、改写或提炼文案时使用本 Skill。\n\n## 原则\n\n1. 保留原意，不臆造事实\n2. 句子通顺简洁，避免套话\n3. 按用户指定语气调整（正式 / 轻松 / 对外客服等）\n4. 先给出改写结果，再可选说明改动要点\n',
 'manual', '', 0, NULL, '', 1, 'system', NOW(), 'system', NOW());

-- ------------------------- 知识库 + 文档 -------------------------
INSERT INTO `agent_knowledge` (`id`, `name`, `code`, `description`, `tags_json`, `embedding_provider_id`, `embedding_model`, `embedding_dim`, `chunk_strategy`, `chunk_size`, `chunk_overlap`, `top_k`, `score_threshold`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '产品常见问题', 'demo-faq', '演示知识库：默认阿里云 text-embedding-v4；填写百炼 API Key 并重建索引后检索生效',
 '["demo","faq"]', 2, 'text-embedding-v4', 0, 'section_recursive', 500, 50, 5, 0.3500, 1, 'system', NOW(), 'system', NOW());

INSERT INTO `agent_knowledge_doc` (`id`, `knowledge_id`, `title`, `source_type`, `file_name`, `file_type`, `file_size`, `content`, `url`, `status`, `chunk_count`, `index_progress`, `char_count`, `error_msg`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 1, '平台入门 FAQ', 'text', '', 'text', 0,
 '# Designer Agent 入门 FAQ\n\n## 智能体商店是什么？\n智能体商店展示已启用且已发布的智能体，点击卡片即可创建会话并对话。\n\n## 如何配置模型？\n演示已预置 DeepSeek（对话 deepseek-flash）与阿里云百炼（Embedding text-embedding-v4、Rerank qwen3-rerank）。分别填写对应 API Key 即可对话 / 建索引。\n\n## 天气助手怎么用？\n已预置国内天气 MCP（weather-china-mcp，数据源为中国天气网，无需额外 Key）。运行环境需安装 Node.js，首次调用会经 npx 自动拉取包。\n\n## 知识库如何生效？\n在知识库中上传或录入文档，配置 Embedding 模型并完成索引后，绑定到智能体即可在对话中检索。\n\n## 工作流有什么用？\n工作流可将 LLM、知识库、HTTP 等节点编排成可复用流程，并作为工具挂到智能体上调用。\n',
 '', 'ready', 0, 0, 420, '', 'system', NOW(), 'system', NOW());

-- ------------------------- 工作流（已发布）-------------------------
INSERT INTO `agent_workflow` (`id`, `name`, `code`, `description`, `nodes_json`, `edges_json`, `draft_nodes_json`, `draft_edges_json`, `version`, `draft_version`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '回声示例', 'demo-echo', '演示工作流：开始直接结束，用于验证挂载与调用链路',
 '[{"key":"start","type":"start","name":"开始","config":"","outputVar":"","x":160,"y":160},{"key":"end","type":"end","name":"结束","config":"","outputVar":"","x":480,"y":160}]',
 '[{"id":"e1","source":"start","target":"end","sourceHandle":null,"targetHandle":null,"label":""}]',
 '[{"key":"start","type":"start","name":"开始","config":"","outputVar":"","x":160,"y":160},{"key":"end","type":"end","name":"结束","config":"","outputVar":"","x":480,"y":160}]',
 '[{"id":"e1","source":"start","target":"end","sourceHandle":null,"targetHandle":null,"label":""}]',
 1, 1, 1, '演示工作流', 'system', NOW(), 'system', NOW());

INSERT INTO `agent_workflow_version` (`id`, `workflow_id`, `version`, `name`, `code`, `description`, `nodes_json`, `edges_json`, `status`, `remark`, `published_by`, `published_at`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 1, 1, '回声示例', 'demo-echo', '演示工作流：开始直接结束，用于验证挂载与调用链路',
 '[{"key":"start","type":"start","name":"开始","config":"","outputVar":"","x":160,"y":160},{"key":"end","type":"end","name":"结束","config":"","outputVar":"","x":480,"y":160}]',
 '[{"id":"e1","source":"start","target":"end","sourceHandle":null,"targetHandle":null,"label":""}]',
 1, '演示工作流', 'system', NOW(), 'system', NOW(), 'system', NOW());

-- ------------------------- 智能体（已发布，出现在商店）-------------------------
-- draft_json / config_json 对齐 AgentSaveDTO（camelCase）
INSERT INTO `agent_agent` (
  `id`, `name`, `code`, `avatar`, `system_prompt`, `provider_id`, `model_code`,
  `temperature`, `top_p`, `max_tokens`, `memory_rounds`,
  `tool_ids_json`, `mcp_ids_json`, `skill_ids_json`, `workflow_ids_json`, `knowledge_ids_json`,
  `embedding_provider_id`, `embedding_model`, `rerank_provider_id`, `rerank_model`,
  `rag_top_k`, `rag_score_threshold`, `file_enabled`, `status`, `remark`,
  `version`, `draft_version`, `draft_json`,
  `create_by`, `create_time`, `update_by`, `update_time`
) VALUES
(1, '通用助手', 'demo-assistant', '',
 '你是 Designer Agent 演示助手。回答简洁准确，不确定时如实说明。可用网页搜索补充公开信息。',
 1, 'deepseek-flash', 0.7000, 1.0000, 0, 10,
 '[1]', '[]', '[]', '[]', '[]',
 NULL, '', NULL, '', 5, 0.3500, 0, 1, '演示：通用对话 + 网页搜索工具',
 1, 1,
 '{"id":1,"name":"通用助手","code":"demo-assistant","avatar":"","systemPrompt":"你是 Designer Agent 演示助手。回答简洁准确，不确定时如实说明。可用网页搜索补充公开信息。","providerId":1,"modelCode":"deepseek-flash","temperature":0.7,"topP":1.0,"maxTokens":0,"memoryRounds":10,"toolIds":[1],"mcpIds":[],"skillIds":[],"workflowIds":[],"knowledgeIds":[],"embeddingProviderId":null,"embeddingModel":"","rerankProviderId":null,"rerankModel":"","ragTopK":5,"ragScoreThreshold":0.35,"fileEnabled":0,"status":1,"remark":"演示：通用对话 + 网页搜索工具"}',
 'system', NOW(), 'system', NOW()),
(2, '写作助手', 'demo-writer', '',
 '你是中文写作助手。优先使用写作润色 Skill：保留原意、通顺简洁，并按用户语气改写。',
 1, 'deepseek-flash', 0.8000, 1.0000, 0, 8,
 '[]', '[]', '[1]', '[]', '[]',
 NULL, '', NULL, '', 5, 0.3500, 0, 1, '演示：挂载写作润色 Skill',
 1, 1,
 '{"id":2,"name":"写作助手","code":"demo-writer","avatar":"","systemPrompt":"你是中文写作助手。优先使用写作润色 Skill：保留原意、通顺简洁，并按用户语气改写。","providerId":1,"modelCode":"deepseek-flash","temperature":0.8,"topP":1.0,"maxTokens":0,"memoryRounds":8,"toolIds":[],"mcpIds":[],"skillIds":[1],"workflowIds":[],"knowledgeIds":[],"embeddingProviderId":null,"embeddingModel":"","rerankProviderId":null,"rerankModel":"","ragTopK":5,"ragScoreThreshold":0.35,"fileEnabled":0,"status":1,"remark":"演示：挂载写作润色 Skill"}',
 'system', NOW(), 'system', NOW()),
(3, '知识问答', 'demo-rag', '',
 '你是产品知识助手。优先依据知识库检索结果作答；检索不足时说明并给出一般性建议，不要编造平台能力。',
 1, 'deepseek-flash', 0.4000, 1.0000, 0, 6,
 '[]', '[]', '[]', '[]', '[1]',
 2, 'text-embedding-v4', 2, 'qwen3-rerank', 5, 0.3500, 0, 1, '演示：绑定 FAQ + 阿里 Embedding/Rerank（需填写百炼 Key 并索引）',
 1, 1,
 '{"id":3,"name":"知识问答","code":"demo-rag","avatar":"","systemPrompt":"你是产品知识助手。优先依据知识库检索结果作答；检索不足时说明并给出一般性建议，不要编造平台能力。","providerId":1,"modelCode":"deepseek-flash","temperature":0.4,"topP":1.0,"maxTokens":0,"memoryRounds":6,"toolIds":[],"mcpIds":[],"skillIds":[],"workflowIds":[],"knowledgeIds":[1],"embeddingProviderId":2,"embeddingModel":"text-embedding-v4","rerankProviderId":2,"rerankModel":"qwen3-rerank","ragTopK":5,"ragScoreThreshold":0.35,"fileEnabled":0,"status":1,"remark":"演示：绑定 FAQ + 阿里 Embedding/Rerank（需填写百炼 Key 并索引）"}',
 'system', NOW(), 'system', NOW()),
(4, '流程助手', 'demo-flow', '',
 '你是流程演示助手。需要时可调用已挂载的回声示例工作流，向用户解释工作流挂载与调用方式。',
 1, 'deepseek-flash', 0.6000, 1.0000, 0, 8,
 '[]', '[]', '[]', '[1]', '[]',
 NULL, '', NULL, '', 5, 0.3500, 0, 1, '演示：挂载回声示例工作流',
 1, 1,
 '{"id":4,"name":"流程助手","code":"demo-flow","avatar":"","systemPrompt":"你是流程演示助手。需要时可调用已挂载的回声示例工作流，向用户解释工作流挂载与调用方式。","providerId":1,"modelCode":"deepseek-flash","temperature":0.6,"topP":1.0,"maxTokens":0,"memoryRounds":8,"toolIds":[],"mcpIds":[],"skillIds":[],"workflowIds":[1],"knowledgeIds":[],"embeddingProviderId":null,"embeddingModel":"","rerankProviderId":null,"rerankModel":"","ragTopK":5,"ragScoreThreshold":0.35,"fileEnabled":0,"status":1,"remark":"演示：挂载回声示例工作流"}',
 'system', NOW(), 'system', NOW()),
(5, '天气助手', 'demo-weather', '',
 '你是天气助手。查询气温、预报时优先调用天气 MCP 工具（中国天气网），用中文简洁回答，并注明地点与数据时间。',
 1, 'deepseek-flash', 0.5000, 1.0000, 0, 6,
 '[]', '[1]', '[]', '[]', '[]',
 NULL, '', NULL, '', 5, 0.3500, 0, 1, '演示：挂载国内天气 MCP（中国天气网，无需 Key）',
 1, 1,
 '{"id":5,"name":"天气助手","code":"demo-weather","avatar":"","systemPrompt":"你是天气助手。查询气温、预报时优先调用天气 MCP 工具（中国天气网），用中文简洁回答，并注明地点与数据时间。","providerId":1,"modelCode":"deepseek-flash","temperature":0.5,"topP":1.0,"maxTokens":0,"memoryRounds":6,"toolIds":[],"mcpIds":[1],"skillIds":[],"workflowIds":[],"knowledgeIds":[],"embeddingProviderId":null,"embeddingModel":"","rerankProviderId":null,"rerankModel":"","ragTopK":5,"ragScoreThreshold":0.35,"fileEnabled":0,"status":1,"remark":"演示：挂载国内天气 MCP（中国天气网，无需 Key）"}',
 'system', NOW(), 'system', NOW());

INSERT INTO `agent_agent_version` (`id`, `agent_id`, `version`, `config_json`, `name`, `code`, `status`, `remark`, `published_by`, `published_at`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 1, 1,
 '{"id":1,"name":"通用助手","code":"demo-assistant","avatar":"","systemPrompt":"你是 Designer Agent 演示助手。回答简洁准确，不确定时如实说明。可用网页搜索补充公开信息。","providerId":1,"modelCode":"deepseek-flash","temperature":0.7,"topP":1.0,"maxTokens":0,"memoryRounds":10,"toolIds":[1],"mcpIds":[],"skillIds":[],"workflowIds":[],"knowledgeIds":[],"embeddingProviderId":null,"embeddingModel":"","rerankProviderId":null,"rerankModel":"","ragTopK":5,"ragScoreThreshold":0.35,"fileEnabled":0,"status":1,"remark":"演示：通用对话 + 网页搜索工具"}',
 '通用助手', 'demo-assistant', 1, '演示：通用对话 + 网页搜索工具', 'system', NOW(), 'system', NOW(), 'system', NOW()),
(2, 2, 1,
 '{"id":2,"name":"写作助手","code":"demo-writer","avatar":"","systemPrompt":"你是中文写作助手。优先使用写作润色 Skill：保留原意、通顺简洁，并按用户语气改写。","providerId":1,"modelCode":"deepseek-flash","temperature":0.8,"topP":1.0,"maxTokens":0,"memoryRounds":8,"toolIds":[],"mcpIds":[],"skillIds":[1],"workflowIds":[],"knowledgeIds":[],"embeddingProviderId":null,"embeddingModel":"","rerankProviderId":null,"rerankModel":"","ragTopK":5,"ragScoreThreshold":0.35,"fileEnabled":0,"status":1,"remark":"演示：挂载写作润色 Skill"}',
 '写作助手', 'demo-writer', 1, '演示：挂载写作润色 Skill', 'system', NOW(), 'system', NOW(), 'system', NOW()),
(3, 3, 1,
 '{"id":3,"name":"知识问答","code":"demo-rag","avatar":"","systemPrompt":"你是产品知识助手。优先依据知识库检索结果作答；检索不足时说明并给出一般性建议，不要编造平台能力。","providerId":1,"modelCode":"deepseek-flash","temperature":0.4,"topP":1.0,"maxTokens":0,"memoryRounds":6,"toolIds":[],"mcpIds":[],"skillIds":[],"workflowIds":[],"knowledgeIds":[1],"embeddingProviderId":2,"embeddingModel":"text-embedding-v4","rerankProviderId":2,"rerankModel":"qwen3-rerank","ragTopK":5,"ragScoreThreshold":0.35,"fileEnabled":0,"status":1,"remark":"演示：绑定 FAQ + 阿里 Embedding/Rerank（需填写百炼 Key 并索引）"}',
 '知识问答', 'demo-rag', 1, '演示：绑定 FAQ + 阿里 Embedding/Rerank（需填写百炼 Key 并索引）', 'system', NOW(), 'system', NOW(), 'system', NOW()),
(4, 4, 1,
 '{"id":4,"name":"流程助手","code":"demo-flow","avatar":"","systemPrompt":"你是流程演示助手。需要时可调用已挂载的回声示例工作流，向用户解释工作流挂载与调用方式。","providerId":1,"modelCode":"deepseek-flash","temperature":0.6,"topP":1.0,"maxTokens":0,"memoryRounds":8,"toolIds":[],"mcpIds":[],"skillIds":[],"workflowIds":[1],"knowledgeIds":[],"embeddingProviderId":null,"embeddingModel":"","rerankProviderId":null,"rerankModel":"","ragTopK":5,"ragScoreThreshold":0.35,"fileEnabled":0,"status":1,"remark":"演示：挂载回声示例工作流"}',
 '流程助手', 'demo-flow', 1, '演示：挂载回声示例工作流', 'system', NOW(), 'system', NOW(), 'system', NOW()),
(5, 5, 1,
 '{"id":5,"name":"天气助手","code":"demo-weather","avatar":"","systemPrompt":"你是天气助手。查询气温、预报时优先调用天气 MCP 工具（中国天气网），用中文简洁回答，并注明地点与数据时间。","providerId":1,"modelCode":"deepseek-flash","temperature":0.5,"topP":1.0,"maxTokens":0,"memoryRounds":6,"toolIds":[],"mcpIds":[1],"skillIds":[],"workflowIds":[],"knowledgeIds":[],"embeddingProviderId":null,"embeddingModel":"","rerankProviderId":null,"rerankModel":"","ragTopK":5,"ragScoreThreshold":0.35,"fileEnabled":0,"status":1,"remark":"演示：挂载国内天气 MCP（中国天气网，无需 Key）"}',
 '天气助手', 'demo-weather', 1, '演示：挂载国内天气 MCP（中国天气网，无需 Key）', 'system', NOW(), 'system', NOW(), 'system', NOW());
