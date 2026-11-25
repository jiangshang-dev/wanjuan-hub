<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { adminReconciliationApi } from '../../api/trade'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate } from '../../utils'
import { channelName, money, paymentChannelOptions, reconciliationResult, reconciliationResultOptions } from '../../utils/trade'

const filters = reactive({ reconciliationNo: '', orderNo: '', channel: undefined, result: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminReconciliationApi.list, () => ({
  reconciliationNo: filters.reconciliationNo.trim() || undefined,
  orderNo: filters.orderNo.trim() || undefined,
  channel: filters.channel,
  result: filters.result,
}))
const detailOpen = ref(false)
const detailLoading = ref(false)
const current = ref(null)
const columns = [
  { title: '对账号', dataIndex: 'reconciliationNo', key: 'reconciliationNo', width: 250 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 240 },
  { title: '支付渠道', key: 'channel', width: 110 },
  { title: '对账结果', key: 'result', width: 120 },
  { title: '本地金额', key: 'expectedAmount', width: 120 },
  { title: '渠道金额', key: 'channelAmount', width: 120 },
  { title: '触发方式', dataIndex: 'triggeredBy', key: 'triggeredBy', width: 130 },
  { title: '对账时间', key: 'reconciledAt', width: 180 },
  { title: '操作', key: 'actions', width: 90, fixed: 'right' },
]

function resetFilters() {
  Object.assign(filters, { reconciliationNo: '', orderNo: '', channel: undefined, result: undefined })
  search()
}

async function openDetail(record) {
  detailOpen.value = true
  detailLoading.value = true
  current.value = null
  try {
    current.value = await adminReconciliationApi.detail(record.reconciliationNo)
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
    <a-alert message="对账记录由真实渠道查单生成；“已修复”表示后端已依据渠道结果修正本地支付或订单状态。" type="info" show-icon class="page-alert" />
    <div class="toolbar">
      <a-space wrap>
        <a-input-search v-model:value="filters.reconciliationNo" allow-clear placeholder="完整对账号" style="width: 240px" @search="search" />
        <a-input-search v-model:value="filters.orderNo" allow-clear placeholder="完整订单号" style="width: 240px" @search="search" />
        <a-select v-model:value="filters.channel" allow-clear placeholder="全部支付渠道" style="width: 140px" :options="paymentChannelOptions" />
        <a-select v-model:value="filters.result" allow-clear placeholder="全部对账结果" style="width: 150px" :options="reconciliationResultOptions" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>
      <a-button @click="load()"><ReloadOutlined /></a-button>
    </div>
    <a-card :bordered="false" class="table-card">
      <a-table row-key="reconciliationNo" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1450 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'reconciliationNo' || column.key === 'orderNo'"><span class="table-code">{{ record[column.key] }}</span></template>
          <template v-else-if="column.key === 'channel'">{{ channelName(record.channel) }}</template>
          <template v-else-if="column.key === 'result'"><a-tag :color="reconciliationResult(record.result).color">{{ reconciliationResult(record.result).text }}</a-tag></template>
          <template v-else-if="column.key === 'expectedAmount'">{{ money(record.expectedAmount) }}</template>
          <template v-else-if="column.key === 'channelAmount'">{{ money(record.channelAmount) }}</template>
          <template v-else-if="column.key === 'reconciledAt'">{{ formatDate(record.reconciledAt) }}</template>
          <template v-else-if="column.key === 'actions'"><a-button v-permission="'payment:reconciliation:list'" type="link" size="small" @click="openDetail(record)">详情</a-button></template>
          <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="支付对账记录" width="700">
      <a-spin :spinning="detailLoading">
        <template v-if="current">
          <a-alert :message="current.detail || '暂无对账说明'" :type="current.result === 'MISMATCH' ? 'error' : 'info'" show-icon style="margin-bottom: 20px" />
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="对账号" :span="2"><span class="table-code">{{ current.reconciliationNo }}</span></a-descriptions-item>
            <a-descriptions-item label="订单号" :span="2"><span class="table-code">{{ current.orderNo }}</span></a-descriptions-item>
            <a-descriptions-item label="对账结果"><a-tag :color="reconciliationResult(current.result).color">{{ reconciliationResult(current.result).text }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="支付渠道">{{ channelName(current.channel) }}</a-descriptions-item>
            <a-descriptions-item label="本地订单金额">{{ money(current.expectedAmount) }}</a-descriptions-item>
            <a-descriptions-item label="渠道支付金额">{{ money(current.channelAmount) }}</a-descriptions-item>
            <a-descriptions-item label="本地订单状态">{{ current.expectedStatus || '-' }}</a-descriptions-item>
            <a-descriptions-item label="渠道支付状态">{{ current.channelStatus || '-' }}</a-descriptions-item>
            <a-descriptions-item label="渠道交易号" :span="2"><span class="table-code">{{ current.channelTradeNo || '-' }}</span></a-descriptions-item>
            <a-descriptions-item label="触发方式">{{ current.triggeredBy || '-' }}</a-descriptions-item>
            <a-descriptions-item label="操作管理员 ID">{{ current.operatorId ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="对账记录 ID">{{ current.id }}</a-descriptions-item>
            <a-descriptions-item label="支付流水 ID">{{ current.paymentId ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="订单 ID">{{ current.orderId ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="对账时间">{{ formatDate(current.reconciledAt) }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDate(current.createTime) }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ formatDate(current.updateTime) }}</a-descriptions-item>
          </a-descriptions>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>
