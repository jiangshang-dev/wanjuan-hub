<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { usersApi } from '../../api'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf, isEnabled } from '../../utils'

const filters = reactive({ keyword: '', status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(usersApi.list, () => ({ keyword: filters.keyword.trim() || undefined, status: filters.status }))
const statusLoadingId = ref('')
const columns = [
  { title: '用户 ID', dataIndex: 'id', key: 'id', width: 180, ellipsis: true },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 150 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 150 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 210 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 140 },
  { title: '状态', key: 'status', width: 110 },
  { title: '注册时间', key: 'createdAt', width: 180 },
  { title: '最近登录', key: 'lastLoginTime', width: 180 },
]

onMounted(load)
async function changeStatus(record, checked) {
  statusLoadingId.value = idOf(record.id)
  try {
    await usersApi.updateStatus(idOf(record.id), checked ? 1 : 0)
    record.status = checked ? 1 : 0
    message.success('用户状态已更新')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusLoadingId.value = ''
  }
}
function resetFilters() {
  filters.keyword = ''
  filters.status = undefined
  search()
}
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input v-model:value="filters.keyword" allow-clear placeholder="搜索用户名、昵称或邮箱" style="width: 250px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input><a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 120px" :options="[{ value: 1, label: '正常' }, { value: 0, label: '禁用' }]" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-button @click="load()"><ReloadOutlined /></a-button></div>
    <a-alert type="info" show-icon message="用户状态变更会立即提交至后端；禁用后请由后端同步处理其登录会话。" class="page-alert" />
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1300 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'id'"><code class="table-code">{{ idOf(record.id) }}</code></template><template v-else-if="column.key === 'status'"><a-switch v-permission="['system:user:status', 'system:user:update']" :checked="isEnabled(record.status)" :loading="statusLoadingId === idOf(record.id)" checked-children="正常" un-checked-children="禁用" @change="(checked) => changeStatus(record, checked)" /></template><template v-else-if="column.key === 'createdAt'">{{ formatDate(record.createdAt || record.createTime) }}</template><template v-else-if="column.key === 'lastLoginTime'">{{ formatDate(record.lastLoginTime || record.lastLoginAt) }}</template><template v-else>{{ record[column.dataIndex] || '-' }}</template></template></a-table></a-card>
  </div>
</template>
