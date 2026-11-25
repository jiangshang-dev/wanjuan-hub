<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { adminPaymentApi } from '../../api/trade'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate } from '../../utils'
import { channelName, money, paymentChannelOptions, paymentStatus, paymentStatusOptions, reconciliationResult, sceneName } from '../../utils/trade'

const filters = reactive({ paymentNo: '', orderNo: '', channel: undefined, status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminPaymentApi.list, () => ({
  paymentNo: filters.paymentNo.trim() || undefined,
  orderNo: filters.orderNo.trim() || undefined,
  channel: filters.channel,
  status: filters.status,
}))
const detailOpen = ref(false)
const detailLoading = ref(false)
const current = ref(null)
const reconcilingNo = ref('')
const reconciliationOpen = ref(false)
const reconciliation = ref(null)
const columns = [
  { title: '支付单号', dataIndex: 'paymentNo', key: 'paymentNo', width: 250 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 250 },
  { title: '渠道 / 场景', key: 'channel', width: 140 },
  { title: '支付金额', key: 'amount', width: 130 },
  { title: '支付状态', key: 'status', width: 110 },
  { title: '渠道状态', dataIndex: 'channelStatus', key: 'channelStatus', width: 140 },
  { title: '支付时间', key: 'paidAt', width: 180 },
  { title: '操作', key: 'actions', width: 210, fixed: 'right' },
]

function resetFilters() {
  Object.assign(filters, { paymentNo: '', orderNo: '', channel: undefined, status: undefined })
  search()
}

async function openDetail(record) {
  detailOpen.value = true
  detailLoading.value = true
  current.value = null
  try {
    current.value = await adminPaymentApi.detail(record.paymentNo)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    detailLoading.value = false
  }
}

async function reconcile(record) {
  if (!record.orderNo) return
  reconcilingNo.value = record.paymentNo
  try {
    reconciliation.value = await adminPaymentApi.reconcile(record.orderNo)
    reconciliationOpen.value = true
    if (current.value?.paymentNo === record.paymentNo) {
      current.value = await adminPaymentApi.detail(record.paymentNo)
    }
    load()
    message.success('渠道查单与对账已完成')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    reconcilingNo.value = ''
  }
}

onMounted(() => load())
</script>

<template>
  <div class="management-page">
    <a-alert message="手动对账会向真实支付渠道查单并生成对账记录；支付结果以服务端和渠道返回为准。" type="info" show-icon class="page-alert" />
    <div class="toolbar">
      <a-space wrap>
        <a-input-search v-model:value="filters.paymentNo" allow-clear placeholder="完整支付单号" style="width: 240px" @search="search" />
        <a-input-search v-model:value="filters.orderNo" allow-clear placeholder="完整订单号" style="width: 240px" @search="search" />
        <a-select v-model:value="filters.channel" allow-clear placeholder="全部支付渠道" style="width: 140px" :options="paymentChannelOptions" />
        <a-select v-model:value="filters.status" allow-clear placeholder="全部支付状态" style="width: 140px" :options="paymentStatusOptions" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>
      <a-button @click="load()"><ReloadOutlined /></a-button>
    </div>
    <a-card :bordered="false" class="table-card">
      <a-table row-key="paymentNo" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1500 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'paymentNo' || column.key === 'orderNo'"><span class="table-code">{{ record[column.key] }}</span></template>
          <template v-else-if="column.key === 'channel'">{{ channelName(record.channel) }} / {{ sceneName(record.scene) }}</template>
          <template v-else-if="column.key === 'amount'"><strong>{{ money(record.amount, record.currency) }}</strong></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="paymentStatus(record.status).color">{{ paymentStatus(record.status).text }}</a-tag></template>
          <template v-else-if="column.key === 'paidAt'">{{ formatDate(record.paidAt) }}</template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button v-permission="'payment:list'" type="link" size="small" @click="openDetail(record)">详情</a-button>
              <a-button v-permission="'payment:reconcile'" type="link" size="small" :loading="reconcilingNo === record.paymentNo" @click="reconcile(record)">查单对账</a-button>
            </a-space>
          </template>
          <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="支付流水详情" width="700">
      <a-spin :spinning="detailLoading">
        <template v-if="current">
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="支付单号" :span="2"><span class="table-code">{{ current.paymentNo }}</span></a-descriptions-item>
            <a-descriptions-item label="订单号" :span="2"><span class="table-code">{{ current.orderNo }}</span></a-descriptions-item>
            <a-descriptions-item label="支付状态"><a-tag :color="paymentStatus(current.status).color">{{ paymentStatus(current.status).text }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="支付金额">{{ money(current.amount, current.currency) }}</a-descriptions-item>
            <a-descriptions-item label="支付渠道">{{ channelName(current.channel) }}</a-descriptions-item>
            <a-descriptions-item label="支付场景">{{ sceneName(current.scene) }}</a-descriptions-item>
            <a-descriptions-item label="渠道状态">{{ current.channelStatus || '-' }}</a-descriptions-item>
            <a-descriptions-item label="币种">{{ current.currency || '-' }}</a-descriptions-item>
            <a-descriptions-item label="渠道交易号" :span="2"><span class="table-code">{{ current.channelTradeNo || '-' }}</span></a-descriptions-item>
            <a-descriptions-item label="支付流水 ID">{{ current.id }}</a-descriptions-item>
            <a-descriptions-item label="订单 ID">{{ current.orderId }}</a-descriptions-item>
            <a-descriptions-item label="支付完成时间">{{ formatDate(current.paidAt) }}</a-descriptions-item>
            <a-descriptions-item label="最近主动查单">{{ formatDate(current.lastQueryAt) }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDate(current.createTime) }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ formatDate(current.updateTime) }}</a-descriptions-item>
            <a-descriptions-item v-if="current.errorMessage" label="渠道错误" :span="2"><span class="error-detail-text">{{ current.errorMessage }}</span></a-descriptions-item>
          </a-descriptions>
          <a-button v-permission="'payment:reconcile'" type="primary" class="drawer-action" :loading="reconcilingNo === current.paymentNo" @click="reconcile(current)">渠道查单并对账</a-button>
        </template>
      </a-spin>
    </a-drawer>

    <a-modal v-model:open="reconciliationOpen" title="支付对账结果" :footer="null" width="680">
      <template v-if="reconciliation">
        <a-alert :message="reconciliation.detail" :type="reconciliation.result === 'MISMATCH' ? 'error' : 'info'" show-icon style="margin-bottom: 20px" />
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item label="对账号" :span="2"><span class="table-code">{{ reconciliation.reconciliationNo }}</span></a-descriptions-item>
          <a-descriptions-item label="对账结果"><a-tag :color="reconciliationResult(reconciliation.result).color">{{ reconciliationResult(reconciliation.result).text }}</a-tag></a-descriptions-item>
          <a-descriptions-item label="触发方式">{{ reconciliation.triggeredBy }}</a-descriptions-item>
          <a-descriptions-item label="本地金额">{{ money(reconciliation.expectedAmount) }}</a-descriptions-item>
          <a-descriptions-item label="渠道金额">{{ money(reconciliation.channelAmount) }}</a-descriptions-item>
          <a-descriptions-item label="本地状态">{{ reconciliation.expectedStatus }}</a-descriptions-item>
          <a-descriptions-item label="渠道状态">{{ reconciliation.channelStatus || '-' }}</a-descriptions-item>
          <a-descriptions-item label="渠道交易号" :span="2">{{ reconciliation.channelTradeNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="对账时间" :span="2">{{ formatDate(reconciliation.reconciledAt) }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-modal>
  </div>
</template>

<style scoped>
.drawer-action { margin-top: 24px; }
</style>
