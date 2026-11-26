<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { adminWithdrawalApi } from '../../api/s6'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf } from '../../utils'
import { money } from '../../utils/trade'

const filters = reactive({ status: undefined, userId: '', withdrawalNo: '' })
const { loading, records, pagination, load, changeTable, search } = useTable(adminWithdrawalApi.list, () => ({
  status: filters.status,
  userId: filters.userId.trim() || undefined,
  withdrawalNo: filters.withdrawalNo.trim() || undefined,
}))
const statusOptions = [
  { value: 'PENDING', label: '待处理' },
  { value: 'SUCCEEDED', label: '转账已确认' },
  { value: 'REJECTED', label: '已拒绝并解冻' },
]
const columns = [
  { title: '提现单号', dataIndex: 'withdrawalNo', key: 'withdrawalNo', width: 240 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 180 },
  { title: '提现金额', key: 'amount', width: 130 },
  { title: '支付宝账号（脱敏）', dataIndex: 'alipayAccount', key: 'alipayAccount', width: 220 },
  { title: '状态', key: 'status', width: 140 },
  { title: '申请时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 160, fixed: 'right' },
]
const detailOpen = ref(false)
const detailLoading = ref(false)
const current = ref(null)
const handleOpen = ref(false)
const handleLoading = ref(false)
const handleSaving = ref(false)
const handleCurrent = ref(null)
const handleFormRef = ref()
const handleForm = reactive({ id: '', action: 'SUCCESS', externalTradeNo: '', note: '' })
const handleRules = {
  externalTradeNo: [
    { validator: (_, value) => {
      if (handleForm.action === 'SUCCESS' && !(value || '').trim()) return Promise.reject(new Error('确认成功必须填写真实支付宝转账流水号'))
      return Promise.resolve()
    } },
    { max: 128, message: '支付宝转账流水号不能超过128个字符' },
  ],
  note: [{ max: 1000, message: '处理备注不能超过1000个字符' }],
}

function withdrawalStatus(value) {
  return {
    PENDING: { text: '待处理', color: 'processing' },
    SUCCEEDED: { text: '转账已确认', color: 'success' },
    REJECTED: { text: '已拒绝并解冻', color: 'default' },
  }[value] || { text: value || '未知', color: 'default' }
}

function resetFilters() {
  Object.assign(filters, { status: undefined, userId: '', withdrawalNo: '' })
  search()
}

async function openDetail(record) {
  detailOpen.value = true
  detailLoading.value = true
  current.value = null
  try {
    current.value = await adminWithdrawalApi.detail(idOf(record.id))
  } catch (error) {
    message.error(errorText(error))
  } finally {
    detailLoading.value = false
  }
}

async function openHandle(record) {
  detailOpen.value = false
  Object.assign(handleForm, { id: idOf(record.id), action: 'SUCCESS', externalTradeNo: '', note: '' })
  handleCurrent.value = null
  handleOpen.value = true
  handleLoading.value = true
  try {
    handleCurrent.value = await adminWithdrawalApi.detail(handleForm.id)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    handleLoading.value = false
  }
}

function changeHandleAction() {
  handleFormRef.value?.clearValidate('externalTradeNo')
}

async function submitHandle() {
  if (!handleCurrent.value) return
  await handleFormRef.value.validate()
  handleSaving.value = true
  try {
    await adminWithdrawalApi.handle(handleForm.id, {
      action: handleForm.action,
      externalTradeNo: handleForm.action === 'SUCCESS' ? handleForm.externalTradeNo.trim() : null,
      note: handleForm.note.trim() || null,
    })
    message.success(handleForm.action === 'SUCCESS' ? '已记录真实线下转账结果' : '提现已拒绝，冻结金额已解冻')
    handleOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    handleSaving.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="management-page">
    <a-alert message="系统不调用也不模拟第三方打款。确认成功前，运营人员须先使用详情中的完整支付宝账号完成真实线下转账，并填写真实转账流水号；拒绝后由后端解冻冻结金额。" type="warning" show-icon class="page-alert" />
    <div class="toolbar">
      <a-space wrap>
        <a-input-search v-model:value="filters.withdrawalNo" allow-clear placeholder="完整提现单号" style="width: 240px" @search="search" />
        <a-input v-model:value="filters.userId" allow-clear placeholder="用户 ID" style="width: 180px" @press-enter="search" />
        <a-select v-model:value="filters.status" allow-clear placeholder="全部提现状态" style="width: 160px" :options="statusOptions" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>
      <a-button @click="load()"><ReloadOutlined /></a-button>
    </div>

    <a-card :bordered="false" class="table-card">
      <a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1350 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'withdrawalNo'"><span class="table-code">{{ record.withdrawalNo }}</span></template>
          <template v-else-if="column.key === 'userId'"><span class="table-code">{{ idOf(record.userId) }}</span></template>
          <template v-else-if="column.key === 'amount'"><strong>{{ money(record.amount) }}</strong></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="withdrawalStatus(record.status).color">{{ withdrawalStatus(record.status).text }}</a-tag></template>
          <template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime) }}</template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button v-permission="'wallet:withdrawal:list'" type="link" size="small" @click="openDetail(record)">详情</a-button>
              <a-button v-if="record.status === 'PENDING'" v-permission="'wallet:withdrawal:handle'" type="link" size="small" @click="openHandle(record)">人工处理</a-button>
            </a-space>
          </template>
          <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="提现详情" width="700">
      <a-spin :spinning="detailLoading">
        <template v-if="current">
          <a-alert message="完整支付宝账号仅用于运营线下转账，请妥善保管，不要通过非安全渠道传播。" type="warning" show-icon class="page-alert" />
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="提现单 ID" :span="2"><span class="table-code">{{ idOf(current.id) }}</span></a-descriptions-item>
            <a-descriptions-item label="提现单号" :span="2"><span class="table-code">{{ current.withdrawalNo }}</span></a-descriptions-item>
            <a-descriptions-item label="用户 ID"><span class="table-code">{{ idOf(current.userId) }}</span></a-descriptions-item>
            <a-descriptions-item label="提现金额"><strong>{{ money(current.amount) }}</strong></a-descriptions-item>
            <a-descriptions-item label="完整支付宝账号" :span="2"><span class="table-code account-text">{{ current.alipayAccount }}</span></a-descriptions-item>
            <a-descriptions-item label="提现状态"><a-tag :color="withdrawalStatus(current.status).color">{{ withdrawalStatus(current.status).text }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="申请时间">{{ formatDate(current.createTime) }}</a-descriptions-item>
            <a-descriptions-item label="真实转账流水号" :span="2"><span class="table-code">{{ current.externalTradeNo || '-' }}</span></a-descriptions-item>
            <a-descriptions-item label="处理备注 / 拒绝原因" :span="2">{{ current.handleNote || '-' }}</a-descriptions-item>
            <a-descriptions-item label="处理管理员 ID"><span class="table-code">{{ current.handledBy ? idOf(current.handledBy) : '-' }}</span></a-descriptions-item>
            <a-descriptions-item label="处理时间">{{ formatDate(current.handledAt) }}</a-descriptions-item>
          </a-descriptions>
          <a-space class="drawer-actions">
            <a-button v-if="current.status === 'PENDING'" v-permission="'wallet:withdrawal:handle'" type="primary" @click="openHandle(current)">人工处理</a-button>
          </a-space>
        </template>
      </a-spin>
    </a-drawer>

    <a-modal v-model:open="handleOpen" title="人工处理提现" ok-text="提交处理结果" cancel-text="取消" :confirm-loading="handleSaving" :ok-button-props="{ disabled: !handleCurrent }" destroy-on-close @ok="submitHandle">
      <a-spin :spinning="handleLoading">
        <template v-if="handleCurrent">
          <a-alert :message="handleForm.action === 'SUCCESS' ? '仅在实际完成支付宝线下转账后确认成功。请勿虚构转账流水号，系统不会代为打款。' : '拒绝后该提现将关闭，后端会解冻对应冻结金额。'" :type="handleForm.action === 'SUCCESS' ? 'warning' : 'info'" show-icon class="page-alert" />
          <a-descriptions :column="1" size="small" bordered class="handle-summary">
            <a-descriptions-item label="提现单号"><span class="table-code">{{ handleCurrent.withdrawalNo }}</span></a-descriptions-item>
            <a-descriptions-item label="提现金额"><strong>{{ money(handleCurrent.amount) }}</strong></a-descriptions-item>
            <a-descriptions-item label="完整支付宝账号"><span class="table-code account-text">{{ handleCurrent.alipayAccount }}</span></a-descriptions-item>
          </a-descriptions>
          <a-form ref="handleFormRef" :model="handleForm" :rules="handleRules" layout="vertical">
            <a-form-item label="处理动作" required>
              <a-radio-group v-model:value="handleForm.action" @change="changeHandleAction">
                <a-radio-button value="SUCCESS">已线下转账，确认成功</a-radio-button>
                <a-radio-button value="REJECT">拒绝并解冻</a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-form-item v-if="handleForm.action === 'SUCCESS'" label="真实支付宝转账流水号" name="externalTradeNo" required>
              <a-input v-model:value="handleForm.externalTradeNo" :maxlength="128" allow-clear placeholder="填写支付宝实际生成的转账流水号" />
            </a-form-item>
            <a-form-item :label="handleForm.action === 'REJECT' ? '拒绝原因' : '处理备注'" name="note">
              <a-textarea v-model:value="handleForm.note" :rows="4" :maxlength="1000" show-count :placeholder="handleForm.action === 'REJECT' ? '记录拒绝原因（可选）' : '记录线下转账核对信息（可选）'" />
            </a-form-item>
          </a-form>
        </template>
      </a-spin>
    </a-modal>
  </div>
</template>

<style scoped>
.account-text { font-size: 14px; font-weight: 600; }
.handle-summary { margin-bottom: 20px; }
</style>
