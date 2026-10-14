<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <BrandLogo class="brand-logo" />
        <div>
          <h1>{{ appTitle }}</h1>
          <p>统一管理后台，让协作更高效</p>
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

        <n-form-item v-if="captchaEnabled" path="captcha" label="验证码">
          <div class="captcha-row">
            <n-input v-model:value="form.captcha" size="large" placeholder="计算结果" clearable>
              <template #prefix><n-icon :component="ShieldCheckmarkOutline" /></template>
            </n-input>
            <img
              v-if="captchaImg"
              class="captcha-img"
              :src="captchaImg"
              alt="验证码"
              title="点击刷新"
              @click="loadCaptcha"
            />
          </div>
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
import { PersonOutline, LockClosedOutline, ShieldCheckmarkOutline } from '@vicons/ionicons5'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { appTitle } from '@/settings'
import BrandLogo from '@/components/BrandLogo.vue'
import { continueSsoReturn, safeInternalRedirect } from '@/utils/sso'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
/** 是否展示验证码（后端：全局开启且该账号失败 ≥ 阈值） */
const captchaEnabled = ref(false)
const captchaImg = ref('')
const form = reactive({
  username: '',
  password: '',
  captcha: '',
  uuid: ''
})

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: ['input', 'blur'] }],
  password: [{ required: true, message: '请输入登录密码', trigger: ['input', 'blur'] }],
  captcha: [
    {
      required: true,
      validator: (_r, v) => {
        if (!captchaEnabled.value) return true
        if (!v) return new Error('请输入验证码')
        return true
      },
      trigger: ['input', 'blur']
    }
  ]
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha(form.username)
    const data = res.data || {}
    captchaEnabled.value = !!data.enabled
    form.uuid = data.uuid || ''
    captchaImg.value = data.img || ''
    form.captcha = ''
  } catch (e) {
    captchaEnabled.value = false
    captchaImg.value = ''
    form.uuid = ''
    form.captcha = ''
  }
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    const payload = {
      username: form.username,
      password: form.password
    }
    if (captchaEnabled.value) {
      payload.captcha = form.captcha
      payload.uuid = form.uuid
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
    // 失败后按账号重新拉取，达到阈值才展示验证码
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

watch(
  () => form.username,
  () => {
    loadCaptcha()
  }
)

onMounted(loadCaptcha)
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

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
  align-items: center;
}

.captcha-row :deep(.n-input) {
  flex: 1;
}

.captcha-img {
  height: 40px;
  width: 120px;
  border-radius: var(--radius);
  cursor: pointer;
  border: 1px solid var(--color-border);
  object-fit: cover;
  flex-shrink: 0;
}
</style>
