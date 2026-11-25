<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { adminResourceApi, adminResourceCategoryApi } from '../../api/resource'
import { useTable } from '../../composables/useTable'
import { accessTypeText, resourcePrice, resourceStatus, resourceStatusOptions } from '../../utils/resource'
import { errorText, formatDate, idOf, pageResult } from '../../utils'

const router = useRouter()
const filters = reactive({ keyword: '', status: undefined, categoryId: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminResourceApi.list, () => ({ keyword: filters.keyword.trim() || undefined, status: filters.status, categoryId: filters.categoryId }))
const categories = ref([])
const detailOpen = ref(false)
const current = ref(null)
const statusOpen = ref(false)
const statusSaving = ref(false)
const statusFormRef = ref()
const statusForm = reactive({ id: '', title: '', status: 'OFFLINE', reason: '' })
const statusRules = { reason: [{ validator: (_, value) => statusForm.status === 'PUBLISHED' || value?.trim() ? Promise.resolve() : Promise.reject(new Error('下线或标记失效时必须填写原因')) }] }
const columns = [
  { title: '资源', key: 'title', width: 300 }, { title: '作者 ID', key: 'authorId', width: 150 },
  { title: '分类 / 访问类型', key: 'category', width: 160 }, { title: '状态', key: 'status', width: 130 },
  { title: '版本', key: 'versions', width: 130 }, { title: '更新时间', key: 'updatedAt', width: 180 },
  { title: '操作', key: 'actions', width: 150, fixed: 'right' },
]

function categoryName(id) { return categories.value.find((item) => String(item.id) === String(id))?.name || `分类 #${id ?? '-'}` }
function openDetail(record) { current.value = record; detailOpen.value = true }
function openStatus(record) {
  Object.assign(statusForm, { id: idOf(record.id), title: record.title, status: record.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED', reason: '' })
  statusOpen.value = true
}
async function saveStatus() {
  await statusFormRef.value.validate()
  statusSaving.value = true
  try {
    await adminResourceApi.updateStatus(statusForm.id, { status: statusForm.status, reason: statusForm.reason.trim() })
    message.success(statusForm.status === 'PUBLISHED' ? '资源已恢复公开' : statusForm.status === 'INVALID' ? '资源已标记失效' : '资源已下线')
    statusOpen.value = false
    detailOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusSaving.value = false
  }
}
function resetFilters() { Object.assign(filters, { keyword: '', status: undefined, categoryId: undefined }); search() }

onMounted(async () => {
  load()
  try {
    categories.value = pageResult(await adminResourceCategoryApi.list({ page: 1, size: 100 })).records
  } catch (error) {
    message.error(errorText(error))
  }
})
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input v-model:value="filters.keyword" allow-clear placeholder="搜索资源标题、摘要或描述" style="width: 250px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input><a-select v-model:value="filters.categoryId" allow-clear placeholder="全部分类" style="width: 140px" :options="categories.map(item => ({ value: String(item.id), label: item.name }))" /><a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 140px" :options="resourceStatusOptions" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-space><a-button v-permission="'audit:task:list'" @click="router.push('/audits?bizType=RESOURCE')">资源审核</a-button><a-button @click="load()"><ReloadOutlined /></a-button></a-space></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1250 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'title'"><div class="admin-blog-title"><strong>{{ record.title }}</strong><span>{{ record.summary }}</span></div></template><template v-else-if="column.key === 'authorId'"><code class="table-code">{{ record.authorId }}</code></template><template v-else-if="column.key === 'category'"><div>{{ categoryName(record.categoryId) }}</div><small>{{ accessTypeText(record.accessType) }} · {{ resourcePrice(record) }}</small></template><template v-else-if="column.key === 'status'"><a-tag :color="resourceStatus(record.status).color">{{ resourceStatus(record.status).text }}</a-tag><div><small>版本：{{ resourceStatus(record.versionStatus).text }}</small></div></template><template v-else-if="column.key === 'versions'">最新 v{{ record.latestVersionNo ?? '-' }}<br>公开 v{{ record.publishedVersionNo ?? '-' }}</template><template v-else-if="column.key === 'updatedAt'">{{ formatDate(record.updateTime || record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-space><a-button type="link" size="small" @click="openDetail(record)">详情</a-button><a-button v-permission="'resource:status'" type="link" size="small" @click="openStatus(record)">状态处理</a-button></a-space></template></template></a-table></a-card>
    <a-drawer v-model:open="detailOpen" title="资源详情（最新版本）" width="720"><template v-if="current"><h2 class="drawer-blog-title">{{ current.title }}</h2><a-descriptions :column="2" size="small" bordered><a-descriptions-item label="资源 ID">{{ current.id }}</a-descriptions-item><a-descriptions-item label="作者 ID">{{ current.authorId }}</a-descriptions-item><a-descriptions-item label="分类">{{ categoryName(current.categoryId) }}</a-descriptions-item><a-descriptions-item label="访问类型">{{ accessTypeText(current.accessType) }}</a-descriptions-item><a-descriptions-item label="价格">{{ resourcePrice(current) }}</a-descriptions-item><a-descriptions-item label="浏览量">{{ current.viewCount || 0 }}</a-descriptions-item><a-descriptions-item label="资源状态"><a-tag :color="resourceStatus(current.status).color">{{ resourceStatus(current.status).text }}</a-tag></a-descriptions-item><a-descriptions-item label="版本状态">{{ resourceStatus(current.versionStatus).text }}</a-descriptions-item><a-descriptions-item label="最新 / 公开版本">v{{ current.latestVersionNo ?? '-' }} / v{{ current.publishedVersionNo ?? '-' }}</a-descriptions-item><a-descriptions-item label="当前详情版本">v{{ current.versionNo ?? '-' }}</a-descriptions-item><a-descriptions-item label="创建时间" :span="2">{{ formatDate(current.createTime) }}</a-descriptions-item><a-descriptions-item label="更新时间" :span="2">{{ formatDate(current.updateTime) }}</a-descriptions-item><a-descriptions-item label="附件文件 ID" :span="2">{{ current.attachmentFileIds?.join(', ') || '-' }}</a-descriptions-item></a-descriptions><div v-if="current.tags?.length" class="drawer-tags"><a-tag v-for="tag in current.tags" :key="tag">{{ tag }}</a-tag></div><section class="drawer-content-section"><h4>摘要</h4><p>{{ current.summary }}</p><h4>资源描述</h4><pre>{{ current.description }}</pre><h4>版权声明</h4><p>{{ current.copyrightNotice }}</p><template v-if="current.auditReason"><h4>审核 / 操作原因</h4><p class="error-detail-text">{{ current.auditReason }}</p></template><a-button v-permission="'resource:status'" type="primary" style="margin-top: 20px" @click="openStatus(current)">处理资源状态</a-button></section></template></a-drawer>
    <a-modal v-model:open="statusOpen" title="资源状态处理" :confirm-loading="statusSaving" ok-text="确认提交" @ok="saveStatus"><a-alert :message="`操作对象：${statusForm.title}`" description="恢复公开要求后端存在已审核通过的公开版本；下线和失效会立即停止公开访问。" type="warning" show-icon style="margin-bottom: 18px" /><a-form ref="statusFormRef" :model="statusForm" :rules="statusRules" layout="vertical"><a-form-item label="目标状态" name="status"><a-radio-group v-model:value="statusForm.status"><a-radio-button value="PUBLISHED">恢复公开</a-radio-button><a-radio-button value="OFFLINE">下线</a-radio-button><a-radio-button value="INVALID">标记失效</a-radio-button></a-radio-group></a-form-item><a-form-item label="操作原因" name="reason"><a-textarea v-model:value="statusForm.reason" :rows="4" :maxlength="500" show-count /></a-form-item></a-form></a-modal>
  </div>
</template>
