<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { permissionsApi, rolesApi } from '../../api'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf, isEnabled, pageResult } from '../../utils'

const filters = reactive({ keyword: '', status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(rolesApi.list, () => ({ keyword: filters.keyword.trim() || undefined, status: filters.status }))
const permissions = ref([])
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref('')
const formRef = ref()
const form = reactive({ name: '', code: '', description: '', sort: 0, status: 1 })
const permissionModalOpen = ref(false)
const permissionSaving = ref(false)
const currentRole = ref(null)
const checkedPermissionIds = ref([])
const halfCheckedPermissionIds = ref([])
const statusLoadingId = ref('')
const rules = {
  name: [{ required: true, whitespace: true, message: '请输入角色名称' }, { max: 50, message: '角色名称不能超过50个字符' }],
  code: [{ required: true, whitespace: true, message: '请输入角色编码' }, { pattern: /^[A-Za-z][\w:-]{1,49}$/, message: '编码以字母开头，可含字母、数字、下划线、冒号和短横线' }],
  sort: [{ type: 'number', min: 0, max: 9999, message: '排序值为0-9999' }],
}
const columns = [
  { title: '角色名称', dataIndex: 'name', key: 'name', width: 180 },
  { title: '角色编码', dataIndex: 'code', key: 'code', width: 180 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 110 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 220, fixed: 'right' },
]

function normalizeNodes(nodes) {
  return nodes.map((node) => ({ ...node, id: idOf(node.id), name: node.name || node.permissionName || node.title, children: normalizeNodes(node.children || []) }))
}
const permissionTree = computed(() => {
  const nodes = normalizeNodes(permissions.value)
  if (nodes.some((node) => node.children.length)) return nodes
  const map = new Map(nodes.map((node) => [node.id, node]))
  const roots = []
  nodes.forEach((node) => {
    const parentId = idOf(node.parentId)
    if (parentId && map.has(parentId) && parentId !== node.id) map.get(parentId).children.push(node)
    else roots.push(node)
  })
  return roots
})

onMounted(async () => {
  load()
  try {
    permissions.value = pageResult(await permissionsApi.list({ page: 1, size: 1000 })).records
  } catch (error) {
    message.error(errorText(error))
  }
})

function openCreate() {
  editingId.value = ''
  Object.assign(form, { name: '', code: '', description: '', sort: 0, status: 1 })
  modalOpen.value = true
  formRef.value?.clearValidate()
}

function openEdit(record) {
  editingId.value = idOf(record.id)
  Object.assign(form, { name: record.name || record.roleName || '', code: record.code || record.roleKey || '', description: record.description || '', sort: Number(record.sort ?? record.orderNum ?? 0), status: isEnabled(record.status) ? 1 : 0 })
  modalOpen.value = true
  formRef.value?.clearValidate()
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = { name: form.name.trim(), code: form.code.trim(), description: form.description.trim() || null, sort: form.sort, status: form.status }
    if (editingId.value) await rolesApi.update(editingId.value, data)
    else await rolesApi.create(data)
    message.success(editingId.value ? '角色已更新' : '角色已创建')
    modalOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}

async function remove(record) {
  try {
    await rolesApi.remove(idOf(record.id))
    message.success('角色已删除')
    load(records.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error) {
    message.error(errorText(error))
  }
}

async function changeStatus(record, checked) {
  statusLoadingId.value = idOf(record.id)
  try {
    await rolesApi.updateStatus(idOf(record.id), checked ? 1 : 0)
    record.status = checked ? 1 : 0
    message.success('状态已更新')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusLoadingId.value = ''
  }
}

function openPermissions(record) {
  currentRole.value = record
  checkedPermissionIds.value = (record.permissionIds || record.permissions || []).map((item) => idOf(typeof item === 'object' ? item.id : item)).filter(Boolean)
  halfCheckedPermissionIds.value = []
  permissionModalOpen.value = true
}

async function savePermissions() {
  permissionSaving.value = true
  try {
    await rolesApi.assignPermissions(idOf(currentRole.value.id), [...new Set([...checkedPermissionIds.value, ...halfCheckedPermissionIds.value])])
    message.success('权限分配已保存')
    permissionModalOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    permissionSaving.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.status = undefined
  search()
}
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input v-model:value="filters.keyword" allow-clear placeholder="搜索角色名称或编码" style="width: 230px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input><a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 120px" :options="[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-space><a-button @click="load()"><ReloadOutlined /></a-button><a-button v-permission="['system:role:add', 'system:role:create']" type="primary" @click="openCreate"><PlusOutlined />新增角色</a-button></a-space></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1200 }" @change="changeTable">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name' || column.key === 'code'">{{ record[column.dataIndex] || record[column.key === 'name' ? 'roleName' : 'roleKey'] || '-' }}</template>
        <template v-else-if="column.key === 'status'"><a-switch v-permission="['system:role:status']" :checked="isEnabled(record.status)" :loading="statusLoadingId === idOf(record.id)" checked-children="启用" un-checked-children="禁用" @change="(checked) => changeStatus(record, checked)" /></template>
        <template v-else-if="column.key === 'createdAt'">{{ formatDate(record.createdAt || record.createTime) }}</template>
        <template v-else-if="column.key === 'actions'"><a-space><a-button v-permission="['system:role:permissions', 'system:role:permission']" type="link" size="small" @click="openPermissions(record)">分配权限</a-button><a-button v-permission="['system:role:edit', 'system:role:update']" type="link" size="small" @click="openEdit(record)">编辑</a-button><a-popconfirm title="确定删除该角色吗？" ok-text="删除" cancel-text="取消" @confirm="remove(record)"><a-button v-permission="['system:role:delete', 'system:role:remove']" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template>
      </template>
    </a-table></a-card>
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑角色' : '新增角色'" :confirm-loading="saving" destroy-on-close @ok="save"><a-form ref="formRef" :model="form" :rules="rules" layout="vertical"><a-form-item label="角色名称" name="name"><a-input v-model:value="form.name" /></a-form-item><a-form-item label="角色编码" name="code"><a-input v-model:value="form.code" :disabled="Boolean(editingId)" placeholder="例如 CONTENT_MANAGER" /></a-form-item><a-form-item label="角色描述" name="description"><a-textarea v-model:value="form.description" :rows="3" :maxlength="200" show-count /></a-form-item><div class="modal-form-grid"><a-form-item label="排序" name="sort"><a-input-number v-model:value="form.sort" :min="0" :max="9999" style="width: 100%" /></a-form-item><a-form-item label="状态" name="status"><a-radio-group v-model:value="form.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]" /></a-form-item></div></a-form></a-modal>
    <a-modal v-model:open="permissionModalOpen" :title="`分配权限：${currentRole?.name || currentRole?.roleName || ''}`" :confirm-loading="permissionSaving" width="560px" @ok="savePermissions"><a-alert type="info" show-icon message="勾选目录会自动关联其子权限，保存时包含半选父级" style="margin-bottom: 16px" /><div class="permission-tree"><a-tree v-if="permissionTree.length" checkable :tree-data="permissionTree" :checked-keys="checkedPermissionIds" :field-names="{ title: 'name', key: 'id', children: 'children' }" default-expand-all @check="(keys, info) => { checkedPermissionIds = keys; halfCheckedPermissionIds = info.halfCheckedKeys }" /><a-empty v-else description="暂无可分配权限" /></div></a-modal>
  </div>
</template>
