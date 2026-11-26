<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons-vue'
import { adminMembershipApi } from '../../api/s6'
import { useTable } from '../../composables/useTable'
import { useAuthStore } from '../../stores/auth'
import { errorText, formatDate, idOf, isEnabled } from '../../utils'
import { money } from '../../utils/trade'

const auth = useAuthStore()
const filters = reactive({ status: undefined })
const { loading, records, pagination, load, changeTable, search } = useTable(adminMembershipApi.list, () => ({ status: filters.status }))
const planModal = ref(false)
const planSaving = ref(false)
const statusSavingId = ref('')
const editingId = ref('')
const planFormRef = ref()
const planForm = reactive({ name: '', description: '', durationDays: 30, price: 0.01, dailyResourceLimit: 1, status: 1, sortOrder: 0 })
const planRules = {
  name: [{ required: true, whitespace: true, message: '请输入套餐名称' }, { max: 100, message: '套餐名称不能超过100个字符' }],
  description: [{ max: 500, message: '套餐说明不能超过500个字符' }],
  durationDays: [{ required: true, type: 'number', min: 1, max: 3650, message: '有效天数须为1至3650天' }],
  price: [{ required: true, type: 'number', min: 0.01, message: '套餐价格不能小于0.01元' }],
  dailyResourceLimit: [{ required: true, type: 'number', min: 1, max: 10000, message: '每日资源额度须为1至10000' }],
  status: [{ required: true, message: '请选择套餐状态' }],
  sortOrder: [{ type: 'number', min: 0, message: '排序值不能小于0' }],
}
const columns = [
  { title: '套餐 ID', dataIndex: 'id', key: 'id', width: 180 },
  { title: '套餐名称', dataIndex: 'name', key: 'name', width: 170 },
  { title: '价格', key: 'price', width: 120 },
  { title: '有效天数', key: 'durationDays', width: 110 },
  { title: '每日资源额度', key: 'dailyResourceLimit', width: 140 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 90 },
  { title: '状态', key: 'status', width: 90 },
  { title: '套餐说明', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '更新时间', key: 'updateTime', width: 180 },
  { title: '操作', key: 'actions', width: 160, fixed: 'right' },
]
const commissionLoading = ref(false)
const commissionSaving = ref(false)
const commissionUpdateTime = ref('')
const commissionForm = reactive({ pointsUnitPrice: 0, memberFixedAmount: 0 })
const commissionRules = {
  pointsUnitPrice: [{ required: true, type: 'number', min: 0, message: '积分折算单价不能小于0' }],
  memberFixedAmount: [{ required: true, type: 'number', min: 0, message: '会员固定返佣不能小于0' }],
}

function openPlan(record) {
  editingId.value = record ? idOf(record.id) : ''
  Object.assign(planForm, record ? {
    name: record.name || '',
    description: record.description || '',
    durationDays: Number(record.durationDays),
    price: Number(record.price),
    dailyResourceLimit: Number(record.dailyResourceLimit),
    status: isEnabled(record.status) ? 1 : 0,
    sortOrder: Number(record.sortOrder || 0),
  } : { name: '', description: '', durationDays: 30, price: 0.01, dailyResourceLimit: 1, status: 1, sortOrder: 0 })
  planModal.value = true
}

function planPayload(record) {
  return {
    name: record.name.trim(),
    description: record.description.trim(),
    durationDays: Number(record.durationDays),
    price: Number(record.price),
    dailyResourceLimit: Number(record.dailyResourceLimit),
    status: Number(record.status),
    sortOrder: Number(record.sortOrder || 0),
  }
}

async function savePlan() {
  await planFormRef.value.validate()
  planSaving.value = true
  try {
    const data = planPayload(planForm)
    if (editingId.value) await adminMembershipApi.update(editingId.value, data)
    else await adminMembershipApi.create(data)
    message.success(editingId.value ? '会员套餐已更新' : '会员套餐已创建')
    planModal.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    planSaving.value = false
  }
}

async function togglePlan(record) {
  statusSavingId.value = idOf(record.id)
  try {
    await adminMembershipApi.update(statusSavingId.value, planPayload({
      ...record,
      description: record.description || '',
      status: isEnabled(record.status) ? 0 : 1,
    }))
    message.success(isEnabled(record.status) ? '会员套餐已停用' : '会员套餐已启用')
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    statusSavingId.value = ''
  }
}

async function loadCommission() {
  commissionLoading.value = true
  try {
    const data = await adminMembershipApi.commissionConfig()
    Object.assign(commissionForm, {
      pointsUnitPrice: Number(data?.pointsUnitPrice || 0),
      memberFixedAmount: Number(data?.memberFixedAmount || 0),
    })
    commissionUpdateTime.value = data?.updateTime || ''
  } catch (error) {
    message.error(errorText(error))
  } finally {
    commissionLoading.value = false
  }
}

async function saveCommission() {
  commissionSaving.value = true
  try {
    await adminMembershipApi.updateCommissionConfig({ ...commissionForm })
    message.success('资源返佣配置已保存')
    loadCommission()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    commissionSaving.value = false
  }
}

onMounted(() => {
  load()
  if (auth.hasPermission('membership:commission:detail')) loadCommission()
})
</script>

