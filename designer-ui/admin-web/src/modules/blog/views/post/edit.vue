<template>
  <div class="page-container post-edit">
    <n-card :bordered="false">
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="90">
        <n-form-item label="标题" path="title">
          <n-input v-model:value="form.title" placeholder="文章标题" maxlength="200" show-count />
        </n-form-item>
        <n-form-item label="封面">
          <n-upload
            class="cover-upload"
            :show-file-list="false"
            accept="image/*"
            :custom-request="handleCoverUpload"
          >
            <div class="cover-uploader" :class="{ 'is-loading': uploading }">
              <img
                v-if="form.coverUrl"
                class="cover-img"
                :src="form.coverUrl"
                alt="封面预览"
              />
              <div v-else class="cover-empty">
                <n-icon :component="CloudUploadOutline" :size="28" />
                <span>点击上传封面</span>
              </div>
              <div v-if="form.coverUrl" class="cover-hover">
                <n-icon :component="CloudUploadOutline" :size="22" />
                <span>更换封面</span>
              </div>
              <div v-if="uploading" class="cover-loading">
                <n-spin :size="24" />
              </div>
              <button
                v-if="form.coverUrl && !uploading"
                type="button"
                class="cover-clear"
                title="清除封面"
                @click.stop.prevent="form.coverUrl = ''"
              >
                <n-icon :component="CloseCircleOutline" :size="18" />
              </button>
            </div>
          </n-upload>
        </n-form-item>
        <n-form-item label="摘要">
          <n-input
            v-model:value="form.summary"
            type="textarea"
            :rows="2"
            placeholder="简短摘要，用于列表展示"
            maxlength="500"
            show-count
          />
        </n-form-item>
        <n-form-item label="正文" path="content">
          <div class="md-wrap">
            <MdEditor
              ref="mdEditorRef"
              v-model="form.content"
              language="zh-CN"
              :preview="showPreview"
              @onUploadImg="handleEditorUploadImg"
            />
            <!-- 图片上传进度：粘贴后即时出现，无需等到全部上传完 -->
            <div v-if="imageUploadingCount > 0" class="md-upload-tip">
              <n-spin :size="14" />
              <span>图片上传中{{ imageUploadingCount > 1 ? ` ×${imageUploadingCount}` : '' }}…</span>
            </div>
          </div>
        </n-form-item>
        <n-form-item label="分类">
          <n-select
            v-model:value="form.categoryId"
            :options="categoryOptions"
            placeholder="选择分类"
            clearable
          />
        </n-form-item>
        <n-form-item label="标签">
          <n-select
            v-model:value="form.tagIds"
            :options="tagOptions"
            multiple
            placeholder="选择标签"
            clearable
          />
        </n-form-item>
        <n-form-item label="URL 标识">
          <n-input v-model:value="form.slug" placeholder="用于文章链接，留空自动生成" />
        </n-form-item>
        <n-form-item label="置顶">
          <n-switch v-model:value="form.isTop" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
        <n-form-item label="允许评论">
          <n-switch v-model:value="form.allowComment" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
        <n-form-item label="SEO 标题">
          <n-input v-model:value="form.seoTitle" placeholder="选填，默认用文章标题" />
        </n-form-item>
        <n-form-item label="SEO 关键词">
          <n-input v-model:value="form.seoKeywords" placeholder="选填，逗号分隔" />
        </n-form-item>
        <n-form-item label="SEO 描述">
          <n-input
            v-model:value="form.seoDescription"
            type="textarea"
            :rows="2"
            placeholder="选填，默认用摘要"
          />
        </n-form-item>
      </n-form>

      <div class="edit-toolbar">
        <n-space>
          <n-button @click="goBack">
            返回
          </n-button>
          <n-button v-perm="'blog:post:edit'" :loading="submitting" @click="handleSave(0)">
            存草稿
          </n-button>
          <n-button
            v-perm="'blog:post:publish'"
            type="primary"
            :loading="submitting"
            @click="handleSave(1)"
          >
            发布
          </n-button>
        </n-space>
      </div>
    </n-card>

    <!--
      评论面板：正文与表单之外独立一块，避免和「保存 / 发布」按钮抢视觉权重。
      只有已保存的文章才有 id，未保存时组件内部会给出提示。
    -->
    <PostCommentPanel :post-id="form.id" :allow-comment="form.allowComment" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { CloseCircleOutline, CloudUploadOutline } from '@vicons/ionicons5'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import {
  createPost,
  getPost,
  listCategories,
  listTags,
  updatePost
} from '@/modules/blog/api'
import { uploadFile } from '@/modules/system/api/file'
import PostCommentPanel from './PostCommentPanel.vue'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const formRef = ref(null)
const mdEditorRef = ref(null)
const submitting = ref(false)
const uploading = ref(false)
// 正在上传的图片 token 集合：>0 时显示 loading，并拦截保存，避免占位符被存进库
const pendingImageUploads = ref(new Set())
const imageUploadingCount = computed(() => pendingImageUploads.value.size)
// 编辑会话标识：切换文章后旧的上传结果不再写回新正文
let editorSession = 0
const loading = ref(false)
const categoryOptions = ref([])
const tagOptions = ref([])

