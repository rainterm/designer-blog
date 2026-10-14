<template>
  <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" @submit.prevent="onSubmit">
    <div v-if="replyTo" class="reply-hint">
      回复 <strong>{{ replyTo.nickname }}</strong>
      <button type="button" @click="$emit('cancel-reply')">取消</button>
    </div>
    <n-grid :cols="2" :x-gap="12">
      <n-gi>
        <n-form-item label="昵称" path="nickname">
          <n-input v-model:value="form.nickname" maxlength="50" placeholder="怎么称呼你" />
        </n-form-item>
      </n-gi>
      <n-gi>
        <n-form-item label="邮箱（可选）" path="email">
          <n-input v-model:value="form.email" maxlength="100" placeholder="不会公开展示" />
        </n-form-item>
      </n-gi>
    </n-grid>
    <n-form-item label="评论" path="content">
      <n-input
        v-model:value="form.content"
        type="textarea"
        :rows="4"
        maxlength="2000"
        show-count
        placeholder="说点什么…"
      />
    </n-form-item>
    <n-form-item label="验证码" path="captchaCode">
      <div class="captcha-row">
        <n-input
          v-model:value="form.captchaCode"
          class="captcha-row__input"
          maxlength="8"
          placeholder="填写计算结果"
        />
        <button
          type="button"
          class="captcha-row__img"
          title="点击刷新"
          aria-label="刷新验证码"
          @click="loadCaptcha"
        >
          <img v-if="captchaImg" :src="captchaImg" alt="验证码" />
          <span v-else>加载中</span>
        </button>
      </div>
    </n-form-item>
    <n-button type="primary" attr-type="submit" :loading="loading">
      提交评论
    </n-button>
  </n-form>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { fetchCommentCaptcha, submitComment } from '@/api/blog'
import { getCommenterKey } from '@/utils/commenterKey'

const props = defineProps({
  postId: { type: [Number, String], required: true },
  replyTo: { type: Object, default: null }
})

const emit = defineEmits(['success', 'cancel-reply'])

const message = useMessage()
const formRef = ref(null)
const loading = ref(false)
/** 验证码：uuid 与图片一起刷新，提交时回传 uuid */
const captchaUuid = ref('')
const captchaImg = ref('')

const form = reactive({
  nickname: '',
  email: '',
  content: '',
  captchaCode: ''
})

const rules = {
  nickname: { required: true, message: '请输入昵称', trigger: 'blur' },
  content: { required: true, message: '请输入评论内容', trigger: 'blur' },
  captchaCode: { required: true, message: '请输入验证码', trigger: 'blur' }
}

/** 拉取（或刷新）验证码；失败时清空，避免拿旧 uuid 提交 */
async function loadCaptcha() {
  form.captchaCode = ''
  captchaUuid.value = ''
  captchaImg.value = ''
  try {
    const res = await fetchCommentCaptcha()
    captchaUuid.value = res.data?.uuid || ''
    captchaImg.value = res.data?.img || ''
  } catch {
    /* 失败提示已由请求层统一弹出 */
  }
}

async function onSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await submitComment({
      postId: Number(props.postId),
      parentId: props.replyTo?.id || null,
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      content: form.content.trim(),
      captchaUuid: captchaUuid.value,
      captchaCode: form.captchaCode.trim(),
      // 让评论人能查看 / 删除自己刚提交的评论
      commenterKey: getCommenterKey()
    })
    message.success('评论已提交，审核通过后将显示')
    form.content = ''
    emit('success')
    emit('cancel-reply')
  } catch {
    /* 失败原因（验证码错 / 太频繁 / 超限）由请求层统一提示 */
  } finally {
    // 验证码是一次性的：无论成功失败都换一张
    loadCaptcha()
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.reply-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.reply-hint button {
  border: none;
  background: none;
  color: var(--color-primary);
  cursor: pointer;
  font-size: 13px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-row__input {
  max-width: 180px;
}

.captcha-row__img {
  flex-shrink: 0;
  width: 120px;
  height: 40px;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-body);
  cursor: pointer;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  font-size: 12px;
  font-family: inherit;
}

.captcha-row__img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-row__img:hover {
  border-color: var(--color-primary);
}
</style>
