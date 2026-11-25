<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { adminOperationApi } from '../../api/s6'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf, isEnabled } from '../../utils'

const contentTypes = [{ value: 'BLOG', label: '技术博客' }, { value: 'RESOURCE', label: '学习资源' }, { value: 'FORUM_TOPIC', label: '论坛主题' }]
const statusOptions = [{ value: 1, label: '启用' }, { value: 0, label: '停用' }]
const publicOptions = [{ value: 1, label: '公开' }, { value: 0, label: '不公开' }]
const statusField = { name: 'status', label: '状态', type: 'radio', options: statusOptions, default: 1, required: true }
const sortField = { name: 'sortOrder', label: '排序值', type: 'number', default: 0, required: true, precision: 0, extra: '数值越小越靠前' }
const date = (title, dataIndex) => ({ title, dataIndex, key: dataIndex, type: 'date', width: 170 })
const configs = {
  announcements: {
    title: '运营公告', permission: 'announcement', columns: [{ title: '标题', dataIndex: 'title', width: 190 }, { title: '内容', dataIndex: 'content', ellipsis: true }, date('发布开始', 'publishStart'), date('发布结束', 'publishEnd'), { title: '排序', dataIndex: 'sortOrder', width: 80 }, { title: '状态', key: 'status', width: 90 }],
    fields: [{ name: 'title', label: '公告标题', type: 'input', required: true, max: 120 }, { name: 'content', label: '公告内容', type: 'textarea', required: true, max: 60000, rows: 7 }, { name: 'publishStart', label: '发布开始时间', type: 'date', extra: '留空表示立即生效' }, { name: 'publishEnd', label: '发布结束时间', type: 'date', extra: '留空表示长期有效' }, sortField, statusField],
  },
  recommendations: {
    title: '推荐位', permission: 'recommendation', filter: 'contentType', columns: [{ title: '内容类型', dataIndex: 'contentType', key: 'contentType', width: 120 }, { title: '内容ID', dataIndex: 'contentId', width: 180 }, { title: '推荐标题', dataIndex: 'title', width: 190 }, { title: '推荐理由', dataIndex: 'reason', ellipsis: true }, { title: '排序', dataIndex: 'sortOrder', width: 80 }, { title: '状态', key: 'status', width: 90 }],
    fields: [{ name: 'contentType', label: '内容类型', type: 'select', options: contentTypes, required: true, default: 'BLOG' }, { name: 'contentId', label: '内容ID', type: 'input', required: true, pattern: /^\d+$/, patternMessage: '内容ID必须为正整数' }, { name: 'title', label: '推荐标题', type: 'input', required: true, max: 120 }, { name: 'reason', label: '推荐理由', type: 'textarea', max: 500, rows: 4 }, sortField, statusField],
  },
  advertisements: {
    title: '广告位', permission: 'ad', filter: 'position', columns: [{ title: '广告标题', dataIndex: 'title', width: 170 }, { title: '位置编码', dataIndex: 'position', width: 130 }, { title: '图片地址', dataIndex: 'imageUrl', ellipsis: true }, { title: '跳转地址', dataIndex: 'targetUrl', ellipsis: true }, date('投放开始', 'startTime'), date('投放结束', 'endTime'), { title: '状态', key: 'status', width: 90 }],
    fields: [{ name: 'title', label: '广告标题', type: 'input', required: true, max: 120 }, { name: 'imageUrl', label: '图片地址', type: 'input', required: true, max: 500 }, { name: 'targetUrl', label: '跳转地址', type: 'input', required: true, max: 500 }, { name: 'position', label: '位置编码', type: 'input', required: true, max: 40 }, { name: 'startTime', label: '投放开始时间', type: 'date' }, { name: 'endTime', label: '投放结束时间', type: 'date' }, sortField, statusField],
  },
  'friend-links': {
    title: '友情链接', permission: 'link', columns: [{ title: '链接名称', dataIndex: 'name', width: 180 }, { title: '链接地址', dataIndex: 'url', ellipsis: true }, { title: '图标地址', dataIndex: 'logoUrl', ellipsis: true }, { title: '排序', dataIndex: 'sortOrder', width: 90 }, { title: '状态', key: 'status', width: 90 }, date('更新时间', 'updateTime')],
    fields: [{ name: 'name', label: '链接名称', type: 'input', required: true, max: 80 }, { name: 'url', label: '链接地址', type: 'input', required: true, max: 500 }, { name: 'logoUrl', label: '图标地址', type: 'input', max: 500 }, sortField, statusField],
  },
  'hot-searches': {
    title: '热门搜索', permission: 'hot-search', columns: [{ title: '搜索关键词', dataIndex: 'keyword', width: 240 }, { title: '搜索热度', dataIndex: 'heat', width: 140 }, { title: '排序', dataIndex: 'sortOrder', width: 100 }, { title: '状态', key: 'status', width: 100 }, date('更新时间', 'updateTime')],
    fields: [{ name: 'keyword', label: '搜索关键词', type: 'input', required: true, max: 100 }, { name: 'heat', label: '搜索热度', type: 'number', min: 0, precision: 0, default: 0, required: true }, sortField, statusField],
  },
  configs: {
    title: '网站参数', permission: 'config', filter: 'publicFlag', columns: [{ title: '参数键', dataIndex: 'configKey', width: 190 }, { title: '参数值', dataIndex: 'configValue', ellipsis: true }, { title: '参数说明', dataIndex: 'description', width: 190, ellipsis: true }, { title: '是否公开', key: 'publicFlag', width: 100 }, { title: '状态', key: 'status', width: 90 }, date('更新时间', 'updateTime')],
    fields: [{ name: 'configKey', label: '参数键', type: 'input', required: true, max: 100, pattern: /^[A-Za-z0-9._-]+$/, patternMessage: '仅支持字母、数字、点、下划线和短横线' }, { name: 'configValue', label: '参数值', type: 'textarea', max: 60000, rows: 7 }, { name: 'description', label: '参数说明', type: 'textarea', max: 500, rows: 3 }, { name: 'publicFlag', label: '是否公开', type: 'radio', options: publicOptions, default: 0, required: true, extra: '仅公开明确允许前台访问的非敏感参数' }, statusField],
  },
}
const route = useRoute()
const router = useRouter()
const activeType = computed(() => configs[route.path.split('/').pop()] ? route.path.split('/').pop() : 'announcements')
const current = computed(() => configs[activeType.value])
const filters = reactive({ keyword: '', status: undefined, contentType: undefined, position: '', publicFlag: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(
  (params) => adminOperationApi.list(activeType.value, params),
  () => ({
    keyword: filters.keyword.trim() || undefined,
    status: filters.status,
    contentType: activeType.value === 'recommendations' ? filters.contentType : undefined,
    position: activeType.value === 'advertisements' ? filters.position.trim() || undefined : undefined,
    publicFlag: activeType.value === 'configs' ? filters.publicFlag : undefined,
  }),
)
const columns = computed(() => [...current.value.columns, { title: '操作', key: 'actions', width: 220, fixed: 'right' }])
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref('')
const formRef = ref()
const form = reactive({})

function fieldRules(field) {
  const rules = []
  if (field.required) rules.push({ required: true, whitespace: field.type === 'input' || field.type === 'textarea', message: `请填写${field.label}` })
  if (field.max) rules.push({ max: field.max, message: `${field.label}不能超过${field.max}个字符` })
  if (field.pattern) rules.push({ pattern: field.pattern, message: field.patternMessage })
  if (field.type === 'number' && field.min !== undefined) rules.push({ type: 'number', min: field.min, message: `${field.label}不能小于${field.min}` })
  return rules
}

function resetFilters() {
  Object.assign(filters, { keyword: '', status: undefined, contentType: undefined, position: '', publicFlag: undefined })
  search()
}

function changeTab(key) {
  router.push(`/operation/${key}`)
}

function permission(action) {
  return `operation:${current.value.permission}:${action}`
}

function openForm(record) {
  editingId.value = record ? idOf(record.id) : ''
  Object.keys(form).forEach((key) => delete form[key])
  current.value.fields.forEach((field) => {
    let value = record ? record[field.name] : field.default
    if (field.type === 'date') value = value ? dayjs(value) : null
    if (field.type === 'number') value = Number(value ?? field.default ?? 0)
    if (field.type === 'input' || field.type === 'textarea') value = value ?? ''
    form[field.name] = value
  })
  modalOpen.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = {}
    current.value.fields.forEach((field) => {
      let value = form[field.name]
      if (field.type === 'date') value = value ? value.format('YYYY-MM-DDTHH:mm:ss') : null
      else if (typeof value === 'string' && field.name !== 'configValue') value = value.trim()
      if ((field.name === 'logoUrl' || field.name === 'description' || field.name === 'reason') && !value) value = field.name === 'logoUrl' ? null : ''
      data[field.name] = value
    })
    if (editingId.value) await adminOperationApi.update(activeType.value, editingId.value, data)
    else await adminOperationApi.create(activeType.value, data)
    message.success(`${current.value.title}${editingId.value ? '已更新' : '已创建'}`)
    modalOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}

async function toggleStatus(record) {
  try {
    await adminOperationApi.updateStatus(activeType.value, idOf(record.id), isEnabled(record.status) ? 0 : 1)
    message.success('状态已更新')
    load()
  } catch (error) {
    message.error(errorText(error))
  }
}

async function remove(record) {
  try {
    await adminOperationApi.remove(activeType.value, idOf(record.id))
    message.success('已删除')
    load(records.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error) {
    message.error(errorText(error))
  }
}

function cellValue(column, record) {
  const value = record[column.dataIndex]
  if (column.type === 'date') return formatDate(value)
  return value === null || value === undefined || value === '' ? '-' : value
}

onMounted(load)
watch(activeType, () => {
  resetFilters()
})
</script>

<template>
  <div class="management-page">
    <a-card :bordered="false" class="panel-card operation-tabs-card"><a-tabs :active-key="activeType" @change="changeTab"><a-tab-pane v-for="(config, key) in configs" :key="key" :tab="config.title" /></a-tabs></a-card>
    <div :key="activeType">
      <div class="toolbar">
        <a-space wrap>
          <a-input v-model:value="filters.keyword" allow-clear placeholder="搜索关键词" style="width: 220px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input>
          <a-select v-if="current.filter === 'contentType'" v-model:value="filters.contentType" allow-clear placeholder="全部内容类型" style="width: 150px" :options="contentTypes" />
          <a-input v-if="current.filter === 'position'" v-model:value="filters.position" allow-clear placeholder="广告位置编码" style="width: 150px" @press-enter="search" />
          <a-select v-if="current.filter === 'publicFlag'" v-model:value="filters.publicFlag" allow-clear placeholder="是否公开" style="width: 130px" :options="publicOptions" />
          <a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 120px" :options="statusOptions" />
          <a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button>
        </a-space>
        <a-space><a-button @click="load()"><ReloadOutlined /></a-button><a-button v-permission="permission('create')" type="primary" @click="openForm()"><PlusOutlined />新增{{ current.title }}</a-button></a-space>
      </div>
      <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1200 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'status'"><a-tag :color="isEnabled(record.status) ? 'success' : 'default'">{{ isEnabled(record.status) ? '启用' : '停用' }}</a-tag></template><template v-else-if="column.key === 'publicFlag'"><a-tag :color="record.publicFlag === 1 ? 'blue' : 'default'">{{ record.publicFlag === 1 ? '公开' : '不公开' }}</a-tag></template><template v-else-if="column.key === 'contentType'"><a-tag color="blue">{{ contentTypes.find((item) => item.value === record.contentType)?.label || record.contentType }}</a-tag></template><template v-else-if="column.key === 'actions'"><a-space><a-button v-permission="permission('update')" type="link" size="small" @click="openForm(record)">编辑</a-button><a-button v-permission="permission('status')" type="link" size="small" @click="toggleStatus(record)">{{ isEnabled(record.status) ? '停用' : '启用' }}</a-button><a-popconfirm title="确定删除该记录吗？" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="remove(record)"><a-button v-permission="permission('delete')" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template><template v-else>{{ cellValue(column, record) }}</template></template></a-table></a-card>
    </div>
    <a-modal v-model:open="modalOpen" :title="`${editingId ? '编辑' : '新增'}${current.title}`" :confirm-loading="saving" width="720px" destroy-on-close @ok="save"><a-form ref="formRef" :model="form" layout="vertical"><div class="operation-form-grid"><a-form-item v-for="field in current.fields" :key="field.name" :label="field.label" :name="field.name" :rules="fieldRules(field)" :extra="field.extra" :class="{ 'full-field': field.type === 'textarea' }"><a-input v-if="field.type === 'input'" v-model:value="form[field.name]" :maxlength="field.max" /><a-textarea v-else-if="field.type === 'textarea'" v-model:value="form[field.name]" :rows="field.rows || 4" :maxlength="field.max" show-count /><a-input-number v-else-if="field.type === 'number'" v-model:value="form[field.name]" :min="field.min" :precision="field.precision" style="width: 100%" /><a-select v-else-if="field.type === 'select'" v-model:value="form[field.name]" :options="field.options" /><a-radio-group v-else-if="field.type === 'radio'" v-model:value="form[field.name]" :options="field.options" /><a-date-picker v-else-if="field.type === 'date'" v-model:value="form[field.name]" show-time style="width: 100%" /></a-form-item></div></a-form></a-modal>
  </div>
</template>

<style scoped>
.operation-tabs-card { margin-bottom: 18px; }
.operation-tabs-card :deep(.ant-card-body) { padding: 0 24px; }
.operation-tabs-card :deep(.ant-tabs-nav) { margin-bottom: 0; }
.operation-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.operation-form-grid .full-field { grid-column: 1 / -1; }
@media (max-width: 600px) { .operation-form-grid { grid-template-columns: 1fr; } }
</style>
