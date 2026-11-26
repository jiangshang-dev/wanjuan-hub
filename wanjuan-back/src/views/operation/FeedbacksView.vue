<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { adminFeedbackApi } from '../../api/s6'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf } from '../../utils'

const statusOptions = [{ value: 'PENDING', label: '待回复' }, { value: 'REPLIED', label: '已回复' }, { value: 'CLOSED', label: '已关闭' }]
const statusMap = {
  PENDING: { text: '待回复', color: 'warning' },
  REPLIED: { text: '已回复', color: 'success' },
  CLOSED: { text: '已关闭', color: 'default' },
}
const filters = reactive({ status: undefined, userId: '', keyword: '' })
const { loading, records, pagination, load, changeTable, search } = useTable(
  adminFeedbackApi.list,
  () => ({ status: filters.status, userId: filters.userId.trim() || undefined, keyword: filters.keyword.trim() || undefined }),
)
const columns = [
  { title: '反馈 ID', dataIndex: 'id', key: 'id', width: 180 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 180 },
  { title: '反馈内容', dataIndex: 'content', ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '管理员回复', dataIndex: 'adminReply', key: 'adminReply', ellipsis: true },
  { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '回复时间', dataIndex: 'replyTime', key: 'replyTime', width: 180 },
  { title: '操作', key: 'actions', width: 150, fixed: 'right' },
]
const replyModal = ref(false)
const replying = ref(false)
const closingId = ref('')
const current = ref(null)
const replyFormRef = ref()
const replyForm = reactive({ reply: '' })
const replyRules = { reply: [{ required: true, whitespace: true, message: '请输入回复内容' }, { max: 2000, message: '回复内容不能超过2000个字符' }] }

function searchFeedbacks() {
  if (filters.userId.trim() && !/^\d+$/.test(filters.userId.trim())) {
    message.warning('用户ID必须为正整数')
    return
  }
  search()
}

function resetFilters() {
  Object.assign(filters, { status: undefined, userId: '', keyword: '' })
  search()
}

function openReply(record) {
  current.value = record
  replyForm.reply = record.adminReply || ''
  replyModal.value = true
}

async function reply() {
  await replyFormRef.value.validate()
  replying.value = true
  try {
    await adminFeedbackApi.reply(idOf(current.value.id), { reply: replyForm.reply.trim() })
    message.success('反馈已回复，并已提醒提交用户')
    replyModal.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    replying.value = false
  }
}

async function close(record) {
  closingId.value = idOf(record.id)
  try {
    await adminFeedbackApi.close(closingId.value)
    message.success('反馈已关闭')
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    closingId.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div class="management-page">
    <div class="toolbar">
      <a-space wrap>
        <a-input v-model:value="filters.keyword" allow-clear :maxlength="200" placeholder="搜索反馈内容" style="width: 220px" @press-enter="searchFeedbacks"><template #prefix><SearchOutlined /></template></a-input>
        <a-input v-model:value="filters.userId" allow-clear placeholder="完整用户ID" style="width: 190px" @press-enter="searchFeedbacks" />
        <a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 130px" :options="statusOptions" />
        <a-button type="primary" @click="searchFeedbacks">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>
      <a-button @click="load()"><ReloadOutlined /></a-button>
    </div>
    <a-card :bordered="false" class="table-card">
      <a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1500 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id' || column.key === 'userId'"><span class="table-code">{{ record[column.dataIndex] }}</span></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="statusMap[record.status]?.color || 'default'">{{ statusMap[record.status]?.text || record.status }}</a-tag></template>
          <template v-else-if="column.key === 'createTime' || column.key === 'replyTime'">{{ formatDate(record[column.dataIndex]) }}</template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button v-if="record.status !== 'CLOSED'" v-permission="'operation:feedback:reply'" type="link" size="small" @click="openReply(record)">{{ record.status === 'REPLIED' ? '重新回复' : '回复' }}</a-button>
              <a-popconfirm v-if="record.status !== 'CLOSED'" title="确定关闭该反馈吗？" description="关闭后不能再回复。" ok-text="确认关闭" cancel-text="取消" @confirm="close(record)">
                <a-button v-permission="'operation:feedback:close'" type="link" danger size="small" :loading="closingId === idOf(record.id)">关闭</a-button>
              </a-popconfirm>
            </a-space>
          </template>
          <template v-else>{{ record[column.dataIndex] || '-' }}</template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="replyModal" title="回复意见反馈" :confirm-loading="replying" ok-text="提交回复" destroy-on-close @ok="reply">
      <div v-if="current" class="feedback-original">
        <div class="feedback-original-meta">用户 ID：{{ current.userId }} · {{ formatDate(current.createTime) }}</div>
        <p>{{ current.content }}</p>
      </div>
      <a-form ref="replyFormRef" :model="replyForm" :rules="replyRules" layout="vertical">
        <a-form-item label="回复内容" name="reply"><a-textarea v-model:value="replyForm.reply" :rows="6" :maxlength="2000" show-count placeholder="回复将写入用户消息中心" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.feedback-original { margin-bottom: 20px; padding: 14px 16px; border-radius: 8px; background: #f5f7fa; }
.feedback-original-meta { color: var(--muted); font-size: 12px; overflow-wrap: anywhere; }
.feedback-original p { margin: 10px 0 0; white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
