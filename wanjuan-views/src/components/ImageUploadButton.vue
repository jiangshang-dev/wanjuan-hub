<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { fileApi } from '../api'
import { errorText } from '../utils/format'

defineProps({ label: { type: String, default: '插入图片' } })
const emit = defineEmits(['uploaded'])
const uploading = ref(false)

function validate(file) {
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
    message.error('仅支持 JPG、PNG、GIF、WebP 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function upload({ file, onSuccess, onError }) {
  uploading.value = true
  try {
    const data = await fileApi.uploadImage(file)
    if (!data?.id || !data?.accessUrl) throw new Error('上传响应缺少图片 ID 或访问地址')
    emit('uploaded', { ...data, name: file.name })
    onSuccess?.(data)
  } catch (error) {
    message.error(errorText(error))
    onError?.(error)
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <a-upload accept="image/jpeg,image/png,image/gif,image/webp" :show-upload-list="false" :before-upload="validate" :custom-request="upload">
    <a-button size="small" :loading="uploading">{{ label }}</a-button>
  </a-upload>
</template>
