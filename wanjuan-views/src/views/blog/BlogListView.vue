<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { blogApi } from '../../api'
import { blogAuthor, blogCover, blogTags, collectionOf, tagName } from '../../utils/blog'
import { errorText, formatDate } from '../../utils/format'

const loading = ref(false)
const categories = ref([])
const records = ref([])
const filters = reactive({ keyword: '', categoryId: undefined })
const pagination = reactive({ current: 1, pageSize: 9, total: 0, showSizeChanger: false, showTotal: (total) => `共 ${total} 篇公开博客` })

async function load() {
  loading.value = true
  try {
    const data = await blogApi.list({ page: pagination.current, size: pagination.pageSize, keyword: filters.keyword.trim() || undefined, categoryId: filters.categoryId })
    records.value = collectionOf(data)
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.current = 1
  load()
}

function selectCategory(value) {
  filters.categoryId = value
  search()
}

function changePage(page) {
  pagination.current = page
  load()
}

onMounted(async () => {
  load()
  try {
    categories.value = collectionOf(await blogApi.categories())
  } catch (error) {
    message.error(errorText(error))
  }
})
</script>

<template>
  <div class="content-page blog-list-page">
    <div class="page-heading blog-heading"><div><span class="eyebrow">TECHNICAL BLOGS</span><h1>技术博客</h1><p>阅读社区成员真实发布的学习与实践记录。</p></div><a-button type="primary" @click="$router.push('/my/blogs/new')">创作博客</a-button></div>
    <div class="blog-filter-bar">
      <a-input-search v-model:value="filters.keyword" allow-clear placeholder="搜索博客标题" class="blog-search" @search="search" @press-enter="search" />
      <div class="category-filter"><button type="button" :class="{ active: filters.categoryId === undefined }" @click="selectCategory(undefined)">全部</button><button v-for="category in categories" :key="category.id" type="button" :class="{ active: String(filters.categoryId) === String(category.id) }" @click="selectCategory(category.id)">{{ category.name }}</button></div>
    </div>
    <a-spin :spinning="loading">
      <div v-if="records.length" class="blog-card-grid">
        <article v-for="blog in records" :key="blog.id" class="blog-card">
          <router-link v-if="blogCover(blog)" class="blog-card-cover" :to="`/blogs/${blog.id}`"><img :src="blogCover(blog)" :alt="blog.title" loading="lazy" /></router-link>
          <div class="blog-card-body">
            <div class="blog-card-meta"><span>{{ blog.categoryName || blog.category?.name || '未分类' }}</span><time>{{ formatDate(blog.publishedAt || blog.publishTime || blog.createdAt || blog.createTime) }}</time></div>
            <h2><router-link :to="`/blogs/${blog.id}`">{{ blog.title }}</router-link></h2>
            <p>{{ blog.summary || '这篇博客暂未填写摘要。' }}</p>
            <div class="blog-card-footer"><span>{{ blogAuthor(blog) }}</span><div v-if="blogTags(blog).length" class="blog-tags"><a-tag v-for="tag in blogTags(blog).slice(0, 3)" :key="tag.id || tag">{{ tagName(tag) }}</a-tag></div></div>
          </div>
        </article>
      </div>
      <a-empty v-else-if="!loading" class="blog-empty" description="暂无符合条件的公开博客"><a-button type="primary" @click="$router.push('/my/blogs/new')">写一篇博客</a-button></a-empty>
    </a-spin>
    <a-pagination v-if="pagination.total > pagination.pageSize" v-model:current="pagination.current" class="list-pagination" :total="pagination.total" :page-size="pagination.pageSize" :show-total="pagination.showTotal" @change="changePage" />
  </div>
</template>
