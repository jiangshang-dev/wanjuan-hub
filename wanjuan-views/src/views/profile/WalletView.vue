<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { walletApi } from '../../api/membership'
import { errorText, formatDate } from '../../utils/format'

const account = ref(null)
const accountLoading = ref(false)
const ledgers = ref([])
const ledgersLoading = ref(false)
const withdrawals = ref([])
const withdrawalsLoading = ref(false)
const activeTab = ref('ledgers')
const withdrawOpen = ref(false)
const withdrawing = ref(false)
const withdrawFormRef = ref()
const withdrawForm = reactive({ amount: undefined, alipayAccount: '' })
const ledgerPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` })
const withdrawalPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` })
const ledgerColumns = [
  { title: '流水说明', key: 'remark', ellipsis: true },
  { title: '可用余额变动', key: 'availableChange', width: 150, align: 'right' },
  { title: '冻结余额变动', key: 'frozenChange', width: 150, align: 'right' },
  { title: '变动后可用', key: 'availableAfter', width: 140, align: 'right' },
  { title: '变动后冻结', key: 'frozenAfter', width: 140, align: 'right' },
  { title: '时间', key: 'createTime', width: 170 },
]
const withdrawalColumns = [
  { title: '提现单号', key: 'withdrawalNo', width: 260 },
  { title: '提现金额', key: 'amount', width: 130, align: 'right' },
  { title: '支付宝账号', key: 'alipayAccount', width: 180 },
  { title: '状态', key: 'status', width: 110 },
  { title: '线下转账流水号', key: 'externalTradeNo', width: 210 },
  { title: '处理说明', key: 'handleNote', width: 190, ellipsis: true },
  { title: '申请时间', key: 'createTime', width: 170 },
  { title: '处理时间', key: 'handledAt', width: 170 },
]
const withdrawalStatuses = {
  PENDING: { text: '人工审核中', color: 'processing' },
  SUCCEEDED: { text: '已转账', color: 'success' },
  REJECTED: { text: '已拒绝并解冻', color: 'default' },
}
const withdrawRules = {
  amount: [
    { required: true, message: '请输入提现金额' },
    {
      validator: async (_rule, value) => {
        const amount = Number(value)
        if (!Number.isFinite(amount) || amount < 0.01) return Promise.reject(new Error('提现金额至少为 0.01 元'))
        if (amount > Number(account.value?.availableBalance || 0)) return Promise.reject(new Error('提现金额不能超过可用余额'))
        return Promise.resolve()
      },
    },
  ],
  alipayAccount: [
    { required: true, whitespace: true, message: '请输入支付宝手机号或邮箱账号' },
    { pattern: /^[A-Za-z0-9._@-]{5,128}$/, message: '请输入 5-128 位有效支付宝账号' },
  ],
}

function walletMoney(value) {
  if (value === null || value === undefined || value === '') return '-'
  const amount = Number(value)
  if (!Number.isFinite(amount)) return String(value)
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', minimumFractionDigits: 2, maximumFractionDigits: 4 }).format(amount)
}

function signedMoney(value) {
  const text = walletMoney(value)
  return Number(value) > 0 ? `+${text}` : text
}

function withdrawalStatus(value) {
  return withdrawalStatuses[value] || { text: value || '未知', color: 'default' }
}

async function loadAccount() {
  accountLoading.value = true
  try {
    account.value = await walletApi.account()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    accountLoading.value = false
  }
}

async function loadLedgers(page = ledgerPagination.current, size = ledgerPagination.pageSize) {
  ledgersLoading.value = true
  try {
    const data = await walletApi.ledgers({ page, size })
    ledgers.value = data?.records || []
    ledgerPagination.current = Number(data?.page || page)
    ledgerPagination.pageSize = Number(data?.size || size)
    ledgerPagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    ledgersLoading.value = false
  }
}

async function loadWithdrawals(page = withdrawalPagination.current, size = withdrawalPagination.pageSize) {
  withdrawalsLoading.value = true
  try {
    const data = await walletApi.withdrawals({ page, size })
    withdrawals.value = data?.records || []
    withdrawalPagination.current = Number(data?.page || page)
    withdrawalPagination.pageSize = Number(data?.size || size)
    withdrawalPagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    withdrawalsLoading.value = false
  }
}

function loadAll() {
  return Promise.all([loadAccount(), loadLedgers(1), loadWithdrawals(1)])
}

function changeLedgers(page) {
  loadLedgers(page.current, page.pageSize)
}

function changeWithdrawals(page) {
  loadWithdrawals(page.current, page.pageSize)
}

async function openWithdraw() {
  Object.assign(withdrawForm, { amount: undefined, alipayAccount: '' })
  withdrawOpen.value = true
  await nextTick()
  withdrawFormRef.value?.clearValidate()
}

