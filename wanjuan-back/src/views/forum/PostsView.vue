<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { adminForumApi } from '../../api/community'
import { useTable } from '../../composables/useTable'
import { blogStatus } from '../../utils/blog'
import { errorText, formatDate, idOf, pageResult } from '../../utils'

const filters = reactive({ topicId: undefined, boardId: undefined, status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminForumApi.posts, () => ({ topicId: filters.topicId, boardId: filters.boardId, status: filters.status }))
const boards = ref([])
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const editorOpen = ref(false)
const editorSaving = ref(false)
const editingId = ref('')
const editorRef = ref()
const editor = reactive({ topicId: undefined, authorId: undefined, parentId: undefined, replyToId: undefined, content: '', imageFileIdsText: '' })
const statusOpen = ref(false)
const statusSaving = ref(false)
const statusRef = ref()
const statusForm = reactive({ id: '', status: 'OFFLINE', reason: '' })
const statusOptions = ['DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'MANUAL_REVIEW', 'OFFLINE'].map((value) => ({ value, label: blogStatus(value).text }))
const editorRules = { topicId: [{ required: true, type: 'number', message: '请输入主题 ID' }], authorId: [{ required: true, type: 'number', message: '请输入作者用户 ID' }], content: [{ required: true, whitespace: true, message: '请输入跟帖内容' }, { max: 50000, message: '内容不能超过50000个字符' }] }
const statusRules = { reason: [{ required: true, whitespace: true, message: '请填写操作原因' }, { max: 500, message: '原因不能超过500个字符' }] }
const columns = [{ title: 'ID / 楼层', key: 'id', width: 130 }, { title: '跟帖内容', key: 'content', width: 360 }, { title: '主题', key: 'topic', width: 120 }, { title: '板块', key: 'board', width: 140 }, { title: '作者', key: 'author', width: 120 }, { title: '状态', key: 'status', width: 110 }, { title: '点赞', dataIndex: 'likeCount', key: 'likeCount', width: 90 }, { title: '发布时间', key: 'createTime', width: 170 }, { title: '操作', key: 'actions', width: 250, fixed: 'right' }]

function boardName(id) {
  return boards.value.find((board) => String(board.id) === String(id))?.name || `板块 #${id}`
}

function resetFilters() {
  Object.assign(filters, { topicId: undefined, boardId: undefined, status: undefined })
  search()
}

async function openDetail(record) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await adminForumApi.post(idOf(record.id))
  } catch (error) {
    message.error(errorText(error))
  } finally {
    detailLoading.value = false
  }
}

function openEditor(record = null) {
  editingId.value = record ? idOf(record.id) : ''
  Object.assign(editor, record ? { topicId: Number(record.topicId), authorId: Number(record.authorId), parentId: record.parentId ? Number(record.parentId) : undefined, replyToId: record.replyToId ? Number(record.replyToId) : undefined, content: record.content || '', imageFileIdsText: (record.imageFileIds || []).join(',') } : { topicId: filters.topicId ? Number(filters.topicId) : undefined, authorId: undefined, parentId: undefined, replyToId: undefined, content: '', imageFileIdsText: '' })
  editorOpen.value = true
}

function imageFileIds() {
  const value = editor.imageFileIdsText.trim()
  if (!value) return []
  const ids = value.split(/[,，\s]+/).filter(Boolean)
  if (ids.length > 20 || ids.some((id) => !/^\d+$/.test(id))) throw new Error('图片文件 ID 需为数字，以逗号分隔，最多20个')
  return [...new Set(ids)]
}

async function saveEditor() {
  await editorRef.value.validate()
  editorSaving.value = true
  try {
    const data = { topicId: editor.topicId, authorId: editor.authorId, parentId: editor.parentId || null, replyToId: editor.replyToId || null, content: editor.content.trim(), imageFileIds: imageFileIds() }
    if (editingId.value) await adminForumApi.updatePost(editingId.value, data)
    else await adminForumApi.createPost(data)
    message.success(editingId.value ? '跟帖已更新并重新提交审核' : '跟帖已创建并提交审核')
    editorOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    editorSaving.value = false
  }
}

