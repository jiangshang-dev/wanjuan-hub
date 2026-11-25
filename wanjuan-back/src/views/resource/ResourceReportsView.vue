<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { adminResourceReportApi } from '../../api/resource'
import { useTable } from '../../composables/useTable'
import { resourceStatus } from '../../utils/resource'
import { errorText, formatDate, idOf } from '../../utils'

const filters = reactive({ status: undefined, resourceId: '' })
const { loading, records, pagination, load, changeTable, search } = useTable(adminResourceReportApi.list, () => ({ status: filters.status, resourceId: String(filters.resourceId).trim() || undefined }))
const handleOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: '', resourceId: '', status: 'CONFIRMED', handleNote: '' })
const statusOptions = [{ value: 'PENDING', label: '待处理' }, { value: 'CONFIRMED', label: '已确认失效' }, { value: 'REJECTED', label: '已驳回' }]
const columns = [
  { title: '举报 ID', dataIndex: 'id', key: 'id', width: 150 }, { title: '资源 ID', key: 'resourceId', width: 160 },
  { title: '链接 ID', key: 'linkId', width: 150 }, { title: '举报人 ID', key: 'reporterId', width: 160 },
  { title: '失效原因', dataIndex: 'reason', key: 'reason', width: 260, ellipsis: true }, { title: '状态', key: 'status', width: 130 },
  { title: '处理说明', dataIndex: 'handleNote', key: 'handleNote', width: 200, ellipsis: true }, { title: '举报时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 110, fixed: 'right' },
]

onMounted(() => load())
function openHandle(record) { Object.assign(form, { id: idOf(record.id), resourceId: idOf(record.resourceId), status: 'CONFIRMED', handleNote: '' }); handleOpen.value = true }
async function saveHandle() {
  await formRef.value.validate()
  saving.value = true
  try {
    await adminResourceReportApi.handle(form.id, { status: form.status, handleNote: form.handleNote.trim() || null })
    message.success(form.status === 'CONFIRMED' ? '已确认失效并处理资源' : '举报已驳回')
    handleOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}
function resetFilters() { Object.assign(filters, { status: undefined, resourceId: '' }); search() }
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input v-model:value="filters.resourceId" allow-clear placeholder="资源 ID" style="width: 200px" @press-enter="search" /><a-select v-model:value="filters.status" allow-clear placeholder="全部处理状态" style="width: 150px" :options="statusOptions" @change="search" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-button @click="load()"><ReloadOutlined /></a-button></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1750 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="['id', 'resourceId', 'linkId', 'reporterId'].includes(column.key)"><code class="table-code">{{ record[column.key] || '-' }}</code></template><template v-else-if="column.key === 'status'"><a-tag :color="record.status === 'PENDING' ? 'processing' : resourceStatus(record.status).color">{{ record.status === 'PENDING' ? '待处理' : resourceStatus(record.status).text }}</a-tag></template><template v-else-if="column.key === 'createdAt'">{{ formatDate(record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-button v-if="record.status === 'PENDING'" v-permission="'resource:report:handle'" type="link" size="small" @click="openHandle(record)">处理</a-button><span v-else>-</span></template><template v-else>{{ record[column.dataIndex] || '-' }}</template></template></a-table></a-card>
    <a-modal v-model:open="handleOpen" title="处理资源失效举报" :confirm-loading="saving" ok-text="确认提交" @ok="saveHandle"><a-alert :message="`举报 #${form.id} · 资源 #${form.resourceId}`" description="确认失效后，后端会禁用对应链接并下线资源；请核实举报原因后操作。" type="warning" show-icon style="margin-bottom: 18px" /><a-form ref="formRef" :model="form" layout="vertical"><a-form-item label="处理结果" name="status"><a-radio-group v-model:value="form.status"><a-radio-button value="CONFIRMED">确认失效</a-radio-button><a-radio-button value="REJECTED">驳回举报</a-radio-button></a-radio-group></a-form-item><a-form-item label="处理说明" name="handleNote" :rules="[{ max: 1000, message: '处理说明不能超过1000个字符' }]"><a-textarea v-model:value="form.handleNote" :rows="5" :maxlength="1000" show-count /></a-form-item></a-form></a-modal>
  </div>
</template>
