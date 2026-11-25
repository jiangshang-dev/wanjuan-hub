<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import CaptchaImage from '../../components/CaptchaImage.vue'
import CodeInput from '../../components/CodeInput.vue'
import { userApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import { errorText } from '../../utils/format'

const auth = useAuthStore()
const passwordRef = ref()
const emailRef = ref()
const phoneRef = ref()
const emailCaptchaRef = ref()
const phoneCaptchaRef = ref()
const passwordSaving = ref(false)
const emailSaving = ref(false)
const phoneSaving = ref(false)
const loading = ref(true)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const emailForm = reactive({ email: '', emailCode: '', captchaId: '', captchaCode: '' })
const phoneForm = reactive({ phone: '', smsCode: '', captchaId: '', captchaCode: '' })
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{8,32}$/, message: '密码需为8-32位且包含字母和数字' },
    { validator: (_, value) => value !== passwordForm.oldPassword ? Promise.resolve() : Promise.reject(new Error('新密码不能与当前密码相同')) },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码' },
    { validator: (_, value) => value === passwordForm.newPassword ? Promise.resolve() : Promise.reject(new Error('两次输入的密码不一致')) },
  ],
}
const captchaRules = [{ required: true, whitespace: true, message: '请输入图形验证码' }]
const codeRules = [{ required: true, pattern: /^\d{4,8}$/, message: '请输入正确的验证码' }]

onMounted(async () => {
  try {
    await auth.loadUser(true)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
})

async function changePassword() {
  await passwordRef.value.validate()
  passwordSaving.value = true
  try {
    await userApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    message.success('密码修改成功')
    passwordRef.value.resetFields()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    passwordSaving.value = false
  }
}

function refreshCaptcha(type) {
  const form = type === 'email' ? emailForm : phoneForm
  form.captchaCode = ''
  nextTick(() => (type === 'email' ? emailCaptchaRef : phoneCaptchaRef).value?.refresh())
}

async function bindEmail() {
  await emailRef.value.validate()
  emailSaving.value = true
  try {
    await userApi.bindEmail({ email: emailForm.email.trim(), emailCode: emailForm.emailCode.trim() })
    message.success('邮箱绑定成功')
    emailRef.value.resetFields()
    await auth.loadUser(true)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    emailSaving.value = false
    refreshCaptcha('email')
  }
}

async function bindPhone() {
  await phoneRef.value.validate()
  phoneSaving.value = true
  try {
    await userApi.bindPhone({ phone: phoneForm.phone.trim(), smsCode: phoneForm.smsCode.trim() })
    message.success('手机号绑定成功')
    phoneRef.value.resetFields()
    await auth.loadUser(true)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    phoneSaving.value = false
    refreshCaptcha('phone')
  }
}
</script>

<template>
  <div class="content-page security-page">
    <div class="page-heading"><div><span class="eyebrow">SECURITY</span><h1>账号安全</h1><p>定期更新密码，并保持绑定信息准确可用。</p></div></div>
    <a-spin :spinning="loading">
      <div class="security-status content-card">
        <div><span class="status-label">当前账号</span><strong>{{ auth.user?.username || auth.user?.account || '-' }}</strong></div>
        <div><span class="status-label">绑定邮箱</span><strong>{{ auth.user?.email || '未绑定' }}</strong></div>
        <div><span class="status-label">绑定手机</span><strong>{{ auth.user?.phone || '未绑定' }}</strong></div>
      </div>
      <a-card :bordered="false" class="content-card security-card" title="修改密码">
        <a-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" layout="vertical" class="security-form" @finish="changePassword">
          <a-form-item label="当前密码" name="oldPassword"><a-input-password v-model:value="passwordForm.oldPassword" autocomplete="current-password" /></a-form-item>
          <div class="form-grid-two">
            <a-form-item label="新密码" name="newPassword"><a-input-password v-model:value="passwordForm.newPassword" autocomplete="new-password" placeholder="8-32位，包含字母和数字" /></a-form-item>
            <a-form-item label="确认新密码" name="confirmPassword"><a-input-password v-model:value="passwordForm.confirmPassword" autocomplete="new-password" /></a-form-item>
          </div>
          <a-button type="primary" html-type="submit" :loading="passwordSaving">确认修改</a-button>
        </a-form>
      </a-card>
      <div class="binding-grid">
        <a-card :bordered="false" class="content-card" title="绑定 / 更换邮箱">
          <a-form ref="emailRef" :model="emailForm" layout="vertical" @finish="bindEmail">
            <a-form-item label="邮箱地址" name="email" :rules="[{ required: true, type: 'email', message: '请输入正确的邮箱地址' }]"><a-input v-model:value="emailForm.email" autocomplete="email" /></a-form-item>
            <a-form-item label="图形验证码" name="captchaCode" :rules="captchaRules"><div class="captcha-row"><a-input v-model:value="emailForm.captchaCode" /><CaptchaImage ref="emailCaptchaRef" v-model:captcha-id="emailForm.captchaId" /></div></a-form-item>
            <a-form-item label="邮箱验证码" name="emailCode" :rules="codeRules"><CodeInput v-model:value="emailForm.emailCode" scene="BIND" :contact="emailForm.email" :captcha-id="emailForm.captchaId" :captcha-code="emailForm.captchaCode" @captcha-consumed="refreshCaptcha('email')" /></a-form-item>
            <a-button type="primary" html-type="submit" :loading="emailSaving">确认绑定</a-button>
          </a-form>
        </a-card>
        <a-card :bordered="false" class="content-card" title="绑定 / 更换手机号">
          <a-form ref="phoneRef" :model="phoneForm" layout="vertical" @finish="bindPhone">
            <a-form-item label="手机号" name="phone" :rules="[{ required: true, pattern: /^1\d{10}$/, message: '请输入正确的11位手机号' }]"><a-input v-model:value="phoneForm.phone" autocomplete="tel" :maxlength="11" /></a-form-item>
            <a-form-item label="图形验证码" name="captchaCode" :rules="captchaRules"><div class="captcha-row"><a-input v-model:value="phoneForm.captchaCode" /><CaptchaImage ref="phoneCaptchaRef" v-model:captcha-id="phoneForm.captchaId" /></div></a-form-item>
            <a-form-item label="短信验证码" name="smsCode" :rules="codeRules"><CodeInput v-model:value="phoneForm.smsCode" type="phone" scene="BIND" :contact="phoneForm.phone" :captcha-id="phoneForm.captchaId" :captcha-code="phoneForm.captchaCode" @captcha-consumed="refreshCaptcha('phone')" /></a-form-item>
            <a-button type="primary" html-type="submit" :loading="phoneSaving">确认绑定</a-button>
          </a-form>
        </a-card>
      </div>
    </a-spin>
  </div>
</template>
