<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { orderApi, refundApi } from '../../api/s5'
import { errorText, formatDate } from '../../utils/format'
import { money, orderStatus, orderStatusOptions, pageRecords, refundStatus, refundStatusOptions } from '../../utils/trade'

const route = useRoute()
const router = useRouter()
const activeTab = ref(route.query.tab === 'refunds' ? 'refunds' : 'orders')
const loading = ref(false)
const records = ref([])
const filters = reactive({ orderNo: '', status: undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` })
const statusOptions = computed(() => activeTab.value === 'orders' ? orderStatusOptions : refundStatusOptions)
const columns = computed(() => activeTab.value === 'orders' ? [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 260 },
  { title: '订单金额', key: 'amount', width: 130 },
  { title: '订单状态', key: 'status', width: 110 },
  { title: '资源授权', key: 'grant', width: 110 },
  { title: '创建时间', key: 'createTime', width: 170 },
  { title: '操作', key: 'actions', width: 130, fixed: 'right' },
] : [
  { title: '退款单号', dataIndex: 'refundNo', key: 'refundNo', width: 250 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 250 },
  { title: '退款金额', key: 'amount', width: 130 },
  { title: '退款状态', key: 'status', width: 120 },
  { title: '申请时间', key: 'createTime', width: 170 },
  { title: '操作', key: 'actions', width: 120, fixed: 'right' },
])

async function load(page = pagination.current, size = pagination.pageSize) {
  loading.value = true
  try {
    const params = { page, size, status: filters.status || undefined }
    const data = activeTab.value === 'orders'
      ? await orderApi.list({ ...params, orderNo: filters.orderNo.trim() || undefined })
      : await refundApi.list(params)
    records.value = pageRecords(data)
    pagination.current = Number(data?.page || page)
    pagination.pageSize = Number(data?.size || size)
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    records.value = []
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

function search() {
  load(1, pagination.pageSize)
}

function reset() {
  Object.assign(filters, { orderNo: '', status: undefined })
  search()
}

function changeTab(key) {
  activeTab.value = key
  Object.assign(filters, { orderNo: '', status: undefined })
  router.replace({ query: { ...route.query, tab: key } })
  load(1, pagination.pageSize)
}

function openOrder(record, pay = false) {
  router.push({ name: 'order-detail', params: { orderNo: record.orderNo }, query: pay ? { pay: '1' } : {} })
}

onMounted(() => load(1))
watch(() => route.query.tab, (tab) => {
  const next = tab === 'refunds' ? 'refunds' : 'orders'
  if (next !== activeTab.value) {
    activeTab.value = next
    load(1)
  }
})
</script>

<template>
  <div class="content-page order-center-page">
    <div class="page-heading">
      <div><span class="eyebrow">MY ORDERS</span><h1>订单中心</h1><p>查看服务端订单状态、支付进度与退款处理结果。</p></div>
      <a-button @click="router.push('/my/resources/entitlements')">已购资源</a-button>
    </div>
    <div class="content-card order-center-card">
      <a-tabs :active-key="activeTab" @change="changeTab">
        <a-tab-pane key="orders" tab="我的订单" />
        <a-tab-pane key="refunds" tab="退款记录" />
      </a-tabs>
      <div class="order-filters">
        <a-space wrap>
          <a-input-search v-if="activeTab === 'orders'" v-model:value="filters.orderNo" allow-clear placeholder="输入完整订单号" style="width: 280px" @search="search" />
          <a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 160px" :options="statusOptions" />
          <a-button type="primary" @click="search">查询</a-button>
          <a-button @click="reset">重置</a-button>
        </a-space>
      </div>
      <a-table :row-key="activeTab === 'orders' ? 'orderNo' : 'refundNo'" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1000 }" @change="(table) => load(table.current, table.pageSize)">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo' || column.key === 'refundNo'"><span class="trade-number">{{ record[column.key] }}</span></template>
          <template v-else-if="column.key === 'amount'"><strong class="trade-amount">{{ money(activeTab === 'orders' ? record.totalAmount : record.amount, record.currency) }}</strong></template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="activeTab === 'orders' ? orderStatus(record.status).color : refundStatus(record.status).color">{{ activeTab === 'orders' ? orderStatus(record.status).text : refundStatus(record.status).text }}</a-tag>
          </template>
          <template v-else-if="column.key === 'grant'">{{ { NONE: '未授权', PENDING: '授权中', GRANTED: '已授权', REVOKED: '已撤销' }[record.grantStatus] || record.grantStatus || '-' }}</template>
          <template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime, true) }}</template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button v-if="activeTab === 'orders' && record.status === 'PENDING_PAYMENT'" type="link" size="small" @click="openOrder(record, true)">去支付</a-button>
              <a-button type="link" size="small" @click="openOrder(record)">详情</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<style scoped>
.order-center-page { max-width: 1240px; }
.order-center-card { padding: 4px 24px 24px; background: #fff; }
.order-filters { margin-bottom: 20px; }
.trade-number { color: #46536d; font-family: "SFMono-Regular", Consolas, monospace; font-size: 12px; word-break: break-all; }
.trade-amount { color: #d4380d; white-space: nowrap; }
@media (max-width: 560px) {
  .order-center-card { padding: 4px 14px 18px; }
  .order-filters :deep(.ant-input-search) { width: 100% !important; }
}
</style>
