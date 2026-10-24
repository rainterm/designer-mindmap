-- =====================================================================
--  system/data.sql
--  默认账号：admin / admin123       guest / guest123
-- =====================================================================

SET NAMES utf8mb4;
USE `designer_mindmap`;

-- ------------------------- 角色 -------------------------
-- is_super=1 表示超级管理员；role_type=1=系统内置（不可修改/删除） 2=自定义
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `sort`, `status`, `is_super`, `role_type`, `data_scope`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '超级管理员', 'admin',  1, 1, 1, 1, 1, '拥有系统全部权限（内置超级管理员）', 'system', NOW(), 'system', NOW()),
(2, '普通用户',   'common', 2, 1, 0, 2, 3, '仅可查看本部门数据，示例角色',          'system', NOW(), 'system', NOW());

-- ------------------------- 部门 -------------------------
INSERT INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(100, 0,   '0',       '总公司', 0, 1, 'system', NOW(), 'system', NOW()),
(101, 100, '0,100',   '研发部门', 1, 1, 'system', NOW(), 'system', NOW()),
(102, 100, '0,100',   '市场部门', 2, 1, 'system', NOW(), 'system', NOW()),
(103, 101, '0,100,101','前端组', 1, 1, 'system', NOW(), 'system', NOW()),
(104, 101, '0,100,101','后端组', 2, 1, 'system', NOW(), 'system', NOW());

-- ------------------------- 用户 -------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `dept_id`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 'admin', '$2a$10$olhl9fc0yfoyXZSrVwL.6.WT1RIFJWBY3vi23ij.lykHkOXa46fRS', '超级管理员', 'admin@example.local', '13800000001', 100, 1, '内置超级管理员账号', 'system', NOW(), 'system', NOW()),
(2, 'guest', '$2a$10$73py8/I2Vo1/nCefZA/MiOgtmn57P/oJ38temCDyb6gAu3M9jkNXu', '访客',   'guest@example.local', '13800000002', 101, 1, '演示用只读账号',       'system', NOW(), 'system', NOW());

-- ------------------------- 参数配置 -------------------------
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '账号登录验证码', 'sys.account.captchaEnabled', 'true', 'Y', '是否开启登录验证码（滑块拼图；开启后仍需连续失败达到阈值才显示）', 'system', NOW(), 'system', NOW()),
(2, '账号登录失败锁定', 'sys.account.lockEnabled', 'true', 'Y', '是否开启登录失败锁定（按账号 + IP 组合锁定，不会影响该账号从其它 IP 登录）', 'system', NOW(), 'system', NOW()),
(3, '登录失败最大次数', 'sys.account.lockMaxRetry', '5', 'Y', '同一账号在同一 IP 下连续失败达到次数后锁定该组合', 'system', NOW(), 'system', NOW()),
(4, '登录锁定时长(分钟)', 'sys.account.lockMinutes', '10', 'Y', '账号 + IP 组合的锁定时长，单位分钟', 'system', NOW(), 'system', NOW()),
(6, 'Token绝对有效期(秒)', 'sys.account.tokenTimeout', '86400', 'Y', 'Token 最长存活时间，到期必须重新登录；默认 86400=1天，最小 60', 'system', NOW(), 'system', NOW()),
(7, 'Token活跃续期(秒)', 'sys.account.tokenActiveTimeout', '1800', 'Y', '有请求则自动续期；连续闲置超过该秒数踢出；默认 1800=30分钟，-1 关闭', 'system', NOW(), 'system', NOW()),
(8, '验证码失败阈值', 'sys.account.captchaFailThreshold', '3', 'Y', '同一账号连续登录失败达到该次数后才要求滑块验证（跨 IP 累计，但只影响是否出验证码、不锁定账号），默认 3', 'system', NOW(), 'system', NOW()),
(9, 'IP登录失败锁定阈值', 'sys.account.lockIpMaxRetry', '30', 'Y', '同一 IP 在统计窗口内累计失败达到该次数后临时锁定该 IP，用于拦截撞库 / 密码喷洒；默认 30', 'system', NOW(), 'system', NOW()),
(10, 'IP锁定时长(分钟)', 'sys.account.lockIpMinutes', '10', 'Y', 'IP 维度锁定时长，单位分钟；默认 10', 'system', NOW(), 'system', NOW());