function openStatus(record) {
  Object.assign(statusForm, { id: idOf(record.id), status: record.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED', reason: '' })
  statusOpen.value = true
}

async function saveStatus() {
  await statusRef.value.validate()
  statusSaving.value = true
  try {
    await adminForumApi.postStatus(statusForm.id, { status: statusForm.status, reason: statusForm.reason.trim() })
    message.success(statusForm.status === 'PUBLISHED' ? '跟帖已恢复公开' : '跟帖已下线')
    statusOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusSaving.value = false
  }
}

async function remove(record) {
  try {
    await adminForumApi.deletePost(idOf(record.id))
    message.success('跟帖已删除')
    load(records.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error) {
    message.error(errorText(error))
  }
}

onMounted(async () => {
  load()
  try {
    boards.value = pageResult(await adminForumApi.boards({ page: 1, size: 100 })).records
  } catch (error) {
    message.error(errorText(error))
  }
})
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input-number v-model:value="filters.topicId" :min="1" placeholder="主题 ID" style="width: 150px" @press-enter="search" /><a-select v-model:value="filters.boardId" allow-clear placeholder="全部板块" style="width: 150px" :options="boards.map(board => ({ value: String(board.id), label: board.name }))" /><a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 150px" :options="statusOptions" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-space><a-button @click="load()"><ReloadOutlined /></a-button><a-button v-permission="'forum:post:create'" type="primary" @click="openEditor()"><PlusOutlined />新增跟帖</a-button></a-space></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1550 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'id'">#{{ record.id }}<br /><small>楼层 {{ record.floorNo ?? '-' }}</small></template><template v-else-if="column.key === 'content'"><div class="admin-blog-title"><strong>{{ record.content }}</strong><span v-if="record.replyToId">引用跟帖 #{{ record.replyToId }}</span></div></template><template v-else-if="column.key === 'topic'">主题 #{{ record.topicId }}</template><template v-else-if="column.key === 'board'">{{ boardName(record.boardId) }}</template><template v-else-if="column.key === 'author'">用户 #{{ record.authorId }}</template><template v-else-if="column.key === 'status'"><a-tag :color="blogStatus(record.status).color">{{ blogStatus(record.status).text }}</a-tag></template><template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-space wrap><a-button v-permission="'forum:post:detail'" type="link" size="small" @click="openDetail(record)">详情</a-button><a-button v-permission="'forum:post:update'" type="link" size="small" @click="openEditor(record)">编辑</a-button><a-button v-if="['PUBLISHED', 'OFFLINE'].includes(record.status)" v-permission="'forum:post:status'" type="link" size="small" :danger="record.status === 'PUBLISHED'" @click="openStatus(record)">{{ record.status === 'PUBLISHED' ? '下线' : '恢复' }}</a-button><a-popconfirm title="确定删除该跟帖吗？" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="remove(record)"><a-button v-permission="'forum:post:delete'" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template><template v-else>{{ record[column.dataIndex] ?? '-' }}</template></template></a-table></a-card>
    <a-drawer v-model:open="detailOpen" title="跟帖最新版本详情" width="680"><a-spin :spinning="detailLoading"><template v-if="detail"><a-descriptions :column="2" size="small" bordered><a-descriptions-item label="跟帖 ID">{{ detail.id }}</a-descriptions-item><a-descriptions-item label="楼层">{{ detail.floorNo }}</a-descriptions-item><a-descriptions-item label="主题 ID">{{ detail.topicId }}</a-descriptions-item><a-descriptions-item label="板块">{{ boardName(detail.boardId) }}</a-descriptions-item><a-descriptions-item label="作者">用户 #{{ detail.authorId }}</a-descriptions-item><a-descriptions-item label="状态"><a-tag :color="blogStatus(detail.status).color">{{ blogStatus(detail.status).text }}</a-tag></a-descriptions-item><a-descriptions-item label="父跟帖">{{ detail.parentId || '-' }}</a-descriptions-item><a-descriptions-item label="引用跟帖">{{ detail.replyToId || '-' }}</a-descriptions-item><a-descriptions-item label="最新版本">v{{ detail.latestVersionNo }}</a-descriptions-item><a-descriptions-item label="公开版本">{{ detail.publishedVersionNo ? `v${detail.publishedVersionNo}` : '暂无' }}</a-descriptions-item><a-descriptions-item label="审核说明" :span="2">{{ detail.auditReason || '-' }}</a-descriptions-item></a-descriptions><section class="drawer-content-section"><h4>Markdown 正文</h4><pre>{{ detail.content || '-' }}</pre><h4>图片文件 ID</h4><p>{{ (detail.imageFileIds || []).join(', ') || '-' }}</p></section></template></a-spin></a-drawer>
    <a-modal v-model:open="editorOpen" :title="editingId ? '编辑跟帖' : '新增跟帖'" width="760px" :confirm-loading="editorSaving" destroy-on-close @ok="saveEditor"><a-form ref="editorRef" :model="editor" :rules="editorRules" layout="vertical"><div class="modal-form-grid"><a-form-item label="主题 ID" name="topicId"><a-input-number v-model:value="editor.topicId" :min="1" :disabled="Boolean(editingId)" style="width: 100%" /></a-form-item><a-form-item label="作者用户 ID" name="authorId"><a-input-number v-model:value="editor.authorId" :min="1" :disabled="Boolean(editingId)" style="width: 100%" /></a-form-item></div><div class="modal-form-grid"><a-form-item label="父跟帖 ID" name="parentId"><a-input-number v-model:value="editor.parentId" :min="1" style="width: 100%" /></a-form-item><a-form-item label="引用跟帖 ID" name="replyToId"><a-input-number v-model:value="editor.replyToId" :min="1" style="width: 100%" /></a-form-item></div><a-form-item label="Markdown 正文" name="content"><a-textarea v-model:value="editor.content" :rows="14" :maxlength="50000" show-count /></a-form-item><a-form-item label="图片文件 ID" extra="以逗号分隔，最多20个；文件须属于作者"><a-input v-model:value="editor.imageFileIdsText" placeholder="例如：101,102" /></a-form-item></a-form></a-modal>
    <a-modal v-model:open="statusOpen" :title="statusForm.status === 'PUBLISHED' ? '恢复跟帖公开' : '下线跟帖'" :confirm-loading="statusSaving" @ok="saveStatus"><a-form ref="statusRef" :model="statusForm" :rules="statusRules" layout="vertical"><a-form-item label="操作原因" name="reason"><a-textarea v-model:value="statusForm.reason" :rows="4" :maxlength="500" show-count /></a-form-item></a-form></a-modal>
  </div>
</template>
