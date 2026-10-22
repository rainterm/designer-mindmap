import { defineStore } from 'pinia'
import { getPublicSystemSetting } from '@/modules/system/api/system'
import { appTitle } from '@/settings'
import { DEFAULT_LAYOUT_MODE } from '@/constants/layout'
import { DEFAULT_PRIMARY } from '@/theme'
import { useAppStore } from '@/store/app'

/** Logo 默认圆角（px），与后端 SysSettingService 保持一致 */
const DEFAULT_LOGO_RADIUS = 6

/** 登录页副标题默认值，与后端 SysSettingService 保持一致 */
const DEFAULT_LOGIN_SUBTITLE = '统一管理后台，让协作更高效'

/**
 * 正在进行的公开配置请求。
 *
 * 放在模块作用域而不是 state：Promise 不需要响应式，塞进 state 会被 Pinia 代理一层。
 * 它的作用是让并发调用复用同一个请求（既避免重复打接口，也让 refresh 能等到它收尾）。
 */
let inflight = null

/**
 * 系统设置（后台「系统设置」菜单维护）：系统名称 / Logo / 默认偏好。
 *
 * 通过公开接口读取，登录前即可用于登录页；登录后由后台页面维护。
 * 默认偏好只在用户尚未在「偏好设置」面板自定义时生效。
 */
export const useSettingStore = defineStore('setting', {
  state: () => ({
    /** 系统名称（登录页 / 侧边栏 / 顶栏 / 浏览器标题） */
    systemName: appTitle,
    /** 登录页副标题，空表示不显示 */
    loginSubtitle: DEFAULT_LOGIN_SUBTITLE,
    /** 系统 Logo 地址，空表示用内置 Logo */
    logoUrl: '',
    /** Logo 圆角（px） */
    logoRadius: DEFAULT_LOGO_RADIUS,
    /** 默认布局模式 */
    defaultLayoutMode: DEFAULT_LAYOUT_MODE,
    /** 默认主题色 */
    defaultPrimaryColor: DEFAULT_PRIMARY,
    /** 是否已成功读取过公开配置 */
    loaded: false,
    /** 是否正在读取中（避免并发重复请求） */
    loading: false
  }),

  actions: {
    /**
     * 读取公开系统设置；失败时沿用兜底值（环境变量 + 内置默认），不阻断启动。
     *
     * 并发调用复用同一个请求：已有请求在飞就直接返回它，不重复打接口。
     */
    loadPublic() {
      if (inflight) return inflight
      if (this.loaded) return
      inflight = this.fetchPublic()
      return inflight
    },

    /** 真正发起请求（由 loadPublic / refresh 保证同一时刻只有一个） */
    async fetchPublic(adopt) {
      this.loading = true
      try {
        const res = await getPublicSystemSetting()
        this.apply(res.data || {}, adopt)
      } catch {
        // 拦截器已提示；此处保持兜底值
      } finally {
        this.loading = false
        this.loaded = true
        inflight = null
      }
    },

    /**
     * 应用一份系统设置（公开接口 / 后台保存后均可复用）。
     *
     * @param {object} data 公开接口下发的配置
     * @param {{ layout?: boolean, color?: boolean }} [adopt] 强制采纳的默认偏好维度，
     *   仅后台「系统设置」保存后传入（见 app store 的 applySystemDefaults）。
     */
    apply(data, adopt) {
      if (data.systemName) this.systemName = data.systemName
      this.loginSubtitle = data.loginSubtitle || ''
      this.logoUrl = data.logoUrl || ''
      this.logoRadius = data.logoRadius != null ? data.logoRadius : DEFAULT_LOGO_RADIUS
      if (data.defaultLayoutMode) this.defaultLayoutMode = data.defaultLayoutMode
      if (data.defaultPrimaryColor) this.defaultPrimaryColor = data.defaultPrimaryColor
      // 用户尚未自定义偏好时，立即应用服务端默认布局与主题色
      useAppStore().applySystemDefaults(this.defaultLayoutMode, this.defaultPrimaryColor, adopt)
    },

    /**
     * 后台保存后重新拉取，让登录页 / 布局立即生效。
     *
     * 必须等在飞的旧请求收尾：它携带的是「保存前」的数据，如果让它晚于新请求返回，
     * 就会把刚保存的值又覆盖回旧值（清空 Logo 保存后 Logo 又变回来就是这个原因）。
     *
     * @param {{ layout?: boolean, color?: boolean }} [adopt] 本次被改动的默认项，
     *   透传给应用层以覆盖「已自定义」的豁免。
     */
    async refresh(adopt) {
      if (inflight) {
        try {
          await inflight
        } catch {
          // 旧请求失败无所谓，下面照样重新拉取
        }
      }
      this.loaded = false
      inflight = this.fetchPublic(adopt)
      return inflight
    }
  }
})
