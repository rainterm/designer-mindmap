# designer-module-mindmap

思维导图业务模块：导图项目 CRUD，内容以 JSON 落库。

管理端内嵌：`designer-ui/admin-web/src/modules/mindmap`（列表进 Layout，编辑页 standalone 全屏）。

独立前端仍保留：`designer-ui/mindmap-web`（SSO 接入，可单独部署）。

分类：`mm_category`（`parent_id` 树形，最多 5 层）+ `mm_project.category_id`，接口前缀 `/admin-api/mindmap/category`。
按父分类筛选时会包含其全部子分类下的导图。
