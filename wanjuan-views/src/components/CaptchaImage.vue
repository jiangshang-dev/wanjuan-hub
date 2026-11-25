<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { authApi } from '../api'
import { errorText } from '../utils/format'

const emit = defineEmits(['update:captchaId', 'loaded'])
const image = ref('')
const loading = ref(false)
const failed = ref(false)
const imageSrc = computed(() => {
  if (!image.value) return ''
  return /^(data:|https?:|blob:)/.test(image.value) ? image.value : `data:image/png;base64,${image.value}`
})

async function refresh() {
  loading.value = true
  failed.value = false
  emit('update:captchaId', '')
  try {
    const data = await authApi.captcha()
    image.value = data?.image || ''
    emit('update:captchaId', data?.captchaId || '')
    if (!image.value) failed.value = true
    emit('loaded', data)
  } catch (error) {
    failed.value = true
    image.value = ''
    throw error
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  try {
    await refresh()
  } catch (error) {
    message.error(errorText(error))
  }
}

onMounted(handleRefresh)
defineExpose({ refresh: handleRefresh })
</script>

<template>
  <button class="captcha-image" type="button" title="点击刷新图形验证码" :disabled="loading" @click="handleRefresh">
    <a-spin v-if="loading" size="small" />
    <img v-else-if="imageSrc" :src="imageSrc" alt="图形验证码" />
    <span v-else>{{ failed ? '加载失败，点击重试' : '点击获取验证码' }}</span>
  </button>
</template>
