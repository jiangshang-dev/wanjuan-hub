<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { resourceApi } from '../../api/resource'
import { accessTypeOptions, accessTypeText, resourcePrice } from '../../utils/resource'
import { errorText, formatDate } from '../../utils/format'
import './resource.css'

const loading = ref(false)
const records = ref([])
const categories = ref([])
const filters = reactive({ keyword: '', categoryId: undefined, accessType: undefined })
const pagination = reactive({ current: 1, pageSize: 9, total: 0, showSizeChanger: false, showTotal: (total) => `共 ${total} 个公开资源` })

function categoryName(id) {
  return categories.value.find((item) => String(item.id) === String(id))?.name || '资源'
}

async function load() {
  loading.value = true
  try {
    const data = await resourceApi.list({ page: pagination.current, size: pagination.pageSize, keyword: filters.keyword.trim() || undefined, categoryId: filters.categoryId, accessType: filters.accessType })
    records.value = Array.isArray(data?.records) ? data.records : []
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}
function search() { pagination.current = 1; load() }
function selectCategory(value) { filters.categoryId = value; search() }
function changePage(page) { pagination.current = page; load() }

onMounted(async () => {
  load()
  try {
    const data = await resourceApi.categories()
    categories.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error(errorText(error))
  }
})
</script>

<template>
  <div class="content-page resource-list-page">
    <div class="page-heading"><div><span class="eyebrow">RESOURCE HUB</span><h1>资源中心</h1><p>发现社区成员真实发布的学习资料与多网盘资源。</p></div><a-space wrap><a-button @click="$router.push('/my/resources/entitlements')">我的授权</a-button><a-button type="primary" @click="$router.push('/my/resources/new')">发布资源</a-button></a-space></div>
    <div class="resource-filter-bar">
      <div class="resource-filter-line"><a-input-search v-model:value="filters.keyword" allow-clear placeholder="搜索资源标题、摘要或描述" @search="search" /><a-select v-model:value="filters.accessType" allow-clear placeholder="全部访问类型" :options="accessTypeOptions" @change="search" /><a-button type="primary" @click="search">查询</a-button></div>
      <div class="category-filter"><button type="button" :class="{ active: filters.categoryId === undefined }" @click="selectCategory(undefined)">全部</button><button v-for="category in categories" :key="category.id" type="button" :class="{ active: String(filters.categoryId) === String(category.id) }" @click="selectCategory(category.id)">{{ category.name }}</button></div>
    </div>
    <a-spin :spinning="loading">
      <div v-if="records.length" class="resource-card-grid">
        <article v-for="resource in records" :key="resource.id" class="resource-card">
          <div class="resource-card-meta"><span>{{ categoryName(resource.categoryId) }}</span><a-tag>{{ accessTypeText(resource.accessType) }}</a-tag></div>
          <h2><router-link :to="`/resources/${resource.id}`">{{ resource.title }}</router-link></h2>
          <p class="resource-card-summary">{{ resource.summary }}</p>
          <div v-if="resource.tags?.length" class="blog-tags"><a-tag v-for="tag in resource.tags.slice(0, 3)" :key="tag">{{ tag }}</a-tag></div>
          <div class="resource-card-footer"><span>{{ formatDate(resource.createTime) }} · {{ resource.viewCount || 0 }} 次浏览</span><strong>{{ resourcePrice(resource) }}</strong></div>
        </article>
      </div>
      <a-empty v-else-if="!loading" class="resource-empty" description="暂无符合条件的公开资源"><a-button type="primary" @click="$router.push('/my/resources/new')">发布第一个资源</a-button></a-empty>
    </a-spin>
    <a-pagination v-if="pagination.total > pagination.pageSize" v-model:current="pagination.current" class="list-pagination" :total="pagination.total" :page-size="pagination.pageSize" :show-total="pagination.showTotal" @change="changePage" />
  </div>
</template>
