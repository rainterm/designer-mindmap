# SSO Demo · 单点登录接入示例

一个最小可运行的 **SSO 客户端**，演示如何接入 Designer Admin 的跨应用单点登录（票据换 token）。
接口与时序说明见 [../../docs/SSO.md](../../docs/SSO.md)。

> 仅用于本地演示，不参与 `deploy/` 生产构建。

## 前置

1. 脚手架后端已启动（`http://localhost:8080`）
2. 管理后台 `designer-ui/admin-web` 已启动（`http://localhost:5173`）

## 启动

```bash
cd designer-ui/sso-demo
pnpm install
pnpm run dev      # http://localhost:5178
```

开发期通过 Vite 代理把 `/admin-api` 转发到 `http://localhost:8080`，保持同源（后端未开启 CORS）。

## 后台配置

### 1. SSO 客户端

初始化数据已内置该客户端（`client_key=sso-demo`、`origin=http://localhost:5178`、`entry_path=/sso`），**无需手动新增**。若换了端口/域名或未跑初始化脚本，再按「系统管理 → SSO 客户端」补字段：

| 字段 | 值 |
|---|---|
| 客户端名称 | SSO Demo |
| 客户端标识 | `sso-demo`（小写、全局唯一） |
| 允许 Origin | `http://localhost:5178` |
| 打开地址 | `http://localhost:5178` |
| 接入路径 | `/sso` |
| 状态 | 启用 |

### 2. 新增外部菜单（可选，用于从后台侧栏打开）

「系统管理 → 菜单管理 → 新增」，类型选“菜单”，**组件**填 `External:sso-demo`（`External:` 后的部分即客户端标识），保存后侧栏出现该菜单，点击即调用 `openSsoClient('sso-demo')` 打开本应用并自动带票据登录。

## 两种进入方式

- **从后台进入（推荐）**：点击上面的外部菜单 → 后台调 `sso/ticket` 签发一次性票据 → 打开 `http://localhost:5178/sso?redirect=/&ticket=xxx` → 本应用换取 token。
- **客户端主动发起**：直接访问 `http://localhost:5178`，点「去后台登录（SSO）」→ 跳后台登录页 → 登录后后台按白名单签发票据回跳本应用 `/sso`。

## 目录结构

```
sso-demo/
├── vite.config.js          # 5178 端口 + /admin-api 代理（同源）
├── .env                    # VITE_ADMIN_LOGIN_URL 等
└── src/
    ├── settings.js         # apiBase / adminLoginUrl
    ├── api/
    │   ├── request.js      # axios 实例：Authorization 放 token 原值；统一 R 处理
    │   └── auth.js         # exchangeSsoTicket / getUserInfo
    ├── utils/auth.js       # 本地 token 读写
    ├── router/index.js     # / 首页、/sso 接入入口、/sso-error
    └── views/
        ├── Home.vue        # 展示登录态与用户信息；未登录时跳后台
        ├── Sso.vue         # 读 ?ticket → exchange → 存 token → 跳 redirect
        └── SsoError.vue
```

## 要点

- **同源**：客户端必须通过代理 / 反代访问 `/admin-api`，否则浏览器跨域会被拦（后端未开 CORS）。
- **token 用法**：请求头 `Authorization` 放 token 原值，**无 `Bearer` 前缀**。
- **票据一次性**：`/sso/exchange` 免登录，但每个票据只能用一次，有效期默认 60s。
- **接入路径一致**：本应用 `/sso` 路由须与后台 SSO 客户端的 `entry_path` 一致。
