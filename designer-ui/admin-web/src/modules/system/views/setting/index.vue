<template>
  <div class="page-container">
    <n-card :bordered="false" class="setting-card">
      <template #header>
        <div class="setting-card__header">
          <span>系统设置</span>
        </div>
      </template>

      <n-spin :show="loading">
        <n-form
          ref="formRef"
          :model="form"
          :rules="formRules"
          label-placement="left"
          label-width="100"
        >
          <n-divider style="margin: 0 0 20px">
            <span class="setting-divider-text">品牌</span>
          </n-divider>

          <n-form-item label="系统名称" path="systemName">
            <n-input
              v-model:value="form.systemName"
              placeholder="显示在登录页、侧边栏与浏览器标题，如 Designer Admin"
            />
          </n-form-item>

          <n-form-item label="登录页副标题" path="loginSubtitle">
            <n-input
              v-model:value="form.loginSubtitle"
              placeholder="显示在登录页系统名称下方，留空则不显示"
              clearable
            />
          </n-form-item>

          <n-form-item label="系统 Logo">
            <div class="logo-field">
              <div class="logo-field__preview">
                <img
                  v-if="form.logoUrl"
                  :src="form.logoUrl"
                  alt="系统 Logo"
                  :style="{ borderRadius: `${form.logoRadius}px` }"
                />
                <span
                  v-else
                  class="logo-field__empty"
                  :style="{ borderRadius: `${form.logoRadius}px` }"
                >
                  内置 Logo
                </span>
              </div>
              <div class="logo-field__actions">
                <n-space align="center">
                  <n-button :loading="uploading" @click="triggerUpload">上传图片</n-button>
                  <n-button v-if="form.logoUrl" text type="error" @click="form.logoUrl = ''">
                    清除
                  </n-button>
                </n-space>
                <n-input
                  v-model:value="form.logoUrl"
                  placeholder="或直接填写图片地址，留空使用内置 Logo"
                  clearable
                />
                <input
                  ref="inputRef"
                  type="file"
                  accept="image/*"
                  class="logo-field__input"
                  @change="onFileChange"
                />
              </div>
            </div>
          </n-form-item>

          <n-form-item label="Logo 圆角">
            <div class="radius-field">
              <n-slider v-model:value="form.logoRadius" :min="0" :max="48" :step="1" />
              <n-input-number
                v-model:value="form.logoRadius"
                :min="0"
                :max="48"
                :show-button="false"
                style="width: 92px"
              >
                <template #suffix>px</template>
              </n-input-number>
            </div>
          </n-form-item>

          <n-divider style="margin: 4px 0 20px">
            <span class="setting-divider-text">默认偏好（用户未自定义时生效）</span>
          </n-divider>

          <n-form-item label="默认布局">
            <LayoutPicker v-model="form.defaultLayoutMode" />
          </n-form-item>

          <n-form-item label="默认主题色">
            <ThemeColorPicker v-model="form.defaultPrimaryColor" />
          </n-form-item>

          <p class="setting-tip">
            系统名称、登录页副标题、Logo、默认偏好保存后立即生效（登录页 / 侧边栏 / 浏览器标题）。
            用户若在「偏好设置」面板自定义过某一项，则不受该项默认值影响；
            本次保存中被改动的默认项会在当前会话立即应用。
          </p>

          <div class="setting-submit">
            <n-button
              v-perm="'system:setting:edit'"
              type="primary"
              :loading="saving"
              @click="handleSubmit"
            >
              保存
            </n-button>
          </div>
        </n-form>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import LayoutPicker from '@/components/LayoutPicker.vue'
import ThemeColorPicker from '@/components/ThemeColorPicker.vue'
import { getSystemSetting, updateSystemSetting } from '@/modules/system/api/system'
import { uploadFile } from '@/modules/system/api/file'
import { DEFAULT_LAYOUT_MODE } from '@/constants/layout'
import { DEFAULT_PRIMARY } from '@/theme'
import { useSettingStore } from '@/store/setting'

const message = useMessage()
const settingStore = useSettingStore()
const formRef = ref(null)
const inputRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)

const form = reactive({
  systemName: '',
  loginSubtitle: '',
  logoUrl: '',
  logoRadius: 6,
  defaultLayoutMode: DEFAULT_LAYOUT_MODE,
  defaultPrimaryColor: DEFAULT_PRIMARY
})

/** 服务端当前的默认偏好（loadSetting 时记录），用于判断保存时改动了哪一项 */
const loadedDefaults = reactive({
  defaultLayoutMode: DEFAULT_LAYOUT_MODE,
  defaultPrimaryColor: DEFAULT_PRIMARY
})

const formRules = {
  systemName: { required: true, message: '请输入系统名称', trigger: 'blur' }
}

function triggerUpload() {
  inputRef.value?.click()
}

async function onFileChange(e) {
  const file = e?.target?.files?.[0]
  if (e?.target) e.target.value = ''
  if (!file) return
  uploading.value = true
  try {
    const res = await uploadFile(file, 'system/logo')
    const url = res.data?.url
    if (!url) throw new Error('上传成功但未返回文件地址')
    form.logoUrl = url
    message.success('上传成功，别忘了保存')
  } catch (err) {
    message.error(`上传失败：${err?.message || ''}`)
  } finally {
    uploading.value = false
  }
}

async function loadSetting() {
  loading.value = true
  try {
    const res = await getSystemSetting()
    const data = res.data || {}
    form.systemName = data.systemName || ''
    form.loginSubtitle = data.loginSubtitle || ''
    form.logoUrl = data.logoUrl || ''
    form.logoRadius = data.logoRadius != null ? data.logoRadius : 6
    form.defaultLayoutMode = data.defaultLayoutMode || DEFAULT_LAYOUT_MODE
    form.defaultPrimaryColor = data.defaultPrimaryColor || DEFAULT_PRIMARY
    // 记下服务端的当前值，保存时用它判断本次到底改了哪一项
    loadedDefaults.defaultLayoutMode = form.defaultLayoutMode
    loadedDefaults.defaultPrimaryColor = form.defaultPrimaryColor
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    // 只采纳本次真正改动过的默认项：保存系统名称时不该顺带重置管理员的个人偏好
    const adopt = {
      layout: form.defaultLayoutMode !== loadedDefaults.defaultLayoutMode,
      color: form.defaultPrimaryColor !== loadedDefaults.defaultPrimaryColor
    }
    await updateSystemSetting({ ...form })
    message.success('保存成功')
    // 重新拉取公开配置，让登录页 / 布局立即生效
    await settingStore.refresh(adopt)
    await loadSetting()
  } finally {
    saving.value = false
  }
}

onMounted(loadSetting)
</script>

<style scoped>
.setting-card__header {
  font-weight: 600;
}

/* 白卡只覆盖表单：宽度收敛并居中，页面背景保持可见 */
.setting-card {
  max-width: 640px;
  margin: 0 auto;
}

.logo-field {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
}

.logo-field__preview {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-body);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.logo-field__preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-field__empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-surface);
}

.logo-field__actions {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.logo-field__input {
  display: none;
}

.radius-field {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.radius-field .n-slider {
  flex: 1;
  min-width: 0;
}

.setting-tip {
  margin: 8px 0 16px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-muted);
}

/* 按钮居中（n-button 为 inline-block，父级 text-align 即可居中） */
.setting-submit {
  text-align: center;
}

.setting-divider-text {
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
