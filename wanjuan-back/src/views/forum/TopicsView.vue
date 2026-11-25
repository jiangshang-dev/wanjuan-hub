<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { adminForumApi } from '../../api/community'
import { useTable } from '../../composables/useTable'
import { blogStatus } from '../../utils/blog'
import { errorText, formatDate, idOf, pageResult } from '../../utils'

const filters = reactive({ keyword: '', status: undefined, boardId: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminForumApi.topics, () => ({ keyword: filters.keyword.trim() || undefined, status: filters.status, boardId: filters.boardId }))
const boards = ref([])
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const editorOpen = ref(false)
const editorSaving = ref(false)
const editingId = ref('')
const editorRef = ref()
const editor = reactive({ authorId: undefined, boardId: undefined, title: '', content: '', imageFileIdsText: '' })
const statusOpen = ref(false)
const statusSaving = ref(false)
const statusRef = ref()
const statusForm = reactive({ id: '', title: '', status: 'OFFLINE', reason: '' })
const moderateOpen = ref(false)
const moderateSaving = ref(false)
const moderateForm = reactive({ id: '', title: '', locked: 0, topped: 0, essence: 0, boardId: undefined })
const statusOptions = ['DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'MANUAL_REVIEW', 'OFFLINE'].map((value) => ({ value, label: blogStatus(value).text }))
const editorRules = { authorId: [{ required: true, type: 'number', message: '请输入作者用户 ID' }], boardId: [{ required: true, message: '请选择板块' }], title: [{ required: true, whitespace: true, message: '请输入标题' }, { max: 120, message: '标题不能超过120个字符' }], content: [{ required: true, whitespace: true, message: '请输入正文' }] }
const statusRules = { reason: [{ required: true, whitespace: true, message: '请填写操作原因' }, { max: 500, message: '原因不能超过500个字符' }] }
const columns = [{ title: '主题', key: 'title', width: 300 }, { title: '板块', key: 'board', width: 140 }, { title: '作者', key: 'author', width: 120 }, { title: '状态', key: 'status', width: 110 }, { title: '标记', key: 'flags', width: 170 }, { title: '互动', key: 'counts', width: 190 }, { title: '更新时间', key: 'updateTime', width: 170 }, { title: '操作', key: 'actions', width: 300, fixed: 'right' }]

function boardName(id) {
  return boards.value.find((board) => String(board.id) === String(id))?.name || `板块 #${id}`
}

function resetFilters() {
  Object.assign(filters, { keyword: '', status: undefined, boardId: undefined })
  search()
}

async function openDetail(record) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await adminForumApi.topic(idOf(record.id))
  } catch (error) {
    message.error(errorText(error))
  } finally {
    detailLoading.value = false
  }
}

function openEditor(record = null) {
  editingId.value = record ? idOf(record.id) : ''
  Object.assign(editor, record ? { authorId: Number(record.authorId), boardId: String(record.boardId), title: record.title || '', content: record.content || '', imageFileIdsText: (record.imageFileIds || []).join(',') } : { authorId: undefined, boardId: filters.boardId, title: '', content: '', imageFileIdsText: '' })
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
    const data = { authorId: editor.authorId, boardId: editor.boardId, title: editor.title.trim(), content: editor.content.trim(), imageFileIds: imageFileIds() }
    if (editingId.value) await adminForumApi.updateTopic(editingId.value, data)
    else await adminForumApi.createTopic(data)
    message.success(editingId.value ? '主题已更新并重新提交审核' : '主题已创建并提交审核')
    editorOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    editorSaving.value = false
  }
}

