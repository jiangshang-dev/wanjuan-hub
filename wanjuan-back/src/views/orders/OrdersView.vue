<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { adminOrderApi, adminPaymentApi } from '../../api/trade'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate } from '../../utils'
import { channelName, grantStatus, money, orderStatus, orderStatusOptions, paymentStatus, reconciliationResult, refundStatus, sceneName } from '../../utils/trade'

const filters = reactive({ orderNo: '', userId: '', status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminOrderApi.list, () => ({
  orderNo: filters.orderNo.trim() || undefined,
  userId: filters.userId.trim() || undefined,
  status: filters.status,
}))
const detailOpen = ref(false)
const detailLoading = ref(false)
const current = ref(null)
const reconciling = ref(false)
const reconciliationOpen = ref(false)
const reconciliation = ref(null)
const itemColumns = [
  { title: '资源名称', dataIndex: 'resourceName', key: 'resourceName' },
  { title: '资源 ID', dataIndex: 'resourceId', key: 'resourceId', width: 180 },
  { title: '下单价格', key: 'price', width: 130 },
]
const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 260 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 160 },
  { title: '资源 ID', dataIndex: 'resourceId', key: 'resourceId', width: 160 },
  { title: '订单金额', key: 'amount', width: 130 },
  { title: '订单状态', key: 'status', width: 110 },
  { title: '授权状态', key: 'grantStatus', width: 110 },
  { title: '创建时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 110, fixed: 'right' },
]

function resetFilters() {
  Object.assign(filters, { orderNo: '', userId: '', status: undefined })
  search()
}

async function openDetail(record) {
  detailOpen.value = true
  detailLoading.value = true
  try {
    current.value = await adminOrderApi.detail(record.orderNo)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    detailLoading.value = false
  }
}