<template>
  <div class="management-page">
    <a-alert message="套餐价格单位为人民币元；每日资源额度表示会员每个自然日可领取的会员资源数量。" type="info" show-icon class="page-alert" />

    <a-card v-if="auth.hasPermission('membership:commission:detail')" :bordered="false" class="panel-card commission-card" title="资源返佣配置">
      <a-spin :spinning="commissionLoading">
        <a-form :model="commissionForm" :rules="commissionRules" layout="vertical" class="commission-form" @finish="saveCommission">
          <div class="modal-form-grid">
            <a-form-item label="积分折算单价（元 / 积分）" name="pointsUnitPrice" extra="用户每扣除1积分，作者获得的返佣金额。">
              <a-input-number v-model:value="commissionForm.pointsUnitPrice" :min="0" :precision="4" :step="0.0001" style="width: 100%" />
            </a-form-item>
            <a-form-item label="会员固定返佣（元 / 次）" name="memberFixedAmount" extra="会员资源首次有效领取时，作者获得的固定返佣。">
              <a-input-number v-model:value="commissionForm.memberFixedAmount" :min="0" :precision="2" :step="0.01" style="width: 100%" />
            </a-form-item>
          </div>
          <a-form-item>
            <a-space wrap>
              <a-button v-permission="'membership:commission:update'" type="primary" html-type="submit" :loading="commissionSaving"><SaveOutlined />保存返佣配置</a-button>
              <a-button :loading="commissionLoading" @click="loadCommission"><ReloadOutlined />重新加载</a-button>
              <span v-if="commissionUpdateTime" class="update-time">最近更新：{{ formatDate(commissionUpdateTime) }}</span>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>

    <div class="toolbar">
      <a-space wrap>
        <a-select v-model:value="filters.status" allow-clear placeholder="全部套餐状态" style="width: 150px" :options="[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]" />
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="filters.status = undefined; search()">重置</a-button>
      </a-space>
      <a-space>
        <a-button @click="load()"><ReloadOutlined /></a-button>
        <a-button v-permission="'membership:plan:create'" type="primary" @click="openPlan()"><PlusOutlined />新增套餐</a-button>
      </a-space>
    </div>

    <a-card :bordered="false" class="table-card">
      <a-table row-key="id" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1500 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id'"><span class="table-code">{{ idOf(record.id) }}</span></template>
          <template v-else-if="column.key === 'price'"><strong>{{ money(record.price) }}</strong></template>
          <template v-else-if="column.key === 'durationDays'">{{ record.durationDays }} 天</template>
          <template v-else-if="column.key === 'dailyResourceLimit'">{{ record.dailyResourceLimit }} 份 / 天</template>
          <template v-else-if="column.key === 'status'"><a-tag :color="isEnabled(record.status) ? 'success' : 'default'">{{ isEnabled(record.status) ? '启用' : '停用' }}</a-tag></template>
          <template v-else-if="column.key === 'updateTime'">{{ formatDate(record.updateTime || record.createTime) }}</template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button v-permission="'membership:plan:update'" type="link" size="small" @click="openPlan(record)">编辑</a-button>
              <a-popconfirm :title="isEnabled(record.status) ? '停用后前台将不可购买该套餐，确定停用吗？' : '确定启用该套餐吗？'" ok-text="确定" cancel-text="取消" @confirm="togglePlan(record)">
                <a-button v-permission="'membership:plan:update'" type="link" size="small" :loading="statusSavingId === idOf(record.id)">{{ isEnabled(record.status) ? '停用' : '启用' }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
          <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="planModal" :title="editingId ? '编辑会员套餐' : '新增会员套餐'" ok-text="保存" cancel-text="取消" :confirm-loading="planSaving" destroy-on-close @ok="savePlan">
      <a-form ref="planFormRef" :model="planForm" :rules="planRules" layout="vertical">
        <div class="modal-form-grid">
          <a-form-item label="套餐名称" name="name"><a-input v-model:value="planForm.name" :maxlength="100" placeholder="如：月度会员" /></a-form-item>
          <a-form-item label="套餐价格（元）" name="price"><a-input-number v-model:value="planForm.price" :min="0.01" :precision="2" :step="0.01" style="width: 100%" /></a-form-item>
          <a-form-item label="有效天数" name="durationDays"><a-input-number v-model:value="planForm.durationDays" :min="1" :max="3650" :precision="0" style="width: 100%" /></a-form-item>
          <a-form-item label="每日资源额度" name="dailyResourceLimit"><a-input-number v-model:value="planForm.dailyResourceLimit" :min="1" :max="10000" :precision="0" style="width: 100%" /></a-form-item>
          <a-form-item label="排序值" name="sortOrder" extra="数值越小越靠前"><a-input-number v-model:value="planForm.sortOrder" :min="0" :precision="0" style="width: 100%" /></a-form-item>
          <a-form-item label="状态" name="status"><a-radio-group v-model:value="planForm.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]" /></a-form-item>
        </div>
        <a-form-item label="套餐说明" name="description"><a-textarea v-model:value="planForm.description" :rows="4" :maxlength="500" show-count placeholder="说明套餐权益与使用规则" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.commission-card { margin-bottom: 18px; }
.commission-form { max-width: 900px; }
.update-time { color: var(--muted); font-size: 13px; }
</style>
