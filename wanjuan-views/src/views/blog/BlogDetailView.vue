<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { blogApi } from '../../api'
import { blogAuthor, blogCover, blogTags, tagName } from '../../utils/blog'
import { errorText, formatDate, renderMarkdown } from '../../utils/format'
import CommentSection from '../../components/CommentSection.vue'
import InteractionButtons from '../../components/InteractionButtons.vue'
import ReportAction from '../../components/ReportAction.vue'

const route = useRoute()
const loading = ref(true)
const blog = ref(null)
const loadError = ref(null)
const contentHtml = computed(() => renderMarkdown(blog.value?.content))
const safeSourceUrl = computed(() => {
  const value = blog.value?.sourceUrl || ''
  return /^https?:\/\//i.test(value) ? value : ''
})

async function load() {
  loading.value = true
  loadError.value = null
  blog.value = null
  try {
    blog.value = await blogApi.detail(route.params.id)
    document.title = `${blog.value?.title || '博客详情'} - 万卷技术社区`
  } catch (error) {
    loadError.value = error
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, (value, oldValue) => { if (value && value !== oldValue) load() })
</script>

<template>
  <div class="content-page blog-detail-page">
    <a-spin :spinning="loading">
      <template v-if="blog">
        <div class="article-back"><router-link to="/blogs">← 返回博客列表</router-link></div>
        <article class="public-article">
          <header class="article-header">
            <div class="article-category">{{ blog.categoryName || blog.category?.name || '技术博客' }}</div>
            <h1>{{ blog.title }}</h1>
            <p v-if="blog.summary" class="article-summary">{{ blog.summary }}</p>
            <div class="article-author"><a-avatar :src="blog.authorAvatar || blog.author?.avatar" :size="38">{{ blogAuthor(blog).slice(0, 1) }}</a-avatar><div><strong>{{ blogAuthor(blog) }}</strong><span>发布于 {{ formatDate(blog.publishedAt || blog.publishTime || blog.createdAt || blog.createTime, true) }}</span></div></div>
          </header>
          <img v-if="blogCover(blog)" class="article-cover" :src="blogCover(blog)" :alt="blog.title" />
          <div class="markdown-body article-content" v-html="contentHtml" />
          <footer class="article-footer">
            <div v-if="blogTags(blog).length" class="article-tags"><span>标签：</span><a-tag v-for="tag in blogTags(blog)" :key="tag.id || tag">{{ tagName(tag) }}</a-tag></div>
            <div v-if="blog.type === 'REPRINT' || blog.originalType === 'REPRINT'" class="article-source">本文转载自 {{ blog.sourceName || '外部来源' }}<a v-if="safeSourceUrl" :href="safeSourceUrl" target="_blank" rel="noopener noreferrer">查看原文 →</a></div>
            <div class="topic-actions blog-interactions"><InteractionButtons target-type="BLOG" :target-id="blog.id" :like-count="blog.likeCount" :favorite-count="blog.favoriteCount" check-state /><ReportAction target-type="BLOG" :target-id="blog.id" :compact="false" /></div>
          </footer>
        </article>
        <CommentSection target-type="BLOG" :target-id="blog.id" />
      </template>
      <a-result v-else-if="loadError" :status="loadError.response?.status === 403 ? '403' : '404'" title="内容暂不可访问" :sub-title="loadError.response?.status === 403 || loadError.response?.status === 404 ? '博客不存在、尚未公开或已下线。' : errorText(loadError)"><template #extra><a-button type="primary" @click="$router.replace('/blogs')">返回公开博客</a-button><a-button @click="load">重新加载</a-button></template></a-result>
    </a-spin>
  </div>
</template>