-- ------------------------- 系统设置（单行）-------------------------
-- 系统名称 / 登录页副标题 / Logo / 默认偏好设置；由「系统管理 → 系统设置」菜单维护
INSERT INTO `sys_setting` (`id`, `system_name`, `login_subtitle`, `logo_url`, `logo_radius`, `default_layout_mode`, `default_primary_color`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 'Designer Admin', '统一管理后台，让协作更高效', '', 6, 'mixed-vertical', '#16A34A', 'system', NOW(), 'system', NOW());

-- ------------------------- 菜单（目录 / 菜单）-------------------------
-- 按大模块划分：系统模块（内含二级目录）/ 示例模块(9000)
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
-- 大模块：系统
(6,  0, '系统模块', 'M', '/sys',      '', '', 'grid',     1, 1, 1, 'system', NOW(), 'system', NOW()),
-- 系统模块下二级目录
(1,  6, '系统管理', 'M', '/system',   '', '', 'settings', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(2,  6, '系统监控', 'M', '/monitor',  '', '', 'pulse',    2, 1, 1, 'system', NOW(), 'system', NOW()),
(3,  6, '文件管理', 'M', '/oss',      '', '', 'folder',   3, 1, 1, 'system', NOW(), 'system', NOW()),
(4,  6, '消息中心', 'M', '/notify',   '', '', 'mail',     4, 1, 1, 'system', NOW(), 'system', NOW()),
(5,  6, '定时任务', 'M', '/schedule', '', '', 'timer',    5, 1, 1, 'system', NOW(), 'system', NOW()),
-- 系统管理
(10, 1, '用户管理', 'C', '/system/user',      'system/user/index',    'system:user:list',     'person',      1, 1, 1, 'system', NOW(), 'system', NOW()),
(22, 1, '部门管理', 'C', '/system/dept',      'system/dept/index',    'system:dept:list',     'git-network', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(25, 1, '岗位管理', 'C', '/system/post',      'system/post/index',    'system:post:list',     'briefcase',   3, 1, 1, 'system', NOW(), 'system', NOW()),
(11, 1, '角色管理', 'C', '/system/role',      'system/role/index',    'system:role:list',     'people',      4, 1, 1, 'system', NOW(), 'system', NOW()),
(12, 1, '菜单管理', 'C', '/system/menu',      'system/menu/index',    'system:menu:list',     'menu',        5, 1, 1, 'system', NOW(), 'system', NOW()),
(13, 1, '字典管理', 'C', '/system/dict',      'system/dict/index',    'system:dict:list',     'book',        6, 1, 1, 'system', NOW(), 'system', NOW()),
(23, 1, '参数设置', 'C', '/system/config',    'system/config/index',  'system:config:list',   'options',     7, 1, 1, 'system', NOW(), 'system', NOW()),
(26, 1, 'SSO客户端', 'C', '/system/ssoclient', 'system/ssoclient/index','system:ssoclient:list','link',       8, 1, 1, 'system', NOW(), 'system', NOW()),
(28, 1, 'OAuth2 客户端', 'C', '/system/oauthclient', 'system/oauthclient/index','system:oauthclient:list','key', 9, 1, 1, 'system', NOW(), 'system', NOW()),
(27, 1, '系统设置', 'C', '/system/setting', 'system/setting/index', 'system:setting:query', 'color', 10, 1, 1, 'system', NOW(), 'system', NOW()),
-- 系统监控
(24, 2, '在线用户', 'C', '/system/online',    'system/online/index',  'system:online:list',   'pulse',       1, 1, 1, 'system', NOW(), 'system', NOW()),
(14, 2, '操作日志', 'C', '/system/operlog',   'system/operlog/index', 'system:operlog:list',  'history',     2, 1, 1, 'system', NOW(), 'system', NOW()),
(15, 2, '登录日志', 'C', '/system/loginlog',  'system/loginlog/index','system:loginlog:list', 'log-in',      3, 1, 1, 'system', NOW(), 'system', NOW()),
-- 文件管理
(16, 3, '文件列表', 'C', '/system/file',      'system/file/index',    'system:file:list',     'folder',      1, 1, 1, 'system', NOW(), 'system', NOW()),
(17, 3, '存储配置', 'C', '/system/storage',   'system/storage/index', 'system:storage:list',  'cloud',       2, 1, 1, 'system', NOW(), 'system', NOW()),
-- 消息中心
(18, 4, '通道配置', 'C', '/system/message',   'system/message/index', 'system:message:list',  'mail',        1, 1, 1, 'system', NOW(), 'system', NOW()),
(19, 4, '消息记录', 'C', '/system/msgrecord', 'system/msgrecord/index','system:msgrecord:list','list',       2, 1, 1, 'system', NOW(), 'system', NOW()),
-- 定时任务
(20, 5, '任务管理', 'C', '/system/job',       'system/job/index',     'system:job:list',      'timer',       1, 1, 1, 'system', NOW(), 'system', NOW()),
(21, 5, '调度日志', 'C', '/system/joblog',    'system/joblog/index',  'system:joblog:list',   'list',        2, 1, 1, 'system', NOW(), 'system', NOW());

-- ------------------------- 菜单（按钮权限）-------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(100, 10, '用户查询', 'F', '', '', 'system:user:query',    '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(101, 10, '用户新增', 'F', '', '', 'system:user:add',      '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(102, 10, '用户修改', 'F', '', '', 'system:user:edit',     '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(103, 10, '用户删除', 'F', '', '', 'system:user:remove',   '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
(104, 10, '重置密码', 'F', '', '', 'system:user:resetPwd', '', 5, 1, 1, 'system', NOW(), 'system', NOW()),
(105, 10, '用户导出', 'F', '', '', 'system:user:export',   '', 6, 1, 1, 'system', NOW(), 'system', NOW()),
(106, 10, '用户导入', 'F', '', '', 'system:user:import',   '', 7, 1, 1, 'system', NOW(), 'system', NOW()),
(110, 11, '角色查询', 'F', '', '', 'system:role:query',    '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(111, 11, '角色新增', 'F', '', '', 'system:role:add',      '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(112, 11, '角色修改', 'F', '', '', 'system:role:edit',     '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(113, 11, '角色删除', 'F', '', '', 'system:role:remove',   '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
(120, 12, '菜单查询', 'F', '', '', 'system:menu:query',    '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(121, 12, '菜单新增', 'F', '', '', 'system:menu:add',      '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(122, 12, '菜单修改', 'F', '', '', 'system:menu:edit',     '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(123, 12, '菜单删除', 'F', '', '', 'system:menu:remove',   '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
(130, 13, '字典查询', 'F', '', '', 'system:dict:query',    '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(131, 13, '字典新增', 'F', '', '', 'system:dict:add',      '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(132, 13, '字典修改', 'F', '', '', 'system:dict:edit',     '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(133, 13, '字典删除', 'F', '', '', 'system:dict:remove',   '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
(134, 13, '字典导出', 'F', '', '', 'system:dict:export',   '', 5, 1, 1, 'system', NOW(), 'system', NOW()),
(135, 13, '字典导入', 'F', '', '', 'system:dict:import',   '', 6, 1, 1, 'system', NOW(), 'system', NOW()),
(160, 16, '文件查询', 'F', '', '', 'system:file:query',    '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(161, 16, '文件上传', 'F', '', '', 'system:file:upload',   '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(162, 16, '文件删除', 'F', '', '', 'system:file:remove',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(170, 17, '配置查询', 'F', '', '', 'system:storage:query', '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(171, 17, '配置新增', 'F', '', '', 'system:storage:add',   '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(172, 17, '配置修改', 'F', '', '', 'system:storage:edit',  '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(173, 17, '配置删除', 'F', '', '', 'system:storage:remove','', 4, 1, 1, 'system', NOW(), 'system', NOW()),
(180, 18, '通道查询', 'F', '', '', 'system:message:query', '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(181, 18, '通道新增', 'F', '', '', 'system:message:add',   '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(182, 18, '通道修改', 'F', '', '', 'system:message:edit',  '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(183, 18, '通道删除', 'F', '', '', 'system:message:remove','', 4, 1, 1, 'system', NOW(), 'system', NOW()),
(184, 18, '发送测试', 'F', '', '', 'system:message:send',  '', 5, 1, 1, 'system', NOW(), 'system', NOW()),
(190, 19, '记录删除', 'F', '', '', 'system:msgrecord:remove','',1, 1, 1, 'system', NOW(), 'system', NOW()),
(191, 19, '发送消息', 'F', '', '', 'system:msgrecord:send',  '',2, 1, 1, 'system', NOW(), 'system', NOW()),
(200, 20, '任务查询', 'F', '', '', 'system:job:query',       '',1, 1, 1, 'system', NOW(), 'system', NOW()),
(201, 20, '任务新增', 'F', '', '', 'system:job:add',         '',2, 1, 1, 'system', NOW(), 'system', NOW()),
(202, 20, '任务修改', 'F', '', '', 'system:job:edit',        '',3, 1, 1, 'system', NOW(), 'system', NOW()),
(203, 20, '任务删除', 'F', '', '', 'system:job:remove',      '',4, 1, 1, 'system', NOW(), 'system', NOW()),
(204, 20, '任务执行', 'F', '', '', 'system:job:run',         '',5, 1, 1, 'system', NOW(), 'system', NOW()),
(210, 21, '日志删除', 'F', '', '', 'system:joblog:remove',   '',1, 1, 1, 'system', NOW(), 'system', NOW()),
(220, 22, '部门查询', 'F', '', '', 'system:dept:query',     '',1, 1, 1, 'system', NOW(), 'system', NOW()),
(221, 22, '部门新增', 'F', '', '', 'system:dept:add',       '',2, 1, 1, 'system', NOW(), 'system', NOW()),
(222, 22, '部门修改', 'F', '', '', 'system:dept:edit',      '',3, 1, 1, 'system', NOW(), 'system', NOW()),
(223, 22, '部门删除', 'F', '', '', 'system:dept:remove',    '',4, 1, 1, 'system', NOW(), 'system', NOW()),
(250, 25, '岗位查询', 'F', '', '', 'system:post:query',     '',1, 1, 1, 'system', NOW(), 'system', NOW()),
(251, 25, '岗位新增', 'F', '', '', 'system:post:add',       '',2, 1, 1, 'system', NOW(), 'system', NOW()),
(252, 25, '岗位修改', 'F', '', '', 'system:post:edit',      '',3, 1, 1, 'system', NOW(), 'system', NOW()),
(253, 25, '岗位删除', 'F', '', '', 'system:post:remove',    '',4, 1, 1, 'system', NOW(), 'system', NOW()),
(230, 23, '参数查询', 'F', '', '', 'system:config:query',   '',1, 1, 1, 'system', NOW(), 'system', NOW()),
(231, 23, '参数新增', 'F', '', '', 'system:config:add',     '',2, 1, 1, 'system', NOW(), 'system', NOW()),
(232, 23, '参数修改', 'F', '', '', 'system:config:edit',    '',3, 1, 1, 'system', NOW(), 'system', NOW()),
(233, 23, '参数删除', 'F', '', '', 'system:config:remove',  '',4, 1, 1, 'system', NOW(), 'system', NOW()),
(240, 24, '在线查询', 'F', '', '', 'system:online:query',   '',1, 1, 1, 'system', NOW(), 'system', NOW()),
(241, 24, '强退用户', 'F', '', '', 'system:online:forceLogout', '',2, 1, 1, 'system', NOW(), 'system', NOW()),
(260, 26, 'SSO查询', 'F', '', '', 'system:ssoclient:query',  '',1, 1, 1, 'system', NOW(), 'system', NOW()),
(261, 26, 'SSO新增', 'F', '', '', 'system:ssoclient:add',    '',2, 1, 1, 'system', NOW(), 'system', NOW()),
(262, 26, 'SSO修改', 'F', '', '', 'system:ssoclient:edit',   '',3, 1, 1, 'system', NOW(), 'system', NOW()),
(263, 26, 'SSO删除', 'F', '', '', 'system:ssoclient:remove', '',4, 1, 1, 'system', NOW(), 'system', NOW()),
(280, 28, 'OAuth2 查询', 'F', '', '', 'system:oauthclient:query',  '',1, 1, 1, 'system', NOW(), 'system', NOW()),
(281, 28, 'OAuth2 新增', 'F', '', '', 'system:oauthclient:add',    '',2, 1, 1, 'system', NOW(), 'system', NOW()),
(282, 28, 'OAuth2 修改', 'F', '', '', 'system:oauthclient:edit',   '',3, 1, 1, 'system', NOW(), 'system', NOW()),
(283, 28, 'OAuth2 删除', 'F', '', '', 'system:oauthclient:remove', '',4, 1, 1, 'system', NOW(), 'system', NOW()),
(284, 28, '重置密钥', 'F', '', '', 'system:oauthclient:resetSecret', '',5, 1, 1, 'system', NOW(), 'system', NOW()),
(290, 27, '设置查询', 'F', '', '', 'system:setting:query', '',1, 1, 1, 'system', NOW(), 'system', NOW()),
(291, 27, '设置修改', 'F', '', '', 'system:setting:edit', '',2, 1, 1, 'system', NOW(), 'system', NOW());

-- ------------------------- OAuth2 开放 Scope（示例）-------------------------
INSERT INTO `sys_oauth_scope` (`id`, `scope_code`, `scope_name`, `path_pattern`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 'open.ping', '连通性探测', '/open-api/system/ping', 1, 1, '第三方联调示例接口', 'system', NOW(), 'system', NOW()),
(2, 'open.agent', '智能体查询', '/open-api/agent/agent/**', 2, 1, '已发布智能体列表与详情', 'system', NOW(), 'system', NOW()),
(3, 'open.agent.chat', '智能体对话', '/open-api/agent/session/**', 3, 1, '智能体会话与消息（含 SSE）', 'system', NOW(), 'system', NOW()),
(4, 'open.workflow', '工作流调用', '/open-api/agent/workflow/**', 4, 1, '工作流列表、详情与执行（含 SSE）', 'system', NOW(), 'system', NOW());

-- ------------------------- SSO 客户端（mindmap-web :5176；sso-demo :5178）-------------------------
INSERT INTO `sys_sso_client` (`id`, `client_name`, `client_key`, `origin`, `open_url`, `entry_path`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '思维导图', 'mindmap', 'http://localhost:5176', 'http://localhost:5176', '/sso', 1, 1, '独立导图前端（designer-ui/mindmap-web）；生产请改成实际域名', 'system', NOW(), 'system', NOW()),
(2, '思维导图(127)', 'mindmap127', 'http://127.0.0.1:5176', 'http://127.0.0.1:5176', '/sso', 2, 1, '本机 127.0.0.1 回跳白名单', 'system', NOW(), 'system', NOW()),
(3, 'SSO Demo', 'sso-demo', 'http://localhost:5178', 'http://localhost:5178', '/sso', 3, 1, '本地 SSO 接入示例（designer-ui/sso-demo）', 'system', NOW(), 'system', NOW());

-- ------------------------- 岗位（全局，部门通过用户关联）-------------------------
INSERT INTO `sys_post` (`id`, `post_name`, `post_code`, `post_level`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '系长', 'XI_ZHANG', 10, 1, 1, '', 'system', NOW(), 'system', NOW()),
(2, '科长', 'KE_ZHANG', 20, 2, 1, '', 'system', NOW(), 'system', NOW()),
(3, '部长', 'BU_ZHANG', 30, 3, 1, '', 'system', NOW(), 'system', NOW());

-- ------------------------- 用户 - 角色 -------------------------
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1), (2, 2);

-- ------------------------- 角色 - 菜单（仅普通角色；超管靠 is_super 拥有全部权限）-------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 6),
(2, 1),
(2, 10), (2, 100),
(2, 22), (2, 220),
(2, 25), (2, 250),
(2, 11), (2, 110),
(2, 12), (2, 120),
(2, 13), (2, 130),
(2, 23), (2, 230),
(2, 27), (2, 290),
(2, 2),
(2, 24), (2, 240),
(2, 14), (2, 15),
(2, 3),
(2, 16), (2, 160),
(2, 17), (2, 170),
(2, 4),
(2, 18), (2, 180),
(2, 19),
(2, 5),
(2, 20), (2, 200),
(2, 21);

-- ------------------------- 字典 -------------------------
-- 前端不再硬编码任何「码 → 文案」：下拉项、列表标签统一由 useDict 读取。
-- 新增一种状态只需插一条 sys_dict_data，页面代码不用改。
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '通用状态',       'sys_normal_disable',      1, '系统模块 status：正常 / 停用', 'system', NOW(), 'system', NOW()),
(2, '启用状态',       'sys_enable_disable',      1, '业务配置 status：启用 / 停用', 'system', NOW(), 'system', NOW()),
(3, '执行结果',       'sys_oper_result',         1, '日志与消息记录的 status：成功 / 失败', 'system', NOW(), 'system', NOW()),
(4, '任务状态',       'sys_job_status',          1, '定时任务 status：正常 / 暂停', 'system', NOW(), 'system', NOW()),
(5, '任务失火策略',   'sys_job_misfire_policy',  1, '定时任务 misfire_policy：默认 / 立即 / 放弃', 'system', NOW(), 'system', NOW()),
(6, '数据权限范围',   'sys_data_scope',          1, '角色 data_scope：1~5', 'system', NOW(), 'system', NOW()),
(7, '菜单类型',       'sys_menu_type',           1, '菜单 menu_type：目录 / 菜单 / 按钮', 'system', NOW(), 'system', NOW()),
(8, '存储访问类型',   'sys_storage_access',      1, '存储配置 is_public：公开 / 私有', 'system', NOW(), 'system', NOW());

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort`, `is_default`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
('sys_normal_disable', '正常', '1', 1, 1, 1, '', 'system', NOW(), 'system', NOW()),
('sys_normal_disable', '停用', '0', 2, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_enable_disable', '启用', '1', 1, 1, 1, '', 'system', NOW(), 'system', NOW()),
('sys_enable_disable', '停用', '0', 2, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_oper_result',    '成功', '1', 1, 1, 1, '', 'system', NOW(), 'system', NOW()),
('sys_oper_result',    '失败', '0', 2, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_job_status',     '正常', '1', 1, 1, 1, '', 'system', NOW(), 'system', NOW()),
('sys_job_status',     '暂停', '0', 2, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_job_misfire_policy', '默认策略', '1', 1, 1, 1, '', 'system', NOW(), 'system', NOW()),
('sys_job_misfire_policy', '立即执行', '2', 2, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_job_misfire_policy', '放弃执行', '3', 3, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_data_scope',     '全部数据',     '1', 1, 1, 1, '', 'system', NOW(), 'system', NOW()),
('sys_data_scope',     '自定义部门',   '2', 2, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_data_scope',     '本部门',       '3', 3, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_data_scope',     '本部门及以下', '4', 4, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_data_scope',     '仅本人',       '5', 5, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_menu_type',      '目录', 'M', 1, 1, 1, '', 'system', NOW(), 'system', NOW()),
('sys_menu_type',      '菜单', 'C', 2, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_menu_type',      '按钮', 'F', 3, 0, 1, '', 'system', NOW(), 'system', NOW()),
('sys_storage_access', '公开', '1', 1, 1, 1, '', 'system', NOW(), 'system', NOW()),
('sys_storage_access', '私有', '0', 2, 0, 1, '', 'system', NOW(), 'system', NOW());

-- ------------------------- 默认本地存储配置 -------------------------
INSERT INTO `sys_storage_config` (`id`, `config_name`, `storage_type`, `is_default`, `status`, `is_public`, `url_prefix`, `allowed_extensions`, `max_size`, `compress_enabled`, `compress_quality`, `config_json`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '本地默认', 'local', 1, 1, 1, '/app-api/system/file/download/',
 'jpg,jpeg,png,gif,webp,bmp,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,txt,md,markdown',
 5242880,
 0, 80,
 '{"localPath":"./data/uploads"}',
 '系统内置本地存储（公开，可匿名访问）；压缩默认关闭，可在编辑里开启', 'system', NOW(), 'system', NOW());

-- ------------------------- 默认站内通知通道（系统接收）-------------------------
INSERT INTO `sys_message_config` (`id`, `config_name`, `channel_type`, `is_default`, `status`, `config_json`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '站内通知', 'inbox', 1, 1, '{}',
 '内置默认：消息进入顶部消息中心，由登录用户接收', 'system', NOW(), 'system', NOW());

-- ------------------------- 示例 / 内置定时任务（默认暂停，可在页面启用/执行）-------------------------
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '示例无参任务', 'DEFAULT', 'demoTask.noParams', '0 0/10 * * * ?', 1, 1, 0, '演示任务：每 10 分钟执行一次（默认暂停）', 'system', NOW(), 'system', NOW()),
(2, '示例有参任务', 'DEFAULT', 'demoTask.withParams(''hello'')', '0 0 2 * * ?', 1, 0, 0, '演示任务：每天 2 点执行（默认暂停）', 'system', NOW(), 'system', NOW()),
(3, '清理系统日志', 'SYSTEM', 'cleanLogTask.cleanAll(30)', '0 0 3 * * ?', 1, 0, 0, '内置：每天 3 点清理 30 天前的操作/登录/调度日志与消息记录（默认暂停）', 'system', NOW(), 'system', NOW());
