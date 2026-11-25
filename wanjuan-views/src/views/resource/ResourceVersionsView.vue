<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { userResourceApi } from '../../api/resource'
import { accessTypeText, resourceStatus } from '../../utils/resource'
import { errorText, formatDate } from '../../utils/format'
import './resource.css'

const route = useRoute()
const loading = ref(true)
const resource = ref(null)
const records = ref([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 个版本` })
const columns = [
  { title: '版本号', key: 'versionNo', width: 100 }, { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '访问类型', key: 'accessType', width: 120 }, { title: '状态', key: 'status', width: 150 },
  { title: '审核说明', key: 'reason', width: 240, ellipsis: true }, { title: '保存时间', key: 'createdAt', width: 170 },
]

async function load(page = pagination.current) {
  loading.value = true
  try {
    const [detail, data] = await Promise.all([
      resource.value ? Promise.resolve(resource.value) : userResourceApi.detail(route.params.id),
      userResourceApi.versions(route.params.id, { page, size: pagination.pageSize }),
    ])
    resource.value = detail
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
  <div class="content-page narrow-page">
    <div class="page-heading"><div><span class="eyebrow">RESOURCE VERSIONS</span><h1>资源版本</h1><p>《{{ resource?.title || '资源' }}》的草稿与审核历史，不包含任何网盘敏感信息。</p></div><a-space><a-button @click="$router.push('/my/resources')">返回列表</a-button><a-button v-if="!['OFFLINE', 'INVALID'].includes(resource?.status)" type="primary" @click="$router.push(`/my/resources/${route.params.id}/edit`)">编辑最新版本</a-button></a-space></div>
    <a-card :bordered="false" class="content-card resource-table-card"><a-table row-key="versionNo" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" :scroll="{ x: 980 }" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'versionNo'"><a-tag>v{{ record.versionNo }}</a-tag></template><template v-else-if="column.key === 'accessType'">{{ accessTypeText(record.accessType) }}</template><template v-else-if="column.key === 'status'"><a-tag :color="resourceStatus(record.status).color">{{ resourceStatus(record.status).text }}</a-tag></template><template v-else-if="column.key === 'reason'"><span :class="{ 'reject-reason': record.status === 'REJECTED' }">{{ record.auditReason || '-' }}</span></template><template v-else-if="column.key === 'createdAt'">{{ formatDate(record.createTime, true) }}</template></template></a-table></a-card>
  </div>
</template>
