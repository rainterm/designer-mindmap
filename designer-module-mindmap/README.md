# designer-module-mindmap

思维导图业务模块：导图项目 CRUD，内容以 JSON 落库。

独立前端：`designer-ui/mindmap-web`（与管理端共用 `/admin-api` + Sa-Token SSO）。

菜单 `component=External:mindmap` 时，管理端侧栏签发一次性票据并打开 mindmap-web。

分类：`mm_category`（`parent_id` 树形，最多 5 层）+ `mm_project.category_id`，接口前缀 `/admin-api/mindmap/category`。
按父分类筛选时会包含其全部子分类下的导图。
