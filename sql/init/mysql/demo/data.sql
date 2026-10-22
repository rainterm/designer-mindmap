-- =====================================================================
--  demo/data.sql（菜单 ID 9000 段）
--  component=demo/index → modules/demo/views/index.vue
-- =====================================================================

SET NAMES utf8mb4;
USE `designer_agent`;

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(9000, 0,    '示例模块', 'M', '/demo',       '',            '',          'cube', 90, 1, 1, 'system', NOW(), 'system', NOW()),
(9001, 9000, 'Demo 页',  'C', '/demo/index', 'demo/index',  'demo:ping', 'cube',  1, 1, 1, 'system', NOW(), 'system', NOW()),
(9010, 9001, 'Demo 探测', 'F', '',            '',            'demo:ping', '',     1, 1, 1, 'system', NOW(), 'system', NOW());