/**
 * 移动端屏幕窄，编辑区与预览区并排会导致两边都难以阅读，
 * 因此默认收起预览（仅编辑）；桌面端仍保持分屏预览。
 * 注意：md-editor-v3 的 preview 只在初始化时生效，后续由内部状态控制。
 */
const MOBILE_QUERY = '(max-width: 768px)'
const showPreview = !window.matchMedia(MOBILE_QUERY).matches
let mobileQuery = null

function handleViewportChange(event) {
  // 视口变窄时自动收起预览；恢复宽屏时不强制打开，尊重用户当前选择
  if (event.matches) mdEditorRef.value?.togglePreview(false)
}

const emptyForm = () => ({
  id: null,
  title: '',
  slug: '',
  summary: '',
  content: '',
  coverUrl: '',
  categoryId: null,
  tagIds: [],
  status: 0,
  isTop: 0,
  allowComment: 1,
  seoTitle: '',
  seoKeywords: '',
  seoDescription: ''
})

const form = reactive(emptyForm())

const formRules = {
  title: { required: true, message: '请输入标题', trigger: 'blur' },
  content: { required: true, message: '请输入正文', trigger: 'blur' }
}

/**
 * 编辑页由列表页 window.open 打开，保存/返回后应关闭当前标签；
 * 若是直接访问 URL（非脚本打开），close() 会被浏览器忽略，此时回退到列表页。
 */
function goBack() {
  window.close()
  setTimeout(() => router.replace('/blog/post'), 100)
}

function resetForm() {
  Object.assign(form, emptyForm())
}

async function loadOptions() {
  const [cats, tags] = await Promise.all([listCategories(), listTags()])
  categoryOptions.value = (cats.data || []).map((c) => ({ label: c.name, value: c.id }))
  tagOptions.value = (tags.data || []).map((t) => ({ label: t.name, value: t.id }))
}

async function loadDetail(id) {
  loading.value = true
  try {
    const res = await getPost(id)
    const p = res.data
    if (!p) {
      message.error('文章不存在')
      resetForm()
      return
    }
    Object.assign(form, {
      id: p.id,
      title: p.title || '',
      slug: p.slug || '',
      summary: p.summary || '',
      content: p.content || '',
      coverUrl: p.coverUrl || '',
      categoryId: p.categoryId ?? null,
      tagIds: Array.isArray(p.tagIds) ? [...p.tagIds] : [],
      status: p.status ?? 0,
      isTop: p.isTop ?? 0,
      allowComment: p.allowComment ?? 1,
      seoTitle: p.seoTitle || '',
      seoKeywords: p.seoKeywords || '',
      seoDescription: p.seoDescription || ''
    })
  } catch (e) {
    message.error(e?.message || '加载文章失败')
    resetForm()
  } finally {
    loading.value = false
  }
}

/** keep-alive 按 path 缓存，需按 query.id 主动回填 */
async function syncFromRoute() {
  // 切换文章即视为新的编辑会话，旧会话里未完成的上传结果直接丢弃
  editorSession += 1
  pendingImageUploads.value.clear()
  const raw = route.query.id
  const id = raw != null && raw !== '' ? Number(raw) : null
  if (id && !Number.isNaN(id)) {
    await loadDetail(id)
  } else {
    resetForm()
  }
}

