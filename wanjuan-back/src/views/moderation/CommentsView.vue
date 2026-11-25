<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { adminCommentApi } from '../../api/community'
import { useTable } from '../../composables/useTable'
import { blogStatus } from '../../utils/blog'
import { errorText, formatDate, idOf } from '../../utils'

const filters = reactive({ targetType: undefined, targetId: undefined, status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminCommentApi.list, () => ({ targetType: filters.targetType, targetId: filters.targetId, status: filters.status }))
const detailOpen = ref(false)
const current = ref(null)
const statusOpen = ref(false)
const statusSaving = ref(false)
const statusRef = ref()
const statusForm = reactive({ id: '', status: 'OFFLINE', reason: '' })
const targetOptions = [{ value: 'BLOG', label: '博客' }, { value: 'FORUM', label: '论坛主题' }, { value: 'RESOURCE', label: '资源' }]
const statusOptions = ['DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'MANUAL_REVIEW', 'OFFLINE'].map((value) => ({ value, label: blogStatus(value).text }))
const statusRules = { reason: [{ validator: (_, value) => statusForm.status === 'PUBLISHED' || value?.trim() ? Promise.resolve() : Promise.reject(new Error('下线评论必须填写原因')) }, { max: 500, message: '原因不能超过500个字符' }] }
const columns = [{ title: '评论 ID', dataIndex: 'id', key: 'id', width: 120 }, { title: '评论内容', key: 'content', width: 360 }, { title: '目标', key: 'target', width: 180 }, { title: '作者', key: 'author', width: 130 }, { title: '回复 / 点赞', key: 'counts', width: 130 }, { title: '状态', key: 'status', width: 110 }, { title: '发布时间', key: 'createTime', width: 180 }, { title: '操作', key: 'actions', width: 170, fixed: 'right' }]

function targetName(type) {
  return targetOptions.find((item) => item.value === type)?.label || type || '-'
}

function resetFilters() {
  Object.assign(filters, { targetType: undefined, targetId: undefined, status: undefined })
  search()
}

function openDetail(record) {
  current.value = record
  detailOpen.value = true
}

function openStatus(record) {
  Object.assign(statusForm, { id: idOf(record.id), status: record.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED', reason: '' })
  statusOpen.value = true
}

async function saveStatus() {
  await statusRef.value.validate()
  statusSaving.value = true
  try {
    await adminCommentApi.updateStatus(statusForm.id, { status: statusForm.status, reason: statusForm.reason.trim() || null })
    message.success(statusForm.status === 'PUBLISHED' ? '评论已恢复公开' : '评论已下线')
    statusOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusSaving.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-select v-model:value="filters.targetType" allow-clear placeholder="全部目标类型" style="width: 150px" :options="targetOptions" /><a-input-number v-model:value="filters.targetId" :min="1" placeholder="目标 ID" style="width: 160px" @press-enter="search" /><a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 150px" :options="statusOptions" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-button @click="load()"><ReloadOutlined /></a-button></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1350 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'content'"><div class="admin-blog-title"><strong>{{ record.content }}</strong><span v-if="record.replyToId">回复评论 #{{ record.replyToId }}</span></div></template><template v-else-if="column.key === 'target'">{{ targetName(record.targetType) }} #{{ record.targetId }}</template><template v-else-if="column.key === 'author'">用户 #{{ record.authorId }}</template><template v-else-if="column.key === 'counts'">{{ record.replyCount || 0 }} / {{ record.likeCount || 0 }}</template><template v-else-if="column.key === 'status'"><a-tag :color="blogStatus(record.status).color">{{ blogStatus(record.status).text }}</a-tag></template><template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-space><a-button type="link" size="small" @click="openDetail(record)">详情</a-button><a-button v-if="['PUBLISHED', 'OFFLINE'].includes(record.status)" v-permission="'comment:status'" type="link" size="small" :danger="record.status === 'PUBLISHED'" @click="openStatus(record)">{{ record.status === 'PUBLISHED' ? '下线' : '恢复' }}</a-button></a-space></template><template v-else>{{ record[column.dataIndex] ?? '-' }}</template></template></a-table></a-card>
    <a-drawer v-model:open="detailOpen" title="评论最新版本详情" width="640"><template v-if="current"><a-descriptions :column="2" size="small" bordered><a-descriptions-item label="评论 ID">{{ current.id }}</a-descriptions-item><a-descriptions-item label="状态"><a-tag :color="blogStatus(current.status).color">{{ blogStatus(current.status).text }}</a-tag></a-descriptions-item><a-descriptions-item label="目标类型">{{ targetName(current.targetType) }}</a-descriptions-item><a-descriptions-item label="目标 ID">{{ current.targetId }}</a-descriptions-item><a-descriptions-item label="作者">用户 #{{ current.authorId }}</a-descriptions-item><a-descriptions-item label="根评论">{{ current.rootId || '-' }}</a-descriptions-item><a-descriptions-item label="父评论">{{ current.parentId || '-' }}</a-descriptions-item><a-descriptions-item label="被回复评论">{{ current.replyToId || '-' }}</a-descriptions-item><a-descriptions-item label="最新版本">v{{ current.latestVersionNo }}</a-descriptions-item><a-descriptions-item label="公开版本">{{ current.publishedVersionNo ? `v${current.publishedVersionNo}` : '暂无' }}</a-descriptions-item><a-descriptions-item label="创建时间" :span="2">{{ formatDate(current.createTime) }}</a-descriptions-item><a-descriptions-item label="审核说明" :span="2">{{ current.auditReason || '-' }}</a-descriptions-item></a-descriptions><section class="drawer-content-section"><h4>评论内容</h4><pre>{{ current.content || '-' }}</pre><h4>图片文件 ID</h4><p>{{ (current.imageFileIds || []).join(', ') || '-' }}</p></section></template></a-drawer>
    <a-modal v-model:open="statusOpen" :title="statusForm.status === 'PUBLISHED' ? '恢复评论公开' : '下线评论'" :confirm-loading="statusSaving" @ok="saveStatus"><a-form ref="statusRef" :model="statusForm" :rules="statusRules" layout="vertical"><a-form-item label="操作原因" name="reason" :extra="statusForm.status === 'PUBLISHED' ? '恢复公开时可选填' : '请说明下线原因'"><a-textarea v-model:value="statusForm.reason" :rows="4" :maxlength="500" show-count /></a-form-item></a-form></a-modal>
  </div>
</template>