function openStatus(record) {
  Object.assign(statusForm, { id: idOf(record.id), title: record.title, status: record.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED', reason: '' })
  statusOpen.value = true
}

async function saveStatus() {
  await statusRef.value.validate()
  statusSaving.value = true
  try {
    await adminForumApi.topicStatus(statusForm.id, { status: statusForm.status, reason: statusForm.reason.trim() })
    message.success(statusForm.status === 'PUBLISHED' ? '主题已恢复公开' : '主题已下线')
    statusOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusSaving.value = false
  }
}

function openModerate(record) {
  Object.assign(moderateForm, { id: idOf(record.id), title: record.title, locked: Number(record.locked || 0), topped: Number(record.topped || 0), essence: Number(record.essence || 0), boardId: String(record.boardId) })
  moderateOpen.value = true
}

async function saveModerate() {
  moderateSaving.value = true
  try {
    await adminForumApi.moderateTopic(moderateForm.id, { locked: moderateForm.locked, topped: moderateForm.topped, essence: moderateForm.essence, boardId: moderateForm.boardId })
    message.success('主题管理设置已保存')
    moderateOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    moderateSaving.value = false
  }
}

async function remove(record) {
  try {
    await adminForumApi.deleteTopic(idOf(record.id))
    message.success('主题已删除')
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
    <div class="toolbar"><a-space wrap><a-input v-model:value="filters.keyword" allow-clear placeholder="搜索主题标题" style="width: 220px" @press-enter="search" /><a-select v-model:value="filters.boardId" allow-clear placeholder="全部板块" style="width: 150px" :options="boards.map(board => ({ value: String(board.id), label: board.name }))" /><a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 150px" :options="statusOptions" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-space><a-button @click="load()"><ReloadOutlined /></a-button><a-button v-permission="'forum:topic:create'" type="primary" @click="openEditor()"><PlusOutlined />新增主题</a-button></a-space></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1550 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'title'"><div class="admin-blog-title"><strong>#{{ record.id }} {{ record.title }}</strong><span>{{ record.content }}</span></div></template><template v-else-if="column.key === 'board'">{{ boardName(record.boardId) }}</template><template v-else-if="column.key === 'author'">用户 #{{ record.authorId }}</template><template v-else-if="column.key === 'status'"><a-tag :color="blogStatus(record.status).color">{{ blogStatus(record.status).text }}</a-tag></template><template v-else-if="column.key === 'flags'"><a-space size="small"><a-tag v-if="Number(record.topped)" color="blue">置顶</a-tag><a-tag v-if="Number(record.essence)" color="gold">精华</a-tag><a-tag v-if="Number(record.locked)">锁定</a-tag><span v-if="!Number(record.topped) && !Number(record.essence) && !Number(record.locked)">-</span></a-space></template><template v-else-if="column.key === 'counts'">跟帖 {{ record.postCount || 0 }} · 赞 {{ record.likeCount || 0 }} · 浏览 {{ record.viewCount || 0 }}</template><template v-else-if="column.key === 'updateTime'">{{ formatDate(record.updateTime || record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-space wrap><a-button v-permission="'forum:topic:detail'" type="link" size="small" @click="openDetail(record)">详情</a-button><a-button v-permission="'forum:topic:update'" type="link" size="small" @click="openEditor(record)">编辑</a-button><a-button v-permission="'forum:topic:moderate'" type="link" size="small" @click="openModerate(record)">管理</a-button><a-button v-if="['PUBLISHED', 'OFFLINE'].includes(record.status)" v-permission="'forum:topic:status'" type="link" size="small" :danger="record.status === 'PUBLISHED'" @click="openStatus(record)">{{ record.status === 'PUBLISHED' ? '下线' : '恢复' }}</a-button><a-popconfirm title="确定删除该主题吗？" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="remove(record)"><a-button v-permission="'forum:topic:delete'" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template></template></a-table></a-card>
    <a-drawer v-model:open="detailOpen" title="主题最新版本详情" width="720"><a-spin :spinning="detailLoading"><template v-if="detail"><h2 class="drawer-blog-title">{{ detail.title }}</h2><a-descriptions :column="2" size="small" bordered><a-descriptions-item label="主题 ID">{{ detail.id }}</a-descriptions-item><a-descriptions-item label="状态"><a-tag :color="blogStatus(detail.status).color">{{ blogStatus(detail.status).text }}</a-tag></a-descriptions-item><a-descriptions-item label="板块">{{ boardName(detail.boardId) }}</a-descriptions-item><a-descriptions-item label="作者">用户 #{{ detail.authorId }}</a-descriptions-item><a-descriptions-item label="最新版本">v{{ detail.latestVersionNo }}</a-descriptions-item><a-descriptions-item label="公开版本">{{ detail.publishedVersionNo ? `v${detail.publishedVersionNo}` : '暂无' }}</a-descriptions-item><a-descriptions-item label="创建时间" :span="2">{{ formatDate(detail.createTime) }}</a-descriptions-item><a-descriptions-item label="审核说明" :span="2">{{ detail.auditReason || '-' }}</a-descriptions-item></a-descriptions><section class="drawer-content-section"><h4>Markdown 正文</h4><pre>{{ detail.content || '-' }}</pre><h4>图片文件 ID</h4><p>{{ (detail.imageFileIds || []).join(', ') || '-' }}</p></section></template></a-spin></a-drawer>
    <a-modal v-model:open="editorOpen" :title="editingId ? '编辑主题' : '新增主题'" width="760px" :confirm-loading="editorSaving" destroy-on-close @ok="saveEditor"><a-form ref="editorRef" :model="editor" :rules="editorRules" layout="vertical"><div class="modal-form-grid"><a-form-item label="作者用户 ID" name="authorId"><a-input-number v-model:value="editor.authorId" :min="1" :disabled="Boolean(editingId)" style="width: 100%" /></a-form-item><a-form-item label="所属板块" name="boardId"><a-select v-model:value="editor.boardId" :disabled="Boolean(editingId)" :options="boards.map(board => ({ value: String(board.id), label: board.name }))" /></a-form-item></div><a-form-item label="标题" name="title"><a-input v-model:value="editor.title" :maxlength="120" show-count /></a-form-item><a-form-item label="Markdown 正文" name="content"><a-textarea v-model:value="editor.content" :rows="14" :maxlength="100000" /></a-form-item><a-form-item label="图片文件 ID" extra="以逗号分隔，最多20个；文件须属于作者"><a-input v-model:value="editor.imageFileIdsText" placeholder="例如：101,102" /></a-form-item></a-form></a-modal>
    <a-modal v-model:open="statusOpen" :title="statusForm.status === 'PUBLISHED' ? '恢复主题公开' : '下线主题'" :confirm-loading="statusSaving" @ok="saveStatus"><a-alert :message="`操作对象：${statusForm.title}`" type="warning" show-icon style="margin-bottom: 18px" /><a-form ref="statusRef" :model="statusForm" :rules="statusRules" layout="vertical"><a-form-item label="操作原因" name="reason"><a-textarea v-model:value="statusForm.reason" :rows="4" :maxlength="500" show-count /></a-form-item></a-form></a-modal>
    <a-modal v-model:open="moderateOpen" title="主题管理设置" :confirm-loading="moderateSaving" @ok="saveModerate"><a-alert :message="`操作对象：${moderateForm.title}`" type="info" show-icon style="margin-bottom: 18px" /><a-form layout="vertical"><a-form-item label="目标板块"><a-select v-model:value="moderateForm.boardId" :options="boards.map(board => ({ value: String(board.id), label: board.name }))" /></a-form-item><a-form-item label="锁定主题"><a-switch v-model:checked="moderateForm.locked" :checked-value="1" :un-checked-value="0" /></a-form-item><a-form-item label="置顶主题"><a-switch v-model:checked="moderateForm.topped" :checked-value="1" :un-checked-value="0" /></a-form-item><a-form-item label="设为精华"><a-switch v-model:checked="moderateForm.essence" :checked-value="1" :un-checked-value="0" /></a-form-item></a-form></a-modal>
  </div>
</template>
