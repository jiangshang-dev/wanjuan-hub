<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { adminBlogApi, adminBlogCategoryApi } from '../../api'
import { useTable } from '../../composables/useTable'
import { blogAuthor, blogStatus, blogTags, tagName } from '../../utils/blog'
import { errorText, formatDate, pageResult } from '../../utils'

const filters = reactive({ keyword: '', status: undefined, categoryId: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminBlogApi.list, () => ({ keyword: filters.keyword.trim() || undefined, status: filters.status, categoryId: filters.categoryId }))
const categories = ref([])
const detailOpen = ref(false)
const currentBlog = ref(null)
const statusOpen = ref(false)
const statusSaving = ref(false)
const statusFormRef = ref()
const statusForm = reactive({ id: '', title: '', status: 'OFFLINE', reason: '' })
const safeSourceUrl = computed(() => /^https?:\/\//i.test(currentBlog.value?.sourceUrl || '') ? currentBlog.value.sourceUrl : '')
const columns = [
  { title: '博客', key: 'title', width: 280 }, { title: '作者', key: 'author', width: 140 },
  { title: '分类', key: 'category', width: 130 }, { title: '状态', key: 'status', width: 110 },
  { title: '更新时间', key: 'updatedAt', width: 180 }, { title: '操作', key: 'actions', width: 200, fixed: 'right' },
]
const statusRules = { reason: [{ validator: (_, value) => statusForm.status === 'PUBLISHED' || value?.trim() ? Promise.resolve() : Promise.reject(new Error('下线博客必须填写原因')) }] }

onMounted(async () => {
  load()
  try {
    categories.value = pageResult(await adminBlogCategoryApi.list({ page: 1, size: 100 })).records
  } catch (error) {
    message.error(errorText(error))
  }
})
function openDetail(record) {
  currentBlog.value = record
  detailOpen.value = true
}
function openStatus(record) {
  Object.assign(statusForm, { id: String(record.id), title: record.title, status: record.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED', reason: '' })
  statusOpen.value = true
}
async function saveStatus() {
  await statusFormRef.value.validate()
  statusSaving.value = true
  try {
    await adminBlogApi.updateStatus(statusForm.id, { status: statusForm.status, reason: statusForm.reason.trim() })
    message.success(statusForm.status === 'PUBLISHED' ? '博客已恢复公开' : '博客已下线')
    statusOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusSaving.value = false
  }
}
function resetFilters() {
  Object.assign(filters, { keyword: '', status: undefined, categoryId: undefined })
  search()
}
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input v-model:value="filters.keyword" allow-clear placeholder="搜索博客标题" style="width: 220px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input><a-select v-model:value="filters.categoryId" allow-clear placeholder="全部分类" style="width: 140px" :options="categories.map(item => ({ value: String(item.id), label: item.name }))" /><a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 140px" :options="['DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'MANUAL_REVIEW', 'OFFLINE'].map(value => ({ value, label: blogStatus(value).text }))" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-button @click="load()"><ReloadOutlined /></a-button></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1100 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'title'"><div class="admin-blog-title"><strong>{{ record.title }}</strong><span>{{ record.summary || '暂无摘要' }}</span></div></template><template v-else-if="column.key === 'author'">{{ blogAuthor(record) }}</template><template v-else-if="column.key === 'category'">{{ record.categoryName || record.category?.name || '-' }}</template><template v-else-if="column.key === 'status'"><a-tag :color="blogStatus(record.status).color">{{ blogStatus(record.status).text }}</a-tag></template><template v-else-if="column.key === 'updatedAt'">{{ formatDate(record.updatedAt || record.updateTime || record.createdAt || record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-space><a-button type="link" size="small" @click="openDetail(record)">详情</a-button><a-button v-if="['PUBLISHED', 'OFFLINE'].includes(record.status)" v-permission="'blog:article:status'" type="link" size="small" :danger="record.status === 'PUBLISHED'" @click="openStatus(record)">{{ record.status === 'PUBLISHED' ? '下线' : '恢复公开' }}</a-button></a-space></template></template></a-table></a-card>
    <a-drawer v-model:open="detailOpen" title="博客详情" width="680"><template v-if="currentBlog"><h2 class="drawer-blog-title">{{ currentBlog.title }}</h2><a-descriptions :column="2" size="small" bordered><a-descriptions-item label="作者">{{ blogAuthor(currentBlog) }}</a-descriptions-item><a-descriptions-item label="状态"><a-tag :color="blogStatus(currentBlog.status).color">{{ blogStatus(currentBlog.status).text }}</a-tag></a-descriptions-item><a-descriptions-item label="分类">{{ currentBlog.categoryName || currentBlog.category?.name || '-' }}</a-descriptions-item><a-descriptions-item label="类型">{{ currentBlog.type === 'REPRINT' || currentBlog.originalType === 'REPRINT' ? '转载' : '原创' }}</a-descriptions-item><a-descriptions-item label="创建时间" :span="2">{{ formatDate(currentBlog.createdAt || currentBlog.createTime) }}</a-descriptions-item></a-descriptions><div v-if="blogTags(currentBlog).length" class="drawer-tags"><a-tag v-for="tag in blogTags(currentBlog)" :key="tag.id || tag">{{ tagName(tag) }}</a-tag></div><section class="drawer-content-section"><h4>摘要</h4><p>{{ currentBlog.summary || '-' }}</p><h4>Markdown 正文</h4><pre>{{ currentBlog.content || '-' }}</pre><template v-if="currentBlog.rejectReason || currentBlog.auditReason || currentBlog.reason"><h4>审核说明</h4><p class="error-detail-text">{{ currentBlog.rejectReason || currentBlog.auditReason || currentBlog.reason }}</p></template><template v-if="currentBlog.sourceName"><h4>转载来源</h4><p>{{ currentBlog.sourceName }} <a v-if="safeSourceUrl" :href="safeSourceUrl" target="_blank" rel="noopener noreferrer">查看原文</a></p></template></section></template></a-drawer>
    <a-modal v-model:open="statusOpen" :title="statusForm.status === 'PUBLISHED' ? '恢复博客公开' : '下线博客'" :confirm-loading="statusSaving" :ok-text="statusForm.status === 'PUBLISHED' ? '确认恢复' : '确认下线'" :ok-button-props="{ danger: statusForm.status === 'OFFLINE' }" @ok="saveStatus"><a-alert :message="`操作对象：${statusForm.title}`" type="warning" show-icon style="margin-bottom: 18px" /><a-form ref="statusFormRef" :model="statusForm" :rules="statusRules" layout="vertical"><a-form-item label="操作原因" name="reason" :extra="statusForm.status === 'PUBLISHED' ? '恢复公开时可填写说明' : '下线原因将提交后端记录'"><a-textarea v-model:value="statusForm.reason" :rows="4" :maxlength="500" show-count /></a-form-item></a-form></a-modal>
  </div>
</template>
