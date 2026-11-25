<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { permissionsApi } from '../../api'
import { useTable } from '../../composables/useTable'
import { errorText, idOf, isEnabled, pageResult } from '../../utils'

const filters = reactive({ keyword: '', type: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(permissionsApi.list, () => ({ keyword: filters.keyword.trim() || undefined, type: filters.type }))
const allPermissions = ref([])
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref('')
const formRef = ref()
const form = reactive({ parentId: undefined, name: '', path: '', icon: '', type: 'MENU', permission: '', sort: 0, status: 1 })
const statusLoadingId = ref('')
const typeOptions = [{ value: 'DIRECTORY', label: '目录' }, { value: 'MENU', label: '菜单' }, { value: 'BUTTON', label: '按钮 / 操作' }]
const rules = {
  name: [{ required: true, whitespace: true, message: '请输入权限名称' }, { max: 50, message: '名称不能超过50个字符' }],
  path: [{ validator: (_, value) => form.type === 'BUTTON' || value?.trim() ? Promise.resolve() : Promise.reject(new Error('目录和菜单必须填写路由路径')) }],
  permission: [{ validator: (_, value) => form.type !== 'BUTTON' || value?.trim() ? Promise.resolve() : Promise.reject(new Error('操作权限必须填写权限标识')) }],
  sort: [{ type: 'number', min: 0, max: 9999, message: '排序值为0-9999' }],
}
const columns = [
  { title: '名称', dataIndex: 'name', key: 'name', width: 180 },
  { title: '类型', key: 'type', width: 120 },
  { title: '路由路径', dataIndex: 'path', key: 'path', width: 210 },
  { title: '权限标识', dataIndex: 'permission', key: 'permission', width: 220 },
  { title: '图标', dataIndex: 'icon', key: 'icon', width: 120 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 110 },
  { title: '操作', key: 'actions', width: 140, fixed: 'right' },
]

function normalizeNodes(nodes) {
  return nodes.map((node) => ({ ...node, id: idOf(node.id), name: node.name || node.permissionName || node.title, children: normalizeNodes(node.children || []) }))
}
function buildTree(nodes) {
  const normalized = normalizeNodes(nodes)
  if (normalized.some((node) => node.children.length)) return normalized
  const map = new Map(normalized.map((node) => [node.id, node]))
  const roots = []
  normalized.forEach((node) => {
    const parentId = idOf(node.parentId)
    if (parentId && map.has(parentId) && parentId !== node.id) map.get(parentId).children.push(node)
    else roots.push(node)
  })
  return roots
}
const treeRecords = computed(() => buildTree(records.value))
const parentOptions = computed(() => {
  function mapNodes(nodes) {
    return nodes.filter((node) => node.type !== 'BUTTON' && node.id !== editingId.value).map((node) => ({ ...node, children: mapNodes(node.children || []) }))
  }
  return mapNodes(buildTree(allPermissions.value))
})

onMounted(async () => {
  load()
  try {
    allPermissions.value = pageResult(await permissionsApi.list({ page: 1, size: 1000 })).records
  } catch (error) {
    message.error(errorText(error))
  }
})

function openCreate() {
  editingId.value = ''
  Object.assign(form, { parentId: undefined, name: '', path: '', icon: '', type: 'MENU', permission: '', sort: 0, status: 1 })
  modalOpen.value = true
  formRef.value?.clearValidate()
}

function openEdit(record) {
  editingId.value = idOf(record.id)
  Object.assign(form, { parentId: record.parentId === null || record.parentId === undefined || Number(record.parentId) === 0 ? undefined : idOf(record.parentId), name: record.name || record.permissionName || '', path: record.path || '', icon: record.icon || '', type: record.type || 'MENU', permission: record.permission || record.perms || '', sort: Number(record.sort ?? record.orderNum ?? 0), status: isEnabled(record.status) ? 1 : 0 })
  modalOpen.value = true
  formRef.value?.clearValidate()
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = { parentId: form.parentId || null, name: form.name.trim(), path: form.path.trim() || null, icon: form.icon.trim() || null, type: form.type, permission: form.permission.trim() || null, sort: form.sort, status: form.status }
    if (editingId.value) await permissionsApi.update(editingId.value, data)
    else await permissionsApi.create(data)
    message.success(editingId.value ? '权限已更新' : '权限已创建')
    modalOpen.value = false
    load()
    allPermissions.value = pageResult(await permissionsApi.list({ page: 1, size: 1000 })).records
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}

async function remove(record) {
  try {
    await permissionsApi.remove(idOf(record.id))
    message.success('权限已删除')
    load()
    allPermissions.value = pageResult(await permissionsApi.list({ page: 1, size: 1000 })).records
  } catch (error) {
    message.error(errorText(error))
  }
}

async function changeStatus(record, checked) {
  statusLoadingId.value = idOf(record.id)
  try {
    await permissionsApi.updateStatus(idOf(record.id), checked ? 1 : 0)
    record.status = checked ? 1 : 0
    message.success('状态已更新')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusLoadingId.value = ''
  }
}

function typeLabel(type) {
  return typeOptions.find((item) => item.value === type)?.label || type || '-'
}

function resetFilters() {
  filters.keyword = ''
  filters.type = undefined
  search()
}
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input v-model:value="filters.keyword" allow-clear placeholder="搜索名称、路径或权限标识" style="width: 250px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input><a-select v-model:value="filters.type" allow-clear placeholder="全部类型" style="width: 140px" :options="typeOptions" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-space><a-button @click="load()"><ReloadOutlined /></a-button><a-button v-permission="['system:permission:add', 'system:permission:create', 'system:menu:add']" type="primary" @click="openCreate"><PlusOutlined />新增权限</a-button></a-space></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="treeRecords" :loading="loading" :pagination="pagination" :scroll="{ x: 1250 }" default-expand-all-rows @change="changeTable">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">{{ record.name || '-' }}</template>
        <template v-else-if="column.key === 'type'"><a-tag :color="record.type === 'DIRECTORY' ? 'purple' : record.type === 'BUTTON' ? 'orange' : 'blue'">{{ typeLabel(record.type) }}</a-tag></template>
        <template v-else-if="column.key === 'path'"><code v-if="record.path" class="table-code">{{ record.path }}</code><span v-else>-</span></template>
        <template v-else-if="column.key === 'permission'"><code v-if="record.permission || record.perms" class="table-code">{{ record.permission || record.perms }}</code><span v-else>-</span></template>
        <template v-else-if="column.key === 'sort'">{{ record.sort ?? record.orderNum ?? 0 }}</template>
        <template v-else-if="column.key === 'status'"><a-switch v-permission="['system:permission:status', 'system:menu:status']" :checked="isEnabled(record.status)" :loading="statusLoadingId === idOf(record.id)" checked-children="启用" un-checked-children="禁用" @change="(checked) => changeStatus(record, checked)" /></template>
        <template v-else-if="column.key === 'actions'"><a-space><a-button v-permission="['system:permission:edit', 'system:permission:update', 'system:menu:edit']" type="link" size="small" @click="openEdit(record)">编辑</a-button><a-popconfirm title="确定删除该权限吗？子级可能同时受影响" ok-text="删除" cancel-text="取消" @confirm="remove(record)"><a-button v-permission="['system:permission:delete', 'system:permission:remove', 'system:menu:delete']" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template>
      </template>
    </a-table></a-card>
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑菜单权限' : '新增菜单权限'" :confirm-loading="saving" destroy-on-close @ok="save"><a-form ref="formRef" :model="form" :rules="rules" layout="vertical"><div class="modal-form-grid"><a-form-item label="权限类型" name="type"><a-select v-model:value="form.type" :options="typeOptions" /></a-form-item><a-form-item label="上级菜单" name="parentId"><a-tree-select v-model:value="form.parentId" allow-clear tree-default-expand-all :tree-data="parentOptions" :field-names="{ label: 'name', value: 'id', children: 'children' }" placeholder="不选择则为顶级" /></a-form-item></div><a-form-item label="名称" name="name"><a-input v-model:value="form.name" /></a-form-item><a-form-item v-if="form.type !== 'BUTTON'" label="路由路径" name="path"><a-input v-model:value="form.path" placeholder="例如 /system/admins" /></a-form-item><div class="modal-form-grid"><a-form-item label="权限标识" name="permission"><a-input v-model:value="form.permission" placeholder="例如 system:admin:list" /></a-form-item><a-form-item v-if="form.type !== 'BUTTON'" label="图标" name="icon"><a-input v-model:value="form.icon" placeholder="例如 user" /></a-form-item></div><div class="modal-form-grid"><a-form-item label="排序" name="sort"><a-input-number v-model:value="form.sort" :min="0" :max="9999" style="width: 100%" /></a-form-item><a-form-item label="状态" name="status"><a-radio-group v-model:value="form.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]" /></a-form-item></div></a-form></a-modal>
  </div>
</template>
