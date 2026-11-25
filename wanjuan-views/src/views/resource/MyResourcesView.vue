<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { resourceApi, userResourceApi } from '../../api/resource'
import { accessTypeText, resourceStatus } from '../../utils/resource'
import { errorText, formatDate } from '../../utils/format'
import './resource.css'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const categories = ref([])
const filters = reactive({ status: undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 个资源` })
const submittingId = ref('')
const columns = [
  { title: '资源', key: 'title', width: 300 },
  { title: '分类 / 访问类型', key: 'category', width: 160 },
  { title: '状态', key: 'status', width: 130 },
  { title: '版本', key: 'versions', width: 130 },
  { title: '最近更新', key: 'updatedAt', width: 160 },
  { title: '审核说明', key: 'reason', width: 200, ellipsis: true },
  { title: '操作', key: 'actions', width: 320, fixed: 'right' },
]
const statusOptions = ['DRAFT', 'PENDING', 'PUBLISHED', 'OFFLINE', 'REJECTED', 'MANUAL_REVIEW', 'INVALID'].map((value) => ({ value, label: resourceStatus(value).text }))

function categoryName(id) { return categories.value.find((item) => String(item.id) === String(id))?.name || `分类 #${id ?? '-'}` }

async function load(page = pagination.current) {
  loading.value = true
  try {
    const data = await userResourceApi.list({ page, size: pagination.pageSize, status: filters.status })
    records.value = Array.isArray(data?.records) ? data.records : []
    pagination.current = page
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}
function search() { load(1) }
function changeTable(page) { pagination.pageSize = page.pageSize; load(page.current) }

function submitResource(record) {
  Modal.confirm({
    title: '提交资源审核',
    content: `确定将《${record.title}》的最新草稿提交审核吗？`,
    okText: '确认提交', cancelText: '取消',
    async onOk() {
      submittingId.value = String(record.id)
      try {
        await userResourceApi.submit(record.id)
        message.success('已提交审核')
        await load()
      } catch (error) {
        message.error(errorText(error))
      } finally {
        submittingId.value = ''
      }
    },
  })
}

async function removeResource(record) {
  try {
    await userResourceApi.remove(record.id)
    message.success('资源已删除')
    load(records.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error) {
    message.error(errorText(error))
  }
}

onMounted(async () => {
  load()
  try {
    const data = await resourceApi.categories()
    categories.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error(errorText(error))
  }
})
</script>

<template>
  <div class="content-page my-resources-page">
    <div class="page-heading"><div><span class="eyebrow">MY RESOURCES</span><h1>我的发布</h1><p>管理资源草稿、审核进度、公开版本与历史记录。</p></div><a-space wrap><a-button @click="router.push('/my/resources/entitlements')">我的授权</a-button><a-button type="primary" @click="router.push('/my/resources/new')">发布资源</a-button></a-space></div>
    <div class="my-blog-toolbar"><a-select v-model:value="filters.status" allow-clear placeholder="全部状态" :options="statusOptions" /><a-button type="primary" @click="search">查询</a-button></div>
    <a-card :bordered="false" class="content-card resource-table-card">
      <a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1420 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'"><div class="resource-table-title"><strong>{{ record.title }}</strong><span>{{ record.summary }}</span></div></template>
          <template v-else-if="column.key === 'category'"><div>{{ categoryName(record.categoryId) }}</div><small>{{ accessTypeText(record.accessType) }}</small></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="resourceStatus(record.status).color">{{ resourceStatus(record.status).text }}</a-tag><div v-if="record.versionStatus"><small>版本：{{ resourceStatus(record.versionStatus).text }}</small></div></template>
          <template v-else-if="column.key === 'versions'"><div class="resource-version-numbers">最新 v{{ record.latestVersionNo ?? '-' }}<br>公开 v{{ record.publishedVersionNo ?? '-' }}</div></template>
          <template v-else-if="column.key === 'updatedAt'">{{ formatDate(record.updateTime || record.createTime, true) }}</template>
          <template v-else-if="column.key === 'reason'"><span :class="{ 'reject-reason': record.status === 'REJECTED' }">{{ record.auditReason || '-' }}</span></template>
          <template v-else-if="column.key === 'actions'"><a-space wrap><a-button v-if="record.status === 'PUBLISHED'" type="link" size="small" @click="router.push(`/resources/${record.id}`)">公开页</a-button><a-button v-if="!['OFFLINE', 'INVALID'].includes(record.status)" type="link" size="small" @click="router.push(`/my/resources/${record.id}/edit`)">编辑</a-button><a-button type="link" size="small" @click="router.push(`/my/resources/${record.id}/versions`)">版本</a-button><a-button v-if="record.status === 'DRAFT'" type="link" size="small" :loading="submittingId === String(record.id)" @click="submitResource(record)">提交审核</a-button><a-popconfirm title="确定删除该资源吗？" description="删除后将停止公开访问，且无法继续编辑。" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="removeResource(record)"><a-button type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
