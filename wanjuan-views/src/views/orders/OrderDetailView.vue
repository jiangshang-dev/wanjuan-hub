<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { orderApi, paymentApi, refundApi } from '../../api/s5'
import { errorText, formatDate } from '../../utils/format'
import { channelName, grantStatus, money, orderStatus, paymentStatus, refundStatus, sceneName } from '../../utils/trade'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(false)
const paying = ref(false)
const querying = ref(false)
const closing = ref(false)
const channel = ref('ALIPAY')
const refundOpen = ref(false)
const refundSaving = ref(false)
const refundForm = reactive({ reason: '' })
const payment = computed(() => order.value?.payment || null)
const refund = computed(() => order.value?.refund || null)
const clientType = computed(() => order.value?.clientType === 'H5' ? 'H5' : 'PC')
const selectedScene = computed(() => {
  if (channel.value === 'ALIPAY') return clientType.value === 'H5' ? 'WAP' : 'PAGE'
  return clientType.value === 'H5' ? 'H5' : 'NATIVE'
})
const channelOptions = computed(() => [
  { value: 'ALIPAY', label: '支付宝', description: clientType.value === 'H5' ? '手机网站支付（WAP）' : '电脑网站支付（PAGE）' },
  { value: 'WECHAT', label: '微信支付', description: clientType.value === 'H5' ? 'H5 支付' : 'Native 扫码支付' },
])
const showPayMethods = computed(() => order.value?.status === 'PENDING_PAYMENT' && !payment.value)
const showPaymentInstruction = computed(() => order.value?.status === 'PENDING_PAYMENT' && ['CREATED', 'PAYING'].includes(payment.value?.status))
const canRequestRefund = computed(() => order.value?.status === 'PAID' && !refund.value)
const h5Url = computed(() => safeExternalUrl(payment.value?.h5Url))

function safeExternalUrl(value) {
  if (!value) return ''
  try {
    const url = new URL(value)
    return ['https:', 'http:'].includes(url.protocol) ? url.href : ''
  } catch {
    return ''
  }
}

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    order.value = await orderApi.detail(route.params.orderNo)
    return order.value
  } catch (error) {
    if (!silent) message.error(errorText(error))
    else throw error
  } finally {
    if (!silent) loading.value = false
  }
}

async function createPayment() {
  paying.value = true
  try {
    const created = await paymentApi.create(order.value.orderNo, { channel: channel.value, scene: selectedScene.value })
    order.value = { ...order.value, payment: created }
    message.success('真实支付单已创建，请在渠道收银台完成支付')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    paying.value = false
  }
}

function openAlipay() {
  if (!payment.value?.formHtml) return
  const checkout = window.open('', '_blank')
  if (!checkout) {
    message.warning('浏览器阻止了收银台窗口，请允许弹出窗口后重试')
    return
  }
  checkout.opener = null
  checkout.document.open()
  checkout.document.write(payment.value.formHtml)
  checkout.document.close()
}

function openWechatH5() {
  if (!h5Url.value) return
  window.open(h5Url.value, '_blank', 'noopener,noreferrer')
}

async function queryPaymentResult() {
  querying.value = true
  try {
    const latest = await load(true)
    if (latest.status === 'PAID') message.success('服务端已确认支付成功')
    else if (latest.status === 'REFUNDING' || latest.status === 'REFUNDED') message.info('订单已进入退款流程')
    else if (latest.status === 'CLOSED') message.info('订单已关闭')
    else message.info('服务端尚未确认支付成功，请稍后再查询')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    querying.value = false
  }
}

async function closeOrder() {
  closing.value = true
  try {
    const latest = await paymentApi.close(order.value.orderNo)
    order.value = latest
    if (latest.status === 'PAID') message.success('渠道查单确认已支付，订单未关闭')
    else message.success('订单已关闭')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    closing.value = false
  }
}

function openRefund() {
  refundForm.reason = ''
  refundOpen.value = true
}

async function submitRefund() {
  const reason = refundForm.reason.trim()
  if (!reason) {
    message.warning('请填写退款原因')
    return
  }
  refundSaving.value = true
  try {
    const created = await refundApi.request(order.value.orderNo, { reason })
    order.value = { ...order.value, status: 'REFUNDING', refund: created }
    refundOpen.value = false
    message.success('退款申请已提交，请等待审核')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    refundSaving.value = false
  }
}

onMounted(() => load())
watch(() => route.params.orderNo, (value, previous) => {
  if (value && value !== previous) load()
})
</script>

