<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { adminPointsApi } from '../../api/s6'
import { useTable } from '../../composables/useTable'
import { errorText, formatDate, idOf, isEnabled } from '../../utils'

const route = useRoute()
const router = useRouter()
const tabs = [{ key: 'rules', label: '积分规则' }, { key: 'levels', label: '等级配置' }, { key: 'adjustments', label: '积分调整与流水' }]
const activeTab = computed(() => {
  const tab = route.meta.pointsTab || route.path.split('/').pop()
  return tabs.some((item) => item.key === tab) ? tab : 'rules'
})
const ruleTable = useTable(adminPointsApi.rules)
const adjustmentTable = useTable(adminPointsApi.adjustments)
const levels = ref([])
const levelsLoading = ref(false)
const ruleModal = ref(false)
const levelModal = ref(false)
const adjustmentModal = ref(false)
const saving = ref(false)
const editingId = ref('')
const ruleFormRef = ref()
const levelFormRef = ref()
const adjustmentFormRef = ref()
const ruleForm = reactive({ code: '', name: '', points: 0, experience: 0, dailyLimit: 0, status: 1, remark: '' })
const levelForm = reactive({ level: 1, name: '', minExperience: 0, icon: '', benefits: '', status: 1, sortOrder: 0 })
const adjustmentForm = reactive({ userId: '', points: 0, experience: 0, reason: '', idempotencyKey: '' })
const ruleRules = {
  code: [{ required: true, whitespace: true, message: '请输入规则编码' }, { max: 40, message: '编码不能超过40个字符' }],
  name: [{ required: true, whitespace: true, message: '请输入规则名称' }, { max: 80, message: '名称不能超过80个字符' }],
  points: [{ required: true, type: 'number', min: 0, message: '奖励积分不能小于0' }],
  experience: [{ required: true, type: 'number', min: 0, message: '奖励经验不能小于0' }],
  dailyLimit: [{ required: true, type: 'number', min: 0, message: '每日上限不能小于0' }],
  remark: [{ max: 500, message: '说明不能超过500个字符' }],
}
const levelRules = {
  level: [{ required: true, type: 'number', min: 1, message: '等级必须大于0' }],
  name: [{ required: true, whitespace: true, message: '请输入等级名称' }, { max: 50, message: '名称不能超过50个字符' }],
  minExperience: [{ required: true, type: 'number', min: 0, message: '最低经验不能小于0' }],
  icon: [{ max: 500, message: '图标地址不能超过500个字符' }],
  benefits: [{ max: 1000, message: '权益说明不能超过1000个字符' }],
}
const adjustmentRules = {
  userId: [{ required: true, message: '请输入用户ID' }, { pattern: /^\d+$/, message: '用户ID必须为正整数' }],
  points: [{ required: true, type: 'number', message: '请输入积分调整值' }],
  experience: [{ required: true, type: 'number', message: '请输入经验调整值' }],
  reason: [{ required: true, whitespace: true, message: '请输入调整原因' }, { max: 500, message: '调整原因不能超过500个字符' }],
  idempotencyKey: [{ required: true, whitespace: true, message: '请生成幂等键' }, { max: 120, message: '幂等键不能超过120个字符' }],
}
const ruleColumns = [
  { title: '规则编码', dataIndex: 'code', width: 170 }, { title: '规则名称', dataIndex: 'name', width: 180 },
  { title: '奖励积分', dataIndex: 'points', width: 110 }, { title: '奖励经验', dataIndex: 'experience', width: 110 },
  { title: '每日上限', dataIndex: 'dailyLimit', width: 110 }, { title: '状态', key: 'status', width: 90 },
  { title: '说明', dataIndex: 'remark', ellipsis: true }, { title: '操作', key: 'actions', width: 90, fixed: 'right' },
]
const levelColumns = [
  { title: '等级', dataIndex: 'level', width: 90 }, { title: '名称', dataIndex: 'name', width: 160 },
  { title: '最低经验', dataIndex: 'minExperience', width: 130 }, { title: '权益说明', dataIndex: 'benefits', ellipsis: true },
  { title: '排序', dataIndex: 'sortOrder', width: 90 }, { title: '状态', key: 'status', width: 90 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 }, { title: '操作', key: 'actions', width: 90, fixed: 'right' },
]
const adjustmentColumns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 180 }, { title: '说明', dataIndex: 'remark', ellipsis: true },
  { title: '积分变动', key: 'points', width: 110 }, { title: '变动后余额', dataIndex: 'balanceAfter', width: 120 },
  { title: '经验变动', key: 'experience', width: 110 }, { title: '变动后经验', dataIndex: 'experienceAfter', width: 130 },
  { title: '业务类型', dataIndex: 'bizType', width: 150 }, { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
]

