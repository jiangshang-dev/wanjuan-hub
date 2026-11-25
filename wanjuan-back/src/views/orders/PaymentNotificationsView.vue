<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { adminPaymentNotificationApi } from '../../api/trade'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate } from '../../utils'
import { channelName, notificationStatus, notificationStatusOptions, paymentChannelOptions } from '../../utils/trade'

const filters = reactive({ notificationId: '', orderNo: '', channel: undefined, processStatus: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminPaymentNotificationApi.list, () => ({
  notificationId: filters.notificationId.trim() || undefined,
  orderNo: filters.orderNo.trim() || undefined,
  channel: filters.channel,
  processStatus: filters.processStatus,
}))
const detailOpen = ref(false)
const detailLoading = ref(false)
const current = ref(null)
const columns = [
  { title: '通知 ID', dataIndex: 'notificationId', key: 'notificationId', width: 260 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 240 },
  { title: '支付渠道', key: 'channel', width: 110 },
  { title: '事件类型', dataIndex: 'eventType', key: 'eventType', width: 160 },
  { title: '处理状态', key: 'processStatus', width: 110 },
  { title: '渠道交易号', dataIndex: 'channelTradeNo', key: 'channelTradeNo', width: 220 },
  { title: '接收时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 90, fixed: 'right' },
]

function resetFilters() {
  Object.assign(filters, { notificationId: '', orderNo: '', channel: undefined, processStatus: undefined })
  search()
}

async function openDetail(record) {
  detailOpen.value = true
  detailLoading.value = true
  current.value = null
  try {
    current.value = await adminPaymentNotificationApi.detail(record.notificationId)
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
    <a-alert message="仅展示支付通知的处理元数据，后端不会返回可能包含敏感信息的原始通知报文。" type="info" show-icon class="page-alert" />
    <div class="toolbar">
      <a-space wrap>
        <a-input-search v-model:value="filters.notificationId" allow-clear placeholder="完整通知 ID" style="width: 250px" @search="search" />
        <a-input-search v-model:value="filters.orderNo" allow-clear placeholder="完整订单号" style="width: 240px" @search="search" />
        <a-select v-model:value="filters.channel" allow-clear placeholder="全部支付渠道" style="width: 140px" :options="paymentChannelOptions" />
        <a-select v-model:value="filters.processStatus" allow-clear placeholder="全部处理状态" style="width: 140px" :options="notificationStatusOptions" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>
      <a-button @click="load()"><ReloadOutlined /></a-button>
    </div>
    <a-card :bordered="false" class="table-card">
      <a-table row-key="notificationId" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1500 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'notificationId' || column.key === 'orderNo' || column.key === 'channelTradeNo'"><span class="table-code">{{ record[column.key] || '-' }}</span></template>
          <template v-else-if="column.key === 'channel'">{{ channelName(record.channel) }}</template>
          <template v-else-if="column.key === 'processStatus'"><a-tag :color="notificationStatus(record.processStatus).color">{{ notificationStatus(record.processStatus).text }}</a-tag></template>
          <template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime) }}</template>
          <template v-else-if="column.key === 'actions'"><a-button v-permission="'payment:notification:list'" type="link" size="small" @click="openDetail(record)">详情</a-button></template>
          <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="支付通知元数据" width="680">
      <a-spin :spinning="detailLoading">
        <a-descriptions v-if="current" :column="2" size="small" bordered>
          <a-descriptions-item label="通知 ID" :span="2"><span class="table-code">{{ current.notificationId }}</span></a-descriptions-item>
          <a-descriptions-item label="订单号" :span="2"><span class="table-code">{{ current.orderNo }}</span></a-descriptions-item>
          <a-descriptions-item label="支付渠道">{{ channelName(current.channel) }}</a-descriptions-item>
          <a-descriptions-item label="处理状态"><a-tag :color="notificationStatus(current.processStatus).color">{{ notificationStatus(current.processStatus).text }}</a-tag></a-descriptions-item>
          <a-descriptions-item label="事件类型" :span="2">{{ current.eventType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="渠道交易号" :span="2"><span class="table-code">{{ current.channelTradeNo || '-' }}</span></a-descriptions-item>
          <a-descriptions-item label="通知记录 ID">{{ current.id }}</a-descriptions-item>
          <a-descriptions-item label="支付流水 ID">{{ current.paymentId ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="订单 ID">{{ current.orderId ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="处理完成时间">{{ formatDate(current.processedAt) }}</a-descriptions-item>
          <a-descriptions-item label="接收时间">{{ formatDate(current.createTime) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDate(current.updateTime) }}</a-descriptions-item>
          <a-descriptions-item v-if="current.errorMessage" label="处理错误" :span="2"><span class="error-detail-text">{{ current.errorMessage }}</span></a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-drawer>
  </div>
</template>
