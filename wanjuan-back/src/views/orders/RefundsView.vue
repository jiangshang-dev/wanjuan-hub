<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { adminRefundApi } from '../../api/trade'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf } from '../../utils'
import { channelName, money, refundStatus, refundStatusOptions } from '../../utils/trade'

const filters = reactive({ status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminRefundApi.list, () => ({ status: filters.status }))
const detailOpen = ref(false)
const current = ref(null)
const reviewOpen = ref(false)
const reviewSaving = ref(false)
const syncingNo = ref('')
const reviewForm = reactive({ id: '', action: 'APPROVE', opinion: '' })
const columns = [
  { title: '退款单号', dataIndex: 'refundNo', key: 'refundNo', width: 250 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 250 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 150 },
  { title: '退款金额', key: 'amount', width: 130 },
  { title: '支付渠道', key: 'channel', width: 110 },
  { title: '退款状态', key: 'status', width: 120 },
  { title: '申请时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 190, fixed: 'right' },
]

function canSync(record) {
  return ['APPROVED', 'PROCESSING', 'ABNORMAL'].includes(record.status)
}

function openDetail(record) {
  current.value = record
  detailOpen.value = true
}

function openReview(record) {
  Object.assign(reviewForm, { id: idOf(record.id), action: 'APPROVE', opinion: '' })
  reviewOpen.value = true
}

async function submitReview() {
  reviewSaving.value = true
  try {
    const updated = await adminRefundApi.review(reviewForm.id, { action: reviewForm.action, opinion: reviewForm.opinion.trim() || null })
    if (current.value?.id === updated.id) current.value = updated
    reviewOpen.value = false
    load()
    message.success(reviewForm.action === 'APPROVE' ? '已同意申请并提交真实渠道退款' : '已拒绝退款申请')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    reviewSaving.value = false
  }
}

async function syncRefund(record) {
  syncingNo.value = record.refundNo
  try {
    const updated = await adminRefundApi.sync(record.refundNo)
    if (current.value?.refundNo === updated.refundNo) current.value = updated
    load()
    message.success('已查询并同步渠道退款状态')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    syncingNo.value = ''
  }
}

onMounted(() => load())
</script>

<template>
  <div class="management-page">
    <a-alert message="同意退款会立即调用真实支付渠道；退款是否成功以渠道结果和同步状态为准。" type="warning" show-icon class="page-alert" />
    <div class="toolbar">
      <a-space wrap>
        <a-select v-model:value="filters.status" allow-clear placeholder="全部退款状态" style="width: 170px" :options="refundStatusOptions" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="filters.status = undefined; search()">重置</a-button>
      </a-space>
      <a-button @click="load()"><ReloadOutlined /></a-button>
    </div>
    <a-card :bordered="false" class="table-card">
      <a-table row-key="refundNo" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1400 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'refundNo' || column.key === 'orderNo'"><span class="table-code">{{ record[column.key] }}</span></template>
          <template v-else-if="column.key === 'amount'"><strong>{{ money(record.amount, record.currency) }}</strong></template>
          <template v-else-if="column.key === 'channel'">{{ channelName(record.channel) }}</template>
          <template v-else-if="column.key === 'status'"><a-tag :color="refundStatus(record.status).color">{{ refundStatus(record.status).text }}</a-tag></template>
          <template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime) }}</template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
              <a-button v-if="record.status === 'PENDING_REVIEW'" v-permission="'refund:review'" type="link" size="small" @click="openReview(record)">审核</a-button>
              <a-button v-if="canSync(record)" v-permission="'refund:sync'" type="link" size="small" :loading="syncingNo === record.refundNo" @click="syncRefund(record)">同步渠道</a-button>
            </a-space>
          </template>
          <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="退款详情" width="680">
      <template v-if="current">
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item label="退款单号" :span="2"><span class="table-code">{{ current.refundNo }}</span></a-descriptions-item>
          <a-descriptions-item label="订单号" :span="2"><span class="table-code">{{ current.orderNo }}</span></a-descriptions-item>
          <a-descriptions-item label="退款状态"><a-tag :color="refundStatus(current.status).color">{{ refundStatus(current.status).text }}</a-tag></a-descriptions-item>
          <a-descriptions-item label="退款金额">{{ money(current.amount, current.currency) }}</a-descriptions-item>
          <a-descriptions-item label="申请用户">用户 #{{ current.userId }}</a-descriptions-item>
          <a-descriptions-item label="支付渠道">{{ channelName(current.channel) }}</a-descriptions-item>
          <a-descriptions-item label="渠道状态">{{ current.channelStatus || '-' }}</a-descriptions-item>
          <a-descriptions-item label="授权状态">{{ current.entitlementRevoked ? '已撤销' : '未撤销' }}</a-descriptions-item>
          <a-descriptions-item label="渠道退款号" :span="2">{{ current.channelRefundNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="退款原因" :span="2">{{ current.reason }}</a-descriptions-item>
          <a-descriptions-item label="审核意见" :span="2">{{ current.reviewOpinion || '-' }}</a-descriptions-item>
          <a-descriptions-item label="申请时间">{{ formatDate(current.createTime) }}</a-descriptions-item>
          <a-descriptions-item label="审核时间">{{ formatDate(current.reviewedAt) }}</a-descriptions-item>
          <a-descriptions-item label="退款成功时间" :span="2">{{ formatDate(current.refundedAt) }}</a-descriptions-item>
          <a-descriptions-item v-if="current.errorMessage" label="渠道错误" :span="2"><span class="error-detail-text">{{ current.errorMessage }}</span></a-descriptions-item>
        </a-descriptions>
        <a-space class="drawer-actions">
          <a-button v-if="current.status === 'PENDING_REVIEW'" v-permission="'refund:review'" type="primary" @click="openReview(current)">审核退款</a-button>
          <a-button v-if="canSync(current)" v-permission="'refund:sync'" :loading="syncingNo === current.refundNo" @click="syncRefund(current)">同步渠道状态</a-button>
        </a-space>
      </template>
    </a-drawer>

    <a-modal v-model:open="reviewOpen" title="退款审核" ok-text="提交审核" cancel-text="取消" :confirm-loading="reviewSaving" @ok="submitReview">
      <a-alert :message="reviewForm.action === 'APPROVE' ? '提交后将调用真实渠道发起全额退款，请核对订单与退款原因。' : '拒绝后订单恢复为已支付状态，请填写审核意见。'" :type="reviewForm.action === 'APPROVE' ? 'warning' : 'info'" show-icon style="margin-bottom: 20px" />
      <a-form layout="vertical">
        <a-form-item label="审核动作" required>
          <a-radio-group v-model:value="reviewForm.action"><a-radio-button value="APPROVE">同意退款</a-radio-button><a-radio-button value="REJECT">拒绝退款</a-radio-button></a-radio-group>
        </a-form-item>
        <a-form-item label="审核意见"><a-textarea v-model:value="reviewForm.opinion" :rows="5" :maxlength="1000" show-count placeholder="记录审核依据或处理说明" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