async function handleCoverUpload({ file, onFinish, onError }) {
  uploading.value = true
  try {
    const res = await uploadFile(file.file, 'blog')
    form.coverUrl = res.data?.url || ''
    message.success('封面上传成功')
    onFinish()
  } catch (e) {
    onError()
  } finally {
    uploading.value = false
  }
}

/**
 * 编辑器内图片上传（粘贴 / 拖拽会走同一个 onUploadImg）
 *
 * 原生实现只在图片全部传完后才把 Markdown 插到「当前光标」，上传期间没有任何反馈，
 * 连续粘贴同一张图还会重复上传。这里做了四件事：
 * 1. 粘贴后立刻在光标处插入占位符，先给出可见的「上传中」反馈；
 * 2. 按图片内容去重：同一张图并发粘贴只发一次请求，传过的直接复用地址；
 * 3. 限制同时上传的数量，避免连续粘贴一批图片时瞬时压满上传接口；
 * 4. 上传结束（成功/失败）后把占位符原位替换成图片或清掉，并保证保存时不被占位符污染。
 */
const UPLOAD_PLACEHOLDER_ALT = '图片上传中…'
const MAX_CONCURRENT_IMAGE_UPLOADS = 2

// 图片内容指纹 → 已上传地址，同一编辑会话内复用
const uploadedImageCache = new Map()
// 图片内容指纹 → 上传中的 Promise，避免同一张图被重复上传
const uploadingImageTasks = new Map()

let runningImageUploads = 0
const waitingImageUploads = []

function acquireUploadSlot() {
  if (runningImageUploads < MAX_CONCURRENT_IMAGE_UPLOADS) {
    runningImageUploads += 1
    return Promise.resolve()
  }
  return new Promise((resolve) => waitingImageUploads.push(resolve))
}

function releaseUploadSlot() {
  const next = waitingImageUploads.shift()
  if (next) {
    // 直接把名额转交给排队者，运行数保持不变
    next()
  } else {
    runningImageUploads = Math.max(0, runningImageUploads - 1)
  }
}

/** 生成图片内容指纹；crypto.subtle 不可用（非安全上下文）时降级为采样哈希 */
async function getImageFingerprint(file) {
  const buffer = await file.arrayBuffer()
  if (globalThis.crypto?.subtle) {
    try {
      const digest = await crypto.subtle.digest('SHA-256', buffer)
      return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0')).join('')
    } catch (e) {
      // 继续走下面的降级方案
    }
  }
  const bytes = new Uint8Array(buffer)
  let hash = 0x811c9dc5
  const step = Math.max(1, Math.floor(bytes.length / 8192))
  for (let i = 0; i < bytes.length; i += step) {
    hash ^= bytes[i]
    hash = Math.imul(hash, 0x01000193) >>> 0
  }
  return `${file.size}-${file.type}-${hash.toString(16)}`
}

/** 上传一张图片；相同内容复用缓存或复用正在进行的请求 */
function uploadEditorImage(file) {
  return getImageFingerprint(file).then((fingerprint) => {
    const cached = uploadedImageCache.get(fingerprint)
    if (cached) return cached

    const pending = uploadingImageTasks.get(fingerprint)
    if (pending) return pending

    const task = acquireUploadSlot()
      .then(() => uploadFile(file, 'blog'))
      .then((res) => {
        const url = res.data?.url || ''
        if (!url) throw new Error('上传接口未返回图片地址')
        uploadedImageCache.set(fingerprint, url)
        return url
      })
      .finally(() => {
        uploadingImageTasks.delete(fingerprint)
        releaseUploadSlot()
      })

    uploadingImageTasks.set(fingerprint, task)
    return task
  })
}

function getEditorView() {
  return mdEditorRef.value?.getEditorView?.()
}

/** 在光标处插入文本，返回是否成功 */
function insertEditorText(text) {
  const view = getEditorView()
  if (!view) return false
  const { from, to } = view.state.selection.main
  view.dispatch({
    changes: { from, to, insert: text },
    selection: { anchor: from + text.length }
  })
  return true
}