<template>
  <div class="content-page order-detail-page">
    <div class="order-back"><router-link to="/my/orders">← 返回订单中心</router-link></div>
    <a-spin :spinning="loading">
      <template v-if="order">
        <div class="page-heading order-heading">
          <div><span class="eyebrow">ORDER DETAIL</span><h1>订单详情</h1><p class="order-number">{{ order.orderNo }}</p></div>
          <a-tag :color="orderStatus(order.status).color" class="order-status-tag">{{ orderStatus(order.status).text }}</a-tag>
        </div>

        <a-card class="content-card order-summary-card" :bordered="false">
          <a-descriptions :column="2" size="small">
            <a-descriptions-item label="订单金额"><strong class="order-amount">{{ money(order.totalAmount, order.currency) }}</strong></a-descriptions-item>
            <a-descriptions-item label="资源授权"><a-tag :color="grantStatus(order.grantStatus).color">{{ grantStatus(order.grantStatus).text }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="客户端">{{ order.clientType }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDate(order.createTime, true) }}</a-descriptions-item>
            <a-descriptions-item label="支付截止">{{ formatDate(order.expireAt, true) }}</a-descriptions-item>
            <a-descriptions-item label="支付时间">{{ formatDate(order.paidAt, true) }}</a-descriptions-item>
            <a-descriptions-item v-if="order.closedAt" label="关闭时间">{{ formatDate(order.closedAt, true) }}</a-descriptions-item>
            <a-descriptions-item v-if="order.closeReason" label="关闭原因">{{ order.closeReason }}</a-descriptions-item>
          </a-descriptions>
          <a-divider />
          <div v-for="item in order.items || []" :key="item.resourceId" class="order-resource">
            <div><strong>{{ item.resourceName }}</strong><p>资源 ID：{{ item.resourceId }}</p></div>
            <span>{{ money(item.price, order.currency) }}</span>
          </div>
          <div v-if="order.status === 'PAID' && order.grantStatus === 'GRANTED'" class="order-actions">
            <a-button type="primary" @click="router.push(`/resources/${order.resourceId}`)">查看已购资源</a-button>
            <a-button @click="router.push('/my/resources/entitlements')">已购授权</a-button>
          </div>
        </a-card>

        <a-card v-if="showPayMethods" class="content-card checkout-card" title="选择支付方式" :bordered="false">
          <a-alert message="支付金额由服务端订单确定，前端不会修改金额。支付结果以服务端通知或查询结果为准。" type="info" show-icon class="checkout-notice" />
          <a-radio-group v-model:value="channel" class="channel-options">
            <a-radio-button v-for="item in channelOptions" :key="item.value" :value="item.value" class="channel-option">
              <strong>{{ item.label }}</strong><span>{{ item.description }}</span>
            </a-radio-button>
          </a-radio-group>
          <a-button type="primary" size="large" :loading="paying" @click="createPayment">创建{{ channelName(channel) }}支付单</a-button>
        </a-card>

        <a-card v-if="payment" class="content-card payment-card" title="支付信息" :bordered="false">
          <template #extra><a-tag :color="paymentStatus(payment.status).color">{{ paymentStatus(payment.status).text }}</a-tag></template>
          <a-descriptions :column="2" size="small">
            <a-descriptions-item label="支付单号"><span class="payment-number">{{ payment.paymentNo }}</span></a-descriptions-item>
            <a-descriptions-item label="支付方式">{{ channelName(payment.channel) }} · {{ sceneName(payment.scene) }}</a-descriptions-item>
            <a-descriptions-item label="支付金额">{{ money(payment.amount, payment.currency) }}</a-descriptions-item>
            <a-descriptions-item label="渠道状态">{{ payment.channelStatus || '-' }}</a-descriptions-item>
            <a-descriptions-item label="渠道交易号">{{ payment.channelTradeNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="最近查单">{{ formatDate(payment.lastQueryAt, true) }}</a-descriptions-item>
          </a-descriptions>
          <a-alert v-if="payment.errorMessage" :message="payment.errorMessage" type="error" show-icon class="checkout-notice" />

          <div v-if="showPaymentInstruction" class="payment-instruction">
            <template v-if="payment.formHtml">
              <a-result status="info" title="支付宝收银台已就绪" sub-title="点击下方按钮打开真实支付宝收银台，完成支付后返回本页查询结果。">
                <template #extra><a-button type="primary" @click="openAlipay">打开支付宝收银台</a-button></template>
              </a-result>
            </template>
            <template v-else-if="payment.codeUrl">
              <div class="native-payment">
                <a-qrcode :value="payment.codeUrl" :size="220" />
                <div><h3>请使用微信扫码支付</h3><p>扫码后在微信内确认支付。二维码仅用于当前真实支付单，请勿重复创建订单。</p></div>
              </div>
            </template>
            <template v-else-if="h5Url">
              <a-result status="info" title="微信 H5 支付单已就绪" sub-title="请前往微信渠道完成支付，完成后返回本页查询服务端结果。">
                <template #extra><a-button type="primary" @click="openWechatH5">前往微信支付</a-button></template>
              </a-result>
            </template>
            <a-empty v-else description="支付单已创建，但暂未获得收银台信息" />
          </div>
          <div class="payment-actions">
            <a-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" :loading="querying" @click="queryPaymentResult">查询支付结果</a-button>
            <a-popconfirm v-if="order.status === 'PENDING_PAYMENT'" title="系统会先向支付渠道查单；未支付才会关闭订单。确定继续吗？" ok-text="查单并取消" cancel-text="返回" @confirm="closeOrder">
              <a-button danger :loading="closing">取消订单</a-button>
            </a-popconfirm>
          </div>
        </a-card>

        <a-card v-if="order.status === 'PENDING_PAYMENT' && !payment" class="content-card payment-actions-card" :bordered="false">
          <a-popconfirm title="确定取消该待支付订单吗？" ok-text="确定取消" cancel-text="返回" @confirm="closeOrder"><a-button danger :loading="closing">取消订单</a-button></a-popconfirm>
        </a-card>

        <a-card v-if="canRequestRefund || refund" class="content-card refund-card" title="退款" :bordered="false">
          <template #extra><a-tag v-if="refund" :color="refundStatus(refund.status).color">{{ refundStatus(refund.status).text }}</a-tag></template>
          <template v-if="refund">
            <a-descriptions :column="2" size="small">
              <a-descriptions-item label="退款单号"><span class="payment-number">{{ refund.refundNo }}</span></a-descriptions-item>
              <a-descriptions-item label="退款金额">{{ money(refund.amount, refund.currency) }}</a-descriptions-item>
              <a-descriptions-item label="退款原因" :span="2">{{ refund.reason }}</a-descriptions-item>
              <a-descriptions-item label="渠道状态">{{ refund.channelStatus || '-' }}</a-descriptions-item>
              <a-descriptions-item label="申请时间">{{ formatDate(refund.createTime, true) }}</a-descriptions-item>
              <a-descriptions-item v-if="refund.reviewOpinion" label="审核意见" :span="2">{{ refund.reviewOpinion }}</a-descriptions-item>
              <a-descriptions-item v-if="refund.refundedAt" label="退款完成">{{ formatDate(refund.refundedAt, true) }}</a-descriptions-item>
              <a-descriptions-item label="授权撤销">{{ refund.entitlementRevoked ? '已撤销' : '未撤销' }}</a-descriptions-item>
            </a-descriptions>
            <a-alert v-if="refund.errorMessage" :message="refund.errorMessage" type="error" show-icon class="checkout-notice" />
          </template>
          <template v-else>
            <p class="refund-tip">仅已支付订单可以申请全额退款，提交后由管理员审核并调用真实支付渠道处理。</p>
            <a-button danger @click="openRefund">申请退款</a-button>
          </template>
        </a-card>
      </template>
      <a-empty v-else-if="!loading" description="未能加载订单详情" />
    </a-spin>

    <a-modal v-model:open="refundOpen" title="申请全额退款" ok-text="提交申请" cancel-text="取消" :confirm-loading="refundSaving" @ok="submitRefund">
      <a-alert message="退款金额以服务端订单和支付单为准，退款成功后资源授权会被撤销。" type="warning" show-icon class="checkout-notice" />
      <a-form layout="vertical"><a-form-item label="退款原因" required><a-textarea v-model:value="refundForm.reason" :rows="5" :maxlength="500" show-count placeholder="请说明退款原因（最多500字）" /></a-form-item></a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.order-detail-page { max-width: 960px; }
.order-back { margin-bottom: 18px; font-size: 13px; }
.order-heading { align-items: center; }
.order-number { font-family: "SFMono-Regular", Consolas, monospace; word-break: break-all; }
.order-status-tag { margin-inline-end: 0; font-size: 14px; }
.order-summary-card, .checkout-card, .payment-card, .refund-card, .payment-actions-card { margin-bottom: 18px; }
.order-amount { color: #d4380d; font-size: 21px; }
.order-resource { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.order-resource strong { color: var(--ink); font-size: 16px; }
.order-resource p { margin: 8px 0 0; color: var(--muted); font-size: 12px; }
.order-resource > span { color: #d4380d; font-weight: 600; }
.order-actions { display: flex; margin-top: 22px; gap: 10px; }
.checkout-notice { margin-bottom: 20px; }
.channel-options { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin-bottom: 22px; gap: 12px; }
.channel-options :deep(.ant-radio-button-wrapper) { display: flex; height: auto; min-height: 78px; padding: 16px 20px; flex-direction: column; align-items: flex-start; justify-content: center; border: 1px solid #d9d9d9; border-inline-start-width: 1px; border-radius: 8px !important; }
.channel-options :deep(.ant-radio-button-wrapper)::before { display: none; }
.channel-option strong, .channel-option span { display: block; }
.channel-option strong { margin-bottom: 5px; font-size: 16px; }
.channel-option span { color: #7a8497; font-size: 12px; }
.payment-number { font-family: "SFMono-Regular", Consolas, monospace; font-size: 12px; word-break: break-all; }
.payment-instruction { padding: 18px; margin-top: 20px; border-radius: 10px; background: #f7f9fc; }
.native-payment { display: flex; align-items: center; justify-content: center; gap: 34px; }
.native-payment h3 { margin: 0 0 12px; }
.native-payment p { max-width: 320px; margin: 0; color: var(--muted); line-height: 1.8; }
.payment-actions { display: flex; margin-top: 22px; gap: 10px; }
.refund-tip { color: var(--muted); line-height: 1.8; }
@media (max-width: 640px) {
  .channel-options { grid-template-columns: 1fr; }
  .native-payment { flex-direction: column; text-align: center; }
  .order-heading { align-items: flex-start; }
}
</style>
