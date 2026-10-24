-- =====================================================================
--  mindmap/data.sql（菜单 ID 10000 段）
--  导图项目为 External:mindmap：管理端侧栏 SSO 打开 mindmap-web（:5176）
-- =====================================================================

SET NAMES utf8mb4;
USE `designer_mindmap`;

-- ------------------------- 菜单 / 按钮权限 -------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(10000, 0,     '思维导图', 'M', '/mindmap',         '',                 '',                       'git-network', 20, 1, 1, 'system', NOW(), 'system', NOW()),
(10001, 10000, '导图项目', 'C', '/mindmap/project', 'External:mindmap', 'mindmap:project:list',   'git-network',  1, 1, 1, 'system', NOW(), 'system', NOW()),
(10010, 10001, '导图查询', 'F', '',                 '',                 'mindmap:project:query',  '',            1, 1, 1, 'system', NOW(), 'system', NOW()),
(10011, 10001, '导图新增', 'F', '',                 '',                 'mindmap:project:add',    '',            2, 1, 1, 'system', NOW(), 'system', NOW()),
(10012, 10001, '导图修改', 'F', '',                 '',                 'mindmap:project:edit',   '',            3, 1, 1, 'system', NOW(), 'system', NOW()),
(10013, 10001, '导图删除', 'F', '',                 '',                 'mindmap:project:remove', '',            4, 1, 1, 'system', NOW(), 'system', NOW());

-- ------------------------- 默认分类 -------------------------
INSERT INTO `mm_category` (`id`, `parent_id`, `name`, `sort`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 0, '学习', 1, 'system', NOW(), 'system', NOW()),
(2, 0, '工作', 2, 'system', NOW(), 'system', NOW()),
(3, 0, '生活', 3, 'system', NOW(), 'system', NOW()),
(4, 1, '读书笔记', 1, 'system', NOW(), 'system', NOW()),
(5, 1, '课程学习', 2, 'system', NOW(), 'system', NOW());
