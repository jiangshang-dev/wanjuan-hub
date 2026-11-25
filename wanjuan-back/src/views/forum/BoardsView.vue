<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { adminsApi } from '../../api'
import { adminForumApi as forumApi } from '../../api/community'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf, isEnabled, pageResult } from '../../utils'

const activeTab = ref('boards')
const keyword = ref('')
const { loading, records, pagination, load, changeTable, search } = useTable(forumApi.boards, () => ({ keyword: keyword.value.trim() || undefined }))
const boardOptions = ref([])
const admins = ref([])
const moderatorLoading = ref(false)
const moderators = ref([])
const moderatorBoardId = ref()
const boardOpen = ref(false)
const boardSaving = ref(false)
const boardEditingId = ref('')
const boardFormRef = ref()
const boardForm = reactive({ sectionId: 1, parentId: undefined, name: '', description: '', sortOrder: 0, status: 1, postPermission: 'USER', replyPermission: 'USER' })
const moderatorOpen = ref(false)
const moderatorSaving = ref(false)
const moderatorEditingId = ref('')
const moderatorFormRef = ref()
const moderatorForm = reactive({ boardId: undefined, adminId: undefined, status: 1 })
const permissionOptions = [{ value: 'USER', label: '普通用户' }, { value: 'MODERATOR', label: '版主' }, { value: 'ADMIN', label: '全局管理员' }]
const boardRules = { sectionId: [{ required: true, type: 'number', message: '请输入分区 ID' }], name: [{ required: true, whitespace: true, message: '请输入板块名称' }, { max: 50, message: '名称不能超过50个字符' }], description: [{ max: 500, message: '简介不能超过500个字符' }] }
const moderatorRules = { boardId: [{ required: true, message: '请选择板块' }], adminId: [{ required: true, message: '请选择管理员' }] }
const boardColumns = [{ title: 'ID', dataIndex: 'id', key: 'id', width: 90 }, { title: '板块', key: 'name', width: 220 }, { title: '分区 ID', dataIndex: 'sectionId', key: 'sectionId', width: 100 }, { title: '父板块', key: 'parent', width: 130 }, { title: '主题数', dataIndex: 'topicCount', key: 'topicCount', width: 100 }, { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 90 }, { title: '状态', key: 'status', width: 90 }, { title: '发帖 / 回复权限', key: 'permissions', width: 190 }, { title: '操作', key: 'actions', width: 150, fixed: 'right' }]
const moderatorColumns = [{ title: '关联 ID', dataIndex: 'id', key: 'id', width: 120 }, { title: '板块', key: 'board', width: 220 }, { title: '管理员', key: 'admin', width: 220 }, { title: '状态', key: 'status', width: 100 }, { title: '创建时间', key: 'createTime', width: 180 }, { title: '操作', key: 'actions', width: 150, fixed: 'right' }]
const adminOptions = computed(() => admins.value.map((admin) => ({ value: String(admin.id), label: `${admin.nickname || admin.username || admin.account || '管理员'} (#${admin.id})` })))

function boardName(id) {
  return boardOptions.value.find((board) => String(board.id) === String(id))?.name || `板块 #${id}`
}

function adminName(id) {
  const admin = admins.value.find((item) => String(item.id) === String(id))
  return admin ? `${admin.nickname || admin.username || admin.account || '管理员'} (#${admin.id})` : `管理员 #${id}`
}

async function loadModerators() {
  moderatorLoading.value = true
  try {
    const data = await forumApi.moderators({ boardId: moderatorBoardId.value })
    moderators.value = Array.isArray(data) ? data : data?.records || []
  } catch (error) {
    message.error(errorText(error))
  } finally {
    moderatorLoading.value = false
  }
}

function openBoard(record = null) {
  boardEditingId.value = record ? idOf(record.id) : ''
  Object.assign(boardForm, record ? { sectionId: Number(record.sectionId), parentId: record.parentId ? String(record.parentId) : undefined, name: record.name || '', description: record.description || '', sortOrder: Number(record.sortOrder || 0), status: isEnabled(record.status) ? 1 : 0, postPermission: record.postPermission || 'USER', replyPermission: record.replyPermission || 'USER' } : { sectionId: 1, parentId: undefined, name: '', description: '', sortOrder: 0, status: 1, postPermission: 'USER', replyPermission: 'USER' })
  boardOpen.value = true
}

