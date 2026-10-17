<template>
  <div class="page-container">
    <n-card :bordered="false" class="setting-card">
      <template #header>
        <div class="setting-card__header">
          <span>站点设置</span>
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
          <n-form-item label="站点名称" path="siteName">
            <n-input v-model:value="form.siteName" placeholder="显示在顶栏与浏览器标题，如 Designer Blog" />
          </n-form-item>

          <n-form-item label="站点副标题">
            <n-input
              v-model:value="form.siteSubtitle"
              placeholder="显示在顶栏站点名右侧，如 记录设计与代码"
            />
          </n-form-item>

          <n-form-item label="站点 Logo">
            <div class="logo-field">
              <div class="logo-field__preview">
                <img
                  v-if="form.logoUrl"
                  :src="form.logoUrl"
                  alt="站点 Logo"
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

          <n-form-item label="站点主题色">
            <ThemeColorPicker v-model="form.themeColor" />
          </n-form-item>

          <n-form-item label="页脚版权" path="copyright">
            <n-input
              v-model:value="form.copyright"
              placeholder="如 © 2026 Designer. All rights reserved."
            />
          </n-form-item>

          <n-form-item label="备案号">
            <n-input v-model:value="form.icp" placeholder="如 粤ICP备00000000号-1，留空不显示" />
          </n-form-item>

          <n-form-item label="备案链接">
            <n-input
              v-model:value="form.icpUrl"
              placeholder="如 https://beian.miit.gov.cn/，留空则备案号不跳转"
            />
          </n-form-item>

          <n-divider style="margin: 4px 0 20px">
            <span class="setting-divider-text">评论</span>
          </n-divider>

          <n-form-item label="开启评论">
            <div class="switch-field">
              <n-switch v-model:value="form.commentEnabled" :checked-value="1" :unchecked-value="0" />
              <span class="switch-field__hint">
                站点总开关。关闭后前台不再显示评论输入框（已有评论仍可阅读），
                但后台仍可正常发表评论与回复。
              </span>
            </div>
          </n-form-item>

          <n-form-item label="站长署名">
            <div class="owner-nickname-field">
              <n-input v-model:value="form.ownerNickname" maxlength="50" placeholder="博主" />
              <span class="switch-field__hint">
                后台回复评论时的默认署名，并会<strong>自动</strong>加入下面的保留昵称，无需重复填写。
                署名只影响文字展示，前台是否显示「博主」标识由服务端身份字段决定。
              </span>
            </div>
          </n-form-item>

          <n-form-item label="保留昵称">
            <div class="owner-nickname-field">
              <n-input
                v-model:value="form.reservedNicknames"
                maxlength="500"
                placeholder="博主,站长,管理员,admin"
              />
              <span class="switch-field__hint">
                逗号分隔（全角逗号、分号、换行也可以）。访客昵称只要<strong>包含</strong>其中任一词就会被拒绝，
                能挡住「博主小号」「admin01」这类夹带式冒充；比较时忽略大小写与空格，全角/半角视为相同。
                所以请填完整、有辨识度的词：填「站」会连「车站」都拦掉。留空表示不拦截任何词。
              </span>
            </div>
          </n-form-item>

          <n-divider style="margin: 4px 0 20px">
            <span class="setting-divider-text">SEO 默认值（文章未单独设置时生效）</span>
          </n-divider>

          <n-form-item label="SEO 关键字">
            <n-input
              v-model:value="form.seoKeywords"
              placeholder="逗号分隔，如 Designer,博客,前端"
            />
          </n-form-item>

          <n-form-item label="SEO 描述">
            <n-input
              v-model:value="form.seoDescription"
              type="textarea"
              :rows="3"
              placeholder="留空则使用站点副标题"
            />
          </n-form-item>

          <p class="setting-tip">
            保存后博客前台立即生效（文章等其它配置不在本页）。站点主题色会覆盖前台的品牌配色，
            影响链接、按钮、标签等所有以主题色渲染的元素。
          </p>

          <div class="setting-submit">
            <n-button
              v-perm="'blog:setting:edit'"
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
import ThemeColorPicker from '@/components/ThemeColorPicker.vue'
import { getSetting, updateSetting } from '@/modules/blog/api'
import { uploadFile } from '@/modules/system/api/file'
import { DEFAULT_PRIMARY } from '@/theme'

const message = useMessage()
const formRef = ref(null)
const inputRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)

const form = reactive({
  siteName: '',
  siteSubtitle: '',
  logoUrl: '',
  logoRadius: 6,
  themeColor: DEFAULT_PRIMARY,
  copyright: '',
  icp: '',
  icpUrl: '',
  seoKeywords: '',
  seoDescription: '',
  commentEnabled: 1,
  ownerNickname: '博主',
  reservedNicknames: '博主,站长,管理员,admin'
})

const formRules = {
  siteName: { required: true, message: '请输入站点名称', trigger: 'blur' }
}

async function loadSetting() {
  loading.value = true
  try {
    const res = await getSetting()
    const data = res.data || {}
    form.siteName = data.siteName || ''
    form.siteSubtitle = data.siteSubtitle || ''
    form.logoUrl = data.logoUrl || ''
    form.logoRadius = data.logoRadius != null ? data.logoRadius : 6
    form.themeColor = data.themeColor || DEFAULT_PRIMARY
    form.copyright = data.copyright || ''
    form.icp = data.icp || ''
    form.icpUrl = data.icpUrl || ''
    form.seoKeywords = data.seoKeywords || ''
    form.seoDescription = data.seoDescription || ''
    // 0 是合法值（关闭），故用 null 判断而非真值
    form.commentEnabled = data.commentEnabled != null ? data.commentEnabled : 1
    form.ownerNickname = data.ownerNickname || '博主'
    // 空串是合法值（表示不拦截任何词），所以只在字段缺失（null/undefined）时回默认
    form.reservedNicknames = data.reservedNicknames != null
      ? data.reservedNicknames
      : '博主,站长,管理员,admin'
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
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
    const res = await uploadFile(file, 'blog/logo')
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

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await updateSetting({ ...form })
    message.success('保存成功')
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

.setting-tip {
  margin: 8px 0 16px;
  font-size: 12px;
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

/* 开关 + 说明文案：说明可能需要换行，故顶部对齐 */
.switch-field {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.switch-field__hint {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-muted);
  padding-top: 3px;
}

/* 站长署名：输入框 + 说明纵向排列 */
.owner-nickname-field {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
