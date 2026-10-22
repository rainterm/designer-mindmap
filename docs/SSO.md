# 单点登录（SSO）接入

本脚手架支持跨应用单点登录：**已登录的后台签发一个一次性票据 → 客户端拿票据换回同一个登录态（token）**，从而免二次登录。

> 可运行案例代码：[`designer-ui/sso-demo`](../designer-ui/sso-demo)（Vue 3 + Vite 客户端，可直接跑）。

## 流程

以"用户访问客户端页面"为起点：

1. 用户访问客户端页面（如 `http://localhost:5176`），客户端发现未登录 → 跳到后台登录页（完整地址 `http://localhost:5173/login?redirect=http://localhost:5176/sso`）
2. 在后台登录（若已登录则直接跳过，无需再输密码）
3. 后台签发一次性票据，跳回客户端：`http://localhost:5176/sso?redirect=/&ticket=xxx`
4. 客户端 `/sso` 页面读取 `ticket`，调 `POST /admin-api/system/auth/sso/exchange` 换回 token
5. 客户端保存 token，进入页面（免二次登录）

票据存 Redis，**一次性**、默认 60s 过期。若从后台菜单直接打开客户端，则从第 3 步开始。

## 接入三步

### 1. 后台配一个 SSO 客户端

「系统管理 → SSO 客户端 → 新增」：

![image-20261020151251644](images/image-20261020151251644.png)

### 2. 客户端加一个 `/sso` 入口

读票据 → 换 token → 跳转：

```vue
<!-- views/Sso.vue -->
<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

onMounted(async () => {
  const ticket = route.query.ticket
  if (!ticket) return router.replace('/sso-error')
  try {
    // 后端统一响应 { code, msg, data }，data 为 { token, username, ... }
    const { data: res } = await axios.post('/admin-api/system/auth/sso/exchange', { ticket })
    if (res.code !== 200) throw new Error(res.msg)
    localStorage.setItem('token', res.data.token)
    const redirect = String(route.query.redirect || '/')
    router.replace(redirect.startsWith('/') ? redirect : '/')
  } catch (e) {
    router.replace('/sso-error')
  }
})
</script>
<template>正在登录…</template>
```

- 之后请求带上 `Authorization: <token>`（token 原值，无 `Bearer`），即复用后台登录态。
- 客户端须与后端**同源**：开发用 Vite `server.proxy` 把 `/admin-api` 代理到后端，生产用 Nginx 反代（后端未开 CORS）。

### 3. 打开客户端

- **客户端主动发起**：未登录时跳后台登录页 `http://localhost:5173/login?redirect=http://localhost:5176/sso`，登录成功后自动带票据跳回。
- **从后台进入**：菜单「组件」填 `External:app1`（`External:` 后即客户端标识），保存后点击该菜单即带票据打开客户端。

## 注意

- 票据**只能用一次**，默认 60s 失效。
- 客户端 `/sso` 路由必须与后台「接入路径」一致，否则票据送不到。
- 带 `redirect` 登录时，后台只校验该地址的 **Origin** 是否在客户端白名单里（不校验具体路径）。
- 客户端标识、Origin 各自全局唯一。