async function saveBoard() {
  await boardFormRef.value.validate()
  boardSaving.value = true
  try {
    const data = { ...boardForm, sectionId: Number(boardForm.sectionId), parentId: boardForm.parentId || null, name: boardForm.name.trim(), description: boardForm.description.trim() || null }
    if (boardEditingId.value) await forumApi.updateBoard(boardEditingId.value, data)
    else await forumApi.createBoard(data)
    message.success(boardEditingId.value ? '板块已更新' : '板块已创建')
    boardOpen.value = false
    load()
    loadOptions()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    boardSaving.value = false
  }
}

async function removeBoard(record) {
  try {
    await forumApi.deleteBoard(idOf(record.id))
    message.success('板块已删除')
    load()
    loadOptions()
  } catch (error) {
    message.error(errorText(error))
  }
}

function openModerator(record = null) {
  moderatorEditingId.value = record ? idOf(record.id) : ''
  Object.assign(moderatorForm, record ? { boardId: String(record.boardId), adminId: String(record.adminId), status: isEnabled(record.status) ? 1 : 0 } : { boardId: moderatorBoardId.value, adminId: undefined, status: 1 })
  moderatorOpen.value = true
}

async function saveModerator() {
  await moderatorFormRef.value.validate()
  moderatorSaving.value = true
  try {
    const data = { boardId: moderatorForm.boardId, adminId: moderatorForm.adminId, status: moderatorForm.status }
    if (moderatorEditingId.value) await forumApi.updateModerator(moderatorEditingId.value, data)
    else await forumApi.createModerator(data)
    message.success(moderatorEditingId.value ? '版主关联已更新' : '版主已关联')
    moderatorOpen.value = false
    loadModerators()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    moderatorSaving.value = false
  }
}

async function removeModerator(record) {
  try {
    await forumApi.deleteModerator(idOf(record.id))
    message.success('版主关联已删除')
    loadModerators()
  } catch (error) {
    message.error(errorText(error))
  }
}

async function loadOptions() {
  try {
    const [boardData, adminData] = await Promise.all([forumApi.boards({ page: 1, size: 100 }), adminsApi.list({ page: 1, size: 100 })])
    boardOptions.value = pageResult(boardData).records
    admins.value = pageResult(adminData).records
  } catch (error) {
    message.error(errorText(error))
  }
}

onMounted(() => {
  load()
  loadModerators()
  loadOptions()
})
</script>

