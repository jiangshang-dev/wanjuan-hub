<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { adminEntitlementApi } from '../../api/trade'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf } from '../../utils'
import { entitlementSource, entitlementStatus, entitlementStatusOptions, money } from '../../utils/trade'

const filters = reactive({ userId: '', resourceId: '', sourceOrderNo: '', status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminEntitlementApi.list, () => ({
  userId: filters.userId.trim() || undefined,
  resourceId: filters.resourceId.trim() || undefined,
  sourceOrderNo: filters.sourceOrderNo.trim() || undefined,
  status: filters.status,
}))
const detailOpen = ref(false)
const detailLoading = ref(false)
const current = ref(null)
const columns = [
  { title: '授权 ID', dataIndex: 'id', key: 'id', width: 170 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 170 },
  { title: '资源 ID', dataIndex: 'resourceId', key: 'resourceId', width: 170 },
  { title: '权益来源', key: 'source', width: 240 },
  { title: '支付金额 / 积分', key: 'consideration', width: 170 },
  { title: '授权状态', key: 'status', width: 110 },
  { title: '授权时间', key: 'grantedAt', width: 180 },
  { title: '撤销时间', key: 'revokedAt', width: 180 },
  { title: '操作', key: 'actions', width: 90, fixed: 'right' },
]

function resetFilters() {
  Object.assign(filters, { userId: '', resourceId: '', sourceOrderNo: '', status: undefined })
  search()
}

async function openDetail(record) {
  detailOpen.value = true
  detailLoading.value = true
  current.value = null
  try {
    current.value = await adminEntitlementApi.detail(idOf(record.id))
  } catch (error) {
    message.error(errorText(error))
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="management-page">
    <a-alert message="这里展示由已支付订单或积分兑换产生的真实资源访问权益；授权撤销由退款等后端业务流程执行。" type="info" show-icon class="page-alert" />
    <div class="toolbar">
      <a-space wrap>
        <a-input v-model:value="filters.userId" allow-clear placeholder="用户 ID" style="width: 170px" @press-enter="search" />
        <a-input v-model:value="filters.resourceId" allow-clear placeholder="资源 ID" style="width: 170px" @press-enter="search" />
        <a-input-search v-model:value="filters.sourceOrderNo" allow-clear placeholder="完整来源订单号" style="width: 240px" @search="search" />
        <a-select v-model:value="filters.status" allow-clear placeholder="全部授权状态" style="width: 140px" :options="entitlementStatusOptions" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>
      <a-button @click="load()"><ReloadOutlined /></a-button>
    </div>
    <a-card :bordered="false" class="table-card">
      <a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1500 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id' || column.key === 'userId' || column.key === 'resourceId'"><span class="table-code">{{ record[column.key] }}</span></template>
          <template v-else-if="column.key === 'source'"><div>{{ entitlementSource(record.sourceType) }}</div><small class="table-code">{{ record.sourceOrderNo || '-' }}</small></template>
          <template v-else-if="column.key === 'consideration'">
            <span v-if="record.amount !== null && record.amount !== undefined">{{ money(record.amount) }}</span>
            <span v-else-if="record.pointsAmount !== null && record.pointsAmount !== undefined">{{ record.pointsAmount }} 积分</span>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'status'"><a-tag :color="entitlementStatus(record.status).color">{{ entitlementStatus(record.status).text }}</a-tag></template>
          <template v-else-if="column.key === 'grantedAt' || column.key === 'revokedAt'">{{ formatDate(record[column.key]) }}</template>
          <template v-else-if="column.key === 'actions'"><a-button v-permission="'resource:entitlement:list'" type="link" size="small" @click="openDetail(record)">详情</a-button></template>
          <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="资源授权详情" width="680">
      <a-spin :spinning="detailLoading">
        <a-descriptions v-if="current" :column="2" size="small" bordered>
          <a-descriptions-item label="授权状态"><a-tag :color="entitlementStatus(current.status).color">{{ entitlementStatus(current.status).text }}</a-tag></a-descriptions-item>
          <a-descriptions-item label="权益来源">{{ entitlementSource(current.sourceType) }}</a-descriptions-item>
          <a-descriptions-item label="授权记录 ID">{{ current.id }}</a-descriptions-item>
          <a-descriptions-item label="用户 ID">{{ current.userId }}</a-descriptions-item>
          <a-descriptions-item label="资源 ID" :span="2">{{ current.resourceId }}</a-descriptions-item>
          <a-descriptions-item label="来源订单号" :span="2"><span class="table-code">{{ current.sourceOrderNo || '-' }}</span></a-descriptions-item>
          <a-descriptions-item label="支付金额">{{ money(current.amount) }}</a-descriptions-item>
          <a-descriptions-item label="消耗积分">{{ current.pointsAmount ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="授权时间">{{ formatDate(current.grantedAt) }}</a-descriptions-item>
          <a-descriptions-item label="撤销时间">{{ formatDate(current.revokedAt) }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDate(current.createTime) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDate(current.updateTime) }}</a-descriptions-item>
          <a-descriptions-item v-if="current.revokeReason" label="撤销原因" :span="2">{{ current.revokeReason }}</a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-drawer>
  </div>
</template>
