<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { adminBlogCategoryApi } from '../../api'
import { useTable } from '../../composables/useTable'
import { errorText, idOf, isEnabled } from '../../utils'

const keyword = ref('')
const { loading, records, pagination, load, changeTable, search } = useTable(adminBlogCategoryApi.list, () => ({ keyword: keyword.value.trim() || undefined }))
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref('')
const formRef = ref()
const form = reactive({ name: '', description: '', sortOrder: 0, status: 1 })
const rules = { name: [{ required: true, whitespace: true, message: '请输入分类名称' }, { max: 50, message: '分类名称不能超过50个字符' }], description: [{ max: 255, message: '描述不能超过255个字符' }], sortOrder: [{ type: 'number', min: 0, max: 9999, message: '排序值为0-9999' }] }
const columns = [{ title: '分类名称', dataIndex: 'name', key: 'name', width: 200 }, { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true }, { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 100 }, { title: '状态', key: 'status', width: 110 }, { title: '操作', key: 'actions', width: 160, fixed: 'right' }]

onMounted(() => load())
function openCreate() {
  editingId.value = ''
  Object.assign(form, { name: '', description: '', sortOrder: 0, status: 1 })
  modalOpen.value = true
}
function openEdit(record) {
  editingId.value = idOf(record.id)
  Object.assign(form, { name: record.name || '', description: record.description || '', sortOrder: Number(record.sortOrder ?? record.sort ?? 0), status: isEnabled(record.status) ? 1 : 0 })
  modalOpen.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = { name: form.name.trim(), description: form.description.trim() || null, sortOrder: form.sortOrder, status: form.status }
    if (editingId.value) await adminBlogCategoryApi.update(editingId.value, data)
    else await adminBlogCategoryApi.create(data)
    message.success(editingId.value ? '分类已更新' : '分类已创建')
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
    await adminBlogCategoryApi.remove(idOf(record.id))
    message.success('分类已删除')
    load(records.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error) {
    message.error(errorText(error))
  }
}
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input v-model:value="keyword" allow-clear placeholder="搜索分类名称" style="width: 240px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input><a-button type="primary" @click="search">查询</a-button></a-space><a-space><a-button @click="load()"><ReloadOutlined /></a-button><a-button v-permission="'blog:category:create'" type="primary" @click="openCreate"><PlusOutlined />新增分类</a-button></a-space></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 850 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'status'"><a-tag :color="isEnabled(record.status) ? 'success' : 'default'">{{ isEnabled(record.status) ? '启用' : '停用' }}</a-tag></template><template v-else-if="column.key === 'sortOrder'">{{ record.sortOrder ?? record.sort ?? 0 }}</template><template v-else-if="column.key === 'actions'"><a-space><a-button v-permission="'blog:category:update'" type="link" size="small" @click="openEdit(record)">编辑</a-button><a-popconfirm title="确定删除该分类吗？" description="已被博客使用的分类可能无法删除。" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="remove(record)"><a-button v-permission="'blog:category:delete'" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template><template v-else>{{ record[column.dataIndex] || '-' }}</template></template></a-table></a-card>
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑分类' : '新增分类'" :confirm-loading="saving" destroy-on-close @ok="save"><a-form ref="formRef" :model="form" :rules="rules" layout="vertical"><a-form-item label="分类名称" name="name"><a-input v-model:value="form.name" :maxlength="50" /></a-form-item><a-form-item label="分类描述" name="description"><a-textarea v-model:value="form.description" :rows="4" :maxlength="255" show-count /></a-form-item><div class="modal-form-grid"><a-form-item label="排序" name="sortOrder"><a-input-number v-model:value="form.sortOrder" :min="0" :max="9999" style="width: 100%" /></a-form-item><a-form-item label="状态" name="status"><a-radio-group v-model:value="form.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]" /></a-form-item></div></a-form></a-modal>
  </div>
</template>
