<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { userBlogApi } from '../../api'
import { blogStatus, collectionOf } from '../../utils/blog'
import { errorText, formatDate } from '../../utils/format'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const filters = reactive({ keyword: '', status: undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 篇` })
const submittingId = ref('')
const columns = [
  { title: '博客', key: 'title', width: 280 },
  { title: '分类', key: 'category', width: 120 },
  { title: '状态', key: 'status', width: 110 },
  { title: '最近更新', key: 'updatedAt', width: 170 },
  { title: '审核说明', key: 'reason', width: 200, ellipsis: true },
  { title: '操作', key: 'actions', width: 300, fixed: 'right' },
]

async function load(page = pagination.current) {
  loading.value = true
  try {
    const data = await userBlogApi.list({ page, size: pagination.pageSize, keyword: filters.keyword.trim() || undefined, status: filters.status })
    records.value = collectionOf(data)
    pagination.current = page
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

function search() { load(1) }
function changeTable(page) {
  pagination.pageSize = page.pageSize
  load(page.current)
}
function canSubmit(record) { return ['DRAFT', 'REJECTED'].includes(record.status) }

function submitBlog(record) {
  Modal.confirm({
    title: '提交审核',
    content: `确定将《${record.title}》的最新草稿提交审核吗？`,
    okText: '确认提交',
    cancelText: '取消',
    async onOk() {
      submittingId.value = String(record.id)
      try {
        await userBlogApi.submit(record.id)
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

async function removeBlog(record) {
  try {
    await userBlogApi.remove(record.id)
    message.success('博客已删除')
    load(records.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error) {
    message.error(errorText(error))
  }
}

onMounted(() => load())
</script>

<template>
  <div class="content-page my-blogs-page">
    <div class="page-heading"><div><span class="eyebrow">MY CREATION</span><h1>我的博客</h1><p>管理草稿、审核进度与历史版本。</p></div><a-button type="primary" @click="router.push('/my/blogs/new')">创作博客</a-button></div>
    <div class="my-blog-toolbar"><a-input-search v-model:value="filters.keyword" allow-clear placeholder="搜索我的博客" @search="search" /><a-select v-model:value="filters.status" allow-clear placeholder="全部状态" :options="[{ value: 'DRAFT', label: '草稿' }, { value: 'PENDING', label: '审核中' }, { value: 'PUBLISHED', label: '已发布' }, { value: 'REJECTED', label: '已驳回' }, { value: 'MANUAL_REVIEW', label: '人工审核' }, { value: 'OFFLINE', label: '已下线' }]" /><a-button type="primary" @click="search">查询</a-button></div>
    <a-card :bordered="false" class="content-card my-blog-table-card">
      <a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1200 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'"><div class="my-blog-title"><strong>{{ record.title }}</strong><span>{{ record.summary || '暂无摘要' }}</span></div></template>
          <template v-else-if="column.key === 'category'">{{ record.categoryName || record.category?.name || '-' }}</template>
          <template v-else-if="column.key === 'status'"><a-tag :color="blogStatus(record.status).color">{{ blogStatus(record.status).text }}</a-tag></template>
          <template v-else-if="column.key === 'updatedAt'">{{ formatDate(record.updatedAt || record.updateTime || record.createdAt || record.createTime, true) }}</template>
          <template v-else-if="column.key === 'reason'"><span :class="{ 'reject-reason': record.status === 'REJECTED' }">{{ record.rejectReason || record.auditReason || record.reason || '-' }}</span></template>
          <template v-else-if="column.key === 'actions'"><a-space wrap><a-button v-if="record.status === 'PUBLISHED'" type="link" size="small" @click="router.push(`/blogs/${record.id}`)">查看</a-button><a-button v-if="record.status !== 'PENDING'" type="link" size="small" @click="router.push(`/my/blogs/${record.id}/edit`)">编辑</a-button><a-button type="link" size="small" @click="router.push(`/my/blogs/${record.id}/versions`)">版本</a-button><a-button v-if="canSubmit(record)" type="link" size="small" :loading="submittingId === String(record.id)" @click="submitBlog(record)">提交审核</a-button><a-popconfirm title="确定删除这篇博客吗？" description="删除入口后将无法继续编辑，请谨慎操作。" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="removeBlog(record)"><a-button type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
