<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { userBlogApi } from '../../api'
import { blogStatus, collectionOf } from '../../utils/blog'
import { errorText, formatDate } from '../../utils/format'

const route = useRoute()
const loading = ref(true)
const blog = ref(null)
const records = ref([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 个版本` })
const columns = [{ title: '版本号', dataIndex: 'versionNo', key: 'versionNo', width: 100 }, { title: '标题', dataIndex: 'title', key: 'title' }, { title: '状态', key: 'status', width: 130 }, { title: '审核说明', key: 'reason', width: 260 }, { title: '保存时间', key: 'createdAt', width: 180 }]

async function load(page = pagination.current) {
  loading.value = true
  try {
    const [detail, data] = await Promise.all([
      blog.value ? Promise.resolve(blog.value) : userBlogApi.detail(route.params.id),
      userBlogApi.versions(route.params.id, { page, size: pagination.pageSize }),
    ])
    blog.value = detail
    records.value = collectionOf(data)
    pagination.current = page
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}
function changeTable(page) {
  pagination.pageSize = page.pageSize
  load(page.current)
}
onMounted(() => load())
</script>

<template>
  <div class="content-page narrow-page">
    <div class="page-heading"><div><span class="eyebrow">VERSION HISTORY</span><h1>版本记录</h1><p>《{{ blog?.title || '博客' }}》的草稿与审核历史。</p></div><a-space><a-button @click="$router.push('/my/blogs')">返回列表</a-button><a-button type="primary" @click="$router.push(`/my/blogs/${route.params.id}/edit`)">编辑最新版本</a-button></a-space></div>
    <a-card :bordered="false" class="content-card my-blog-table-card"><a-table row-key="versionNo" :columns="columns" :data-source="records" :loading="loading" :pagination="pagination" @change="changeTable"><template #bodyCell="{ column, record }"><template v-if="column.key === 'versionNo'"><a-tag>v{{ record.versionNo }}</a-tag></template><template v-else-if="column.key === 'status'"><a-tag :color="blogStatus(record.status).color">{{ blogStatus(record.status).text }}</a-tag></template><template v-else-if="column.key === 'reason'"><span :class="{ 'reject-reason': record.status === 'REJECTED' }">{{ record.auditReason || record.rejectReason || record.reason || '-' }}</span></template><template v-else-if="column.key === 'createdAt'">{{ formatDate(record.createdAt || record.createTime, true) }}</template></template></a-table></a-card>
  </div>
</template>