/** 把文档中所有目标占位符替换为给定文本，返回是否命中 */
function replaceEditorText(target, replacement) {
  const view = getEditorView()
  if (!view) return false
  const content = view.state.doc.toString()
  const changes = []
  let index = content.indexOf(target)
  while (index !== -1) {
    changes.push({ from: index, to: index + target.length, insert: replacement })
    index = content.indexOf(target, index + target.length)
  }
  if (!changes.length) return false
  view.dispatch({ changes })
  return true
}

async function handleEditorUploadImg(files, callback) {
  const images = (Array.isArray(files) ? files : []).filter(
    (file) => file && /^image\//.test(file.type || '')
  )
  if (!images.length) return

  // 上传是异步的，切换文章后不要再往新正文里写结果
  const session = editorSession
  const isCurrentSession = () => session === editorSession

  await Promise.all(
    images.map(async (file) => {
      const token = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`
      const placeholder = `![${UPLOAD_PLACEHOLDER_ALT}](uploading:${token})\n`
      pendingImageUploads.value.add(token)
      const inserted = insertEditorText(placeholder)

      try {
        const url = await uploadEditorImage(file)
        if (!isCurrentSession()) return
        const markdown = `![](${url})\n`
        // 占位符可能被撤销/手动删除，兜底交给编辑器回调按光标插入
        if (!inserted || !replaceEditorText(placeholder, markdown)) {
          callback?.([url])
        }
      } catch (e) {
        // 失败提示由 axios 拦截器统一弹出，这里只清理占位符
        if (isCurrentSession()) replaceEditorText(placeholder, '')
      } finally {
        pendingImageUploads.value.delete(token)
      }
    })
  )
}

async function handleSave(status) {
  if (imageUploadingCount.value > 0) {
    message.warning('图片正在上传，请等待上传完成后再保存')
    return
  }
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = {
      ...form,
      status,
      tagIds: form.tagIds || []
    }
    if (form.id) {
      await updatePost(payload)
      message.success(status === 1 ? '已发布' : '草稿已保存')
    } else {
      await createPost(payload)
      message.success(status === 1 ? '已发布' : '草稿已保存')
    }
    goBack()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  mobileQuery = window.matchMedia(MOBILE_QUERY)
  mobileQuery.addEventListener('change', handleViewportChange)
  await loadOptions()
  await syncFromRoute()
})

onBeforeUnmount(() => {
  mobileQuery?.removeEventListener('change', handleViewportChange)
})

watch(
  () => route.query.id,
  () => {
    syncFromRoute()
  }
)
</script>

<style scoped>
/* 独立页（不走 RootLayout）：自行铺满视口，并留出比列表页更宽松的边距 */
.post-edit {
  min-height: 100vh;
  padding: 16px 20px 24px;
}

.edit-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--n-border-color);
  gap: 12px;
  flex-wrap: wrap;
}
.md-wrap {
  position: relative;
  width: 100%;
  min-height: 420px;
}
.md-upload-tip {
  position: absolute;
  right: 12px;
  bottom: 12px;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  color: var(--n-text-color-2);
  background: var(--n-color-popover, #fff);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  /* 不要挡住编辑与预览的操作 */
  pointer-events: none;
}
.md-wrap :deep(.md-editor) {
  height: 480px;
}
.cover-upload {
  display: inline-block;
}
.cover-upload :deep(.n-upload-trigger) {
  display: block;
}
.cover-uploader {
  position: relative;
  width: 240px;
  height: 135px;
  overflow: hidden;
  border-radius: 6px;
  border: 1px dashed var(--n-border-color);
  background: var(--n-color-embedded);
  cursor: pointer;
  transition: border-color 0.2s;
}
.cover-uploader:hover {
  border-color: var(--n-primary-color);
}
.cover-img {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}
.cover-empty,
.cover-hover,
.cover-loading {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
}
.cover-empty {
  color: var(--n-text-color-3);
}
.cover-hover {
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.2s;
}
.cover-uploader:hover .cover-hover {
  opacity: 1;
}
.cover-loading {
  background: rgba(255, 255, 255, 0.65);
}
.cover-clear {
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 50%;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s, background 0.2s;
}
.cover-uploader:hover .cover-clear {
  opacity: 1;
}
.cover-clear:hover {
  background: rgba(208, 48, 80, 0.9);
}
</style>
