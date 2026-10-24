import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import { useUserStore } from '@/store/user'
import { setUnauthorizedHandler } from '@/api/request'
import { startLoading, finishLoading } from '@/utils/pageLoading'
import { continueSsoReturn } from '@/utils/sso'

/** 壳层静态视图（login / dashboard / error） */
const shellViews = import.meta.glob('../views/**/*.vue')

/** 业务模块视图：src/modules/<module>/views/** */
const moduleViews = import.meta.glob('../modules/*/views/**/*.vue')

/**
 * 解析后端菜单 component 字符串。
 * 约定：`system/user/index` → `modules/system/views/user/index.vue`
 *       `demo/index` → `modules/demo/views/index.vue`
 * 壳层兜底：仍支持 `views/` 下路径（极少使用）。
 */
function resolveComponent(component) {
  if (!component || component === 'Layout') {
    return Layout
  }
  const slash = component.indexOf('/')
  if (slash > 0) {
    const mod = component.slice(0, slash)
    const rest = component.slice(slash + 1)
    const moduleKey = `../modules/${mod}/views/${rest}.vue`
    if (moduleViews[moduleKey]) {
      return moduleViews[moduleKey]
    }
  }
  const shellKey = `../views/${component}.vue`
  if (shellViews[shellKey]) {
    return shellViews[shellKey]
  }
  console.warn(`[router] 未找到视图组件：${component}`)
  return () => import('@/views/error/404.vue')
}

