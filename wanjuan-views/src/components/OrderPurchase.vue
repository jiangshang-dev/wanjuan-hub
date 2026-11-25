<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { orderApi } from '../api/s5'
import { getAccessToken } from '../utils/token'
import { errorText } from '../utils/format'

const props = defineProps({
  resourceId: { type: [String, Number], required: true },
  clientType: { type: String, default: '' },
  buttonText: { type: String, default: '购买资源' },
  block: { type: Boolean, default: false },
  autoNavigate: { type: Boolean, default: true },
})
const emit = defineEmits(['created'])
const route = useRoute()
const router = useRouter()
const creating = ref(false)
const resolvedClientType = computed(() => props.clientType || (window.matchMedia('(max-width: 768px)').matches ? 'H5' : 'PC'))
const storageKey = computed(() => `wanjuan:order-key:${props.resourceId}`)

function newIdempotencyKey() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID()
  return `order-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

function idempotencyKey() {
  let value = sessionStorage.getItem(storageKey.value)
  if (!value) {
    value = newIdempotencyKey()
    sessionStorage.setItem(storageKey.value, value)
  }
  return value
}

async function createOrder() {
  if (!getAccessToken()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  creating.value = true
  try {
    const order = await orderApi.create({
      resourceId: String(props.resourceId),
      clientType: resolvedClientType.value,
      idempotencyKey: idempotencyKey(),
    })
    sessionStorage.removeItem(storageKey.value)
    emit('created', order)
    message.success('订单已创建，金额以服务端订单为准')
    if (props.autoNavigate) {
      router.push({ name: 'order-detail', params: { orderNo: order.orderNo }, query: { pay: '1' } })
    }
  } catch (error) {
    message.error(errorText(error))
  } finally {
    creating.value = false
  }
}

watch(() => props.resourceId, () => {
  creating.value = false
})
</script>

<template>
  <a-button type="primary" :loading="creating" :block="block" @click="createOrder">{{ buttonText }}</a-button>
</template>
