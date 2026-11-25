<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { auditApi } from '../../api'
import { useTable } from '../../composables/useTable'
import { auditStatus } from '../../utils/blog'
import { errorText, formatDate, idOf } from '../../utils'

const filters = reactive({ status: undefined, bizType: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(auditApi.list, () => ({ status: filters.status, bizType: filters.bizType }))
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const currentTask = ref(null)
const retryingId = ref('')
const reviewOpen = ref(false)
const reviewSaving = ref(false)
const reviewFormRef = ref()
const reviewForm = reactive({ id: '', action: 'APPROVE', opinion: '' })
const task = computed(() => detail.value?.task || currentTask.value)
const reviewRules = { opinion: [{ validator: (_, value) => reviewForm.action === 'APPROVE' || value?.trim() ? Promise.resolve() : Promise.reject(new Error('驳回时必须填写审核意见')) }] }
const columns = [{ title: '任务 ID', dataIndex: 'id', key: 'id', width: 170, ellipsis: true }, { title: '业务类型', dataIndex: 'bizType', key: 'bizType', width: 150 }, { title: '业务 ID', dataIndex: 'bizId', key: 'bizId', width: 170, ellipsis: true }, { title: '版本', dataIndex: 'versionNo', key: 'versionNo', width: 80 }, { title: '状态', key: 'status', width: 130 }, { title: '原因', key: 'reason', width: 220, ellipsis: true }, { title: '重试次数', dataIndex: 'retryCount', key: 'retryCount', width: 100 }, { title: '创建时间', key: 'createdAt', width: 180 }, { title: '操作', key: 'actions', width: 210, fixed: 'right' }]

onMounted(() => load())
async function openDetail(record) {
  currentTask.value = record
  detail.value = null
  detailOpen.value = true
  detailLoading.value = true
  try {
    detail.value = await auditApi.detail(idOf(record.id))
  } catch (error) {
    message.error(errorText(error))
  } finally {
    detailLoading.value = false
  }
}
async function retry(record) {
  retryingId.value = idOf(record.id)
  try {
    await auditApi.retry(idOf(record.id))
    message.success('已重新提交审核')
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    retryingId.value = ''
  }
}
function openReview(record) {
  Object.assign(reviewForm, { id: idOf(record.id), action: 'APPROVE', opinion: '' })
  reviewOpen.value = true
}
async function saveReview() {
  await reviewFormRef.value.validate()
  reviewSaving.value = true
  try {
    await auditApi.review(reviewForm.id, { action: reviewForm.action, opinion: reviewForm.opinion.trim() })
    message.success(reviewForm.action === 'APPROVE' ? '人工审核已通过' : '已驳回该内容')
    reviewOpen.value = false
    detailOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    reviewSaving.value = false
  }
}
function resetFilters() {
  filters.status = undefined
  filters.bizType = undefined
  search()
}
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-select v-model:value="filters.status" allow-clear placeholder="全部审核状态" style="width: 160px" :options="['PENDING', 'PROCESSING', 'APPROVED', 'REJECTED', 'MANUAL_REVIEW', 'FAILED'].map(value => ({ value, label: auditStatus(value).text }))" @change="search" /><a-select v-model:value="filters.bizType" allow-clear placeholder="全部业务类型" style="width: 170px" :options="[{ value: 'BLOG_ARTICLE', label: '博客文章' }]" @change="search" /><a-button @click="resetFilters">重置</a-button></a-space><a-button @click="load()"><ReloadOutlined /></a-button></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1500 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'id' || column.key === 'bizId'"><code class="table-code">{{ idOf(record[column.dataIndex]) }}</code></template><template v-else-if="column.key === 'status'"><a-tag :color="auditStatus(record.status).color">{{ auditStatus(record.status).text }}</a-tag></template><template v-else-if="column.key === 'reason'">{{ record.reason || '-' }}</template><template v-else-if="column.key === 'createdAt'">{{ formatDate(record.createdAt || record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-space><a-button v-permission="'audit:task:detail'" type="link" size="small" @click="openDetail(record)">详情</a-button><a-popconfirm v-if="record.status === 'FAILED'" title="确定重新提交该失败任务吗？" ok-text="确认重试" cancel-text="取消" @confirm="retry(record)"><a-button v-permission="'audit:task:retry'" type="link" size="small" :loading="retryingId === idOf(record.id)">重试</a-button></a-popconfirm><a-button v-if="['MANUAL_REVIEW', 'PENDING'].includes(record.status)" v-permission="'audit:task:review'" type="link" size="small" @click="openReview(record)">人工审核</a-button></a-space></template><template v-else>{{ record[column.dataIndex] ?? '-' }}</template></template></a-table></a-card>
    <a-drawer v-model:open="detailOpen" title="审核任务详情" width="720"><a-spin :spinning="detailLoading"><template v-if="task"><div class="audit-detail-heading"><h3>任务 #{{ task.id }}</h3><a-tag :color="auditStatus(task.status).color">{{ auditStatus(task.status).text }}</a-tag></div><a-descriptions :column="2" size="small" bordered><a-descriptions-item label="业务类型">{{ task.bizType || '-' }}</a-descriptions-item><a-descriptions-item label="业务 ID">{{ task.bizId || '-' }}</a-descriptions-item><a-descriptions-item label="版本">v{{ task.versionNo ?? '-' }}</a-descriptions-item><a-descriptions-item label="重试次数">{{ task.retryCount ?? 0 }}</a-descriptions-item><a-descriptions-item label="创建时间" :span="2">{{ formatDate(task.createdAt || task.createTime) }}</a-descriptions-item><a-descriptions-item label="完成时间" :span="2">{{ formatDate(task.finishedAt) }}</a-descriptions-item><a-descriptions-item label="原因" :span="2">{{ task.reason || '-' }}</a-descriptions-item></a-descriptions><section class="audit-detail-section"><h4>逐项审核结果</h4><div v-if="detail?.items?.length" class="audit-item-list"><article v-for="item in detail.items" :key="item.id" class="audit-item"><div class="audit-item-title"><strong>{{ item.itemType || '内容审核' }}</strong><a-tag :color="auditStatus(item.status).color">{{ auditStatus(item.status).text }}</a-tag></div><pre>{{ item.content || '-' }}</pre><p>建议：{{ item.suggestion || '-' }}</p><p>原因：{{ item.reason || '-' }}</p><small v-if="item.providerRequestId">服务商请求 ID：{{ item.providerRequestId }}</small></article></div><a-empty v-else description="暂无逐项审核结果" /></section><section class="audit-detail-section"><h4>人工审核记录</h4><a-timeline v-if="detail?.manualRecords?.length"><a-timeline-item v-for="record in detail.manualRecords" :key="record.id" :color="record.action === 'APPROVE' ? 'green' : 'red'"><div class="manual-record"><strong>{{ record.action === 'APPROVE' ? '人工通过' : '人工驳回' }}</strong><span>{{ formatDate(record.createdAt || record.createTime) }}</span><p>{{ record.opinion || '未填写意见' }}</p><small>审核人：{{ record.reviewerName || record.adminName || record.reviewerId || record.adminId || '-' }}</small></div></a-timeline-item></a-timeline><a-empty v-else description="暂无人工作业记录" /></section><a-space v-if="['MANUAL_REVIEW', 'PENDING'].includes(task.status)" class="drawer-actions"><a-button v-permission="'audit:task:review'" type="primary" @click="openReview(task)">处理人工审核</a-button></a-space></template></a-spin></a-drawer>
    <a-modal v-model:open="reviewOpen" title="人工审核确认" :confirm-loading="reviewSaving" :ok-text="reviewForm.action === 'APPROVE' ? '确认通过' : '确认驳回'" :ok-button-props="{ danger: reviewForm.action === 'REJECT' }" @ok="saveReview"><a-alert message="审核结果将直接提交后端并影响内容状态，请核对后操作。" type="warning" show-icon style="margin-bottom: 18px" /><a-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" layout="vertical"><a-form-item label="审核动作" name="action"><a-radio-group v-model:value="reviewForm.action"><a-radio-button value="APPROVE">通过</a-radio-button><a-radio-button value="REJECT">驳回</a-radio-button></a-radio-group></a-form-item><a-form-item label="审核意见" name="opinion"><a-textarea v-model:value="reviewForm.opinion" :rows="4" :maxlength="500" show-count :placeholder="reviewForm.action === 'APPROVE' ? '可填写通过说明' : '请填写明确的驳回原因'" /></a-form-item></a-form></a-modal>
  </div>
</template>
