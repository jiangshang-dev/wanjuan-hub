<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { adminResourceTagApi } from '../../api/resource'
import { useTable } from '../../composables/useTable'
import { errorText, idOf } from '../../utils'

const keyword = ref('')
const { loading, records, pagination, load, changeTable, search } = useTable(adminResourceTagApi.list, () => ({ keyword: keyword.value.trim() || undefined }))
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref('')
const formRef = ref()
const name = ref('')
const rules = { name: [{ required: true, whitespace: true, message: '请输入标签名称' }, { max: 50, message: '标签名称不能超过50个字符' }] }
const columns = [{ title: '标签 ID', dataIndex: 'id', key: 'id', width: 220 }, { title: '标签名称', dataIndex: 'name', key: 'name' }, { title: '操作', key: 'actions', width: 160, fixed: 'right' }]

onMounted(() => load())
function openCreate() { editingId.value = ''; name.value = ''; modalOpen.value = true }
function openEdit(record) { editingId.value = idOf(record.id); name.value = record.name || ''; modalOpen.value = true }
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = { name: name.value.trim() }
    if (editingId.value) await adminResourceTagApi.update(editingId.value, data)
    else await adminResourceTagApi.create(data)
    message.success(editingId.value ? '资源标签已更新' : '资源标签已创建')
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
    await adminResourceTagApi.remove(idOf(record.id))
    message.success('资源标签已删除')
    load(records.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error) {
    message.error(errorText(error))
  }
}
</script>

<template>
  <div class="management-page">
    <div class="toolbar"><a-space wrap><a-input v-model:value="keyword" allow-clear placeholder="搜索资源标签" style="width: 240px" @press-enter="search"><template #prefix><SearchOutlined /></template></a-input><a-button type="primary" @click="search">查询</a-button></a-space><a-space><a-button @click="load()"><ReloadOutlined /></a-button><a-button v-permission="'resource:tag:create'" type="primary" @click="openCreate"><PlusOutlined />新增标签</a-button></a-space></div>
    <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 700 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'id'"><code class="table-code">{{ record.id }}</code></template><template v-else-if="column.key === 'actions'"><a-space><a-button v-permission="'resource:tag:update'" type="link" size="small" @click="openEdit(record)">编辑</a-button><a-popconfirm title="确定删除该标签吗？" description="已被资源使用的标签无法删除。" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="remove(record)"><a-button v-permission="'resource:tag:delete'" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template><template v-else>{{ record[column.dataIndex] ?? '-' }}</template></template></a-table></a-card>
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑资源标签' : '新增资源标签'" :confirm-loading="saving" destroy-on-close @ok="save"><a-form ref="formRef" :model="{ name }" :rules="rules" layout="vertical"><a-form-item label="标签名称" name="name"><a-input v-model:value="name" :maxlength="50" /></a-form-item></a-form></a-modal>
  </div>
</template>
