<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { FlagOutlined } from '@ant-design/icons-vue'
import { userReportApi } from '../api/s3'
import { errorText } from '../utils/format'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [String, Number], required: true },
  compact: { type: Boolean, default: true },
})
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const open = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ reason: undefined, detail: '' })
const rules = { reason: [{ required: true, message: '请选择举报原因' }], detail: [{ max: 1000, message: '补充说明不能超过1000个字符' }] }
const reasonOptions = ['垃圾广告', '违法违规', '辱骂攻击', '抄袭侵权', '不实信息', '其他'].map((value) => ({ value, label: value }))

function show() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  Object.assign(form, { reason: undefined, detail: '' })
  open.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await userReportApi.create({ targetType: props.targetType, targetId: props.targetId, reason: form.reason, detail: form.detail.trim() || null })
    message.success('举报已提交，可在我的互动中查看处理进度')
    open.value = false
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-button type="text" size="small" class="report-button" @click="show"><FlagOutlined /><span v-if="!compact">举报</span></a-button>
  <a-modal v-model:open="open" title="举报内容" ok-text="提交举报" cancel-text="取消" :confirm-loading="saving" destroy-on-close @ok="submit">
    <a-alert message="请如实举报违规内容，恶意举报可能影响账号使用。" type="info" show-icon class="report-alert" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item label="举报原因" name="reason"><a-select v-model:value="form.reason" :options="reasonOptions" placeholder="请选择原因" /></a-form-item>
      <a-form-item label="补充说明" name="detail"><a-textarea v-model:value="form.detail" :rows="5" :maxlength="1000" show-count placeholder="请描述具体问题，最多1000字" /></a-form-item>
    </a-form>
  </a-modal>
</template>
