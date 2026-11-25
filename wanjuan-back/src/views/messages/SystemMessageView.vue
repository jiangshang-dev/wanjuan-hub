<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { SendOutlined } from '@ant-design/icons-vue'
import { adminMessageApi } from '../../api/s6'
import { errorText } from '../../utils'

const formRef = ref()
const sending = ref(false)
const form = reactive({ userId: '', title: '', content: '', dedupKey: '' })
const rules = {
  userId: [{ pattern: /^\d*$/, message: '用户ID必须为正整数' }],
  title: [{ required: true, whitespace: true, message: '请输入消息标题' }, { max: 120, message: '标题不能超过120个字符' }],
  content: [{ required: true, whitespace: true, message: '请输入消息正文' }, { max: 2000, message: '正文不能超过2000个字符' }],
  dedupKey: [{ max: 120, message: '幂等键不能超过120个字符' }],
}

function newDedupKey() {
  const id = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`
  form.dedupKey = `SYSTEM_MESSAGE:${id}`
}

async function send() {
  await formRef.value.validate()
  sending.value = true
  try {
    const count = await adminMessageApi.sendSystem({
      userId: form.userId.trim() || null,
      title: form.title.trim(),
      content: form.content.trim(),
      dedupKey: form.dedupKey.trim() || null,
    })
    message.success(form.userId ? '系统消息已发送给指定用户' : `全站系统消息已发送，共 ${Number(count || 0)} 条`)
    Object.assign(form, { userId: '', title: '', content: '', dedupKey: '' })
    formRef.value.clearValidate()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    sending.value = false
  }
}

newDedupKey()
</script>

<template>
  <div class="management-page system-message-page">
    <a-alert message="系统消息会写入用户消息中心。留空用户ID将向全站用户广播，请在发送前仔细核对内容。" type="info" show-icon class="page-alert" />
    <a-card :bordered="false" class="panel-card" title="发送系统消息">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="system-message-form" @finish="send">
        <a-form-item label="接收用户" name="userId" extra="填写完整用户ID则单独发送；留空则广播全部用户。"><a-input v-model:value="form.userId" placeholder="留空表示全站用户" /></a-form-item>
        <a-form-item label="消息标题" name="title"><a-input v-model:value="form.title" :maxlength="120" show-count placeholder="请输入标题" /></a-form-item>
        <a-form-item label="消息正文" name="content"><a-textarea v-model:value="form.content" :rows="9" :maxlength="2000" show-count placeholder="请输入需要发送的系统通知内容" /></a-form-item>
        <a-form-item label="幂等键" name="dedupKey" extra="同一次广播共用此键，避免重复发送。"><a-input v-model:value="form.dedupKey" :maxlength="120"><template #addonAfter><a-button type="link" size="small" @click="newDedupKey">重新生成</a-button></template></a-input></a-form-item>
        <a-form-item><a-popconfirm :title="form.userId ? '确定发送给该用户吗？' : '确定向全站用户广播吗？'" ok-text="确认发送" cancel-text="取消" @confirm="send"><a-button v-permission="'message:system:send'" type="primary" :loading="sending"><SendOutlined />发送消息</a-button></a-popconfirm></a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.system-message-page { max-width: 900px; }
.system-message-form { max-width: 760px; }
</style>
