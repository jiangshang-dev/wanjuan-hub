<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { adminReportApi } from '../../api/community'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf } from '../../utils'

const filters = reactive({ status: undefined, targetType: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminReportApi.list, () => ({ status: filters.status, targetType: filters.targetType }))
const detailOpen = ref(false)
const current = ref(null)
const handleOpen = ref(false)
const handleSaving = ref(false)
const handleForm = reactive({ id: '', action: 'OFFLINE', note: '' })
const targetOptions = [{ value: 'BLOG', label: '博客' }, { value: 'FORUM', label: '论坛主题' }, { value: 'FORUM_POST', label: '论坛跟帖' }, { value: 'COMMENT', label: '评论' }, { value: 'RESOURCE', label: '资源' }]
const statusOptions = [{ value: 'PENDING', label: '待处理' }, { value: 'PROCESSED', label: '已处理' }, { value: 'REJECTED', label: '已驳回' }]
const columns = [{ title: '举报 ID', dataIndex: 'id', key: 'id', width: 120 }, { title: '举报目标', key: 'target', width: 190 }, { title: '原因', key: 'reason', width: 180 }, { title: '补充说明', key: 'detail', width: 260, ellipsis: true }, { title: '举报人', key: 'reporter', width: 130 }, { title: '状态', key: 'status', width: 110 }, { title: '提交时间', key: 'createTime', width: 180 }, { title: '操作', key: 'actions', width: 170, fixed: 'right' }]

function targetName(type) {
  return targetOptions.find((item) => item.value === type)?.label || type || '-'
}

function statusInfo(status) {
  return { PENDING: { text: '待处理', color: 'processing' }, PROCESSED: { text: '已处理', color: 'success' }, REJECTED: { text: '已驳回', color: 'default' } }[status] || { text: status || '未知', color: 'default' }
}

function actionName(action) {
  return { OFFLINE: '下线内容', IGNORE: '保留内容', REJECT: '驳回举报' }[action] || action || '-'
}

function resetFilters() {
  Object.assign(filters, { status: undefined, targetType: undefined })
  search()
}

function openDetail(record) {
  current.value = record
  detailOpen.value = true
}

function openHandle(record) {
  Object.assign(handleForm, { id: idOf(record.id), action: 'OFFLINE', note: '' })
  handleOpen.value = true
}

async function saveHandle() {
  handleSaving.value = true
  try {
    await adminReportApi.handle(handleForm.id, { action: handleForm.action, note: handleForm.note.trim() || null })
    message.success('举报处理结果已提交')
    handleOpen.value = false
    detailOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    handleSaving.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-select v-model:value="filters.status" allow-clear placeholder="全部处理状态" style="width: 150px" :options="statusOptions" /><a-select v-model:value="filters.targetType" allow-clear placeholder="全部目标类型" style="width: 160px" :options="targetOptions" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-button @click="load()"><ReloadOutlined /></a-button></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1350 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'target'">{{ targetName(record.targetType) }} #{{ record.targetId }}</template><template v-else-if="column.key === 'reason'"><strong>{{ record.reason }}</strong></template><template v-else-if="column.key === 'detail'">{{ record.detail || '-' }}</template><template v-else-if="column.key === 'reporter'">用户 #{{ record.reporterId }}</template><template v-else-if="column.key === 'status'"><a-tag :color="statusInfo(record.status).color">{{ statusInfo(record.status).text }}</a-tag></template><template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-space><a-button type="link" size="small" @click="openDetail(record)">详情</a-button><a-button v-if="record.status === 'PENDING'" v-permission="'report:handle'" type="link" size="small" @click="openHandle(record)">处理</a-button></a-space></template><template v-else>{{ record[column.dataIndex] ?? '-' }}</template></template></a-table></a-card>
    <a-drawer v-model:open="detailOpen" title="举报详情" width="640"><template v-if="current"><a-descriptions :column="2" size="small" bordered><a-descriptions-item label="举报 ID">{{ current.id }}</a-descriptions-item><a-descriptions-item label="状态"><a-tag :color="statusInfo(current.status).color">{{ statusInfo(current.status).text }}</a-tag></a-descriptions-item><a-descriptions-item label="目标类型">{{ targetName(current.targetType) }}</a-descriptions-item><a-descriptions-item label="目标 ID">{{ current.targetId }}</a-descriptions-item><a-descriptions-item label="举报人">用户 #{{ current.reporterId }}</a-descriptions-item><a-descriptions-item label="处理人">{{ current.handlerId ? `管理员 #${current.handlerId}` : '-' }}</a-descriptions-item><a-descriptions-item label="提交时间" :span="2">{{ formatDate(current.createTime) }}</a-descriptions-item><a-descriptions-item label="处理时间" :span="2">{{ formatDate(current.handleTime) }}</a-descriptions-item><a-descriptions-item label="处理动作" :span="2">{{ actionName(current.action) }}</a-descriptions-item></a-descriptions><section class="drawer-content-section"><h4>举报原因</h4><p>{{ current.reason }}</p><h4>补充说明</h4><pre>{{ current.detail || '-' }}</pre><h4>处理说明</h4><p>{{ current.handleNote || '-' }}</p></section><a-button v-if="current.status === 'PENDING'" v-permission="'report:handle'" type="primary" @click="openHandle(current)">处理该举报</a-button></template></a-drawer>
    <a-modal v-model:open="handleOpen" title="处理举报" :confirm-loading="handleSaving" ok-text="提交处理结果" cancel-text="取消" @ok="saveHandle"><a-alert message="下线违规内容会直接影响其公开状态，请核对举报证据后操作。" type="warning" show-icon style="margin-bottom: 18px" /><a-form layout="vertical"><a-form-item label="处理动作" required><a-radio-group v-model:value="handleForm.action"><a-radio-button value="OFFLINE">下线内容</a-radio-button><a-radio-button value="IGNORE">保留内容</a-radio-button><a-radio-button value="REJECT">驳回举报</a-radio-button></a-radio-group></a-form-item><a-form-item label="处理说明"><a-textarea v-model:value="handleForm.note" :rows="5" :maxlength="1000" show-count placeholder="记录判断依据或处理说明" /></a-form-item></a-form></a-modal>
  </div>
</template>