/** 静态路由：无需权限即可访问 */
export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', public: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', public: true }
  },
  {
    path: '/sso-error',
    name: 'SsoError',
    component: () => import('@/views/error/sso.vue'),
    meta: { title: '单点登录失败', public: true }
  },
  /** 独立全屏页：新标签打开、不走后台 Layout（无侧栏/顶栏） */
  {
    path: '/mindmap/project/edit',
    name: 'MindmapProjectEdit',
    component: () => import('@/modules/mindmap/views/project/edit.vue'),
    meta: {
      title: '导图编辑',
      standalone: true,
      hidden: true,
      keepAlive: false,
      authPerm: 'mindmap:project:edit'
    }
  },
  {
    path: '/agent/workflow/edit',
    name: 'AgentWorkflowEdit',
    component: () => import('@/modules/agent/views/workflow/edit.vue'),
    meta: {
      title: '工作流设计',
      standalone: true,
      hidden: true,
      keepAlive: false,
      authPerm: 'agent:workflow:edit'
    }
  },
  {
    path: '/agent/session/chat',
    name: 'AgentSessionChat',
    component: () => import('@/modules/agent/views/session/chat.vue'),
    meta: {
      title: '对话',
      standalone: true,
      hidden: true,
      keepAlive: false,
      authPerm: 'agent:session:chat'
    }
  },
  {
    path: '/public/agent/:token',
    name: 'AgentPublicChat',
    component: () => import('@/modules/agent/views/public/chat.vue'),
    meta: {
      title: '智能体公开对话',
      public: true,
      standalone: true,
      hidden: true,
      keepAlive: false
    }
  },
  {
    path: '/',
    name: 'RootLayout',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '运行监控', icon: 'dashboard', affix: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 把后端返回的路由树注册到 RootLayout 下
 *
 * 目录节点（component=Layout）只用于导航分组，实际页面按叶子节点扁平注册，
 * 避免出现嵌套的 Layout 造成侧边栏重复渲染。
 */
export function registerDynamicRoutes(routers = []) {
  const leaves = collectLeaves(routers)
  // 长路径优先，减少与短路径的歧义
  leaves.sort((a, b) => String(b.path || '').length - String(a.path || '').length)
  leaves.forEach((leaf) => {
    const path = leaf.path?.replace(/^\//, '') || ''
    if (!path) return
    const comp = String(leaf.component || '')
    if (
      leaf.meta?.external ||
      leaf.meta?.linkUrl ||
      comp.toLowerCase().startsWith('external') ||
      comp.toLowerCase().startsWith('link')
    ) {
      return
    }
    if (String(leaf.path || '').startsWith('http://') || String(leaf.path || '').startsWith('https://')) {
      return
    }
    const fullPath = `/${path}`
    // 已有同名路径（含常量路由兜底）则跳过，避免重复注册
    if (router.getRoutes().some((r) => r.path === fullPath || r.path === path)) {
      return
    }
    let name = leaf.name || `Route${leaf.id}`
    // 名称冲突时不跳过整条路由，退化为唯一名注册（路径才是导航依据）
    if (router.hasRoute(name)) {
      name = `Route${leaf.id}`
      if (router.hasRoute(name)) return
    }
    router.addRoute('RootLayout', {
      path,
      name,
      component: resolveComponent(leaf.component),
      meta: {
        title: leaf.meta?.title || name,
        icon: leaf.meta?.icon || '',
        keepAlive: leaf.meta?.keepAlive !== false,
        hidden: !!leaf.meta?.hidden,
        perm: leaf.perms || ''
      }
    })
  })
  // 必须在动态路由之后注册，否则会抢先匹配业务页
  ensureCatchAllRoute()
}

/** 未匹配路径落到 404（挂在布局下，保留侧栏） */
function ensureCatchAllRoute() {
  if (router.hasRoute('CatchAll')) {
    return
  }
  router.addRoute('RootLayout', {
    path: ':pathMatch(.*)*',
    name: 'CatchAll',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true }
  })
}

function collectLeaves(nodes, acc = []) {
  for (const node of nodes || []) {
    if (node.children && node.children.length > 0) {
      // 列表页下挂跳转页时，父节点本身也是页面，需一并注册
      if (node.component && node.component !== 'Layout') {
        acc.push(node)
      }
      collectLeaves(node.children, acc)
    } else if (node.component && node.component !== 'Layout') {
      acc.push(node)
    }
  }
  return acc
}

// ------------------------------------------------------------------
// 全局前置守卫（Vue Router 5：用 return，勿再调 next）
// ------------------------------------------------------------------
router.beforeEach(async (to) => {
  // 任意导航开始即进入 loading（含 200ms 延迟防闪烁），afterEach 统一收尾
  startLoading()
  const userStore = useUserStore()

  // 公开页面直接放行
  if (to.meta?.public) {
    if (to.path === '/login' && userStore.token) {
      const redirect = to.query.redirect
      // 已登录且带跨站 SSO 回调：发 ticket 回客户端；失败进错误页，勿丢进首页
      if (redirect && !String(redirect).startsWith('/')) {
        try {
          if (await continueSsoReturn(redirect)) {
            finishLoading()
            return false
          }
          return {
            path: '/sso-error',
            query: {
              msg: '回跳地址无效或不在 SSO 客户端白名单中',
              redirect: String(redirect)
            },
            replace: true
          }
        } catch (e) {
          return {
            path: '/sso-error',
            query: {
              msg: e?.message || '单点登录签发失败，请检查 SSO 客户端配置',
              redirect: String(redirect)
            },
            replace: true
          }
        }
      }
      return '/'
    }
    return true
  }

  if (!userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (userStore.routesLoaded) {
    // 动态路由已就绪但仍无匹配 → 404（CatchAll 命中时 to.name 为 CatchAll）
    if (to.matched.length === 0 || to.name === 'CatchAll') {
      return { path: '/404', replace: true }
    }
    // 不走后端菜单注册的独立页需自行声明权限，防止手输 URL 越权
    if (to.meta?.authPerm && !userStore.hasPerm(to.meta.authPerm)) {
      return { path: '/403', query: { perm: to.meta.authPerm }, replace: true }
    }
    return true
  }

  // 首次进入：拉取用户信息 + 动态路由
  try {
    await userStore.loadUserInfo()
    const routers = await userStore.loadRouters()
    registerDynamicRoutes(routers)
    // 重新匹配一次；勿把 fullPath（含 ?query）塞进 path
    return { path: to.path, query: to.query, hash: to.hash, replace: true }
  } catch (e) {
    userStore.reset()
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

router.afterEach(() => {
  // vue-router 会在确认导航前解析完懒加载 chunk，此处收尾再叠加短暂驻留做淡出
  finishLoading()
})

router.onError(() => {
  finishLoading()
})

// 令牌失效时统一跳转登录页
setUnauthorizedHandler(() => {
  const userStore = useUserStore()
  userStore.reset()
  if (router.currentRoute.value.path !== '/login') {
    router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  }
})

export default router
