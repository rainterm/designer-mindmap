import { createApp, watch } from 'vue'
import { createPinia } from 'pinia'
import naive from 'naive-ui'

import App from './App.vue'
import router from './router'
import permission from './directives/permission'
import { applyThemeCssVars } from './theme'
import { setupSearchActionsAlign } from './utils/searchActionsAlign'
import { applyFavicon } from './utils/favicon'
import { appTitle } from './settings'
import { useAppStore } from './store/app'
import { useSettingStore } from './store/setting'
import DataTable from './components/DataTable'
import SearchBar from './components/SearchBar.vue'
import PageTable from './components/PageTable.vue'
import PaginationBar from './components/PaginationBar.vue'
import Dialog from './components/Dialog.vue'
import DetailDrawer from './components/DetailDrawer.vue'
import './styles/index.css'

applyThemeCssVars()
document.title = appTitle

const app = createApp(App)

// 注意顺序：Pinia 必须先于 Router 安装，路由守卫中会用到 store
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(naive)

// 系统名称 / Logo / 默认偏好来自后端「系统设置」，公开接口无需登录即可读取
const appStore = useAppStore(pinia)
const settingStore = useSettingStore(pinia)
settingStore.loadPublic()
// 浏览器标题跟随系统名称（拿不到时回退环境变量 VITE_APP_TITLE）
watch(
  () => settingStore.systemName,
  (name) => {
    document.title = name || appTitle
  },
  { immediate: true }
)
// 浏览器标签图标跟随系统 Logo + 圆角（标签图标不读页面 CSS，必须主动设置）
watch(
  () => [settingStore.logoUrl, settingStore.logoRadius, appStore.primaryColor],
  ([logoUrl, logoRadius, primaryColor]) => applyFavicon(logoUrl, logoRadius, primaryColor),
  { immediate: true }
)
// 覆盖默认表格，注入统一空状态 + 自动高度
app.component('NDataTable', DataTable)
app.component('SearchBar', SearchBar)
app.component('PageTable', PageTable)
app.component('PaginationBar', PaginationBar)
app.component('Dialog', Dialog)
app.component('DetailDrawer', DetailDrawer)

app.directive('perm', permission)

app.mount('#app')
setupSearchActionsAlign()

// 首次导航（守卫内会拉取用户信息/菜单）完成后移除首屏动画；
// 此期间组件层 PageLoading 已接管遮罩，两者交叉淡入淡出不留白屏
router.isReady().finally(() => {
  window.__removeBootLoading && window.__removeBootLoading()
})
