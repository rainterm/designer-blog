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
              v-model="form.content"
              language="zh-CN"
              :preview="true"
              @onUploadImg="handleEditorUploadImg"
            />
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
import { onMounted, reactive, ref, watch } from 'vue'
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
const submitting = ref(false)
const uploading = ref(false)
const loading = ref(false)
const categoryOptions = ref([])
const tagOptions = ref([])

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

/** 编辑器内图片上传 */
async function handleEditorUploadImg(files, callback) {
  const urls = []
  for (const file of files) {
    const res = await uploadFile(file, 'blog')
    const url = res.data?.url || ''
    if (url) urls.push(url)
  }
  callback(urls)
}

async function handleSave(status) {
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
  await loadOptions()
  await syncFromRoute()
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
  width: 100%;
  min-height: 420px;
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
