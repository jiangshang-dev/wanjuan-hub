<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { adminsApi, rolesApi } from '../../api'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf, isEnabled, pageResult } from '../../utils'

const filters = reactive({ keyword: '', status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminsApi.list, () => ({ keyword: filters.keyword.trim() || undefined, status: filters.status }))
const roles = ref([])
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref('')
const formRef = ref()
const form = reactive({ username: '', password: '', nickname: '', email: '', phone: '', status: 1 })
const roleModalOpen = ref(false)
const roleSaving = ref(false)
const roleAdmin = ref(null)
const selectedRoleIds = ref([])
const statusLoadingId = ref('')
const rules = {
  username: [{ required: true, whitespace: true, message: '请输入账号' }, { pattern: /^[\w-]{3,32}$/, message: '账号为3-32位字母、数字、下划线或短横线' }],
  password: [{ required: true, message: '请输入初始密码' }, { pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{8,32}$/, message: '密码需为8-32位且包含字母和数字' }],
  nickname: [{ max: 30, message: '昵称不能超过30个字符' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址' }],
  phone: [{ pattern: /^1\d{10}$/, message: '请输入正确的11位手机号' }],
}
const columns = [
  { title: '账号', dataIndex: 'username', key: 'username', width: 150 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 140 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 200 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 140 },
  { title: '角色', key: 'roles', width: 180 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 230, fixed: 'right' },
]

onMounted(async () => {
  load()
  try {
    roles.value = pageResult(await rolesApi.list({ page: 1, size: 1000 })).records
  } catch (error) {
    message.error(errorText(error))
  }
})

function openCreate() {
  editingId.value = ''
  Object.assign(form, { username: '', password: '', nickname: '', email: '', phone: '', status: 1 })
  modalOpen.value = true
  formRef.value?.clearValidate()
}

function openEdit(record) {
  editingId.value = idOf(record.id)
  Object.assign(form, { username: record.username || record.account || '', password: '', nickname: record.nickname || '', email: record.email || '', phone: record.phone || '', status: isEnabled(record.status) ? 1 : 0 })
  modalOpen.value = true
  formRef.value?.clearValidate()
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = { username: form.username.trim(), nickname: form.nickname.trim() || null, email: form.email.trim() || null, phone: form.phone.trim() || null, status: form.status }
    if (editingId.value) await adminsApi.update(editingId.value, data)
    else await adminsApi.create({ ...data, password: form.password })
    message.success(editingId.value ? '管理员已更新' : '管理员已创建')
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
    await adminsApi.remove(idOf(record.id))
    message.success('管理员已删除')
    load(records.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error) {
    message.error(errorText(error))
  }
}

async function changeStatus(record, checked) {
  statusLoadingId.value = idOf(record.id)
  try {
    await adminsApi.updateStatus(idOf(record.id), checked ? 1 : 0)
    record.status = checked ? 1 : 0
    message.success('状态已更新')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusLoadingId.value = ''
  }
}

function openRoles(record) {
  roleAdmin.value = record
  selectedRoleIds.value = (record.roleIds || record.roles || []).map((role) => idOf(typeof role === 'object' ? role.id : role)).filter(Boolean)
  roleModalOpen.value = true
}

async function saveRoles() {
  roleSaving.value = true
  try {
    await adminsApi.assignRoles(idOf(roleAdmin.value.id), selectedRoleIds.value)
    message.success('角色分配已保存')
    roleModalOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    roleSaving.value = false
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
    <div class="toolbar"><a-space wrap><a-input v-model:value="filters.keyword" allow-clear placeholder="搜索账号、昵称或邮箱" style="width: 240px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input><a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 120px" :options="[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]" /><a-button type="primary" @click="search">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space><a-space><a-button @click="load()"><ReloadOutlined /></a-button><a-button v-permission="['system:admin:add', 'system:admin:create', 'system:admins:create']" type="primary" @click="openCreate"><PlusOutlined />新增管理员</a-button></a-space></div>
    <a-card :bordered="false" class="table-card">
      <a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1300 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'roles'"><a-space v-if="record.roles?.length" wrap><a-tag v-for="role in record.roles" :key="role.id || role">{{ typeof role === 'object' ? role.name : role }}</a-tag></a-space><span v-else>-</span></template>
          <template v-else-if="column.key === 'status'"><a-switch v-permission="['system:admin:status', 'system:admin:update']" :checked="isEnabled(record.status)" :loading="statusLoadingId === idOf(record.id)" checked-children="启用" un-checked-children="禁用" @change="(checked) => changeStatus(record, checked)" /></template>
          <template v-else-if="column.key === 'createdAt'">{{ formatDate(record.createdAt || record.createTime) }}</template>
          <template v-else-if="column.key === 'actions'"><a-space><a-button v-permission="['system:admin:roles', 'system:admin:role']" type="link" size="small" @click="openRoles(record)">分配角色</a-button><a-button v-permission="['system:admin:edit', 'system:admin:update']" type="link" size="small" @click="openEdit(record)">编辑</a-button><a-popconfirm title="确定删除该管理员吗？" ok-text="删除" cancel-text="取消" @confirm="remove(record)"><a-button v-permission="['system:admin:delete', 'system:admin:remove']" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑管理员' : '新增管理员'" :confirm-loading="saving" destroy-on-close @ok="save">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="账号" name="username"><a-input v-model:value="form.username" :disabled="Boolean(editingId)" /></a-form-item>
        <a-form-item v-if="!editingId" label="初始密码" name="password"><a-input-password v-model:value="form.password" autocomplete="new-password" /></a-form-item>
        <a-form-item label="昵称" name="nickname"><a-input v-model:value="form.nickname" /></a-form-item>
        <div class="modal-form-grid"><a-form-item label="邮箱" name="email"><a-input v-model:value="form.email" /></a-form-item><a-form-item label="手机号" name="phone"><a-input v-model:value="form.phone" :maxlength="11" /></a-form-item></div>
        <a-form-item label="状态" name="status"><a-radio-group v-model:value="form.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]" /></a-form-item>
      </a-form>
    </a-modal>
    <a-modal v-model:open="roleModalOpen" :title="`分配角色：${roleAdmin?.nickname || roleAdmin?.username || ''}`" :confirm-loading="roleSaving" @ok="saveRoles">
      <a-alert message="保存后将以后端分配结果为准" type="info" show-icon style="margin-bottom: 18px" />
      <a-checkbox-group v-model:value="selectedRoleIds" class="role-checkbox-group"><a-checkbox v-for="role in roles" :key="idOf(role.id)" :value="idOf(role.id)" :disabled="!isEnabled(role.status)">{{ role.name || role.roleName }}<span class="option-code">{{ role.code || role.roleKey }}</span></a-checkbox></a-checkbox-group>
      <a-empty v-if="!roles.length" description="暂无可分配角色" />
    </a-modal>
  </div>
</template>