async function reconcile() {
  if (!current.value?.orderNo) return
  reconciling.value = true
  try {
    reconciliation.value = await adminPaymentApi.reconcile(current.value.orderNo)
    reconciliationOpen.value = true
    current.value = await adminOrderApi.detail(current.value.orderNo)
    load()
    message.success('渠道查单与对账已完成')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    reconciling.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="management-page">
    <div class="toolbar">
      <a-space wrap>
        <a-input-search v-model:value="filters.orderNo" allow-clear placeholder="完整订单号" style="width: 250px" @search="search" />
        <a-input v-model:value="filters.userId" allow-clear placeholder="购买用户 ID" style="width: 170px" @press-enter="search" />
        <a-select v-model:value="filters.status" allow-clear placeholder="全部订单状态" style="width: 150px" :options="orderStatusOptions" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>
      <a-button @click="load()"><ReloadOutlined /></a-button>
    </div>
    <a-card :bordered="false" class="table-card">
      <a-table row-key="orderNo" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1350 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo'"><span class="table-code">{{ record.orderNo }}</span></template>
          <template v-else-if="column.key === 'amount'"><strong>{{ money(record.totalAmount, record.currency) }}</strong></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="orderStatus(record.status).color">{{ orderStatus(record.status).text }}</a-tag></template>
          <template v-else-if="column.key === 'grantStatus'"><a-tag :color="grantStatus(record.grantStatus).color">{{ grantStatus(record.grantStatus).text }}</a-tag></template>
          <template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime) }}</template>
          <template v-else-if="column.key === 'actions'"><a-button v-permission="'order:detail'" type="link" size="small" @click="openDetail(record)">详情</a-button></template>
          <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="订单与支付详情" width="760">
      <a-spin :spinning="detailLoading">
        <template v-if="current">
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="订单号" :span="2"><span class="table-code">{{ current.orderNo }}</span></a-descriptions-item>
            <a-descriptions-item label="订单状态"><a-tag :color="orderStatus(current.status).color">{{ orderStatus(current.status).text }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="授权状态"><a-tag :color="grantStatus(current.grantStatus).color">{{ grantStatus(current.grantStatus).text }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="订单金额">{{ money(current.totalAmount, current.currency) }}</a-descriptions-item>
            <a-descriptions-item label="客户端">{{ current.clientType }}</a-descriptions-item>
            <a-descriptions-item label="用户 ID">{{ current.userId }}</a-descriptions-item>
            <a-descriptions-item label="资源 ID">{{ current.resourceId }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDate(current.createTime) }}</a-descriptions-item>
            <a-descriptions-item label="支付截止">{{ formatDate(current.expireAt) }}</a-descriptions-item>
            <a-descriptions-item label="支付时间">{{ formatDate(current.paidAt) }}</a-descriptions-item>
            <a-descriptions-item label="关闭时间">{{ formatDate(current.closedAt) }}</a-descriptions-item>
            <a-descriptions-item v-if="current.closeReason" label="关闭原因" :span="2">{{ current.closeReason }}</a-descriptions-item>
          </a-descriptions>

          <section class="trade-detail-section">
            <h3>订单明细</h3>
            <a-table row-key="resourceId" :columns="itemColumns" :data-source="current.items || []" :pagination="false" size="small">
              <template #bodyCell="{ column, record }"><template v-if="column.key === 'price'">{{ money(record.price, current.currency) }}</template><template v-else>{{ record[column.dataIndex] ?? '-' }}</template></template>
            </a-table>
          </section>

          <section class="trade-detail-section">
            <div class="trade-section-heading"><h3>支付流水</h3><a-button v-if="current.payment" v-permission="'payment:reconcile'" type="primary" size="small" :loading="reconciling" @click="reconcile">渠道查单并对账</a-button></div>
            <a-empty v-if="!current.payment" description="订单尚未发起支付" />
            <a-descriptions v-else :column="2" size="small" bordered>
              <a-descriptions-item label="支付单号" :span="2"><span class="table-code">{{ current.payment.paymentNo }}</span></a-descriptions-item>
              <a-descriptions-item label="支付状态"><a-tag :color="paymentStatus(current.payment.status).color">{{ paymentStatus(current.payment.status).text }}</a-tag></a-descriptions-item>
              <a-descriptions-item label="支付渠道">{{ channelName(current.payment.channel) }} / {{ sceneName(current.payment.scene) }}</a-descriptions-item>
              <a-descriptions-item label="支付金额">{{ money(current.payment.amount, current.payment.currency) }}</a-descriptions-item>
              <a-descriptions-item label="渠道状态">{{ current.payment.channelStatus || '-' }}</a-descriptions-item>
              <a-descriptions-item label="渠道交易号" :span="2">{{ current.payment.channelTradeNo || '-' }}</a-descriptions-item>
              <a-descriptions-item label="支付时间">{{ formatDate(current.payment.paidAt) }}</a-descriptions-item>
              <a-descriptions-item label="最近查单">{{ formatDate(current.payment.lastQueryAt) }}</a-descriptions-item>
              <a-descriptions-item v-if="current.payment.errorMessage" label="渠道错误" :span="2"><span class="error-detail-text">{{ current.payment.errorMessage }}</span></a-descriptions-item>
            </a-descriptions>
          </section>

          <section v-if="current.refund" class="trade-detail-section">
            <h3>退款信息</h3>
            <a-descriptions :column="2" size="small" bordered>
              <a-descriptions-item label="退款单号" :span="2"><span class="table-code">{{ current.refund.refundNo }}</span></a-descriptions-item>
              <a-descriptions-item label="退款状态"><a-tag :color="refundStatus(current.refund.status).color">{{ refundStatus(current.refund.status).text }}</a-tag></a-descriptions-item>
              <a-descriptions-item label="退款金额">{{ money(current.refund.amount, current.refund.currency) }}</a-descriptions-item>
              <a-descriptions-item label="退款原因" :span="2">{{ current.refund.reason }}</a-descriptions-item>
              <a-descriptions-item label="审核意见" :span="2">{{ current.refund.reviewOpinion || '-' }}</a-descriptions-item>
            </a-descriptions>
          </section>
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
.trade-detail-section { margin-top: 26px; }
.trade-detail-section h3 { margin: 0 0 14px; font-size: 16px; }
.trade-section-heading { display: flex; margin-bottom: 14px; align-items: center; justify-content: space-between; gap: 16px; }
.trade-section-heading h3 { margin: 0; }
</style>
