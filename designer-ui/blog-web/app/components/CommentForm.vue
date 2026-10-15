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
    <n-form-item v-if="captchaVisible" label="安全验证">
      <SliderCaptcha ref="captchaRef" :api="captchaApi" @verified="onVerified" />
    </n-form-item>
    <n-button type="primary" attr-type="submit" :loading="submitting">提交评论</n-button>
  </n-form>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { SliderCaptcha } from '@captcha'
import { submitComment } from '@/api/blog'
import { CODE } from '@/api/request'
import { getCommenterKey } from '@/utils/commenterKey'
import { matchReservedNickname } from '@/utils/nickname'
import { captchaApi } from '@/utils/captchaApi'
import { useSiteSetting } from '@/composables/useSiteSetting'

const props = defineProps({
  postId: { type: [Number, String], required: true },
  replyTo: { type: Object, default: null }
})

const emit = defineEmits(['success', 'cancel-reply'])

const message = useMessage()
/**
 * 保留昵称名单：由后端 `/site` 接口下发（`site.reservedNicknames`），
 * 与后端实际拦截同源。前端只比对、不自己维护名单，避免两边漂移。
 */
const site = useSiteSetting()
const reservedNicknames = computed(() => site.value.reservedNicknames || [])
const formRef = ref(null)
const captchaRef = ref(null)
const submitting = ref(false)

/**
 * 滑块面板默认不出现在页面上（也就不请求、不占版面）。
 * 首次提交时先校验昵称/内容，通过了才展开 —— 和原来「验证码按需展示」的取舍一致。
 */
const captchaVisible = ref(false)

/**
 * /captcha/check 下发的二次校验凭据。**一次性**：被服务端消费后必须重新拖滑块，
 * 所以每次提交失败（除限流外）都要清掉它。
 */
const captchaVerification = ref('')

const form = reactive({
  nickname: '',
  email: '',
  content: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    {
      // 保留昵称（后台可配 + 站长署名）由后端下发，规则也是「包含」
      validator: (_rule, value) => {
        const hit = matchReservedNickname(value, reservedNicknames.value)
        // 返回 Error 而不是布尔：naive-ui 会用它渲染提示，从而带上命中的具体词
        return hit ? new Error(`昵称不能包含保留词「${hit}」`) : true
      },
      trigger: ['blur', 'input']
    }
  ],
  content: { required: true, message: '请输入评论内容', trigger: 'blur' }
}

async function onSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  // 还没拖滑块：展开面板，等 SliderCaptcha 验证通过后自动提交（见 onVerified）
  if (!captchaVerification.value) {
    if (!captchaVisible.value) {
      captchaVisible.value = true
      message.info('请拖动滑块完成验证')
    }
    return
  }

  await doSubmit()
}

/** 滑块对齐成功，拿到凭据后直接提交，省掉一次多余的点击 */
async function onVerified(credential) {
  captchaVerification.value = credential
  await doSubmit()
}

async function doSubmit() {
  if (submitting.value) return
  submitting.value = true
  try {
    await submitComment({
      postId: Number(props.postId),
      parentId: props.replyTo?.id || null,
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      content: form.content.trim(),
      captchaVerification: captchaVerification.value,
      // 让评论人能查看 / 删除自己刚提交的评论
      commenterKey: getCommenterKey()
    })
    message.success('评论已提交，审核通过后将显示')
    form.content = ''
    resetCaptcha()
    emit('success')
    emit('cancel-reply')
  } catch (error) {
    // 失败原因（验证码错 / 太频繁 / 超限）由请求层统一提示。
    // 429 是限流在「进入业务代码之前」就拦下了这次提交，凭据根本没被服务端消费，
    // 手上这张仍然有效 —— 清掉会逼用户白拖一次，还会多消耗一次验证码额度。
    if (error?.code !== CODE.TOO_MANY_REQUESTS) {
      captchaVerification.value = ''
      // 面板已经展开就原地换一张（SliderCaptcha 会顺带清掉「验证通过」状态），
      // 用户直接重拖即可，不必再点一次提交
      if (captchaVisible.value) {
        captchaRef.value?.refresh()
      }
    }
  } finally {
    submitting.value = false
  }
}

function resetCaptcha() {
  captchaVerification.value = ''
  captchaVisible.value = false
}
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
</style>
