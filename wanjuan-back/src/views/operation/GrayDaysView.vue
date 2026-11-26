<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { adminSiteExperienceApi } from '../../api/s6'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf, isEnabled } from '../../utils'

const statusOptions = [{ value: 1, label: '启用' }, { value: 0, label: '停用' }]
const filters = reactive({ dateRange: [], status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(
  adminSiteExperienceApi.grayDays,
  () => ({
    startDate: filters.dateRange?.[0]?.format('YYYY-MM-DD'),
    endDate: filters.dateRange?.[1]?.format('YYYY-MM-DD'),
    status: filters.status,
  }),
)
const columns = [
  { title: '灰色日期', dataIndex: 'grayDate', width: 140 },
  { title: '灰色原因', dataIndex: 'reason', ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: '操作', key: 'actions', width: 150, fixed: 'right' },
]
const modalOpen = ref(false)
const saving = ref(false)
const statusLoadingId = ref('')
const editingId = ref('')
const formRef = ref()
const form = reactive({ grayDate: null, reason: '', status: 1 })
const formRules = {
  grayDate: [{ required: true, message: '请选择灰色日期' }],
  reason: [{ required: true, whitespace: true, message: '请输入灰色原因' }, { max: 500, message: '灰色原因不能超过500个字符' }],
  status: [{ required: true, message: '请选择状态' }],
}

function resetFilters() {
  Object.assign(filters, { dateRange: [], status: undefined })
  search()
}

function openForm(record) {
  editingId.value = record ? idOf(record.id) : ''
  Object.assign(form, record ? {
    grayDate: dayjs(record.grayDate),
    reason: record.reason || '',
    status: isEnabled(record.status) ? 1 : 0,
  } : { grayDate: null, reason: '', status: 1 })
  modalOpen.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = { grayDate: form.grayDate.format('YYYY-MM-DD'), reason: form.reason.trim(), status: form.status }
    if (editingId.value) await adminSiteExperienceApi.updateGrayDay(editingId.value, data)
    else await adminSiteExperienceApi.createGrayDay(data)
    message.success(editingId.value ? '灰色日已更新' : '灰色日已创建')
    modalOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}

async function toggleStatus(record) {
  statusLoadingId.value = idOf(record.id)
  try {
    await adminSiteExperienceApi.updateGrayDayStatus(statusLoadingId.value, isEnabled(record.status) ? 0 : 1)
    message.success('灰色日状态已更新')
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusLoadingId.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div class="management-page">
    <div class="toolbar">
      <a-space wrap>
        <a-range-picker v-model:value="filters.dateRange" :placeholder="['开始日期', '结束日期']" />
        <a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 120px" :options="statusOptions" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>
      <a-space>
        <a-button @click="load()"><ReloadOutlined /></a-button>
        <a-button v-permission="'operation:gray-day:create'" type="primary" @click="openForm()"><PlusOutlined />新增灰色日</a-button>
      </a-space>
    </div>
    <a-card :bordered="false" class="table-card">
      <a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1000 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'"><a-tag :color="isEnabled(record.status) ? 'success' : 'default'">{{ isEnabled(record.status) ? '启用' : '停用' }}</a-tag></template>
          <template v-else-if="column.key === 'updateTime'">{{ formatDate(record.updateTime || record.createTime) }}</template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button v-permission="'operation:gray-day:update'" type="link" size="small" @click="openForm(record)">编辑</a-button>
              <a-button v-permission="'operation:gray-day:status'" type="link" size="small" :loading="statusLoadingId === idOf(record.id)" @click="toggleStatus(record)">{{ isEnabled(record.status) ? '停用' : '启用' }}</a-button>
            </a-space>
          </template>
          <template v-else>{{ record[column.dataIndex] || '-' }}</template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑灰色日' : '新增灰色日'" :confirm-loading="saving" destroy-on-close @ok="save">
      <a-form ref="formRef" :model="form" :rules="formRules" layout="vertical">
        <a-form-item label="灰色日期" name="grayDate"><a-date-picker v-model:value="form.grayDate" style="width: 100%" /></a-form-item>
        <a-form-item label="灰色原因" name="reason"><a-textarea v-model:value="form.reason" :rows="4" :maxlength="500" show-count placeholder="请输入灰色模式展示原因" /></a-form-item>
        <a-form-item label="状态" name="status"><a-radio-group v-model:value="form.status" :options="statusOptions" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
