<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { adminResourceDiskTypeApi } from '../../api/resource'
import { useTable } from '../../composables/useTable'
import { errorText, idOf, isEnabled } from '../../utils'

const keyword = ref('')
const { loading, records, pagination, load, changeTable, search } = useTable(adminResourceDiskTypeApi.list, () => ({ keyword: keyword.value.trim() || undefined }))
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref('')
const formRef = ref()
const form = reactive({ code: '', name: '', allowedHosts: '', sortOrder: 0, status: 1 })
const rules = {
  code: [{ required: true, whitespace: true, message: '请输入网盘编码' }, { pattern: /^[A-Z0-9_]{2,32}$/, message: '编码须为2-32位大写字母、数字或下划线' }],
  name: [{ required: true, whitespace: true, message: '请输入网盘名称' }, { max: 50, message: '网盘名称不能超过50个字符' }],
  allowedHosts: [{ required: true, whitespace: true, message: '请输入域名白名单' }, { max: 2000, message: '域名白名单不能超过2000个字符' }],
  sortOrder: [{ type: 'number', message: '请输入排序值' }],
}
const columns = [{ title: '网盘编码', dataIndex: 'code', key: 'code', width: 150 }, { title: '网盘名称', dataIndex: 'name', key: 'name', width: 160 }, { title: '域名白名单', dataIndex: 'allowedHosts', key: 'allowedHosts', ellipsis: true }, { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 90 }, { title: '状态', key: 'status', width: 100 }, { title: '操作', key: 'actions', width: 160, fixed: 'right' }]

onMounted(() => load())
function openCreate() { editingId.value = ''; Object.assign(form, { code: '', name: '', allowedHosts: '', sortOrder: 0, status: 1 }); modalOpen.value = true }
function openEdit(record) {
  editingId.value = idOf(record.id)
  Object.assign(form, { code: record.code || '', name: record.name || '', allowedHosts: record.allowedHosts || '', sortOrder: Number(record.sortOrder ?? 0), status: isEnabled(record.status) ? 1 : 0 })
  modalOpen.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = { code: form.code.trim(), name: form.name.trim(), allowedHosts: form.allowedHosts.trim(), sortOrder: form.sortOrder, status: form.status }
    if (editingId.value) await adminResourceDiskTypeApi.update(editingId.value, data)
    else await adminResourceDiskTypeApi.create(data)
    message.success(editingId.value ? '网盘类型已更新' : '网盘类型已创建')
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
    await adminResourceDiskTypeApi.remove(idOf(record.id))
    message.success('网盘类型已删除')
    load(records.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error) {
    message.error(errorText(error))
  }
}
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input v-model:value="keyword" allow-clear placeholder="搜索网盘类型" style="width: 240px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input><a-button type="primary" @click="search">查询</a-button></a-space><a-space><a-button @click="load()"><ReloadOutlined /></a-button><a-button v-permission="'resource:disk-type:create'" type="primary" @click="openCreate"><PlusOutlined />新增网盘</a-button></a-space></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1050 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'code'"><code class="table-code">{{ record.code }}</code></template><template v-else-if="column.key === 'status'"><a-tag :color="isEnabled(record.status) ? 'success' : 'default'">{{ isEnabled(record.status) ? '启用' : '停用' }}</a-tag></template><template v-else-if="column.key === 'actions'"><a-space><a-button v-permission="'resource:disk-type:update'" type="link" size="small" @click="openEdit(record)">编辑</a-button><a-popconfirm title="确定删除该网盘类型吗？" description="已被资源链接使用的网盘类型无法删除。" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="remove(record)"><a-button v-permission="'resource:disk-type:delete'" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template><template v-else>{{ record[column.dataIndex] ?? '-' }}</template></template></a-table></a-card>
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑网盘类型' : '新增网盘类型'" :confirm-loading="saving" destroy-on-close @ok="save"><a-form ref="formRef" :model="form" :rules="rules" layout="vertical"><div class="modal-form-grid"><a-form-item label="网盘编码" name="code"><a-input v-model:value="form.code" :maxlength="32" placeholder="如 BAIDU" /></a-form-item><a-form-item label="网盘名称" name="name"><a-input v-model:value="form.name" :maxlength="50" /></a-form-item></div><a-form-item label="域名白名单" name="allowedHosts" extra="多个域名使用英文逗号分隔，不含协议和路径"><a-textarea v-model:value="form.allowedHosts" :rows="4" :maxlength="2000" show-count placeholder="pan.example.com,share.example.com" /></a-form-item><div class="modal-form-grid"><a-form-item label="排序" name="sortOrder"><a-input-number v-model:value="form.sortOrder" style="width: 100%" /></a-form-item><a-form-item label="状态" name="status"><a-radio-group v-model:value="form.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]" /></a-form-item></div></a-form></a-modal>
  </div>
</template>
