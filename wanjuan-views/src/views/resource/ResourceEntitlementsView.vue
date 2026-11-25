<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { userResourceApi } from '../../api/resource'
import { entitlementSource, resourceStatus } from '../../utils/resource'
import { errorText, formatDate } from '../../utils/format'
import './resource.css'

const loading = ref(false)
const records = ref([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 条授权记录` })
const columns = [
  { title: '资源', key: 'resource', width: 150 }, { title: '权益来源', key: 'source', width: 140 },
  { title: '来源订单号', dataIndex: 'sourceOrderNo', key: 'sourceOrderNo', width: 200, ellipsis: true },
  { title: '支付 / 积分', key: 'amount', width: 140 }, { title: '状态', key: 'status', width: 110 },
  { title: '授权时间', key: 'grantedAt', width: 170 }, { title: '撤销时间', key: 'revokedAt', width: 170 },
]

async function load(page = pagination.current) {
  loading.value = true
  try {
    const data = await userResourceApi.entitlements({ page, size: pagination.pageSize })
    records.value = Array.isArray(data?.records) ? data.records : []
    pagination.current = page
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}
function changeTable(page) { pagination.pageSize = page.pageSize; load(page.current) }
onMounted(() => load())
</script>

<template>
  <div class="content-page my-resources-page">
    <div class="page-heading"><div><span class="eyebrow">MY ENTITLEMENTS</span><h1>我的已购与授权</h1><p>查看积分兑换、购买及其他来源的真实资源访问权益。</p></div><a-space><a-button @click="$router.push('/resources')">浏览资源</a-button><a-button type="primary" @click="$router.push('/my/resources')">我的发布</a-button></a-space></div>
    <a-card :bordered="false" class="content-card resource-table-card">
      <a-table row-key="resourceId" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 1100 }" @change="changeTable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'resource'"><router-link :to="`/resources/${record.resourceId}`">资源 #{{ record.resourceId }}</router-link></template>
          <template v-else-if="column.key === 'source'">{{ entitlementSource(record.sourceType) }}</template>
          <template v-else-if="column.key === 'amount'"><span v-if="record.amount != null">¥{{ record.amount }}</span><span v-else-if="record.pointsAmount != null">{{ record.pointsAmount }} 积分</span><span v-else>-</span></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="resourceStatus(record.status).color">{{ resourceStatus(record.status).text }}</a-tag></template>
          <template v-else-if="column.key === 'grantedAt'">{{ formatDate(record.grantedAt, true) }}</template>
          <template v-else-if="column.key === 'revokedAt'">{{ formatDate(record.revokedAt, true) }}</template>
          <template v-else>{{ record[column.dataIndex] || '-' }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
