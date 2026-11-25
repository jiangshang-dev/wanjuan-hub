<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { message } from 'ant-design-vue'
import { authApi } from '../api'
import { errorText } from '../utils/format'

const props = defineProps({
  value: { type: String, default: '' },
  scene: { type: String, required: true },
  type: { type: String, default: 'email' },
  contact: { type: String, default: '' },
  captchaId: { type: String, default: '' },
  captchaCode: { type: String, default: '' },
})
const emit = defineEmits(['update:value', 'captcha-consumed'])
const sending = ref(false)
const countdown = ref(0)
let timer

function validateTarget() {
  if (props.type === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(props.contact)) {
    message.warning('请先输入正确的邮箱地址')
    return false
  }
  if (props.type === 'phone' && !/^1\d{10}$/.test(props.contact)) {
    message.warning('请先输入正确的手机号')
    return false
  }
  if (!props.captchaId || !props.captchaCode.trim()) {
    message.warning('请先输入图形验证码')
    return false
  }
  return true
}

async function send() {
  if (!validateTarget() || sending.value || countdown.value) return
  sending.value = true
  const payload = {
    scene: props.scene,
    captchaId: props.captchaId,
    captchaCode: props.captchaCode.trim(),
  }
  payload[props.type === 'phone' ? 'phone' : 'email'] = props.contact.trim()
  try {
    const api = props.type === 'phone' ? authApi.sendSmsCode : authApi.sendEmailCode
    await api(payload)
    message.success(props.type === 'phone' ? '短信验证码已发送' : '邮箱验证码已发送')
    countdown.value = 60
    timer = window.setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) window.clearInterval(timer)
    }, 1000)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    sending.value = false
    emit('captcha-consumed')
  }
}

onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <div class="code-input-row">
    <a-input
      :value="value"
      :maxlength="6"
      :placeholder="type === 'phone' ? '请输入短信验证码' : '请输入邮箱验证码'"
      @update:value="emit('update:value', $event)"
    />
    <a-button class="code-button" :loading="sending" :disabled="countdown > 0" @click="send">
      {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
    </a-button>
  </div>
</template>