function newIdempotencyKey(prefix) {
  const id = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`
  return `${prefix}:${id}`
}

async function loadLevels() {
  levelsLoading.value = true
  try {
    levels.value = await adminPointsApi.levels() || []
  } catch (error) {
    message.error(errorText(error))
  } finally {
    levelsLoading.value = false
  }
}

function loadActive() {
  if (activeTab.value === 'rules') ruleTable.load()
  else if (activeTab.value === 'levels') loadLevels()
  else adjustmentTable.load()
}

function changeTab(key) {
  router.push(`/points/${key}`)
}

function openRule(record) {
  editingId.value = record ? idOf(record.id) : ''
  Object.assign(ruleForm, record ? {
    code: record.code || '', name: record.name || '', points: Number(record.points || 0), experience: Number(record.experience || 0),
    dailyLimit: Number(record.dailyLimit || 0), status: isEnabled(record.status) ? 1 : 0, remark: record.remark || '',
  } : { code: '', name: '', points: 0, experience: 0, dailyLimit: 0, status: 1, remark: '' })
  ruleModal.value = true
}

async function saveRule() {
  await ruleFormRef.value.validate()
  saving.value = true
  try {
    const data = { ...ruleForm, code: ruleForm.code.trim(), name: ruleForm.name.trim(), remark: ruleForm.remark.trim() }
    if (editingId.value) await adminPointsApi.updateRule(editingId.value, data)
    else await adminPointsApi.createRule(data)
    message.success(editingId.value ? '积分规则已更新' : '积分规则已创建')
    ruleModal.value = false
    ruleTable.load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}

function openLevel(record) {
  editingId.value = record ? idOf(record.id) : ''
  Object.assign(levelForm, record ? {
    level: Number(record.level), name: record.name || '', minExperience: Number(record.minExperience || 0), icon: record.icon || '',
    benefits: record.benefits || '', status: isEnabled(record.status) ? 1 : 0, sortOrder: Number(record.sortOrder || 0),
  } : { level: levels.value.length + 1, name: '', minExperience: 0, icon: '', benefits: '', status: 1, sortOrder: levels.value.length + 1 })
  levelModal.value = true
}

async function saveLevel() {
  await levelFormRef.value.validate()
  saving.value = true
  try {
    const data = { ...levelForm, name: levelForm.name.trim(), icon: levelForm.icon.trim() || null, benefits: levelForm.benefits.trim() }
    if (editingId.value) await adminPointsApi.updateLevel(editingId.value, data)
    else await adminPointsApi.createLevel(data)
    message.success(editingId.value ? '等级配置已更新' : '等级配置已创建')
    levelModal.value = false
    loadLevels()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}

function openAdjustment() {
  Object.assign(adjustmentForm, { userId: '', points: 0, experience: 0, reason: '', idempotencyKey: newIdempotencyKey('ADMIN_POINTS') })
  adjustmentModal.value = true
}

async function saveAdjustment() {
  await adjustmentFormRef.value.validate()
  saving.value = true
  try {
    await adminPointsApi.adjust({ ...adjustmentForm, userId: adjustmentForm.userId.trim(), reason: adjustmentForm.reason.trim(), idempotencyKey: adjustmentForm.idempotencyKey.trim() })
    message.success('用户积分与经验已调整')
    adjustmentModal.value = false
    adjustmentTable.load(1)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}

onMounted(loadActive)
watch(activeTab, loadActive)
</script>

<template>
  <div class="management-page">
    <a-card :bordered="false" class="panel-card points-tabs-card"><a-tabs :active-key="activeTab" @change="changeTab"><a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label" /></a-tabs></a-card>
    <template v-if="activeTab === 'rules'">
      <div class="toolbar"><span class="section-description">配置审核与社区业务的积分、经验奖励及每日上限。</span><a-space><a-button @click="ruleTable.load()"><ReloadOutlined /></a-button><a-button v-permission="'points:rule:create'" type="primary" @click="openRule()"><PlusOutlined />新增规则</a-button></a-space></div>
      <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="ruleColumns" :data-source="ruleTable.records.value" :loading="ruleTable.loading.value" :pagination="ruleTable.pagination" :scroll="{ x: 1150 }" @change="ruleTable.changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'status'"><a-tag :color="isEnabled(record.status) ? 'success' : 'default'">{{ isEnabled(record.status) ? '启用' : '停用' }}</a-tag></template><template v-else-if="column.key === 'actions'"><a-button v-permission="'points:rule:update'" type="link" size="small" @click="openRule(record)">编辑</a-button></template><template v-else-if="column.key === 'dailyLimit'">{{ record.dailyLimit === 0 ? '不限' : record.dailyLimit }}</template></template></a-table></a-card>
    </template>
    <template v-else-if="activeTab === 'levels'">
      <div class="toolbar"><span class="section-description">等级按最低经验匹配，停用等级不会参与前台等级计算。</span><a-space><a-button @click="loadLevels"><ReloadOutlined /></a-button><a-button v-permission="'points:level:create'" type="primary" @click="openLevel()"><PlusOutlined />新增等级</a-button></a-space></div>
      <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="levelColumns" :data-source="levels" :loading="levelsLoading" :pagination="false" :scroll="{ x: 1100 }"><template #bodyCell="{ column, record }"><template v-if="column.key === 'status'"><a-tag :color="isEnabled(record.status) ? 'success' : 'default'">{{ isEnabled(record.status) ? '启用' : '停用' }}</a-tag></template><template v-else-if="column.key === 'updateTime'">{{ formatDate(record.updateTime || record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-button v-permission="'points:level:update'" type="link" size="small" @click="openLevel(record)">编辑</a-button></template></template></a-table></a-card>
    </template>
    <template v-else>
      <div class="toolbar"><span class="section-description">查询全部积分流水，或通过幂等调整修正用户积分与经验。</span><a-space><a-button @click="adjustmentTable.load()"><ReloadOutlined /></a-button><a-button v-permission="'points:adjustment:create'" type="primary" @click="openAdjustment"><PlusOutlined />调整积分</a-button></a-space></div>
      <a-card :bordered="false" class="table-card"><a-table row-key="id" :columns="adjustmentColumns" :data-source="adjustmentTable.records.value" :loading="adjustmentTable.loading.value" :pagination="adjustmentTable.pagination" :scroll="{ x: 1250 }" @change="adjustmentTable.changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'userId'"><span class="table-code">{{ record.userId }}</span></template><template v-else-if="column.key === 'points'"><span :class="Number(record.changeAmount) >= 0 ? 'amount-positive' : 'amount-negative'">{{ Number(record.changeAmount) > 0 ? '+' : '' }}{{ record.changeAmount }}</span></template><template v-else-if="column.key === 'experience'"><span :class="Number(record.experienceChange) >= 0 ? 'amount-positive' : 'amount-negative'">{{ Number(record.experienceChange) > 0 ? '+' : '' }}{{ record.experienceChange }}</span></template><template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime) }}</template></template></a-table></a-card>
    </template>

    <a-modal v-model:open="ruleModal" :title="editingId ? '编辑积分规则' : '新增积分规则'" :confirm-loading="saving" destroy-on-close @ok="saveRule"><a-form ref="ruleFormRef" :model="ruleForm" :rules="ruleRules" layout="vertical"><div class="modal-form-grid"><a-form-item label="规则编码" name="code"><a-input v-model:value="ruleForm.code" :maxlength="40" :disabled="Boolean(editingId)" placeholder="如 SIGN_IN" /></a-form-item><a-form-item label="规则名称" name="name"><a-input v-model:value="ruleForm.name" :maxlength="80" /></a-form-item><a-form-item label="奖励积分" name="points"><a-input-number v-model:value="ruleForm.points" :min="0" style="width: 100%" /></a-form-item><a-form-item label="奖励经验" name="experience"><a-input-number v-model:value="ruleForm.experience" :min="0" style="width: 100%" /></a-form-item><a-form-item label="每日获取上限" name="dailyLimit" extra="0 表示不限"><a-input-number v-model:value="ruleForm.dailyLimit" :min="0" style="width: 100%" /></a-form-item><a-form-item label="状态" name="status"><a-radio-group v-model:value="ruleForm.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]" /></a-form-item></div><a-form-item label="规则说明" name="remark"><a-textarea v-model:value="ruleForm.remark" :rows="3" :maxlength="500" show-count /></a-form-item></a-form></a-modal>
    <a-modal v-model:open="levelModal" :title="editingId ? '编辑等级' : '新增等级'" :confirm-loading="saving" destroy-on-close @ok="saveLevel"><a-form ref="levelFormRef" :model="levelForm" :rules="levelRules" layout="vertical"><div class="modal-form-grid"><a-form-item label="等级" name="level"><a-input-number v-model:value="levelForm.level" :min="1" :precision="0" style="width: 100%" /></a-form-item><a-form-item label="等级名称" name="name"><a-input v-model:value="levelForm.name" :maxlength="50" /></a-form-item><a-form-item label="最低经验" name="minExperience"><a-input-number v-model:value="levelForm.minExperience" :min="0" :precision="0" style="width: 100%" /></a-form-item><a-form-item label="排序" name="sortOrder"><a-input-number v-model:value="levelForm.sortOrder" :precision="0" style="width: 100%" /></a-form-item></div><a-form-item label="图标地址" name="icon"><a-input v-model:value="levelForm.icon" :maxlength="500" placeholder="可选" /></a-form-item><a-form-item label="权益说明" name="benefits"><a-textarea v-model:value="levelForm.benefits" :rows="3" :maxlength="1000" show-count /></a-form-item><a-form-item label="状态" name="status"><a-radio-group v-model:value="levelForm.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]" /></a-form-item></a-form></a-modal>
    <a-modal v-model:open="adjustmentModal" title="调整用户积分与经验" :confirm-loading="saving" destroy-on-close @ok="saveAdjustment"><a-alert message="调整会直接写入用户账户和不可变流水，请确认用户ID及调整值。" type="warning" show-icon class="page-alert" /><a-form ref="adjustmentFormRef" :model="adjustmentForm" :rules="adjustmentRules" layout="vertical"><a-form-item label="用户ID" name="userId"><a-input v-model:value="adjustmentForm.userId" placeholder="请输入完整用户ID" /></a-form-item><div class="modal-form-grid"><a-form-item label="积分调整值" name="points" extra="扣减请输入负数"><a-input-number v-model:value="adjustmentForm.points" :precision="0" style="width: 100%" /></a-form-item><a-form-item label="经验调整值" name="experience" extra="扣减请输入负数"><a-input-number v-model:value="adjustmentForm.experience" :precision="0" style="width: 100%" /></a-form-item></div><a-form-item label="调整原因" name="reason"><a-textarea v-model:value="adjustmentForm.reason" :rows="3" :maxlength="500" show-count /></a-form-item><a-form-item label="幂等键" name="idempotencyKey"><a-input v-model:value="adjustmentForm.idempotencyKey" :maxlength="120"><template #addonAfter><a-button type="link" size="small" @click="adjustmentForm.idempotencyKey = newIdempotencyKey('ADMIN_POINTS')">重新生成</a-button></template></a-input></a-form-item></a-form></a-modal>
  </div>
</template>

<style scoped>
.points-tabs-card { margin-bottom: 18px; }
.points-tabs-card :deep(.ant-card-body) { padding: 0 24px; }
.points-tabs-card :deep(.ant-tabs-nav) { margin-bottom: 0; }
.section-description { color: var(--muted); font-size: 13px; }
.amount-positive { color: #16865c; font-weight: 600; }
.amount-negative { color: #c63b4d; font-weight: 600; }
</style>
