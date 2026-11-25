<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { operationLogsApi } from '../../api'
import { useTable } from '../../composables/useTable'
import { formatDate } from '../../utils'

const filters = reactive({ keyword: '', dateRange: null })
const { loading, records, pagination, load, changeTable, search } = useTable(operationLogsApi.list, () => ({
  keyword: filters.keyword.trim() || undefined,
  startTime: filters.dateRange?.[0] ? filters.dateRange[0].startOf('day').format('YYYY-MM-DD HH:mm:ss') : undefined,
  endTime: filters.dateRange?.[1] ? filters.dateRange[1].endOf('day').format('YYYY-MM-DD HH:mm:ss') : undefined,
}))
const detailOpen = ref(false)
const currentLog = ref(null)
const columns = [
  { title: '操作时间', key: 'createdAt', width: 180 },
  { title: '操作人', key: 'operator', width: 140 },
  { title: '模块', dataIndex: 'module', key: 'module', width: 130 },
  { title: '操作', key: 'operation', width: 180 },
  { title: '请求', key: 'request', width: 260 },
  { title: 'IP 地址', key: 'ip', width: 140 },
  { title: '结果', key: 'status', width: 90 },
  { title: '耗时', key: 'duration', width: 100 },
  { title: '详情', key: 'detail', width: 80, fixed: 'right' },
]

onMounted(load)
function resetFilters() {
  filters.keyword = ''
  filters.dateRange = null
  search()
}
function openDetail(record) {
  currentLog.value = record
  detailOpen.value = true
}
function successStatus(status) {
  return status === 1 || status === '1' || status === 'SUCCESS' || status === true || status === 200 || status === '200'
}
function formatJson(value) {
  if (!value) return '-'
  if (typeof value === 'object') return JSON.stringify(value, null, 2)
  try { return JSON.stringify(JSON.parse(value), null, 2) } catch { return String(value) }
}
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input v-model:value="filters.keyword" allow-clear placeholder="搜索操作人、模块或请求路径" style="width: 260px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input><a-range-picker v-model:value="filters.dateRange" :placeholder="['开始日期', '结束日期']" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-button @click="load()"><ReloadOutlined /></a-button></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1350 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'createdAt'">{{ formatDate(record.createdAt || record.createTime || record.operationTime) }}</template><template v-else-if="column.key === 'operator'">{{ record.adminName || record.username || record.operator || '-' }}</template><template v-else-if="column.key === 'operation'">{{ record.operation || record.title || record.action || '-' }}</template><template v-else-if="column.key === 'request'"><div class="request-cell"><a-tag v-if="record.method || record.requestMethod">{{ record.method || record.requestMethod }}</a-tag><code class="table-code">{{ record.path || record.requestUri || record.url || '-' }}</code></div></template><template v-else-if="column.key === 'ip'">{{ record.ip || record.ipAddress || record.operationIp || '-' }}</template><template v-else-if="column.key === 'status'"><a-tag :color="successStatus(record.status) ? 'success' : 'error'">{{ successStatus(record.status) ? '成功' : '失败' }}</a-tag></template><template v-else-if="column.key === 'duration'">{{ record.duration ?? record.costTime ?? record.executionTime ?? '-' }}<span v-if="record.duration != null || record.costTime != null || record.executionTime != null"> ms</span></template><template v-else-if="column.key === 'detail'"><a-button type="link" size="small" @click="openDetail(record)">查看</a-button></template><template v-else>{{ record[column.dataIndex] || '-' }}</template></template></a-table></a-card>
    <a-drawer v-model:open="detailOpen" title="操作日志详情" width="560"><a-descriptions v-if="currentLog" :column="1" bordered size="small"><a-descriptions-item label="操作人">{{ currentLog.adminName || currentLog.username || currentLog.operator || '-' }}</a-descriptions-item><a-descriptions-item label="操作">{{ currentLog.operation || currentLog.title || currentLog.action || '-' }}</a-descriptions-item><a-descriptions-item label="请求地址">{{ currentLog.path || currentLog.requestUri || currentLog.url || '-' }}</a-descriptions-item><a-descriptions-item label="IP 地址">{{ currentLog.ip || currentLog.ipAddress || currentLog.operationIp || '-' }}</a-descriptions-item><a-descriptions-item label="时间">{{ formatDate(currentLog.createdAt || currentLog.createTime || currentLog.operationTime) }}</a-descriptions-item></a-descriptions><div v-if="currentLog" class="log-detail-section"><h4>请求参数</h4><pre>{{ formatJson(currentLog.params || currentLog.requestParams || currentLog.requestBody) }}</pre><h4>响应内容</h4><pre>{{ formatJson(currentLog.response || currentLog.responseBody || currentLog.result) }}</pre><template v-if="currentLog.errorMessage || currentLog.error"><h4>错误信息</h4><pre class="error-detail">{{ currentLog.errorMessage || currentLog.error }}</pre></template></div></a-drawer>
  </div>
</template>