<template>
  <div class="management-page">
    <a-tabs v-model:active-key="activeTab"><a-tab-pane key="boards" tab="板块管理" /><a-tab-pane key="moderators" tab="版主管理" /></a-tabs>
    <template v-if="activeTab === 'boards'">
      <div class="toolbar"><a-space wrap><a-input v-model:value="keyword" allow-clear placeholder="搜索板块名称" style="width: 220px" @press-enter="search" /><a-button type="primary" @click="search">查询</a-button></a-space><a-space><a-button @click="load()"><ReloadOutlined /></a-button><a-button v-permission="'forum:board:create'" type="primary" @click="openBoard()"><PlusOutlined />新增板块</a-button></a-space></div>
      <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="boardColumns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1250 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'name'"><div class="admin-blog-title"><strong>{{ record.name }}</strong><span>{{ record.description || '暂无简介' }}</span></div></template><template v-else-if="column.key === 'parent'">{{ record.parentId ? boardName(record.parentId) : '一级板块' }}</template><template v-else-if="column.key === 'status'"><a-tag :color="isEnabled(record.status) ? 'success' : 'default'">{{ isEnabled(record.status) ? '启用' : '停用' }}</a-tag></template><template v-else-if="column.key === 'permissions'">{{ record.postPermission }} / {{ record.replyPermission }}</template><template v-else-if="column.key === 'actions'"><a-space><a-button v-permission="'forum:board:update'" type="link" size="small" @click="openBoard(record)">编辑</a-button><a-popconfirm title="确定删除该板块吗？" description="存在子板块或主题时无法删除。" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="removeBoard(record)"><a-button v-permission="'forum:board:delete'" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template><template v-else>{{ record[column.dataIndex] ?? '-' }}</template></template></a-table></a-card>
    </template>
    <template v-else>
      <div class="toolbar"><a-space wrap><a-select v-model:value="moderatorBoardId" allow-clear placeholder="全部板块" style="width: 220px" :options="boardOptions.map(board => ({ value: String(board.id), label: board.name }))" @change="loadModerators" /><a-button @click="loadModerators">查询</a-button></a-space><a-button v-permission="'forum:moderator:create'" type="primary" @click="openModerator()"><PlusOutlined />关联版主</a-button></div>
      <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="moderatorColumns" :data-source="moderators" :loading="moderatorLoading" :pagination="false" :scroll="{ x: 950 }"><template #bodyCell="{ column, record }"><template v-if="column.key === 'board'">{{ boardName(record.boardId) }}</template><template v-else-if="column.key === 'admin'">{{ adminName(record.adminId) }}</template><template v-else-if="column.key === 'status'"><a-tag :color="isEnabled(record.status) ? 'success' : 'default'">{{ isEnabled(record.status) ? '启用' : '停用' }}</a-tag></template><template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-space><a-button v-permission="'forum:moderator:update'" type="link" size="small" @click="openModerator(record)">编辑</a-button><a-popconfirm title="确定删除该版主关联吗？" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="removeModerator(record)"><a-button v-permission="'forum:moderator:delete'" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template><template v-else>{{ record[column.dataIndex] ?? '-' }}</template></template></a-table></a-card>
    </template>
    <a-modal v-model:open="boardOpen" :title="boardEditingId ? '编辑板块' : '新增板块'" :confirm-loading="boardSaving" destroy-on-close @ok="saveBoard"><a-form ref="boardFormRef" :model="boardForm" :rules="boardRules" layout="vertical"><div class="modal-form-grid"><a-form-item label="分区 ID" name="sectionId"><a-input-number v-model:value="boardForm.sectionId" :min="1" style="width: 100%" /></a-form-item><a-form-item label="父板块" name="parentId"><a-select v-model:value="boardForm.parentId" allow-clear placeholder="一级板块" :options="boardOptions.filter(board => String(board.id) !== boardEditingId).map(board => ({ value: String(board.id), label: board.name }))" /></a-form-item></div><a-form-item label="板块名称" name="name"><a-input v-model:value="boardForm.name" :maxlength="50" /></a-form-item><a-form-item label="板块简介" name="description"><a-textarea v-model:value="boardForm.description" :rows="3" :maxlength="500" show-count /></a-form-item><div class="modal-form-grid"><a-form-item label="排序" name="sortOrder"><a-input-number v-model:value="boardForm.sortOrder" :min="0" style="width: 100%" /></a-form-item><a-form-item label="状态" name="status"><a-radio-group v-model:value="boardForm.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]" /></a-form-item></div><div class="modal-form-grid"><a-form-item label="发帖权限" name="postPermission"><a-select v-model:value="boardForm.postPermission" :options="permissionOptions" /></a-form-item><a-form-item label="回复权限" name="replyPermission"><a-select v-model:value="boardForm.replyPermission" :options="permissionOptions" /></a-form-item></div></a-form></a-modal>
    <a-modal v-model:open="moderatorOpen" :title="moderatorEditingId ? '编辑版主关联' : '关联版主'" :confirm-loading="moderatorSaving" destroy-on-close @ok="saveModerator"><a-form ref="moderatorFormRef" :model="moderatorForm" :rules="moderatorRules" layout="vertical"><a-form-item label="板块" name="boardId"><a-select v-model:value="moderatorForm.boardId" :options="boardOptions.map(board => ({ value: String(board.id), label: board.name }))" /></a-form-item><a-form-item label="管理员" name="adminId"><a-select v-model:value="moderatorForm.adminId" show-search option-filter-prop="label" :options="adminOptions" /></a-form-item><a-form-item label="状态" name="status"><a-radio-group v-model:value="moderatorForm.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]" /></a-form-item></a-form></a-modal>
  </div>
</template>
