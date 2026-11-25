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
const agreementType = ref('')
const form = reactive({ username: '', email: '', password: '', confirmPassword: '', emailCode: '', agreementAccepted: false, captchaId: '', captchaCode: '' })
const rules = {
  username: [
    { required: true, whitespace: true, message: '请输入用户名' },
    { pattern: /^[\w-]{3,32}$/, message: '用户名为3-32位字母、数字、下划线或短横线' },
  ],
  email: [{ required: true, type: 'email', message: '请输入正确的邮箱地址' }],
  password: [
    { required: true, message: '请设置密码' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{8,32}$/, message: '密码需为8-32位且包含字母和数字' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码' },
    { validator: (_, value) => value === form.password ? Promise.resolve() : Promise.reject(new Error('两次输入的密码不一致')) },
  ],
  emailCode: [{ required: true, pattern: /^\d{4,8}$/, message: '请输入正确的邮箱验证码' }],
  captchaCode: [{ required: true, whitespace: true, message: '请输入图形验证码' }],
  agreementAccepted: [{ validator: (_, value) => value ? Promise.resolve() : Promise.reject(new Error('请阅读并同意用户协议和隐私政策')) }],
}

function refreshCaptcha() {
  form.captchaCode = ''
  nextTick(() => captchaRef.value?.refresh())
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await authApi.register({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      emailCode: form.emailCode.trim(),
      agreementAccepted: form.agreementAccepted,
    })
    message.success('注册成功，请登录')
    router.replace({ name: 'login', query: { account: form.email.trim() } })
  } catch (error) {
    message.error(errorText(error))
    refreshCaptcha()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthShell title="创建账号" subtitle="注册一个属于你的万卷社区账号">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" required-mark="optional" @finish="submit">
      <a-form-item label="用户名" name="username">
        <a-input v-model:value="form.username" size="large" autocomplete="username" placeholder="3-32位字母、数字、下划线或短横线" />
      </a-form-item>
      <a-form-item label="邮箱地址" name="email">
        <a-input v-model:value="form.email" size="large" autocomplete="email" placeholder="name@example.com" />
      </a-form-item>
      <a-form-item label="设置密码" name="password">
        <a-input-password v-model:value="form.password" size="large" autocomplete="new-password" placeholder="8-32位，包含字母和数字" />
      </a-form-item>
      <a-form-item label="确认密码" name="confirmPassword">
        <a-input-password v-model:value="form.confirmPassword" size="large" autocomplete="new-password" placeholder="请再次输入密码" />
      </a-form-item>
      <a-form-item label="图形验证码" name="captchaCode">
        <div class="captcha-row">
          <a-input v-model:value="form.captchaCode" size="large" :maxlength="8" placeholder="请输入图中字符" />
          <CaptchaImage ref="captchaRef" v-model:captcha-id="form.captchaId" />
        </div>
      </a-form-item>
      <a-form-item label="邮箱验证码" name="emailCode">
        <CodeInput v-model:value="form.emailCode" scene="REGISTER" :contact="form.email" :captcha-id="form.captchaId" :captcha-code="form.captchaCode" @captcha-consumed="refreshCaptcha" />
      </a-form-item>
      <a-form-item name="agreementAccepted">
        <a-checkbox v-model:checked="form.agreementAccepted">
          我已阅读并同意<a @click.prevent="agreementType = 'user'">《用户协议》</a>和<a @click.prevent="agreementType = 'privacy'">《隐私政策》</a>
        </a-checkbox>
      </a-form-item>
      <a-button type="primary" html-type="submit" size="large" block :loading="submitting">注册</a-button>
      <p class="form-footer">已有账号？<router-link to="/login">返回登录</router-link></p>
    </a-form>
    <a-modal :open="Boolean(agreementType)" :title="agreementType === 'user' ? '用户协议' : '隐私政策'" :footer="null" @cancel="agreementType = ''">
      <div v-if="agreementType === 'user'" class="agreement-content">
        <p>注册即表示你承诺遵守法律法规和社区规范，仅提交自己有权发布的内容，并妥善保管账号及验证码。</p>
        <p>请勿利用社区从事违法、侵权、骚扰或破坏服务安全的行为。发现异常使用时，请立即修改密码。</p>
      </div>
      <div v-else class="agreement-content">
        <p>我们仅为账号注册、登录、安全校验及资料维护处理你主动提供的用户名、邮箱、手机号和资料信息。</p>
        <p>验证码仅用于对应操作场景。你可以通过个人资料和账号安全页面维护或更新相关信息。</p>
      </div>
    </a-modal>
  </AuthShell>
</template>
