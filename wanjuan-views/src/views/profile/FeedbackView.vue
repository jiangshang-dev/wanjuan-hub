<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { feedbackApi } from '../../api'
import { collectionOf } from '../../utils/blog'
import { errorText, formatDate } from '../../utils/format'

const statusOptions = [
  { value: 'PENDING', label: '待处理', color: 'gold' },
  { value: 'REPLIED', label: '已回复', color: 'green' },
  { value: 'CLOSED', label: '已关闭', color: 'default' },
]
const content = ref('')
const submitting = ref(false)
const loading = ref(false)
const records = ref([])
const filters = reactive({ status: undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (total) => `共 ${total} 条反馈` })
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

function statusInfo(status) {
  return statusOptions.find((item) => item.value === status) || { label: status || '未知状态', color: 'default' }
}

async function load(page = 1) {
  loading.value = true
  try {
    const data = await feedbackApi.list({ page, size: pagination.pageSize, status: filters.status })
    records.value = collectionOf(data)
    pagination.current = Number(data?.page || page)
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

async function submit() {
  const value = content.value.trim()
  if (!value) {
    message.warning('请输入反馈内容')
    return
  }
  submitting.value = true
  try {
    await feedbackApi.create({ content: value })
    content.value = ''
    message.success('反馈已提交')
    await load(1)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    submitting.value = false
  }
}

async function openDetail(record) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await feedbackApi.detail(record.id)
  } catch (error) {
    detailOpen.value = false
    message.error(errorText(error))
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="content-page feedback-page">
    <div class="page-heading">
      <div><span class="eyebrow">FEEDBACK</span><h1>我的反馈</h1><p>提交产品建议或使用问题，并在这里查看处理进度与管理员回复。</p></div>
      <a-button @click="$router.push('/my')">返回个人中心</a-button>
    </div>
    <section class="content-card feedback-editor">
      <h2>提交新反馈</h2>
      <a-textarea v-model:value="content" :rows="5" :maxlength="2000" show-count placeholder="请具体描述你遇到的问题、期望的改进或相关使用场景（1～2000 字）" />
      <div class="feedback-editor-actions"><span>反馈内容仅用于问题处理与服务改进</span><a-button type="primary" :loading="submitting" @click="submit">提交反馈</a-button></div>
    </section>
    <section class="content-card feedback-list-card">
      <div class="feedback-list-heading"><h2>反馈记录</h2><a-select v-model:value="filters.status" allow-clear placeholder="全部处理状态" style="width: 150px" :options="statusOptions" @change="load(1)" /></div>
      <a-spin :spinning="loading">
        <div v-if="records.length" class="feedback-list">
          <article v-for="record in records" :key="record.id" class="feedback-item">
            <div class="feedback-item-heading"><a-tag :color="statusInfo(record.status).color">{{ statusInfo(record.status).label }}</a-tag><span>反馈 #{{ record.id }}</span><time>{{ formatDate(record.createTime, true) }}</time></div>
            <p class="feedback-content">{{ record.content }}</p>
            <div v-if="record.adminReply" class="feedback-reply-preview"><strong>管理员回复</strong><p>{{ record.adminReply }}</p></div>
            <div class="feedback-item-footer"><span v-if="record.replyTime">回复于 {{ formatDate(record.replyTime, true) }}</span><span v-else>暂无管理员回复</span><a-button size="small" @click="openDetail(record)">查看详情</a-button></div>
          </article>
        </div>
        <a-empty v-else-if="!loading" description="暂无符合条件的反馈" />
      </a-spin>
      <a-pagination v-if="pagination.total > pagination.pageSize" v-model:current="pagination.current" class="list-pagination" :total="pagination.total" :page-size="pagination.pageSize" :show-total="pagination.showTotal" @change="load" />
    </section>
    <a-modal v-model:open="detailOpen" title="反馈详情" :footer="null" width="680px">
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <div class="feedback-detail-meta"><a-tag :color="statusInfo(detail.status).color">{{ statusInfo(detail.status).label }}</a-tag><span>反馈 #{{ detail.id }}</span><span>提交于 {{ formatDate(detail.createTime, true) }}</span></div>
          <section class="feedback-detail-section"><h3>反馈内容</h3><p>{{ detail.content }}</p></section>
          <section class="feedback-detail-section admin-reply"><h3>管理员回复</h3><p>{{ detail.adminReply || '暂无回复，请耐心等待处理。' }}</p><time v-if="detail.replyTime">回复于 {{ formatDate(detail.replyTime, true) }}</time></section>
        </template>
      </a-spin>
    </a-modal>
  </div>
</template>

<style scoped>
.feedback-page { max-width: 980px; }
.feedback-editor { padding: 24px 26px; margin-bottom: 20px; background: #fff; }
.feedback-editor h2, .feedback-list-heading h2 { margin: 0 0 16px; color: var(--ink); font-size: 18px; }
.feedback-editor-actions { display: flex; margin-top: 14px; align-items: center; justify-content: space-between; gap: 16px; }
.feedback-editor-actions > span { color: var(--muted); font-size: 12px; }
.feedback-list-card { padding: 24px 26px; background: #fff; }
.feedback-list-heading { display: flex; margin-bottom: 4px; align-items: center; justify-content: space-between; gap: 16px; }
.feedback-item { padding: 22px 0; border-bottom: 1px solid var(--line); }
.feedback-item:last-child { border-bottom: 0; }
.feedback-item-heading, .feedback-detail-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 9px; color: var(--muted); font-size: 12px; }
.feedback-item-heading .ant-tag, .feedback-detail-meta .ant-tag { margin-inline-end: 0; }
.feedback-item-heading time { margin-left: auto; }
.feedback-content { margin: 14px 0; color: #35415a; line-height: 1.8; white-space: pre-wrap; word-break: break-word; }
.feedback-reply-preview { padding: 13px 16px; border-radius: 8px; background: #f5f8ff; }
.feedback-reply-preview strong { color: var(--primary); font-size: 12px; }
.feedback-reply-preview p { margin: 6px 0 0; color: #536078; font-size: 13px; line-height: 1.7; white-space: pre-wrap; }
.feedback-item-footer { display: flex; margin-top: 14px; align-items: center; justify-content: space-between; gap: 16px; color: var(--muted); font-size: 12px; }
.feedback-detail-section { padding: 18px 0; border-bottom: 1px solid var(--line); }
.feedback-detail-section:last-child { border-bottom: 0; }
.feedback-detail-section h3 { margin: 0 0 10px; color: var(--ink); font-size: 15px; }
.feedback-detail-section p { margin: 0; color: #455169; line-height: 1.85; white-space: pre-wrap; word-break: break-word; }
.feedback-detail-section time { display: block; margin-top: 10px; color: var(--muted); font-size: 12px; }
.feedback-detail-section.admin-reply { margin-top: 4px; }
@media (max-width: 560px) { .feedback-editor, .feedback-list-card { padding: 20px 16px; } .feedback-editor-actions { align-items: flex-start; flex-direction: column; } .feedback-editor-actions .ant-btn { align-self: flex-end; } .feedback-item-heading time { width: 100%; margin-left: 0; } }
</style>
