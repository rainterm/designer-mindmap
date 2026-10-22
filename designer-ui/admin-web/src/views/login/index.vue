<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <BrandLogo class="brand-logo" :src="settingStore.logoUrl" :radius="settingStore.logoRadius" />
        <div>
          <h1>{{ settingStore.systemName }}</h1>
          <p v-if="settingStore.loginSubtitle">{{ settingStore.loginSubtitle }}</p>
        </div>
      </div>

      <n-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <n-form-item path="username" label="账号">
          <n-input v-model:value="form.username" size="large" placeholder="请输入登录账号" clearable>
            <template #prefix><n-icon :component="PersonOutline" /></template>
          </n-input>
        </n-form-item>
        <n-form-item path="password" label="密码">
          <n-input
            v-model:value="form.password"
            size="large"
            type="password"
            show-password-on="click"
            placeholder="请输入登录密码"
          >
            <template #prefix><n-icon :component="LockClosedOutline" /></template>
          </n-input>
        </n-form-item>

        <!--
          人机验证面板按需展示（后端：全局开启且该账号失败 ≥ 阈值）。
          默认不出现，也就不请求、不占版面；仅在连续失败达阈值后展示。
        -->
        <n-form-item v-if="captchaVisible" label="安全验证">
          <SliderCaptcha ref="captchaRef" :api="captchaApi" @verified="onVerified" />
        </n-form-item>

        <n-button type="primary" size="large" block :loading="loading" @click="handleLogin">
          登 录
        </n-button>
      </n-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { PersonOutline, LockClosedOutline } from '@vicons/ionicons5'
import { SliderCaptcha } from '@captcha'
import { getCaptcha } from '@/api/auth'
import { captchaApi } from '@/api/captcha'
import { buildPasswordRules } from '@/constants/password'
import { useUserStore } from '@/store/user'
import { useSettingStore } from '@/store/setting'
import BrandLogo from '@/components/BrandLogo.vue'
import { continueSsoReturn, safeInternalRedirect } from '@/utils/sso'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()
const settingStore = useSettingStore()

const formRef = ref(null)
const loading = ref(false)
/** 该账号本次登录是否需要人机验证（后端按失败次数判定） */
const captchaEnabled = ref(false)
/**
 * 滑块面板是否已展开。与 {@link captchaEnabled} 分开：需要验证 ≠ 已经把面板摆出来，
 * 首次点「登录」时才展开，免得一进页面就在密码框下面杵一张图。
 */
const captchaVisible = ref(false)
const captchaRef = ref(null)

const form = reactive({
  username: '',
  password: ''
})

/**
 * /app-api/captcha/check 下发的二次校验凭据。**一次性**：服务端在登录流程里消费掉它
 * （无论登录成败），所以每次提交失败后都必须清掉并重新拖滑块。
 */
const captchaVerification = ref('')

/**
 * 账号长度上限，与后端 LoginDTO.USERNAME_MAX_LENGTH 保持一致
 * （designer-module-system/.../model/dto/LoginDTO.java），改动时两边一起改。
 */
const USERNAME_MAX_LENGTH = 50

const rules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: ['input', 'blur'] },
    {
      max: USERNAME_MAX_LENGTH,
      message: `账号长度不能超过 ${USERNAME_MAX_LENGTH} 个字符`,
      trigger: ['input', 'blur']
    }
  ],
  // 登录只卡长度、不卡复杂度：复杂度是对「新设密码」的要求，
  // 放到登录上会让纯字母/纯数字的老密码即使输对也登不进来（后端 LoginDTO 同理）
  password: buildPasswordRules({
    required: true,
    requiredMessage: '请输入登录密码',
    complexity: false
  })
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha(form.username)
    captchaEnabled.value = !!(res.data || {}).enabled
  } catch {
    // 拿不到「要不要验」时按「不需要」处理：真正需要时服务端仍会拒绝，
    // 用户重新提交一次就会再拉一次状态，不会把自己卡在登录页上
    captchaEnabled.value = false
  }
}

