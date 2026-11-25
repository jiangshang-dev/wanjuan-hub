<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { messageApi } from '../../api/s6'
import { errorText, formatDate } from '../../utils/format'

const types = [
  { value: 'AUDIT', label: '审核消息', color: 'blue' },
  { value: 'PAYMENT', label: '支付消息', color: 'green' },
  { value: 'SYSTEM', label: '系统消息', color: 'purple' },
  { value: 'INTERACTION', label: '互动消息', color: 'orange' },
  { value: 'POINTS', label: '积分消息', color: 'gold' },
]
const loading = ref(false)
const preferencesOpen = ref(false)
const preferencesSaving = ref(false)
const records = ref([])
const unreadCount = ref(0)
const filters = reactive({ type: undefined, read: undefined })
const preferences = reactive({ auditEnabled: true, paymentEnabled: true, systemEnabled: true, interactionEnabled: true })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` })

function typeInfo(type) {
  return types.find((item) => item.value === type) || { label: type || '其他消息', color: 'default' }
}

async function loadUnread() {
  try {
    unreadCount.value = Number(await messageApi.unreadCount() || 0)
  } catch (error) {
    message.error(errorText(error))
  }
}

async function load(page = pagination.current, size = pagination.pageSize) {
  loading.value = true
  try {
    const data = await messageApi.list({ page, size, type: filters.type, read: filters.read })
    records.value = data?.records || []
    pagination.current = Number(data?.page || page)
    pagination.pageSize = Number(data?.size || size)
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

function search() {
  load(1)
}

function reset() {
  filters.type = undefined
  filters.read = undefined
  load(1)
}

async function markRead(record) {
  if (record.read) return
  try {
    await messageApi.markRead(record.id)
    record.read = true
    record.readTime = new Date().toISOString()
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (error) {
    message.error(errorText(error))
  }
}

async function markAllRead() {
  try {
    const count = await messageApi.markAllRead()
    records.value.forEach((record) => { record.read = true })
    unreadCount.value = 0
    message.success(`已将 ${Number(count || 0)} 条消息标记为已读`)
    if (filters.read === false) load(1)
  } catch (error) {
    message.error(errorText(error))
  }
}

async function openPreferences() {
  preferencesOpen.value = true
  try {
    Object.assign(preferences, await messageApi.preferences())
  } catch (error) {
    message.error(errorText(error))
  }
}

async function savePreferences() {
  preferencesSaving.value = true
  try {
    Object.assign(preferences, await messageApi.updatePreferences({ ...preferences }))
    preferencesOpen.value = false
    message.success('消息偏好已保存')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    preferencesSaving.value = false
  }
}

onMounted(() => {
  load(1)
  loadUnread()
})
</script>

<template>
  <div class="content-page messages-page">
    <div class="page-heading">
      <div><span class="eyebrow">NOTIFICATIONS</span><h1>消息中心 <a-badge :count="unreadCount" :overflow-count="99" /></h1><p>审核、支付、系统与互动通知统一在这里处理。</p></div>
      <a-space wrap><a-button @click="openPreferences">消息偏好</a-button><a-button type="primary" :disabled="!unreadCount" @click="markAllRead">全部已读</a-button></a-space>
    </div>
    <div class="content-card message-filter">
      <a-space wrap>
        <a-select v-model:value="filters.type" allow-clear placeholder="全部消息分类" style="width: 160px" :options="types" @change="search" />
        <a-select v-model:value="filters.read" allow-clear placeholder="全部阅读状态" style="width: 150px" :options="[{ value: false, label: '未读' }, { value: true, label: '已读' }]" @change="search" />
        <a-button type="primary" @click="search">查询</a-button><a-button @click="reset">重置</a-button>
      </a-space>
    </div>
    <div class="content-card message-list-card">
      <a-spin :spinning="loading">
        <div v-if="records.length" class="message-list">
          <article v-for="record in records" :key="record.id" class="message-item" :class="{ unread: !record.read }">
            <span class="message-read-dot" :class="{ active: !record.read }" />
            <div class="message-main">
              <div class="message-title"><a-tag :color="typeInfo(record.type).color">{{ typeInfo(record.type).label }}</a-tag><h2>{{ record.title }}</h2><a-tag v-if="!record.read" color="red">未读</a-tag></div>
              <p>{{ record.content }}</p>
              <div class="message-meta"><time>{{ formatDate(record.createTime, true) }}</time><span v-if="record.bizType">{{ record.bizType }}<template v-if="record.bizId"> · {{ record.bizId }}</template></span><span v-if="record.readTime">已读于 {{ formatDate(record.readTime, true) }}</span></div>
            </div>
            <a-button v-if="!record.read" size="small" @click="markRead(record)">标为已读</a-button>
          </article>
        </div>
        <a-empty v-else-if="!loading" description="暂无符合条件的消息" />
      </a-spin>
      <a-pagination v-if="pagination.total > pagination.pageSize" v-model:current="pagination.current" v-model:page-size="pagination.pageSize" class="list-pagination" :total="pagination.total" :show-size-changer="true" :show-total="pagination.showTotal" @change="load" />
    </div>
    <a-modal v-model:open="preferencesOpen" title="消息接收偏好" ok-text="保存偏好" cancel-text="取消" :confirm-loading="preferencesSaving" @ok="savePreferences">
      <p class="preference-tip">关闭后，对应分类的新消息将不再接收；已有消息不受影响。</p>
      <div class="preference-list">
        <div><span><strong>审核消息</strong><small>内容审核结果与人工审核进度</small></span><a-switch v-model:checked="preferences.auditEnabled" /></div>
        <div><span><strong>支付消息</strong><small>订单支付与退款结果</small></span><a-switch v-model:checked="preferences.paymentEnabled" /></div>
        <div><span><strong>系统消息</strong><small>平台公告与账号相关通知</small></span><a-switch v-model:checked="preferences.systemEnabled" /></div>
        <div><span><strong>互动消息</strong><small>点赞、评论、关注等社区互动</small></span><a-switch v-model:checked="preferences.interactionEnabled" /></div>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.messages-page { max-width: 980px; }
.message-filter { padding: 18px 22px; margin-bottom: 18px; }
.message-list-card { padding: 0 26px 24px; }
.message-item { display: flex; padding: 24px 0; align-items: flex-start; gap: 14px; border-bottom: 1px solid var(--line); }
.message-item.unread { background: linear-gradient(90deg, rgba(49,87,213,.035), transparent); }
.message-read-dot { flex: 0 0 7px; width: 7px; height: 7px; margin-top: 9px; border-radius: 50%; background: #d5dae3; }
.message-read-dot.active { background: #ef4d5f; box-shadow: 0 0 0 4px rgba(239,77,95,.09); }
.message-main { min-width: 0; flex: 1; }
.message-title { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; }
.message-title .ant-tag { margin: 0; }
.message-title h2 { margin: 0; color: var(--ink); font-size: 16px; }
.message-main > p { margin: 12px 0; color: #536078; line-height: 1.8; white-space: pre-wrap; }
.message-meta { display: flex; flex-wrap: wrap; gap: 14px; color: #8b95a8; font-size: 12px; }
.preference-tip { color: var(--muted); line-height: 1.7; }
.preference-list { margin-top: 20px; border-top: 1px solid var(--line); }
.preference-list > div { display: flex; padding: 18px 0; align-items: center; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--line); }
.preference-list strong, .preference-list small { display: block; }
.preference-list small { margin-top: 5px; color: var(--muted); }
@media (max-width: 560px) { .message-list-card { padding-inline: 16px; } .message-item > .ant-btn { padding-inline: 8px; } .message-title h2 { width: 100%; order: 3; } }
</style>
