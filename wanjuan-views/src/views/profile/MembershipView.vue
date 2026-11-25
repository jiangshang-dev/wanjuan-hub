<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { pointsApi } from '../../api/s6'
import { errorText, formatDate } from '../../utils/format'

const loading = ref(true)
const signing = ref(false)
const recordsLoading = ref(false)
const account = ref(null)
const membership = ref(null)
const levels = ref([])
const records = ref([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` })
const columns = [
  { title: '流水说明', key: 'remark', ellipsis: true },
  { title: '积分变动', key: 'points', width: 130, align: 'right' },
  { title: '变动后余额', dataIndex: 'balanceAfter', key: 'balanceAfter', width: 130, align: 'right' },
  { title: '经验变动', key: 'experience', width: 120, align: 'right' },
  { title: '业务类型', dataIndex: 'bizType', key: 'bizType', width: 150 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
]
const currentLevel = computed(() => membership.value?.currentLevel)
const nextLevel = computed(() => membership.value?.nextLevel)
const levelProgress = computed(() => {
  if (!membership.value) return 0
  if (!nextLevel.value) return 100
  const start = Number(currentLevel.value?.minExperience || 0)
  const end = Number(nextLevel.value.minExperience || 0)
  const current = Number(membership.value.experience || 0)
  if (end <= start) return 100
  return Math.max(0, Math.min(100, Math.round(((current - start) / (end - start)) * 100)))
})

async function loadOverview() {
  loading.value = true
  const results = await Promise.allSettled([pointsApi.account(), pointsApi.membership(), pointsApi.levels()])
  if (results[0].status === 'fulfilled') account.value = results[0].value
  if (results[1].status === 'fulfilled') membership.value = results[1].value
  if (results[2].status === 'fulfilled') levels.value = results[2].value || []
  const failed = results.find((result) => result.status === 'rejected')
  if (failed) message.error(errorText(failed.reason))
  loading.value = false
}

async function loadRecords(page = pagination.current, size = pagination.pageSize) {
  recordsLoading.value = true
  try {
    const data = await pointsApi.records({ page, size })
    records.value = data?.records || []
    pagination.current = Number(data?.page || page)
    pagination.pageSize = Number(data?.size || size)
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    recordsLoading.value = false
  }
}

async function signIn() {
  signing.value = true
  try {
    account.value = await pointsApi.signIn()
    message.success('签到成功，积分与经验已更新')
    await Promise.all([loadRecords(1), pointsApi.membership().then((data) => { membership.value = data })])
  } catch (error) {
    message.error(errorText(error))
    loadOverview()
  } finally {
    signing.value = false
  }
}

function changeRecords(page) {
  loadRecords(page.current, page.pageSize)
}

onMounted(() => {
  loadOverview()
  loadRecords(1)
})
</script>

<template>
  <div class="content-page membership-page">
    <div class="page-heading">
      <div><span class="eyebrow">MEMBERSHIP</span><h1>会员中心</h1><p>查看真实成长等级、积分账户与每日签到状态。</p></div>
      <a-button type="primary" size="large" :loading="signing" :disabled="account?.signedToday" @click="signIn">{{ account?.signedToday ? '今日已签到' : '每日签到' }}</a-button>
    </div>
    <a-spin :spinning="loading">
      <section class="member-hero">
        <div class="member-level-mark">LV.{{ currentLevel?.level || '-' }}</div>
        <div class="member-level-info">
          <span>当前等级</span>
          <h2>{{ currentLevel?.name || '暂无等级信息' }}</h2>
          <p>{{ currentLevel?.benefits || '完成社区互动，持续积累成长经验。' }}</p>
          <a-progress :percent="levelProgress" :show-info="false" stroke-color="#8eb0ff" trail-color="rgba(255,255,255,.16)" />
          <div class="level-progress-copy">
            <span>当前经验 {{ membership?.experience ?? account?.experience ?? 0 }}</span>
            <span v-if="nextLevel">距 {{ nextLevel.name }} 还需 {{ membership?.experienceToNext ?? 0 }} 经验</span>
            <span v-else>已达到当前最高等级</span>
          </div>
        </div>
      </section>
      <div class="points-stat-grid">
        <a-card :bordered="false"><a-statistic title="可用积分" :value="account?.balance ?? 0" /></a-card>
        <a-card :bordered="false"><a-statistic title="累计积分" :value="account?.totalPoints ?? 0" /></a-card>
        <a-card :bordered="false"><a-statistic title="当前经验" :value="account?.experience ?? membership?.experience ?? 0" /></a-card>
      </div>
      <a-card :bordered="false" class="content-card level-card" title="成长等级">
        <div v-if="levels.length" class="level-list">
          <div v-for="item in levels" :key="item.level" class="level-item" :class="{ current: item.level === currentLevel?.level }">
            <img v-if="item.icon" :src="item.icon" :alt="item.name" />
            <span v-else class="level-item-badge">LV.{{ item.level }}</span>
            <div><strong>{{ item.name }}</strong><p>{{ item.benefits || '暂无权益说明' }}</p><small>{{ item.minExperience }} 经验解锁</small></div>
            <a-tag v-if="item.level === currentLevel?.level" color="blue">当前等级</a-tag>
          </div>
        </div>
        <a-empty v-else description="暂无启用的等级配置" />
      </a-card>
    </a-spin>
    <a-card :bordered="false" class="content-card points-records-card" title="积分与经验流水">
      <a-table :columns="columns" :data-source="records" :loading="recordsLoading" :pagination="pagination" row-key="id" :scroll="{ x: 900 }" @change="changeRecords">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'remark'"><strong>{{ record.remark || record.bizType || '积分变动' }}</strong><div v-if="record.bizId" class="record-biz-id">业务 ID：{{ record.bizId }}</div></template>
          <template v-else-if="column.key === 'points'"><span :class="Number(record.changeAmount) >= 0 ? 'amount-positive' : 'amount-negative'">{{ Number(record.changeAmount) > 0 ? '+' : '' }}{{ record.changeAmount }}</span></template>
          <template v-else-if="column.key === 'experience'"><span :class="Number(record.experienceChange) >= 0 ? 'amount-positive' : 'amount-negative'">{{ Number(record.experienceChange) > 0 ? '+' : '' }}{{ record.experienceChange }}</span></template>
          <template v-else-if="column.key === 'createTime'">{{ formatDate(record.createTime, true) }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.member-hero { display: flex; padding: 34px 38px; align-items: center; gap: 30px; border-radius: 16px; color: #fff; background: linear-gradient(125deg, #172a50, #3157a5); box-shadow: 0 16px 38px rgba(24, 47, 91, .14); }
.member-level-mark { display: flex; flex: 0 0 108px; height: 108px; align-items: center; justify-content: center; border: 1px solid rgba(255,255,255,.28); border-radius: 50%; color: #dce7ff; background: rgba(255,255,255,.07); font-size: 24px; font-weight: 700; }
.member-level-info { min-width: 0; flex: 1; }
.member-level-info > span { color: #b9c9e8; font-size: 12px; }
.member-level-info h2 { margin: 7px 0; font-size: 26px; }
.member-level-info p { margin: 0 0 22px; color: #c8d4eb; }
.level-progress-copy { display: flex; margin-top: 9px; justify-content: space-between; gap: 12px; color: #c8d4eb; font-size: 12px; }
.points-stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin: 20px 0; }
.points-stat-grid .ant-card { border: 1px solid var(--line); border-radius: 13px; }
.level-card { margin-bottom: 20px; }
.level-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.level-item { display: flex; padding: 18px; align-items: center; gap: 14px; border: 1px solid var(--line); border-radius: 11px; }
.level-item.current { border-color: #9db3ec; background: #f5f8ff; }
.level-item img, .level-item-badge { display: flex; flex: 0 0 48px; width: 48px; height: 48px; align-items: center; justify-content: center; border-radius: 10px; object-fit: cover; }
.level-item-badge { color: var(--primary); background: #eaf0ff; font-size: 12px; font-weight: 700; }
.level-item > div { min-width: 0; flex: 1; }
.level-item p { margin: 5px 0; color: var(--muted); font-size: 12px; }
.level-item small { color: #8a94a6; }
.points-records-card :deep(.ant-card-body) { padding: 0; }
.points-records-card :deep(.ant-card-head) { padding: 0 24px; }
.record-biz-id { margin-top: 5px; color: #8a94a6; font-size: 11px; }
.amount-positive { color: #16865c; font-weight: 600; }
.amount-negative { color: #c63b4d; font-weight: 600; }
@media (max-width: 700px) { .member-hero { padding: 26px 22px; align-items: flex-start; flex-direction: column; } .member-level-mark { flex-basis: 76px; width: 76px; height: 76px; font-size: 18px; } .points-stat-grid, .level-list { grid-template-columns: 1fr; } .level-progress-copy { flex-direction: column; } }
</style>