async function submitWithdrawal() {
  try {
    await withdrawFormRef.value.validate()
  } catch {
    return
  }
  withdrawing.value = true
  try {
    await walletApi.applyWithdrawal({ amount: withdrawForm.amount, alipayAccount: withdrawForm.alipayAccount.trim() })
    withdrawOpen.value = false
    activeTab.value = 'withdrawals'
    message.success('提现申请已提交，金额已冻结，请等待人工审核与线下转账')
    await loadAll()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    withdrawing.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="content-page wallet-page">
    <div class="page-heading">
      <div><span class="eyebrow">COMMISSION WALLET</span><h1>返佣钱包</h1><p>查看资源返佣收入、余额变动与支付宝提现进度。</p></div>
      <a-space wrap><a-button :loading="accountLoading" @click="loadAll">刷新</a-button><a-button type="primary" @click="openWithdraw">申请提现</a-button></a-space>
    </div>
    <a-alert message="提现申请提交后，相应金额会进入冻结余额。管理员人工审核后通过线下支付宝转账，平台不会自动打款；处理结果以提现记录为准。" type="info" show-icon class="wallet-notice" />
    <a-spin :spinning="accountLoading">
      <div class="wallet-stat-grid">
        <a-card :bordered="false"><span>可用余额</span><strong class="available">{{ walletMoney(account?.availableBalance ?? 0) }}</strong><small>可用于申请支付宝提现</small></a-card>
        <a-card :bordered="false"><span>冻结余额</span><strong>{{ walletMoney(account?.frozenBalance ?? 0) }}</strong><small>提现审核与人工转账期间冻结</small></a-card>
        <a-card :bordered="false"><span>累计返佣</span><strong>{{ walletMoney(account?.totalIncome ?? 0) }}</strong><small>积分兑换与会员资源领取产生的收入</small></a-card>
      </div>
    </a-spin>
    <a-card :bordered="false" class="content-card wallet-records-card">
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="ledgers" tab="钱包流水">
          <a-table :columns="ledgerColumns" :data-source="ledgers" :loading="ledgersLoading" :pagination="ledgerPagination" row-key="id" :scroll="{ x: 1050 }" @change="changeLedgers">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'remark'"><strong>{{ record.remark || record.bizType || '钱包变动' }}</strong><div class="ledger-biz">{{ record.bizType }}<template v-if="record.bizId"> · {{ record.bizId }}</template></div></template>
              <template v-else-if="column.key === 'availableChange'"><span :class="Number(record.availableChange) >= 0 ? 'amount-positive' : 'amount-negative'">{{ signedMoney(record.availableChange) }}</span></template>
              <template v-else-if="column.key === 'frozenChange'"><span :class="Number(record.frozenChange) >= 0 ? 'amount-positive' : 'amount-negative'">{{ signedMoney(record.frozenChange) }}</span></template>
              <template v-else-if="column.key === 'availableAfter'">{{ walletMoney(record.availableAfter) }}</template>
              <template v-else-if="column.key === 'frozenAfter'">{{ walletMoney(record.frozenAfter) }}</template>
              <template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime, true) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="withdrawals" tab="提现记录">
          <a-table :columns="withdrawalColumns" :data-source="withdrawals" :loading="withdrawalsLoading" :pagination="withdrawalPagination" row-key="id" :scroll="{ x: 1530 }" @change="changeWithdrawals">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'withdrawalNo'"><span class="withdrawal-no">{{ record.withdrawalNo }}</span></template>
              <template v-else-if="column.key === 'amount'"><strong>{{ walletMoney(record.amount) }}</strong></template>
              <template v-else-if="column.key === 'alipayAccount'">{{ record.alipayAccount || '-' }}</template>
              <template v-else-if="column.key === 'status'"><a-tag :color="withdrawalStatus(record.status).color">{{ withdrawalStatus(record.status).text }}</a-tag></template>
              <template v-else-if="column.key === 'externalTradeNo'"><span class="withdrawal-no">{{ record.externalTradeNo || '-' }}</span></template>
              <template v-else-if="column.key === 'handleNote'">{{ record.handleNote || '-' }}</template>
              <template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime, true) }}</template>
              <template v-else-if="column.key === 'handledAt'">{{ formatDate(record.handledAt, true) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="withdrawOpen" title="申请支付宝提现" ok-text="提交人工审核" cancel-text="取消" :confirm-loading="withdrawing" @ok="submitWithdrawal">
      <a-alert message="此处只创建提现申请，不会即时到账。管理员将人工审核并在线下完成支付宝转账，拒绝后冻结金额会退回可用余额。" type="warning" show-icon class="withdraw-notice" />
      <a-form ref="withdrawFormRef" :model="withdrawForm" :rules="withdrawRules" layout="vertical">
        <a-form-item label="提现金额（元）" name="amount" :extra="`当前可提现 ${walletMoney(account?.availableBalance ?? 0)}，最多保留两位小数`">
          <a-input-number v-model:value="withdrawForm.amount" string-mode :precision="2" :min="0.01" :step="0.01" class="full-width" placeholder="0.00" />
        </a-form-item>
        <a-form-item label="支付宝手机号或邮箱账号" name="alipayAccount">
          <a-input v-model:value="withdrawForm.alipayAccount" :maxlength="128" autocomplete="off" placeholder="请输入接收人工转账的支付宝账号" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.wallet-page { max-width: 1180px; }
.wallet-notice { margin-bottom: 20px; }
.wallet-stat-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; margin-bottom: 20px; }
.wallet-stat-grid .ant-card { border: 1px solid var(--line); border-radius: 13px; }
.wallet-stat-grid span, .wallet-stat-grid small { display: block; color: var(--muted); }
.wallet-stat-grid strong { display: block; margin: 12px 0; color: var(--ink); font-size: 27px; overflow-wrap: anywhere; }
.wallet-stat-grid strong.available { color: #16785a; }
.wallet-stat-grid small { font-size: 12px; line-height: 1.7; }
.wallet-records-card :deep(.ant-card-body) { padding: 4px 24px 20px; }
.ledger-biz { margin-top: 6px; color: #8a94a6; font-size: 11px; overflow-wrap: anywhere; }
.amount-positive { color: #16865c; font-weight: 600; }
.amount-negative { color: #c63b4d; font-weight: 600; }
.withdrawal-no { font-family: "SFMono-Regular", Consolas, monospace; font-size: 12px; overflow-wrap: anywhere; }
.withdraw-notice { margin-bottom: 20px; }
@media (max-width: 700px) { .wallet-stat-grid { grid-template-columns: 1fr; } .wallet-records-card :deep(.ant-card-body) { padding-inline: 14px; } }
</style>
