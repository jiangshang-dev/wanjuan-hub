<script setup>
import { nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import AuthShell from '../../components/AuthShell.vue'
import CaptchaImage from '../../components/CaptchaImage.vue'
import CodeInput from '../../components/CodeInput.vue'
import { authApi } from '../../api'
import { errorText } from '../../utils/format'

const router = useRouter()
const formRef = ref()
const captchaRef = ref()
const submitting = ref(false)
const form = reactive({ email: '', emailCode: '', newPassword: '', confirmPassword: '', captchaId: '', captchaCode: '' })
const rules = {
  email: [{ required: true, type: 'email', message: '请输入正确的邮箱地址' }],
  emailCode: [{ required: true, pattern: /^\d{4,8}$/, message: '请输入正确的邮箱验证码' }],
  newPassword: [
    { required: true, message: '请设置新密码' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{8,32}$/, message: '密码需为8-32位且包含字母和数字' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码' },
    { validator: (_, value) => value === form.newPassword ? Promise.resolve() : Promise.reject(new Error('两次输入的密码不一致')) },
  ],
  captchaCode: [{ required: true, whitespace: true, message: '请输入图形验证码' }],
}

function refreshCaptcha() {
  form.captchaCode = ''
  nextTick(() => captchaRef.value?.refresh())
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await authApi.resetPassword({ email: form.email.trim(), emailCode: form.emailCode.trim(), newPassword: form.newPassword })
    message.success('密码重置成功，请使用新密码登录')
    router.replace('/login')
  } catch (error) {
    message.error(errorText(error))
    refreshCaptcha()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthShell title="重置密码" subtitle="通过注册邮箱验证身份后设置新密码">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" required-mark="optional" @finish="submit">
      <a-form-item label="注册邮箱" name="email">
        <a-input v-model:value="form.email" size="large" autocomplete="email" placeholder="name@example.com" />
      </a-form-item>
      <a-form-item label="图形验证码" name="captchaCode">
        <div class="captcha-row">
          <a-input v-model:value="form.captchaCode" size="large" :maxlength="8" placeholder="请输入图中字符" />
          <CaptchaImage ref="captchaRef" v-model:captcha-id="form.captchaId" />
        </div>
      </a-form-item>
      <a-form-item label="邮箱验证码" name="emailCode">
        <CodeInput v-model:value="form.emailCode" scene="RESET" :contact="form.email" :captcha-id="form.captchaId" :captcha-code="form.captchaCode" @captcha-consumed="refreshCaptcha" />
      </a-form-item>
      <a-form-item label="新密码" name="newPassword">
        <a-input-password v-model:value="form.newPassword" size="large" autocomplete="new-password" placeholder="8-32位，包含字母和数字" />
      </a-form-item>
      <a-form-item label="确认新密码" name="confirmPassword">
        <a-input-password v-model:value="form.confirmPassword" size="large" autocomplete="new-password" placeholder="请再次输入新密码" />
      </a-form-item>
      <a-button type="primary" html-type="submit" size="large" block :loading="submitting">重置密码</a-button>
      <p class="form-footer">想起密码了？<router-link to="/login">返回登录</router-link></p>
    </a-form>
  </AuthShell>
</template>
