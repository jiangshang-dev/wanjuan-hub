<script setup>
import { nextTick, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import AuthShell from '../../components/AuthShell.vue'
import CaptchaImage from '../../components/CaptchaImage.vue'
import CodeInput from '../../components/CodeInput.vue'
import { authApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import { errorText } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const captchaRef = ref()
const mode = ref('account')
const submitting = ref(false)
const form = reactive({ account: typeof route.query.account === 'string' ? route.query.account : '', password: '', email: '', emailCode: '', phone: '', smsCode: '', captchaId: '', captchaCode: '' })
const rules = {
  account: [{ required: true, whitespace: true, message: '请输入用户名或邮箱' }],
  password: [{ required: true, message: '请输入密码' }],
  email: [{ required: true, type: 'email', message: '请输入正确的邮箱地址' }],
  emailCode: [{ required: true, pattern: /^\d{4,8}$/, message: '请输入正确的邮箱验证码' }],
  phone: [{ required: true, pattern: /^1\d{10}$/, message: '请输入正确的手机号' }],
  smsCode: [{ required: true, pattern: /^\d{4,8}$/, message: '请输入正确的短信验证码' }],
  captchaCode: [{ required: true, whitespace: true, message: '请输入图形验证码' }],
}

function refreshCaptcha() {
  form.captchaCode = ''
  nextTick(() => captchaRef.value?.refresh())
}

function changeMode() {
  formRef.value?.clearValidate()
  refreshCaptcha()
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    let call
    if (mode.value === 'account') {
      call = () => authApi.login({ account: form.account.trim(), password: form.password, captchaId: form.captchaId, captchaCode: form.captchaCode.trim() })
    } else if (mode.value === 'email') {
      call = () => authApi.loginByEmail({ email: form.email.trim(), emailCode: form.emailCode.trim(), captchaId: form.captchaId, captchaCode: form.captchaCode.trim() })
    } else {
      call = () => authApi.loginByPhone({ phone: form.phone.trim(), smsCode: form.smsCode.trim(), captchaId: form.captchaId, captchaCode: form.captchaCode.trim() })
    }
    await auth.authenticate(call)
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') && !route.query.redirect.startsWith('//') ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (error) {
    message.error(errorText(error))
    refreshCaptcha()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthShell title="欢迎回来" subtitle="登录后继续你的学习与交流">
    <a-tabs v-model:active-key="mode" centered @change="changeMode">
      <a-tab-pane key="account" tab="账号密码" />
      <a-tab-pane key="email" tab="邮箱验证码" />
      <a-tab-pane key="phone" tab="手机验证码" />
    </a-tabs>
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" required-mark="optional" @finish="submit">
      <template v-if="mode === 'account'">
        <a-form-item label="用户名或邮箱" name="account">
          <a-input v-model:value="form.account" size="large" autocomplete="username" placeholder="请输入用户名或邮箱" @press-enter="submit" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="form.password" size="large" autocomplete="current-password" placeholder="请输入密码" @press-enter="submit" />
        </a-form-item>
      </template>
      <template v-else-if="mode === 'email'">
        <a-form-item label="邮箱地址" name="email">
          <a-input v-model:value="form.email" size="large" autocomplete="email" placeholder="name@example.com" />
        </a-form-item>
        <a-form-item label="邮箱验证码" name="emailCode">
          <CodeInput v-model:value="form.emailCode" scene="LOGIN" :contact="form.email" :captcha-id="form.captchaId" :captcha-code="form.captchaCode" @captcha-consumed="refreshCaptcha" />
        </a-form-item>
      </template>
      <template v-else>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="form.phone" size="large" autocomplete="tel" :maxlength="11" placeholder="请输入11位手机号" />
        </a-form-item>
        <a-form-item label="短信验证码" name="smsCode">
          <CodeInput v-model:value="form.smsCode" type="phone" scene="LOGIN" :contact="form.phone" :captcha-id="form.captchaId" :captcha-code="form.captchaCode" @captcha-consumed="refreshCaptcha" />
        </a-form-item>
      </template>
      <a-form-item label="图形验证码" name="captchaCode">
        <div class="captcha-row">
          <a-input v-model:value="form.captchaCode" size="large" :maxlength="8" placeholder="请输入图中字符" @press-enter="submit" />
          <CaptchaImage ref="captchaRef" v-model:captcha-id="form.captchaId" />
        </div>
      </a-form-item>
      <div class="form-between">
        <span>没有账号？<router-link to="/register">立即注册</router-link></span>
        <router-link to="/reset-password">忘记密码？</router-link>
      </div>
      <a-button type="primary" html-type="submit" size="large" block :loading="submitting">登录</a-button>
    </a-form>
  </AuthShell>
</template>
