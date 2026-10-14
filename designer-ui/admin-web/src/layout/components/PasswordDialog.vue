<template>
  <Dialog
    :show="show"
    title="修改密码"
    confirm-text="确认修改"
    :loading="saving"
    @update:show="emit('update:show', $event)"
    @confirm="handleConfirm"
  >
    <n-alert type="info" :bordered="false" class="password-dialog__alert">
      修改密码后当前登录状态将失效，需要重新登录。
    </n-alert>

    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      label-width="90"
      class="password-dialog__form"
    >
      <n-form-item label="原密码" path="oldPassword">
        <n-input
          v-model:value="form.oldPassword"
          type="password"
          show-password-on="click"
          placeholder="请输入当前密码"
        />
      </n-form-item>
      <n-form-item label="新密码" path="newPassword">
        <n-input
          v-model:value="form.newPassword"
          type="password"
          show-password-on="click"
          placeholder="请输入新密码"
        />
      </n-form-item>
      <n-form-item label="确认密码" path="confirmPassword">
        <n-input
          v-model:value="form.confirmPassword"
          type="password"
          show-password-on="click"
          placeholder="请再次输入新密码"
        />
      </n-form-item>
    </n-form>
  </Dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { useRouter } from 'vue-router'
import { changePassword } from '@/api/auth'
import { useUserStore } from '@/store/user'

const props = defineProps({
  show: { type: Boolean, default: false }
})
const emit = defineEmits(['update:show'])

const message = useMessage()
const router = useRouter()
const userStore = useUserStore()

const saving = ref(false)
const formRef = ref(null)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: ['input', 'blur'] }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: ['input', 'blur'] },
    { min: 6, max: 50, message: '密码长度需在 6~50 之间', trigger: ['input', 'blur'] }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: ['input', 'blur'] },
    {
      validator: (rule, value) => value === form.newPassword,
      message: '两次输入的密码不一致',
      trigger: ['input', 'blur']
    }
  ]
}

async function handleConfirm() {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    emit('update:show', false)
    message.success('密码修改成功，请重新登录')
    await userStore.logout()
    router.replace('/login')
  } finally {
    saving.value = false
  }
}

// 每次打开清空表单与校验状态，避免残留上一次的输入
watch(
  () => props.show,
  (visible) => {
    if (!visible) return
    Object.assign(form, { oldPassword: '', newPassword: '', confirmPassword: '' })
    formRef.value?.restoreValidation()
  }
)
</script>

<style scoped>
.password-dialog__alert {
  margin-bottom: 12px;
}

.password-dialog__form {
  padding-top: 4px;
}
</style>