function resetCaptcha() {
  captchaVerification.value = ''
  captchaVisible.value = false
  captchaRef.value?.reset()
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }

  // 需要验证但还没拖：展开面板，等 SliderCaptcha 通过后自动提交（见 onVerified）
  if (captchaEnabled.value && !captchaVerification.value) {
    if (!captchaVisible.value) {
      captchaVisible.value = true
      message.info('请拖动滑块完成验证')
    }
    return
  }

  await doLogin()
}

/** 滑块对齐成功，拿到凭据后直接提交，省掉一次多余的点击 */
async function onVerified(credential) {
  captchaVerification.value = credential
  await doLogin()
}

async function doLogin() {
  if (loading.value) return
  loading.value = true
  try {
    const payload = {
      username: form.username,
      password: form.password
    }
    // 不需要验证时不必带：服务端只在启用了验证码的账号上消费这个字段
    if (captchaVerification.value) {
      payload.captchaVerification = captchaVerification.value
    }
    await userStore.login(payload)
    message.success('登录成功')
    const redirect = route.query.redirect
    try {
      if (await continueSsoReturn(redirect)) return
    } catch (e) {
      router.replace({
        path: '/sso-error',
        query: {
          msg: e?.message || 'SSO 回跳校验失败，请检查「SSO客户端」白名单配置',
          redirect: redirect ? String(redirect) : undefined
        }
      })
      return
    }
    router.replace(safeInternalRedirect(redirect))
  } catch (e) {
    // 失败原因由请求层统一提示。凭据已被服务端消费掉（无论登录成败），
    // 这里必须清掉并重新拉状态 —— 失败一次就可能刚好越过阈值，面板要立刻出现
    captchaVerification.value = ''
    await loadCaptcha()
    if (captchaVisible.value) {
      // 面板已展开就原地换一张（SliderCaptcha 会顺带清掉「验证通过」状态），
      // 用户直接重拖即可，不必再点一次登录
      captchaRef.value?.refresh()
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => form.username,
  () => {
    // 阈值是按账号算的，换账号必须把上一轮的状态整个丢掉，
    // 否则会把 A 账号的「需要验证」和已拖出来的凭据带到 B 账号上
    resetCaptcha()
    loadCaptcha()
  }
)

onMounted(() => {
  // 系统名称 / Logo 由公开接口下发，登录前即可展示（已加载则直接返回）
  settingStore.loadPublic()
  loadCaptcha()
})
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(ellipse 70% 55% at 12% 18%, color-mix(in srgb, var(--color-primary) 14%, transparent), transparent 70%),
    radial-gradient(ellipse 55% 50% at 88% 82%, color-mix(in srgb, var(--color-primary-light) 40%, transparent), transparent 65%),
    linear-gradient(
      160deg,
      color-mix(in srgb, var(--color-primary) 6%, var(--color-body)) 0%,
      var(--color-body) 48%,
      color-mix(in srgb, var(--color-primary) 8%, var(--color-body)) 100%
    );
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(color-mix(in srgb, var(--color-primary) 6%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--color-primary) 6%, transparent) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 45%, #000 20%, transparent 75%);
  pointer-events: none;
}

.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 40px 36px 28px;
  background-color: var(--color-surface);
  border: 1px solid color-mix(in srgb, var(--color-primary) 8%, var(--color-border));
  border-radius: var(--radius);
  box-shadow: 0 16px 40px color-mix(in srgb, var(--color-primary) 10%, transparent);
  /* 验证码面板展开后卡片会变高，窗口矮时要能滚动而不是把「登录」按钮顶出视口 */
  max-height: 100vh;
  overflow-y: auto;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 32px;
}

.brand-logo {
  width: 48px;
  height: 48px;
}

.login-brand h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text);
}

.login-brand p {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-text-muted);
}
</style>
